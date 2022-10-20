package com.industry.sparkterminalclient.thread;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by Two on 12/5/2014.
 */
public class ThreadManager extends ThreadPoolExecutor {
    private final static String TAG = ThreadManager.class.getName();

    // Constants
    private final static int CORE_POOL_SIZE = (Runtime.getRuntime().availableProcessors() < 2) ? Runtime.getRuntime().availableProcessors() : 2;
    private final static int MAX_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private final static long KEEP_ALIVE_TIME = Long.MAX_VALUE;
    private final static TimeUnit TIME_UNIT = TimeUnit.NANOSECONDS;
    private final static int QUEUE_LIMIT = 600;


    // Members
    private static ArrayBlockingQueue<Runnable> mRunnablePoolQueue = new  ArrayBlockingQueue<Runnable>(QUEUE_LIMIT);
    private static AsyncThreadFactory mThreadFactory = new AsyncThreadFactory();


    // Singleton
    private static ThreadManager mInstance;
    public static synchronized ThreadManager getInstance() {
        if(mInstance == null) {
            mInstance = new ThreadManager();
        }
        return mInstance;
    }


    // Constructor
    private ThreadManager() {
        super(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME, TIME_UNIT,
                mRunnablePoolQueue, mThreadFactory);
    }
}
