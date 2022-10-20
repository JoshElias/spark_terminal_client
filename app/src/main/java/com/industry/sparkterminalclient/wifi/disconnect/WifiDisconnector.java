package com.industry.sparkterminalclient.wifi.disconnect;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.util.Log;

import com.industry.sparkterminalclient.Utility;
import com.industry.sparkterminalclient.event.EventConstants;
import com.industry.sparkterminalclient.event.EventManager;
import com.industry.sparkterminalclient.wifi.WifiConstants;
import com.industry.sparkterminalclient.wifi.WifiUtility;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import bolts.Task;

/**
 * Created by Two on 1/8/2015.
 */
public class WifiDisconnector implements IWifiDisconnector {
    private static final String TAG = WifiDisconnector.class.getName();


    // Constants
    private final static int DISCONNECT_WIFI_TIMEOUT = 15000;
    private final static int DISCONNECT_WIFI_INTERVAL = 200;


    // Dependencies
    private Context mContext;
    private WifiManager mWifiManager;
    private EventManager mEventManager;


    // Members
    private Object mDisconnectWifiLock = new Object();
    private Handler mDisconnectWifiHandler = new Handler(Looper.getMainLooper());
    private Task<Void>.TaskCompletionSource mDisconnectWifiTask;
    private AtomicLong mDisconnectStartTime = new AtomicLong();


    // Singleton
    private static WifiDisconnector mInstance;
    public static synchronized WifiDisconnector getInstance(Context context) {
        if(mInstance == null) {
            mInstance = new WifiDisconnector(context);
        }
        return mInstance;
    }


    // Constructor
    public WifiDisconnector(Context context) {
        mContext = context;
        mWifiManager = ((WifiManager)context.getSystemService(Context.WIFI_SERVICE));
        mEventManager = EventManager.getInstance(context);
    }


    // Methods
    public synchronized Task<Void> disconnectFromWifi() {
        final Task<Void>.TaskCompletionSource done = Task.create();
        try {
            Log.e(TAG, "Inside disconnect to wifi");
            synchronized(mDisconnectWifiLock) {

                // Finish off the last launch task if any
                finishWifiDisconnectTask(new Exception("Another disconnect was requested before this one could finish"));
                // Assign this new one
                mDisconnectWifiTask = done;

                // Are we even connected??
                if(!WifiUtility.isConnectedViaWifi(mContext)) {
                    Log.e(TAG, "Already disconnected");
                    finishWifiDisconnectTask();

                // Attempted to disconnect to wifi
                } else if(!attemptDisconnect()) {
                    Log.e(TAG, "Failed to attempt disconnect");
                    finishWifiDisconnectTask(new Exception("Unable to disconnect from current network"));

                    // Listen for the wifi connection
                } else {
                    Log.e(TAG, "Setting up for listening for disconnect");
                    listenForWifiDisconnect();
                }
            }

        } catch (Exception e) {
            finishWifiDisconnectTask(e);
        } finally {
            return done.getTask();
        }
    }

    private boolean attemptDisconnect() {
        return mWifiManager.disconnect();
    }

    private void listenForWifiDisconnect() {
        Log.e(TAG, "listening for wifi disconnect");

        // Start the timer
        mDisconnectStartTime.set(new Date().getTime());

        mDisconnectWifiHandler.post( new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized (mDisconnectWifiLock) {

                        Log.e(TAG, "Time since disconnect attempt: "+(new Date().getTime() - mDisconnectStartTime.get()));

                        // Check if launch timed out
                        if ((new Date().getTime() - mDisconnectStartTime.get()) > DISCONNECT_WIFI_TIMEOUT) {
                            throw new Exception("Disconnection timed out");

                        // Check if we're disconnected
                        } else if (!WifiUtility.isConnectedViaWifi(mContext)) {
                            Log.e(TAG, "WE'VE DISCONNECTED!");
                            mEventManager.emitEvent(EventConstants.EVENT_WIFI_DISCONNECT);
                            finishWifiDisconnectTask();

                            // Run the check again after interval
                        } else {
                            mDisconnectWifiHandler.postDelayed(this, DISCONNECT_WIFI_INTERVAL);
                        }
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Mistakes were made!");
                    finishWifiDisconnectTask(e);
                }
            }
        });
    }

    private void finishWifiDisconnectTask() {
        finishWifiDisconnectTask(null);
    }

    private void finishWifiDisconnectTask(Throwable t) {
        Log.e(TAG, "finishing Wifi task");
        Utility.finishTask(mDisconnectWifiTask, t);
    }
}
