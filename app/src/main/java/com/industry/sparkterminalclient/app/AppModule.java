package com.industry.sparkterminalclient.app;

import android.app.DownloadManager;
import android.content.Context;
import android.content.IntentFilter;

import com.industry.sparkterminalclient.Utility;
import com.industry.sparkterminalclient.event.EventManager;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import bolts.Task;

/**
 * Created by Two on 2/9/2015.
 */
public abstract class AppModule {

    // Constants
    protected static final String APP_EVENT_NAME = "EVENT_APP_BASE";


    // Dependencies
    protected Context mContext;
    protected EventManager mEventManager;


    // Constructor
    protected AppModule(Context context) {
        mContext = context;
        mEventManager = EventManager.getInstance(context);
    }


    // Methods
    protected <T> void finishTask(Task<T>.TaskCompletionSource task, Exception e) {
        Utility.finishTask(task, e);
    }

    protected <T> void finishTask(Task<T>.TaskCompletionSource task, Object... args) {
        mEventManager.emitEvent(APP_EVENT_NAME, args);
        Utility.finishTask(task);
    }
}
