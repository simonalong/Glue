package com.simonalong.glue;

import com.simonalong.glue.event.CommandEventDispatcher;
import com.simonalong.glue.event.CommandProcessor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutorService;

import static com.simonalong.glue.GlueConstant.LOG_PRE;

/**
 * @author shizi
 * @since 2020/3/3 下午10:26
 */
@Slf4j
public class AbstractRemote {

    /**
     * 添加对应的Controller
     *
     * @param controllerInstance 添加{@link GlueServer.NettyConnectHandler}注解的类的实例
     * @param executorService    线程池
     */
    public void addController(Object controllerInstance, ExecutorService executorService) {
        CommandEventDispatcher.addCmdProcessor(CommandProcessor.parse(controllerInstance, executorService));
    }

    /**
     * 添加命令处理类
     * <p>
     * 注意：
     * 参数类需要有注解{@link GlueServer.NettyConnectHandler}，否则没有用
     *
     * @param controllerInstance 处理类实例
     */
    public void addController(Object controllerInstance) {
        addController(controllerInstance, null);
    }

    /**
     * 添加对应的Controller
     *
     * @param controllerClass 添加{@link GlueServer.NettyConnectHandler}注解的类
     * @param executorService 线程池
     */
    public void addController(Class<?> controllerClass, ExecutorService executorService) {
        Constructor c;
        try {
            c = controllerClass.getDeclaredConstructor();
            c.setAccessible(true);
            addController(c.newInstance(), executorService);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            log.error(LOG_PRE + "add controller fail, class: {}", controllerClass, e);
        }
    }

    public void addController(Class<?> controllerClass) {
        Constructor c;
        try {
            c = controllerClass.getDeclaredConstructor();
            c.setAccessible(true);
            addController(c.newInstance());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            log.error(LOG_PRE + "add controller fail, class: {}", controllerClass, e);
        }
    }
}
