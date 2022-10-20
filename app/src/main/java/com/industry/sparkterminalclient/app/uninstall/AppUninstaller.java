package com.industry.sparkterminalclient.app.uninstall;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.industry.sparkterminalclient.KnoxManager;
import com.industry.sparkterminalclient.Utility;
import com.industry.sparkterminalclient.event.EventConstants;
import com.industry.sparkterminalclient.event.EventManager;
import com.industry.sparkterminalclient.thread.ThreadManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import bolts.Continuation;
import bolts.Task;

/**
 * Created by Two on 2/6/2015.
 */
public class AppUninstaller implements IAppUninstaller {
    private final static String TAG = AppUninstaller.class.getName();


    // Constants
    private final static int UNINSTALLL_APP_TIMEOUT = 120000;
    private final static int UNINSTALLL_APP_MONITOR_INTERVAL = 100;
    private static final int QUEUE_LIMIT = 200;
    private static final String MANIFEST_CHECKSUM_LINE_PREFIX = "SHA1-Digest:";


    // Dependencies
    private Context mContext;
    private KnoxManager mKnoxManager;
    private EventManager mEventManager;


    // Members
    private ConcurrentLinkedQueue<UninstallItem> mUninstallQueue = new ConcurrentLinkedQueue<UninstallItem>();
    private AtomicBoolean mIsUninstalling = new AtomicBoolean(false);
    private Object mUninstallLock = new Object();
    private Handler mUninstallHandler = new Handler(Looper.getMainLooper());


    // Inner Classes
    private class UninstallItem {
        public String packageName;
        public Task<Void>.TaskCompletionSource task;
        public long startTime;

        public UninstallItem(String _packageName, Task<Void>.TaskCompletionSource _task) throws Exception {
            packageName = _packageName;
            task = _task;
        }
    }


    // Singleton
    private static AppUninstaller mInstance;
    public static synchronized AppUninstaller getInstance(Context context) {
        if(mInstance == null) {
            mInstance = new AppUninstaller(context);
        }
        return mInstance;
    }


    // Constructor
    public AppUninstaller(Context context) {
        mContext = context;
        mKnoxManager = KnoxManager.getInstance(context);
        mEventManager = EventManager.getInstance(context);
    }


    // Methods
    public synchronized Task<Void> uninstallApp(String packageName) {
        final Task<Void>.TaskCompletionSource done = Task.create();
        try {
            synchronized (mUninstallLock) {
                // Verify Inputs
                if(packageName == null || packageName.isEmpty()) {
                    throw new Exception("Invalid args passed to installOrUninstallApp");
                }

                // Check Queue Limit
                if(mUninstallQueue.size() > QUEUE_LIMIT) {
                    throw new Exception("Install Queue limit reached");
                }

                // Queue Install
                mUninstallQueue.add(new UninstallItem(packageName, done));
            }

            if(!mIsUninstalling.get()) {
                performNextUninstall();
            }

        } catch(Throwable t) {
            Utility.finishTask(done, t);
        } finally {
            return done.getTask();
        }
    }

    public synchronized Task<Void> uninstallApps(ArrayList<String> packageNames) {
        Task<Void> task = Task.forResult(null);
        for(final String packageName : packageNames) {
            task = task.continueWithTask(new Continuation<Void, Task<Void>>() {
                @Override
                public Task<Void> then(Task<Void> ignored) throws Exception {
                    return uninstallApp(packageName);
                }
            }, ThreadManager.getInstance());
        }
        return task;
    }

    private void performNextUninstall() {
        synchronized (mUninstallLock) {

            // Do we have any pending Uninstall Requests?
            if (mUninstallQueue.size() > 0) {
                mIsUninstalling.set(true);

                // Get the next ready uninstall file from the queue
                final UninstallItem uninstallItem = mUninstallQueue.poll();

                // Start the Timer!
                uninstallItem.startTime = new Date().getTime();

                // Uninstall
                if (mKnoxManager.isApplicationInstalled(uninstallItem.packageName)) {
                    mKnoxManager.uninstallApplication(uninstallItem.packageName, false);
                    listenForAppUninstall(uninstallItem);
                } else {
                    finishUninstall(uninstallItem);
                }


            } else {
                mIsUninstalling.set(false);
            }
        }
    }

    private void listenForAppUninstall(final UninstallItem uninstallItem) {
        mUninstallHandler.post( new Runnable() {
            @Override
            public void run() {

                try {
                    // Check if uninstall timed out
                    if ((new Date().getTime() - uninstallItem.startTime) > UNINSTALLL_APP_TIMEOUT) {
                        mKnoxManager.removeAppPackageNameFromWhiteList(uninstallItem.packageName);
                        mEventManager.emitEvent(EventConstants.EVENT_APP_UNINSTALL);
                        finishUninstall(uninstallItem, new Exception("Uninstall timeout out for apk: " + uninstallItem.packageName));

                    // Check if has uninstalled
                    } else if (!mKnoxManager.isApplicationInstalled(uninstallItem.packageName)) {
                        finishUninstall(uninstallItem);

                    // Check status again on interval
                    } else {
                        mUninstallHandler.postDelayed(this, UNINSTALLL_APP_MONITOR_INTERVAL);
                    }
                } catch (Throwable t) {
                    finishUninstall(uninstallItem, t);
                }
            }
        });
    }

    private void finishUninstall(UninstallItem installOrUninstallItem) {
        finishUninstall(installOrUninstallItem, null);
    }

    private void finishUninstall(UninstallItem uninstallItem, Throwable t) {
        Utility.finishTask(uninstallItem.task, t);
        performNextUninstall();
    }
}
