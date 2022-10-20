package com.industry.sparkterminalclient.app.start;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.industry.sparkterminalclient.KnoxManager;
import com.industry.sparkterminalclient.Utility;
import com.industry.sparkterminalclient.app.start.IAppLaunchable;
import com.industry.sparkterminalclient.event.EventConstants;
import com.industry.sparkterminalclient.event.EventManager;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import bolts.Task;

/**
 * Created by Two on 11/27/2014.
 */
public class AppStarter extends implements IAppStarter {
    private final static String TAG = AppStarter.class.getName();


    // Constants
    private final static int START_APP_TIMEOUT = 3000;
    private final static int START_APP_MONITOR_INTERVAL = 100;


    // Dependencies
    private KnoxManager mKnoxManager;


    // Members
    private Object mStartLock = new Object();
    private Handler mAppStartHandler = new Handler(Looper.getMainLooper());
    private String mPackageName;
    private Task<Void>.TaskCompletionSource mStartTask;
    private AtomicLong mStartTime = new AtomicLong();


    // Singleton
    private static AppStarter mInstance;
    public static synchronized AppStarter getInstance(Context context) {
        if(mInstance == null) {
            mInstance = new AppStarter(context);
        }
        return mInstance;
    }

    // Constructor
    public AppStarter(Context context) {
        mContext = context;
        mKnoxManager = KnoxManager.getInstance(context);
        mEventManager = EventManager.getInstance(context);
    }


    // Methods
    public synchronized Task<Void> startApp(final String packageName) {
        Log.e(TAG, "Launching app");
        final Task.TaskCompletionSource done = Task.create();
        try {
            synchronized(mStartLock) {

                // Finish off the last launch task if any
                Utility.finishTask(done, new Exception("Another app was requested for launch before this one could be completed"));

                // Start New App Launch
                mPackageName = packageName;
                mStartTask = done;
                mStartTime = new Date().getTime();

                // Is this app even installed?
                if(!mKnox.isApplicationInstalled(packageName)) {
                    Utility.finishTask(done, new Exception("The requested app is not installed: " + packageName));

                // Check if it's already running
                } else if (mKnox.isApplicationRunning(packageName)) {
                    Utility.finishTask(done);

                // Attempted to start
                } else if(mKnox.startApp(packageName, null)) {
                    listenForAppStart();

                // Failed to start
                } else {
                    Utility.finishTask(done, new Exception("Spark was unable to start APK with the packageName: "+packageName));
                }
            }

        } catch (Exception e) {
            Utility.finishTask(mStartTask, e);
        } finally {
            return done.getTask();
        }
    }

    private void listenForAppStart() {
        mAppStartHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized (mStartLock) {

                        // Check if launch timed out
                        if ((new Date().getTime() - mStartTime) > START_APP_TIMEOUT) {
                            throw new Exception("Opening new app timed out");

                            // Check if the app is running yet;
                        } else if (mKnox.isApplicationRunning(mPackageName)) {
                            mEventManager.emitEvent(EventConstants.EVENT_APP_START, mPackageName);
                            Utility.finishTask(mStartTask);

                            // Run the check again after interval
                        } else {
                            mAppStartHandler.postDelayed(this, START_APP_MONITOR_INTERVAL);
                        }
                    }

                } catch (Exception e) {
                    Utility.finishTask(mStartTask, e);
                }
            }
        });
    }
}
