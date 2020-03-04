package com.github.glue;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author shizi
 * @since 2020/3/3 下午8:39
 */
@Slf4j
@SuppressWarnings("all")
@UtilityClass
public class ThreadPoolFactory {

    /**
     * 获取单例线程池
     *
     * @param threadName 线程名
     * @return 线程池
     */
    public ThreadPoolExecutor getSinglePool(String threadName) {
        return getPool(threadName, 1, 1, 0, new LinkedBlockingQueue<>(20000), false, "block");
    }

    /**
     * 获取可变更线程池
     *
     * @param threadName 线程名
     * @return 线程池
     */
    public ThreadPoolExecutor getCachePool(String threadName) {
        return getPool(threadName, 1, Integer.MAX_VALUE, 60 * 1000, new SynchronousQueue<>(), true, "abort");
    }

    /**
     * 获取固定数目线程池
     *
     * @param threadName 线程池名字
     * @param coreSize   核心线程个数
     * @return 线程池
     */
    public ThreadPoolExecutor getFixedPool(String threadName, Integer coreSize) {
        return getPool(threadName, coreSize, coreSize, 0, new LinkedBlockingQueue<Runnable>(20000), false, "block");
    }

    /**
     * 构造默认系统cpu个数的线程池
     *
     * @param threadName           线程池名字
     * @param aliveSecondTime      存活时间（毫秒）
     * @param aliveMilliSecondTime 是否允许核心线程超时清理
     * @param rejectHandler        拒绝策略
     * @return 线程池
     */
    public ThreadPoolExecutor getSystemCorePool(String threadName, Integer aliveSecondTime, Boolean aliveMilliSecondTime, String rejectHandler) {
        return getPool(threadName, Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors(), aliveSecondTime,
            new LinkedBlockingQueue<Runnable>(20000), aliveMilliSecondTime, rejectHandler);
    }

    /**
     * 创建线程池
     *
     * @param threadName            线程名字
     * @param coreSize              核心线程数
     * @param maxSize               最大线程数
     * @param aliveMilliSecondTime  存活时间（毫秒）
     * @param blockingQueue         阻塞队列
     * @param alloCoreThreadTimeout 是否允许核心线程超时清理
     * @param rejectHandler         拒绝策略：
     *                              abort（抛异常）
     *                              discard（丢弃任务）
     *                              discardOldest（丢弃队列中最老的）
     *                              callRun（直接运行）
     *                              block（阻塞）
     * @return
     */
    public ThreadPoolExecutor getPool(String threadName, Integer coreSize, Integer maxSize, Integer aliveMilliSecondTime, BlockingQueue<Runnable> blockingQueue,
        Boolean alloCoreThreadTimeout, String rejectHandler) {
        if (null == rejectHandler) {
            throw new RuntimeException("reject is null");
        }
        RejectedExecutionHandler rejectedExecutionHandler;

        switch (rejectHandler) {
            case "abort":
                rejectedExecutionHandler = new ThreadPoolExecutor.AbortPolicy();
                break;
            case "discard":
                rejectedExecutionHandler = new ThreadPoolExecutor.DiscardPolicy();
                break;
            case "discardOldest":
                rejectedExecutionHandler = new ThreadPoolExecutor.DiscardOldestPolicy();
                break;
            case "callRun":
                rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();
                break;
            case "block":
                rejectedExecutionHandler = new BlockRejectedExecutionHandler();
                break;
            default:
                throw new RuntimeException("not support reject:" + rejectHandler);
        }

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(coreSize, maxSize, aliveMilliSecondTime, TimeUnit.MILLISECONDS, blockingQueue, new ThreadFactory() {
            private AtomicInteger atomicInteger = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, threadName + "_" + atomicInteger.getAndIncrement());
            }
        }, rejectedExecutionHandler);
        if (alloCoreThreadTimeout) {
            threadPoolExecutor.allowCoreThreadTimeOut(alloCoreThreadTimeout);
        }
        return threadPoolExecutor;
    }

    /**
     * 重写拒绝策略，用于在任务量超大情况下任务的阻塞提交，防止任务丢失
     */
    private class BlockRejectedExecutionHandler implements RejectedExecutionHandler {

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            try {
                executor.getQueue().put(r);
            } catch (InterruptedException e) {
                log.warn("thread interrupt", e);
                throw new RuntimeException(e);
            }
        }
    }
}
