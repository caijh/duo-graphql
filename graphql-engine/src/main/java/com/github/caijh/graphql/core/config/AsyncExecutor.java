package com.github.caijh.graphql.core.config;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 异步任务
 *
 * @author xuwenzhen
 */
@Configuration
@EnableAsync
@ConfigurationProperties("executor.default")
public class AsyncExecutor implements AsyncConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(AsyncExecutor.class);

    private final int queueSize = 10000;

    private int coreSize = 0;

    private final int maxSize = 100;

    private final int keepAliveTime = 10;

    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(this.queueSize);

    private Executor executor;

    @Override
    public Executor getAsyncExecutor() {
        if (this.executor != null) {
            return this.executor;
        }
        if (this.coreSize <= 0) {
            this.coreSize = Runtime.getRuntime().availableProcessors();
        }

        logger.info("coreSize={}, maxSize={}, keepAliveTime={}, queueSize={}", this.coreSize, this.maxSize, this.keepAliveTime, this.queueSize);
        this.executor = new ThreadPoolExecutor(
                this.coreSize,
                this.maxSize,
                this.keepAliveTime,
                TimeUnit.SECONDS,
                this.queue,
                Thread::new,
                new ThreadPoolExecutor.DiscardPolicy()
        );
        return this.executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> logger.error("async error, method: {}", method.getName(), ex);
    }

    @PreDestroy
    public void preDestroy() {
        while (this.queue.peek() != null) {
            logger.info("default executor queue size: {}", this.queue.size());
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                logger.error("Thread.sleep(1);发生错误", e);
            }
        }

        logger.info("default executor queue is clear!");
    }

}
