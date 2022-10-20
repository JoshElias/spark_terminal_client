package com.industry.sparkterminalclient.tests.base;

import android.test.ServiceTestCase;
import android.util.Log;

import com.industry.sparkterminalclient.KnoxManager;
import com.industry.sparkterminalclient.activities.SparkClientService;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by Two on 12/4/2014.
 */
public class SparkClientTestCase extends ServiceTestCase<SparkClientService> {
    private static final String TAG = SparkClientTestCase.class.getName();

    private KnoxManager mKnox;
    private CountDownLatch mAsyncLatch = new CountDownLatch(1);

    public SparkClientTestCase() {
        super(SparkClientService.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mKnox = KnoxManager.getInstance(getContext());
        Thread.setDefaultUncaughtExceptionHandler( new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                Log.e(TAG, "Error in Spark Test Case");
                throwable.printStackTrace();
            }
        });
    }


    public boolean awaitForAsyncLatch() throws Exception {
        return awaitForAsyncLatch(5000);
    }
    public boolean awaitForAsyncLatch(long timeout) throws Exception {
        return mAsyncLatch.await(timeout, TimeUnit.MILLISECONDS);
    }

    public void countDownAsyncLatch() {
        mAsyncLatch.countDown();
    }
    public void resetAsyncLatch() {
        mAsyncLatch = new CountDownLatch(1);
    }
}