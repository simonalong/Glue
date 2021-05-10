package com.simonalong.glue.event;

import com.alibaba.fastjson.JSONObject;
import com.simonalong.glue.NettyCommand;
import com.simonalong.glue.NettyErrorResponse;
import com.simonalong.glue.annotation.CommandMapping;
import com.simonalong.glue.annotation.GlueController;
import com.simonalong.glue.ThreadPoolFactory;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static com.simonalong.glue.GlueConstant.DEFAULT_GROUP_STR;
import static com.simonalong.glue.GlueConstant.LOG_PRE;

/**
 * 命令解析器
 *
 * @author shizi
 * @since 2020/3/3 下午7:09
 */
@Slf4j
public class CommandProcessor {

    private final Map<CommandEvent, MethodRunnerWrapper> commandMap = new ConcurrentHashMap<>();
    @Getter
    private String group;
    /**
     * 当前类型
     */
    private Class<?> tClass;
    private Object instance;
    private ExecutorService executorService;

    private CommandProcessor() {}

    public static CommandProcessor parse(Object controllerInstance, ExecutorService executorService) {
        CommandProcessor processor = new CommandProcessor();
        processor.tClass = controllerInstance.getClass();
        processor.instance = controllerInstance;
        processor.executorService = executorService;
        if (null != processor.doParse()) {
            return processor;
        }
        return null;
    }

    void process(ChannelHandlerContext ctx, NettyCommand command) {
        if (!commandMap.containsKey(command.getEvent())) {
            return;
        }

        if (null != executorService) {
            executorService.submit(() -> run(ctx, command));
        } else {
            run(ctx, command);
        }
    }

    /**
     * 命令处理器运行
     * <p>
     * 只有返回值不为空而且配置了返回值命令才会向对方发起命令
     *
     * @param ctx     通道上下文
     * @param command 命令
     */
    private void run(ChannelHandlerContext ctx, NettyCommand command) {
        MethodRunnerWrapper runnerWrapper = commandMap.get(command.getEvent());
        try {
            Object rspValue = runnerWrapper.run(command.getData());
            if (null != rspValue) {
                String responseCmd = runnerWrapper.getResponseCmd();
                if (null != responseCmd && !"".equals(responseCmd)) {
                    NettyCommand rsp = new NettyCommand();
                    rsp.setEvent(new CommandEvent(runnerWrapper.getGroup(), responseCmd));
                    rsp.setData(rspValue);
                    ctx.channel().writeAndFlush(rsp);
                }
            }
        } catch (Throwable e) {
            NettyErrorResponse errorResponse = new NettyErrorResponse();
            errorResponse.setCommand(command);
            errorResponse.setErrMsg(e.getMessage());

            NettyCommand rsp = new NettyCommand();
            rsp.setEvent(new CommandEvent(command.getEvent().getGroup(), runnerWrapper.getErrorCmd()));
            rsp.setData(errorResponse);
            ctx.channel().writeAndFlush(rsp);
        }
    }

    private void generateExecutorFromAnnotation(GlueController glueController) {
        if (null != this.executorService) {
            return;
        }
        String nullStr = "null";
        String executor = glueController.executor();
        if (nullStr.equals(executor)) {
            return;
        }

        int coreSize = glueController.coreSize();
        String commandEventProcessor = "Netty_server_event_processor";
        switch (executor) {
            case "single":
                this.executorService = ThreadPoolFactory.getSinglePool(commandEventProcessor);
                break;
            case "fixed":
                if (0 == coreSize) {
                    coreSize = Runtime.getRuntime().availableProcessors();
                }
                this.executorService = ThreadPoolFactory.getFixedPool(commandEventProcessor, coreSize);
                break;
            case "cache":
                this.executorService = ThreadPoolFactory.getCachePool(commandEventProcessor);
                break;
            default:
                throw new RuntimeException("not support executor: " + executor);
        }
    }

    private CommandProcessor doParse() {
        Assert.assertNotNull(tClass);
        if (tClass.isAnnotationPresent(GlueController.class)) {
            GlueController glueController = (GlueController) tClass.getDeclaredAnnotation(GlueController.class);
            this.group = getGroup(glueController);
            generateExecutorFromAnnotation(glueController);
            Stream.of(tClass.getDeclaredMethods()).forEach(method -> {
                if (method.isAnnotationPresent(CommandMapping.class)) {
                    CommandMapping commandMapping = method.getDeclaredAnnotation(CommandMapping.class);
                    MethodRunnerWrapper methodRunner = new MethodRunnerWrapper(group, commandMapping.response(), commandMapping.error(), method);
                    methodRunner.setInstance(instance);

                    Parameter[] parameters = method.getParameters();
                    // 只对第一个参数进行设置
                    if (null != parameters && parameters.length >= 1) {
                        Class<?> parameterClass = parameters[0].getType();
                        // 参数类型必须可以序列化
                        if (!Serializable.class.isAssignableFrom(parameterClass)) {
                            throw new RuntimeException("parameterClass: " + parameterClass.getName() + " not support serializable in method" + method.getName());
                        }

                        // 不支持void类型
                        if (parameterClass.equals(Void.class) || parameterClass.equals(void.class)) {
                            throw new RuntimeException("parameter class: " + parameterClass.getName() + " not support void in method" + method.getName());
                        }

                        methodRunner.setParameterClass(parameterClass);
                    } else {
                        throw new RuntimeException("the method(" + method.getName() + ") must have at least one parameter");
                    }

                    methodRunner.setReturnValueClass(method.getReturnType());

                    CommandEvent event = new CommandEvent(group, commandMapping.request());
                    commandMap.putIfAbsent(event, methodRunner);
                }
            });
            return this;
        } else {
            log.warn(LOG_PRE + "class:{} cannot parse for not have annotation: NettyController", instance.getClass().getCanonicalName());
            return null;
        }
    }

    private String getGroup(GlueController glueController) {
        if (!DEFAULT_GROUP_STR.equals(glueController.group())) {
            return glueController.group();
        }

        return glueController.value();
    }

    static class MethodRunnerWrapper {

        @Getter
        private String group;
        /**
         * 响应命令
         */
        @Getter
        private String responseCmd;
        /**
         * 执行错误时候的命令
         */
        @Getter
        private String errorCmd;
        private Method method;
        @Setter
        private Object instance;
        @Setter
        private Class<?> parameterClass;
        @Setter
        private Class<?> returnValueClass;

        MethodRunnerWrapper(String group, String responseCmd, String errorCmd, Method method) {
            this.group = group;
            this.responseCmd = responseCmd;
            this.errorCmd = errorCmd;
            this.method = method;
        }

        public Object run(Object parameterValue) throws Throwable {
            if (null != returnValueClass) {
                try {
                    if (parameterValue instanceof JSONObject) {
                        return method.invoke(instance, ((JSONObject) parameterValue).toJavaObject(parameterClass));
                    } else {
                        return method.invoke(instance, parameterValue);
                    }
                } catch (InvocationTargetException e) {
                    log.error(LOG_PRE + "class:{} method:{} run fail", instance.getClass().getName(), method.getName(), e.getTargetException());
                    throw e.getTargetException();
                }
            }
            return null;
        }
    }
}
