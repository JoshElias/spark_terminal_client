package com.industry.sparkterminalclient.thread;

import java.util.concurrent.ThreadFactory;

/**
 * Created by Two on 12/19/2014.
 */
public class AsyncThreadFactory implements ThreadFactory {
    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    }
}