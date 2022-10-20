package com.industry.sparkterminalclient.state;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.industry.sparkterminalclient.R;
import com.industry.sparkterminalclient.event.EventConstants;
import com.industry.sparkterminalclient.event.EventManager;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Two on 1/13/2015.
 */
public class StateMonitor implements IStateMonitor {
    private static final String TAG = StateMonitor.class.getName();


    // Constants
    private static final int IDLE_CHECK_INTERVAL = 30000;


    // Dependencies
    Context mContext;
    WindowManager mWindowManager;
    LayoutInflater mLayoutInflater;
    EventManager mEventManager;


    // Members
    private Handler mInputMonitorHandler = new Handler(Looper.getMainLooper());
    private View mInputMonitorShim;
    private WindowManager.LayoutParams mOverlayLayoutParams;
    private AtomicBoolean mIsMonitoringState = new AtomicBoolean(false);
    private AtomicBoolean mHasReceivedInput = new AtomicBoolean(true);
    private AtomicLong mLastReceivedInput = new AtomicLong(new Date().getTime());


    // Singleton
    private static StateMonitor mInstance;
    public static StateMonitor getInstance(Context context) {
        if(mInstance == null) {
            mInstance = new StateMonitor(context);
        }
        return mInstance;
    }


    // Constructor
    private StateMonitor(Context context) {
        mContext = context;
        mWindowManager = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        mLayoutInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mEventManager = EventManager.getInstance(context);
    }


    // Methods


    public synchronized void startMonitor() {
        mIsMonitoringState.set(true);
        startIdleState();
        mInputMonitorHandler.post( new Runnable() {
            @Override
            public void run() {

                // Should we trigger the Idle state?
                long currentTime = new Date().getTime();
                if(mHasReceivedInput.get() && (currentTime - mLastReceivedInput.get()) > IDLE_CHECK_INTERVAL) {
                    startIdleState();
                }

                // Should we continue monitoring state?
                if(mIsMonitoringState.get()) {
                    mInputMonitorHandler.postDelayed(this, IDLE_CHECK_INTERVAL);
                }
            }
        });
    }

    public synchronized void stopMonitor() {
        mIsMonitoringState.set(false);
    }

    private View getShim() {
        if(mInputMonitorShim == null) {
            mOverlayLayoutParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    PixelFormat.TRANSPARENT);
            mInputMonitorShim = mLayoutInflater.inflate(R.layout.touch_detection, null);
            //mInputMonitorShim.setBackgroundColor(Color.GREEN);
            mInputMonitorShim.setOnTouchListener(mOnTouchListener);
        }
        return mInputMonitorShim;
    }

    private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            startBusyState();
            return false;
        }
    };

    private void displayShim() {
        mWindowManager.addView(getShim(), mOverlayLayoutParams);
    }

    private void hideShim() {
        mWindowManager.removeView(getShim());
    }


   public void startIdleState() {
        if(mHasReceivedInput.get()) {
            displayShim();
            mHasReceivedInput.set(false);
            mEventManager.emitEvent(EventConstants.EVENT_STATE_IDLE);
        }
    }

    public void startBusyState() {
        mLastReceivedInput.set(new Date().getTime());
        if (!mHasReceivedInput.get()) {
            hideShim();
            mHasReceivedInput.set(true);
            mEventManager.emitEvent(EventConstants.EVENT_STATE_BUSY);
        }
    }
}
