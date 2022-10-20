package com.industry.sparkterminalclient.app.toggle;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.util.Log;

import com.industry.sparkterminalclient.KnoxManager;
import com.industry.sparkterminalclient.Utility;
import com.industry.sparkterminalclient.event.EventConstants;
import com.industry.sparkterminalclient.event.EventManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import bolts.Task;

/**
 * Created by Two on 12/18/2014.
 */

public class AppToggler implements IAppToggler {
    private static final String TAG = AppToggler.class.getName();


    // Constants
    private static final int QUEUE_LIMIT = 200;
    private static final int TOGGLE_INTERVAL = 100;
    private static final int TOGGLE_TIMEOUT = 3000;


    // Dependencies
    private Context mContext;
    private KnoxManager mKnoxManager;
    private EventManager mEventManager;


    // Members
    private ConcurrentLinkedQueue<ToggleItem> mToggleQueue = new ConcurrentLinkedQueue<ToggleItem>();
    private AtomicBoolean mIsToggling = new AtomicBoolean(false);
    private Object mToggleLock = new Object();
    private Handler mToggleHandler = new Handler(Looper.getMainLooper());


    // Inner Classes
    private class ToggleItem {
        public Task<Void>.TaskCompletionSource task;
        public String packageName;
        public boolean isEnabling;
        public long startTime;

        public ToggleItem(Task<Void>.TaskCompletionSource _task, String _packageName, boolean _isEnabling) {
            task = _task;
            packageName = _packageName;
            isEnabling = _isEnabling;
        }
    }


    // Singleton
    private static AppToggler mInstance;
    public static synchronized AppToggler getInstance(Context context) {
        if(mInstance == null) {
            mInstance = new AppToggler(context);
        }
        return mInstance;
    }


    // Constructor
    public AppToggler(Context context) {
        mContext = context;
        mKnoxManager = KnoxManager.getInstance(context);
        mEventManager = EventManager.getInstance(context);
    }


    // Methods
    public synchronized Task<Void> enableApp(String packageName) {
        return enableApps(new ArrayList<String>(Arrays.asList(packageName)));
    }

    public synchronized Task<Void> disableApp(String packageName) {
        return disableApps(new ArrayList<String>(Arrays.asList(packageName)));
    }

    public synchronized Task<Void> enableApps(ArrayList<String> packageNames) {
        return toggleApps(packageNames, true);
    }

    public synchronized Task<Void> disableApps(ArrayList<String> packageNames) {
        Log.e(TAG, "disabling apps");
        return toggleApps(packageNames, false);
    }


    private synchronized Task<Void> toggleApp(final String packageName, final boolean isEnabling) {
        Log.d(TAG, "ToggleApp Fired: "+isEnabling);
        final Task<Void>.TaskCompletionSource done = Task.create();
        try {
            synchronized (mToggleLock) {
                // Verify inputs
                if(packageName == null || packageName.isEmpty()) {
                    throw new Exception("Invalid args passed to toggleApp");
                }

                // Verify Queue limit
                if (mToggleQueue.size() > QUEUE_LIMIT) {
                    throw new Exception("Toggle Queue limit reached");
                }

                // Add Download to the Queue
                Log.e(TAG, "Adding to q");
                mToggleQueue.add(new ToggleItem(done, packageName, isEnabling));
            }

            if(!mIsToggling.get()) {
                performNextToggle();
            }


        } catch( Throwable t ) {
            Utility.finishTask(done, t);
        } finally {
            return done.getTask();
        }
    }

    private synchronized Task<Void> toggleApps(ArrayList<String> packageNames, final boolean isEnabling) {
        ArrayList<Task<Void>> tasks = new ArrayList<Task<Void>>();
        for(final String packageName : packageNames) {
            tasks.add(toggleApp(packageName, isEnabling));
        }
        return Task.whenAll(tasks);
    }

    public void performNextToggle() {
        Log.d(TAG, "Performing next Toggle Item");

        synchronized (mToggleLock) {

            // Do we have any pending Install Requests?
            if (mToggleQueue.size() > 0) {
                mIsToggling.set(true);

                final ToggleItem toggleItem = mToggleQueue.poll();
                toggleItem.startTime = new Date().getTime();

                try {

                    // Is this app even installed?
                    if(!mKnoxManager.isApplicationInstalled(toggleItem.packageName)) {
                        finishToggle(toggleItem, new Exception("The requested app is not installed: " + toggleItem.packageName));

                    // Should we enable?
                    } else if(toggleItem.isEnabling) {
                        Log.e(TAG, "trying to enable");
                        if(mKnoxManager.getApplicationStateEnabled(toggleItem.packageName)) {
                            Log.e(TAG, "is enabled");
                            finishToggle(toggleItem);
                        } else {
                            Log.e(TAG, "is enabled");
                            mKnoxManager.setEnableApplication(toggleItem.packageName);
                            listenForAppToggle(toggleItem);
                        }

                    // Should we disable?
                    } else {
                        Log.e(TAG, "trying to disable");
                        if(!mKnoxManager.getApplicationStateEnabled(toggleItem.packageName)) {
                            Log.e(TAG, "is disabled");
                            finishToggle(toggleItem);
                        } else {
                            Log.e(TAG, "was disabled");
                            mKnoxManager.setDisableApplication(toggleItem.packageName);
                            listenForAppToggle(toggleItem);
                        }
                    }

                } catch (Throwable t) {
                    finishToggle(toggleItem, t);
                }

            } else {
                mIsToggling.set(false);
            }
        }
    }

    private void listenForAppToggle(final ToggleItem toggleItem) {
        mToggleHandler.post( new Runnable() {
            @Override
            public void run() {
                try {
                    // Check if install timed out
                    Log.e(TAG, "Checking if installed");
                    if ((new Date().getTime() - toggleItem.startTime) > TOGGLE_TIMEOUT) {
                        throw new Exception("Toggling app timed out");

                    // Check if has enabled
                    } else if (toggleItem.isEnabling && mKnoxManager.getApplicationStateEnabled(toggleItem.packageName)) {
                        Log.e(TAG, "Finished enable!");
                        mEventManager.emitEvent(EventConstants.EVENT_APP_ENABLE, toggleItem.packageName);
                        finishToggle(toggleItem);

                    // Check if has disabled
                    } else if (!toggleItem.isEnabling && !mKnoxManager.getApplicationStateEnabled(toggleItem.packageName)) {
                        Log.e(TAG, "Finished disable!");
                        mEventManager.emitEvent(EventConstants.EVENT_APP_DISABLE, toggleItem.packageName);
                        finishToggle(toggleItem);

                    // Check status on interval
                    } else {
                        mToggleHandler.postDelayed(this, TOGGLE_INTERVAL);
                    }

                } catch (Throwable t) {
                    finishToggle(toggleItem, t);
                }
            }
        });
    }

    private void finishToggle(ToggleItem toggleItem) {
        finishToggle(toggleItem, null);
    }

    private void finishToggle(ToggleItem toggleItem, Throwable t) {
        Utility.finishTask(toggleItem.task, t);
        performNextToggle();
    }
}