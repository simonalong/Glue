package com.simonalong.glue.annotation;


import java.lang.annotation.*;

import static com.simonalong.glue.GlueConstant.DEFAULT_GROUP_STR;

/**
 * @author shizi
 * @since 2020/3/3 下午6:55
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NettyController {

    /**
     * 对数据请求分组
     * <p>
     * 注意：同group()，都设置，则按照group()为准
     *
     * @return 分组
     */
    String value() default DEFAULT_GROUP_STR;

    /**
     * 对数据请求分组
     *
     * @return 分组
     */
    String group() default DEFAULT_GROUP_STR;

    /**
     * 分组命令中设置的线程池。线程池类型只接受：single, fixed, cache
     * <p>
     * 注意：配置了该参数也建议配置下下面的参数，如果不采用默认，也可以在api中设置自定义
     *
     * @return 线程池类型
     */
    String executor() default "null";

    /**
     * 默认核心线程池大小
     * <p>
     * 注意： 只有参数executor配置，该参数才生效，如果设置为0，则采用当前机器的cpu个数
     *
     * @return 线程池核心个数
     */
    int coreSize() default 0;
}
