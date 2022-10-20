package com.industry.sparkterminalclient.tests;

import android.util.Log;

import com.industry.sparkterminalclient.app.SparkAppManager;
import com.industry.sparkterminalclient.tests.base.SparkClientTestCase;
import com.industry.sparkterminalclient.tests.base.SparkTerminalTestData;
import com.industry.sparkterminalclient.thread.ThreadManager;

import bolts.Capture;
import bolts.Continuation;
import bolts.Task;

/**
 * Created by Two on 12/4/2014.
 */

public class AppLaunchTest  extends SparkClientTestCase {
    private final static String TAG = AppLaunchTest.class.getName();

    private SparkAppManager mAppManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mAppManager = SparkAppManager.getInstance(getContext());
    }


    // Test Functions

    public void testLaunch() throws Exception {
        resetAsyncLatch();

        mAppManager.launchApp(SparkTerminalTestData.testPackageNames.get(0)).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(Task<Void> task) throws Exception {
                assertNull(task.getError());
                countDownAsyncLatch();
                return null;
            }
        }, ThreadManager.getInstance());

        if(!awaitForAsyncLatch(5000)) {
            throw new Exception("Failed to testExecutionSingle");
        }
    }

    public void testLaunchBulk() throws Exception {
        resetAsyncLatch();
        Log.e(TAG, "Launching bulk");
        final Capture<String> currentPackageName = new Capture<String>();
        Task<Void> task = Task.forResult(null);
        for (final String packageName : SparkTerminalTestData.testPackageNames) {
            // For each item, extend the task with a function to delete the item.
            task = task.continueWithTask(new Continuation<Void, Task<Void>>() {
                public Task<Void> then(Task<Void> ignored) throws Exception {
                    Log.e(TAG, "Queuing package for Launch: "+packageName);
                    // Return a task that will be marked as completed when the delete is finished.
                    currentPackageName.set(packageName);
                    return mAppManager.launchApp(packageName);
                }
            }, ThreadManager.getInstance());
        }
        task.continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(Task<Void> task) throws Exception {
                if (task.isFaulted()) {
                    Log.e(TAG, "Last packageName: "+currentPackageName.get());
                    task.getError().printStackTrace();
                }
                assertNull(task.getError());
                countDownAsyncLatch();
                return null;
            }
        }, ThreadManager.getInstance());

        if(!awaitForAsyncLatch(15000000)) {
            throw new Exception("Failed to testExecutionSingle");
        }
    }
}
