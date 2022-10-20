package com.industry.sparkterminalclient.tests;

import com.industry.sparkterminalclient.app.SparkAppManager;
import com.industry.sparkterminalclient.tests.base.SparkClientTestCase;
import com.industry.sparkterminalclient.tests.base.IThreadSafeTest;
import com.industry.sparkterminalclient.tests.base.SparkTerminalTestData;
import com.industry.sparkterminalclient.thread.ThreadManager;

import java.util.ArrayList;
import java.util.Map;

import bolts.Continuation;
import bolts.Task;

/**
 * Created by Two on 12/4/2014.
 */
public class AppInstallTest  extends SparkClientTestCase implements IThreadSafeTest {

    private SparkAppManager mAppManager;

    private ArrayList<String> testHIDs;

    private ArrayList<String> getTestHIDs() {
        if (testHIDs == null) {
            testHIDs = new ArrayList<String>();
            for (Map.Entry<String, String> entry : SparkTerminalTestData.testPackageUrls.entrySet()) {
                testHIDs.add(entry.getKey());
            }
        }
        return testHIDs;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mAppManager = SparkAppManager.getInstance(getContext());
    }


    // Test Functions

    public void testExecutionSingle() throws Exception {
        resetAsyncLatch();
        mAppManager.installApp("testHID_1").continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(Task<Void> task) throws Exception {
                assertNull(task.getError());
                countDownAsyncLatch();
                return null;
            }
        }, ThreadManager.getInstance());

        if (!awaitForAsyncLatch(30000)) {
            throw new Exception("Failed to testExecutionSingle");
        }
    }

    public void testExecutionSeries() throws Exception {
        resetAsyncLatch();

        ArrayList<String> hidTaskSubset = new ArrayList<String>();
        int index = 0;
        for (String hid : getTestHIDs()) {
            hidTaskSubset.add(hid);
            if (++index > 5)
                break;
        }

        mAppManager.installApps(hidTaskSubset).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(Task<Void> task) throws Exception {
                assertNull(task.getError());
                countDownAsyncLatch();
                return null;
            }
        }, ThreadManager.getInstance());

        if (!awaitForAsyncLatch(150000)) {
            throw new Exception("Failed to testExecutionSeries");
        }
    }

    public void testExecutionBulk() throws Exception {
        resetAsyncLatch();

        mAppManager.installApps(getTestHIDs()).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(Task<Void> task) throws Exception {
                assertNull(task.getError());
                countDownAsyncLatch();
                return null;
            }
        }, ThreadManager.getInstance());

        if (!awaitForAsyncLatch(15000000)) {
            throw new Exception("Failed to testExecutionBulkSeries");
        }
    }
}
