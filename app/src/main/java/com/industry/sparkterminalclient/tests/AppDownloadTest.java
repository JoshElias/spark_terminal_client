package com.industry.sparkterminalclient.tests;

import android.util.Log;

import com.industry.sparkterminalclient.Utility;
import com.industry.sparkterminalclient.app.SparkAppManager;
import com.industry.sparkterminalclient.tests.base.SparkClientTestCase;
import com.industry.sparkterminalclient.tests.base.IThreadSafeTest;
import com.industry.sparkterminalclient.tests.base.SparkTerminalTestData;
import com.industry.sparkterminalclient.thread.ThreadManager;

import java.util.HashMap;
import java.util.Map;

import bolts.Continuation;
import bolts.Task;

/**
 * Created by Two on 12/4/2014.
 */
public class AppDownloadTest extends SparkClientTestCase implements IThreadSafeTest {
    private final static String TAG = AppDownloadTest.class.getName();

    private SparkAppManager mAppManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mAppManager = SparkAppManager.getInstance(getContext());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        // Delete all the games we've just downloaded
        if(SparkAppManager.getSparkAPKDirectory().exists()) {
            SparkAppManager.getSparkAPKDirectory().delete();
        }
    }


    // Test Functions

    public void testExecutionSingle() throws Exception {
        Log.e(TAG, "Starting Single");
        resetAsyncLatch();
        Map.Entry<String, String> entry = SparkTerminalTestData.testPackageUrls.entrySet().iterator().next();
        mAppManager.downloadApp(entry.getKey(), entry.getValue()).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(Task<Void> task) throws Exception {
                if(task.isFaulted()) {
                    task.getError().printStackTrace();
                }
                assertNull(task.getError());
                countDownAsyncLatch();
                return null;
            }
        }, ThreadManager.getInstance());

        if(!awaitForAsyncLatch(10000)) {
            throw new Exception("Failed to testExecutionSingle");
        }
        Utility.recursiveDelete(SparkAppManager.getSparkAPKDirectory());

        Log.e(TAG, "Finishing Single");
    }

    public void testExecutionSeries() throws Exception {
        Log.e(TAG, "Starting Series");
        resetAsyncLatch();

        HashMap<String,String> hidDict = new HashMap<String, String>();
        int index = 0;
        for(Map.Entry<String,String> entry : SparkTerminalTestData.testPackageUrls.entrySet()) {
            hidDict.put(entry.getKey(), entry.getValue());
            if(++index > 5)
                break;
        }

        mAppManager.downloadApps(hidDict).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(Task<Void> task) throws Exception {
                if(task.isFaulted()) {
                    task.getError().printStackTrace();
                }
                assertNull(task.getError());
                countDownAsyncLatch();
                return null;
            }
        }, ThreadManager.getInstance());

        if(!awaitForAsyncLatch(150000)) {
            throw new Exception("Failed to testExecutionSeries");
        }
        Utility.recursiveDelete(SparkAppManager.getSparkAPKDirectory());
        Log.e(TAG, "Finishing Series");
    }

    public void testExecutionBulk() throws Exception {
        Log.e(TAG, "Starting Bulk Download");
        resetAsyncLatch();

        mAppManager.downloadApps(SparkTerminalTestData.testPackageUrls).continueWith(new Continuation<Void, Void>() {
            @Override
            public Void then(Task<Void> task) throws Exception {
                Log.e(TAG, "Fucking dooone");
                if (task.isFaulted()) {
                    task.getError().printStackTrace();
                }
                assertNull(task.getError());
                countDownAsyncLatch();
                return null;
            }
        }, ThreadManager.getInstance());

        if(!awaitForAsyncLatch(15000000)) {
            throw new Exception("Failed to testExecutionBulkParallel");
        }
        //Utility.recursiveDelete(SparkAppManager.getSparkAPKDirectory());
        Log.e(TAG, "Finishing Bulk Parallel");
    }
}
