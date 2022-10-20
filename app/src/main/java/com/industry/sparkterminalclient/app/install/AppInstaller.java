package com.industry.sparkterminalclient.app.install;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.industry.sparkterminalclient.KnoxManager;
import com.industry.sparkterminalclient.Utility;
import com.industry.sparkterminalclient.app.AppModule;
import com.industry.sparkterminalclient.app.AppUtility;
import com.industry.sparkterminalclient.event.EventConstants;
import com.industry.sparkterminalclient.event.EventManager;
import com.industry.sparkterminalclient.thread.ThreadManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import bolts.Continuation;
import bolts.Task;

/**
 * Created by Two on 2/6/2015.
 */
public class AppInstaller extends AppModule implements IAppInstaller {
    private static final String TAG = AppInstaller.class.getName();


    // Constants
    private final static int INSTALLL_APP_TIMEOUT = 120000;
    private final static int INSTALLL_APP_MONITOR_INTERVAL = 100;
    private static final int QUEUE_LIMIT = 200;


    // Dependencies
    private KnoxManager mKnoxManager;


    // Members
    private ConcurrentLinkedQueue<InstallItem> mInstallQueue = new ConcurrentLinkedQueue<InstallItem>();
    private AtomicBoolean mIsInstalling = new AtomicBoolean(false);
    private Object mInstallLock = new Object();
    private Handler mInstallHandler = new Handler(Looper.getMainLooper());


    // Inner Classes
    private class InstallItem {
        public File file;
        public String packageName;
        public Task<Void>.TaskCompletionSource task;
        public long startTime;

        public InstallItem(File _file, Task<Void>.TaskCompletionSource _task) throws Exception {
            file = _file;
            task = _task;
            packageName = AppUtility.getPackageNameFromAPK(mContext, file);
        }
    }


    // Singleton
    private static AppInstaller mInstance;
    public static synchronized AppInstaller getInstance(Context context) {
        if(mInstance == null) {
            mInstance = new AppInstaller(context);
        }
        return mInstance;
    }


    // Constructor
    public AppInstaller(Context context) {
        super(context);

        mKnoxManager = KnoxManager.getInstance(context);
    }


    // Methods
    public synchronized Task<Void> installApp(File file) {
        final Task<Void>.TaskCompletionSource done = Task.create();
        try {
            synchronized (mInstallLock) {
                // Verify Inputs
                if(file == null || !file.exists()) {
                    throw new Exception("Invalid args passed to installApp");
                }

                // Check Queue Limit
                if(mInstallQueue.size() > QUEUE_LIMIT) {
                    throw new Exception("Install Queue limit reached");
                }

                // Queue Install
                mInstallQueue.add(new InstallItem(file, done));
            }

            if(!mIsInstalling.get()) {
                performNextInstall();
            }

        } catch(Throwable t) {
            Utility.finishTask(done, t);
        } finally {
            return done.getTask();
        }
    }

    public synchronized Task<Void> installApps(ArrayList<File> files) {
        Task<Void> task = Task.forResult(null);
        for(final File file : files) {
            task = task.continueWithTask(new Continuation<Void, Task<Void>>() {
                @Override
                public Task<Void> then(Task<Void> ignored) throws Exception {
                    return installApp(file);
                }
            }, ThreadManager.getInstance());
        }
        return task;
    }

    private void performNextInstall() {

        synchronized (mInstallLock) {

            // Do we have any pending Install Requests?
            if (mInstallQueue.size() > 0) {
                mIsInstalling.set(true);

                // Get the next ready install file from the queue
                final InstallItem installItem = mInstallQueue.poll();

                // Start the Timer!
                installItem.startTime = new Date().getTime();

                // Install/Update the APK
                if (!installUpdateAPK(installItem)) {
                    finishInstall(installItem, new Exception("Installing an application package has failed."));
                } else {
                    listenForAppInstall(installItem);
                }

            } else {
                mIsInstalling.set(false);
            }
        }
    }

    private void listenForAppInstall(final InstallItem installItem) {
        mInstallHandler.post( new Runnable() {
            @Override
            public void run() {

                try {

                    // Check if install timed out
                    if ((new Date().getTime() - installItem.startTime) > INSTALLL_APP_TIMEOUT) {
                        finishInstall(installItem, new Exception("Install timeout out for apk: " + installItem.packageName));

                    // Check if has installed
                    } else if (mKnoxManager.isApplicationInstalled(installItem.packageName)) {
                        finishInstall(installItem);

                    // Check status again on interval
                    } else {
                        mInstallHandler.postDelayed(this, INSTALLL_APP_MONITOR_INTERVAL);
                    }
                } catch (Throwable t) {
                    finishInstall(installItem, t);
                }
            }
        });
    }

    private boolean installUpdateAPK(InstallItem installItem) throws SecurityException {
        try {
            // White list the apk about to be installed
            if(!mKnoxManager.addAppPackageNameToWhiteList(installItem.packageName)) {
                throw new Exception("Unable to add package name to whitelist: "+ installItem.packageName);
            }

            // Should we install or update?
            String apkPath = installItem.file.getPath();
            boolean isInstalled = mKnoxManager.isApplicationInstalled(installItem.packageName);
            if( isInstalled && mKnoxManager.updateApplication(apkPath) ) {
                mEventManager.emitEvent(EventConstants.EVENT_APP_UPDATE, installItem.packageName);
                return true;
            } else if(!isInstalled && mKnoxManager.installApplication(apkPath, false)) {
                mEventManager.emitEvent(EventConstants.EVENT_APP_INSTALL, installItem.packageName);
                return true;
            } else {
                return false;
            }

        } catch( Exception e ) {
            Log.e(TAG, "Unable to install APK");
            e.printStackTrace();
            return false;
        }
    }

    private void finishTask(InstallItem installOrUninstallItem) {
        finishInstall(installOrUninstallItem, null);
    }

    private void finishInstall(InstallItem installOrUninstallItem, Throwable t) {
        Utility.finishTask(installOrUninstallItem.task, t);
        performNextInstall();
    }
}
