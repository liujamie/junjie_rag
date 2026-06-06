package com.junjie.rag.common;

import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 支持 MDC 上下文传播的线程池，确保异步任务中 traceId 不丢失
 */
public class MdcThreadPoolExecutor extends ThreadPoolExecutor {

    public MdcThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
                                  long keepAliveTime, TimeUnit unit,
                                  BlockingQueue<Runnable> workQueue,
                                  ThreadFactory threadFactory,
                                  RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    @Override
    public void execute(Runnable command) {
        super.execute(wrapWithMdc(command));
    }

    private static Runnable wrapWithMdc(Runnable task) {
        Map<String, String> context = MDC.getCopyOfContextMap();
        return () -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            try {
                if (context != null && !context.isEmpty()) {
                    MDC.setContextMap(context);
                }
                task.run();
            } finally {
                if (previous != null && !previous.isEmpty()) {
                    MDC.setContextMap(previous);
                } else {
                    MDC.clear();
                }
            }
        };
    }
}
