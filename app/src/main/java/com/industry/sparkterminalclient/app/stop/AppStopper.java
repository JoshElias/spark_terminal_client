package com.industry.sparkterminalclient.app.stop;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.industry.sparkterminalclient.KnoxManager;
import com.industry.sparkterminalclient.event.EventConstants;
import com.industry.sparkterminalclient.event.EventManager;
import com.industry.sparkterminalclient.Utility;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import bolts.Task;

/**
 * Created by Two on 11/27/2014.
 */

public class AppStopper implements IAppStopper {
    private static final String TAG = AppStopper.class.getName();


    // Constants
    private final static int STOP_APP_INTERVAL = 100;
    private final static int STOP_APP_TIMEOUT = 3000;
    private static final int QUEUE_LIMIT = 200;


    // Dependencies
    private Context mContext;
    private KnoxManager mKnoxManager;
    private EventManager mEventManager;


    // Members
    private ConcurrentLinkedQueue<StopItem> mStopItemQueue = new ConcurrentLinkedQueue<StopItem>();
    private AtomicBoolean mIsStoppingApps = new AtomicBoolean(false);
    private Object mStopLock = new Object();
    private Handler mAppStopHandler = new Handler(Looper.getMainLooper());


    // Inner Classes
    private class StopItem {
        long startTime;
        Task<Void>.TaskCompletionSource task;
        String  packageName;
        public StopItem(String _packageName, Task<Void>.TaskCompletionSource _task) {
            packageName = _packageName;
            task = _task;
        }
    }


    // Singleton
    private static AppStopper mInstance;
    public static synchronized AppStopper getInstance(Context context) {
        if(mInstance == null) {
            mInstance = new AppStopper(context);
        }
        return mInstance;
    }


    // Constructor
    private AppStopper(Context context) {
        mContext = context;
        mKnoxManager = KnoxManager.getInstance(context);
        mEventManager = EventManager.getInstance(context);
    }


    // Methods
    public synchronized Task<Void> stopApp(String packageName) {
        Log.e(TAG, "Queuing app for stopping");
        final Task<Void>.TaskCompletionSource done = Task.create();
        try {
            synchronized (mStopLock) {
                // Verify input
                if(packageName == null || packageName.isEmpty()) {
                    throw new Exception("Invalid args passed to stopApp");
                }

                // Check Queue Limit
                if(mStopItemQueue.size() > QUEUE_LIMIT) {
                    throw new Exception("Stop App Queue limit reached");
                }

                // Queue Install
                Log.e(TAG, "Adding to queue");
                mStopItemQueue.add(new StopItem(packageName, done));
            }

            if(!mIsStoppingApps.get()) {
                performNextAppStop();
            }

        } catch(Throwable t) {
            Utility.finishTask(done, t);
        } finally {
            return done.getTask();
        }
    }

    public synchronized Task<Void> stopApps(ArrayList<String> packageNames) {
        ArrayList<Task<Void>> tasks = new ArrayList<Task<Void>>();
        for (final String packageName : packageNames) {
            tasks.add(stopApp(packageName));
        }
        return Task.whenAll(tasks);
    }

    private void performNextAppStop () {
        Log.e(TAG, "Performing next close");

        synchronized (mStopLock) {

            // Do we have any pending Install Requests?
            if (mStopItemQueue.size() > 0) {
                Log.e(TAG, "Actually processing close");
                mIsStoppingApps.set(true);

                // Get the next ready install file from the queue
                final StopItem closeItem = mStopItemQueue.poll();

                // Start the Timer!
                closeItem.startTime = new Date().getTime();

                // Is this app even installed?
               if(!mKnoxManager.isApplicationInstalled(closeItem.packageName)) {
                   finishedClosingApp(closeItem, new Exception("The requested app is not installed: " + closeItem.packageName));

               // Check if the app is running at all
               } else if(!mKnoxManager.isApplicationRunning(closeItem.packageName)) {
                   Log.e(TAG, "The app wsa never running");
                   finishedClosingApp(closeItem);

               // Try to close the app and wipe it's data
               } else if(!mKnoxManager.stopApp(closeItem.packageName) || !mKnoxManager.wipeAppData(closeItem.packageName)) {
                   Log.e(TAG, "err?");
                   finishedClosingApp(closeItem, new Exception("Unable to stop apk properly: " + closeItem.packageName));

               } else {
                   Log.e(TAG, "Listening for closing");
                   listenForStop(closeItem);
               }

            } else {
                mIsStoppingApps.set(false);
            }
        }
    };

    private void listenForStop(final StopItem closeItem) {
        Log.e(TAG, "Listening for closing");
        mAppStopHandler.post(new Runnable() {
            @Override
            public void run() {
                try {

                    // Check if this batch has timed out
                    Log.e(TAG, "Checking for Timeout");
                    if ((new Date().getTime() - closeItem.startTime) > STOP_APP_TIMEOUT) {
                        finishedClosingApp(closeItem, new Exception("Unable to close all other apps while launching the current one"));

                    // Check if the app is still running
                    } else if (!mKnoxManager.isApplicationRunning(closeItem.packageName)) {

                        // Wipe Recent Tasks
                        if(mKnoxManager.wipeRecentTasks()) {
                            mEventManager.emitEvent(EventConstants.EVENT_APP_STOP, closeItem.packageName);
                            finishedClosingApp(closeItem);
                        } else {
                            finishedClosingApp(closeItem, new Exception("Knox unable to wipe recent tasks"));
                        }

                    // Check the status again on an interval
                    } else {
                        Log.e(TAG, "Checking status again");
                        mAppStopHandler.postDelayed(this, STOP_APP_INTERVAL);
                    }

                } catch (Throwable t) {
                    finishedClosingApp(closeItem, t);
                }
            }
        });
    }

    private void finishedClosingApp(StopItem closeItem) {
        finishedClosingApp(closeItem, null);
    }

    private void finishedClosingApp(StopItem closeItem, Throwable t) {
        Log.e(TAG, "Finished Closing App");
        performNextAppStop();
    }
}
