package com.industry.sparkterminalclient.tests;

import com.industry.sparkterminalclient.app.SparkAppManager;
import com.industry.sparkterminalclient.tests.base.SparkClientTestCase;
import com.industry.sparkterminalclient.tests.base.IThreadSafeTest;
import com.industry.sparkterminalclient.tests.base.SparkTerminalTestData;
import com.industry.sparkterminalclient.thread.ThreadManager;

import java.util.ArrayList;

import bolts.Continuation;
import bolts.Task;

/**
 * Created by Two on 12/4/2014.
 */

public class AppEnableTest extends SparkClientTestCase implements IThreadSafeTest {

    private SparkAppManager mAppManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mAppManager = SparkAppManager.getInstance(getContext());
    }


    // Test Functions

    public void testExecutionSingle() throws Exception {
        resetAsyncLatch();
        mAppManager.enableApp(SparkTerminalTestData.testPackageNames.get(0)).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(Task<Void> task) throws Exception {
                assertNull(task.getError());
                countDownAsyncLatch();
                return null;
            }
        }, ThreadManager.getInstance());

        if(!awaitForAsyncLatch()) {
            throw new Exception("Failed to testExecutionSingle");
        }
    }

    public void testExecutionSeries() throws Exception {
        resetAsyncLatch();

        ArrayList<String> packageNames = new ArrayList<String>();
        for(int i=0 ; i < 5 ; i++) {
            packageNames.add(SparkTerminalTestData.testPackageNames.get(i));
        }

        mAppManager.enableApps(packageNames).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(Task<Void> task) throws Exception {
                assertNull(task.getError());
                countDownAsyncLatch();
                return null;
            }
        }, ThreadManager.getInstance());

        if(!awaitForAsyncLatch()) {
            throw new Exception("Failed to testExecutionSeries");
        }
    }

    public void testExecutionBulk() throws Exception {
        resetAsyncLatch();

        mAppManager.enableApps(SparkTerminalTestData.testPackageNames).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(Task<Void> task) throws Exception {
                assertNull(task.getError());
                countDownAsyncLatch();
                return null;
            }
        }, ThreadManager.getInstance());

        if(!awaitForAsyncLatch(15000000)) {
            throw new Exception("Failed to testExecutionBulkSeries");
        }
    }
}