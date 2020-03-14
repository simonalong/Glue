package com.simonalong.glue.annotation;

import java.lang.annotation.*;

/**
 * @author shizi
 * @since 2020/3/3 下午6:56
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CommandMapping {

    /**
     * 过滤外部的命令，只有为该字段的时候才会接收
     *
     * @return 请求命令
     */
    String request() default "";

    /**
     * 在函数处理完之后，会将该返回值和对应的命令封装为对应的请求数据给请求方
     * <p>
     * 注意：只有修饰的函数有返回值的时候，该属性才会生效
     *
     * @return 响应命令
     */
    String response() default "";

    /**
     * 异常情况下的命令
     * @return 异常命令
     */
    String error() default "";
}
