package com.payment.orchestrator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    @Value("${async.workflow.core-pool-size:10}")
    private int corePoolSize;

    @Value("${async.workflow.max-pool-size:50}")
    private int maxPoolSize;

    @Value("${async.workflow.queue-capacity:100}")
    private int queueCapacity;

    @Value("${async.workflow.thread-name-prefix:workflow-}")
    private String threadNamePrefix;

    // dedicated executor for payment workflow steps — isolated from default Spring async pool
    @Bean(name = "workflowExecutor")
    public Executor workflowExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
