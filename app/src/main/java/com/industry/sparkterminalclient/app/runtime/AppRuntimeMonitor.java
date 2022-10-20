package com.industry.sparkterminalclient.app.runtime;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.industry.sparkterminalclient.KnoxManager;
import com.industry.sparkterminalclient.app.AppConstants;
import com.industry.sparkterminalclient.app.AppModule;
import com.industry.sparkterminalclient.event.EventConstants;
import com.industry.sparkterminalclient.event.EventManager;
import com.industry.sparkterminalclient.event.IEventListener;

import java.util.ArrayList;

/**
 * Created by Two on 2/5/2015.
 */
public class AppRuntimeMonitor extends AppModule implements IAppRuntimeMonitor {
    private static final String TAG = AppRuntimeMonitor.class.getName();


    // Constants
    private static final int RUNTIME_MONITOR_INTERVAL = 1000;


    // Dependencies
    KnoxManager mKnoxManager;


    // Members
    Handler mRuntimeHandler = new Handler(Looper.getMainLooper());
    Object mRuntimeLock = new Object();
    ArrayList<String> mActiveApps = new ArrayList<String>();


    // Constructor
    public AppRuntimeMonitor(Context context) {
        super(context);

        mKnoxManager = KnoxManager.getInstance(context);

        listenForAppStart();
        listenForAppStop();
    }


    // Methods
    private void listenForAppStart() {
        mEventManager.registerListener(EventConstants.EVENT_APP_START, new IEventListener() {
            @Override
            public void onEvent(Object... args) {
                synchronized (mRuntimeLock) {
                    if (args.length < 1 || args[0] == null || !(args[0] instanceof String)) {
                        Log.e(TAG, "Ignoring app start event due to invalid args");
                        return;
                    }
                    String packageName = (String) args[0];
                    mActiveApps.add(packageName);
                    monitorRuntime();
                }
            }
        });
    }

    private void listenForAppStop() {
        mEventManager.registerListener(EventConstants.EVENT_APP_STOP, new IEventListener() {
            @Override
            public void onEvent(Object... args) {
                synchronized (mRuntimeLock) {
                    if (args.length < 1 || args[0] == null || !(args[0] instanceof String)) {
                        Log.e(TAG, "Ignoring app stop event due to invalid args");
                        return;
                    }
                    String packageName = (String) args[0];
                    int index = mActiveApps.indexOf(packageName);
                    if (index != -1) {
                        mActiveApps.remove(index);
                    }
                }
            }
        });
    }


    public void monitorRuntime() {
        mRuntimeHandler.post( new Runnable() {
            @Override
            public void run() {
                synchronized (mRuntimeLock) {
                    int crashedAppIndex = -1;
                    for(int i = 0; i < mActiveApps.size(); i++) {
                        String packageName = mActiveApps.get(i);
                        if (!mKnoxManager.isApplicationRunning(packageName)) {
                            mEventManager.emitEvent(EventConstants.EVENT_APP_STOP, packageName);
                            crashedAppIndex = i;
                        }
                    }

                    if(crashedAppIndex != -1) {
                        mActiveApps.remove(crashedAppIndex);
                    }

                    if(!mActiveApps.isEmpty()) {
                        mRuntimeHandler.postDelayed(this, RUNTIME_MONITOR_INTERVAL);
                    }
                }
            }
        });
    }
}
