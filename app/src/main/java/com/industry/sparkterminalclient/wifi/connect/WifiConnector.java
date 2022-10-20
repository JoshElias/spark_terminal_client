package com.industry.sparkterminalclient.wifi.connect;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.industry.sparkterminalclient.Utility;
import com.industry.sparkterminalclient.app.start.AppStarter;
import com.industry.sparkterminalclient.event.EventConstants;
import com.industry.sparkterminalclient.event.EventManager;
import com.industry.sparkterminalclient.wifi.WifiConstants;
import com.industry.sparkterminalclient.wifi.WifiUtility;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import bolts.Task;

/**
 * Created by Two on 1/8/2015.
 */
public class WifiConnector implements IWifiConnector {
    private static final String TAG = AppStarter.class.getName();


    // Constants
    private final static int CONNECT_WIFI_TIMEOUT = 15000;
    private final static int CONNECT_WIFI_INTERVAL = 200;


    // Dependencies
    private Context mContext;
    private WifiManager mWifiManager;
    private EventManager mEventManager;


    // Members
    private Object mConnectWifiLock = new Object();
    private Handler mConnectWifiHandler = new Handler(Looper.getMainLooper());
    private Task<Void>.TaskCompletionSource mConnectWifiTask;
    private AtomicLong mConnectStartTime = new AtomicLong();


    // Singleton
    private static WifiConnector mInstance;
    public static synchronized WifiConnector getInstance(Context context) {
        if(mInstance == null) {
            mInstance = new WifiConnector(context);
        }
        return mInstance;
    }


    // Constructor
    private WifiConnector(Context context) {
        mContext = context;
        mWifiManager = ((WifiManager)context.getSystemService(Context.WIFI_SERVICE));
        mEventManager = EventManager.getInstance(context);
    }


    // Methods
    public synchronized Task<Void> connectToWifi(String ssid, String preSharedKey, String encryptionType) {
        final Task<Void>.TaskCompletionSource done = Task.create();
        try {
            synchronized(mConnectWifiLock) {
                // Verify Inputs
                if(ssid == null || encryptionType == null || encryptionType.isEmpty()) {
                    throw new Exception("Invalid args passed to connectToWifi");
                }

                // Finish off the last launch task if any
                finishWifiConnectTask(new Exception("Another network was requested before this one could connect"));
                // Assign this new one
                mConnectWifiTask = done;

                // Add quotes to ssid and password because Android is dumb
                ssid = "\""+ssid+"\"";
                preSharedKey = "\""+preSharedKey+"\"";

                // Is this ssid already connected?
                if(WifiUtility.isNetworkConnected(mContext, ssid)) {
                    finishWifiConnectTask();

                // Attempted to connect to wifi
                } else if(!attemptToConnect(ssid, preSharedKey, encryptionType)) {
                    finishWifiConnectTask(new Exception("Unable to connect"));

                // Listen for the wifi connection
                } else {
                    listenForWifiConnection(ssid);
                }
            }

        } catch (Exception e) {
            finishWifiConnectTask(e);
        } finally {
            return done.getTask();
        }
    }

    private boolean attemptToConnect(String ssid, String preSharedKey, String encryptionType) {
        if( ssid == null || preSharedKey == null || encryptionType == null) {
            Log.e(TAG, "Invalid args passed to attempt to connect");
            return false;
        }

        Log.e(TAG, "SSID: "+ssid);
        Log.e(TAG, "PreShared Key: "+preSharedKey);
        Log.e(TAG, "Encryption Type: "+encryptionType);


        // Build Wifi Config
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = ssid;

        // Customize based on encryption type
        if(encryptionType.equals(WifiConstants.WIFI_ENCRYPTION_OPEN)) {
            Log.e(TAG, "Configuring OPEN Wifi config");
            wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

        } else if(encryptionType.equals(WifiConstants.WIFI_ENCRYPTION_WEP)) {
            Log.e(TAG, "Configuring WEP Wifi config");
            wifiConfig.wepKeys[0] = preSharedKey;
            wifiConfig.wepTxKeyIndex = 0;
            wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);

        } else if(encryptionType.equals(WifiConstants.WIFI_ENCRYPTION_WPA)) {
            Log.e(TAG, "Configuring WPA Wifi config");
            wifiConfig.preSharedKey = preSharedKey;

        } else {
            Log.e(TAG, "Invalid encrpytion type passed to attempt to connect");
            return false;
        }

        // If we're connected, disconnect
        if(WifiUtility.isConnectedViaWifi(mContext) && !mWifiManager.disconnect()) {
            Log.e(TAG, "Unable to disconnect from previous network :(");
            return false;
        }

        // Remove all prior networks
        List<WifiConfiguration> list = mWifiManager.getConfiguredNetworks();
        for( WifiConfiguration i : list ) {
            mWifiManager.removeNetwork(i.networkId);
        }
        int netId = mWifiManager.addNetwork(wifiConfig);

       return (mWifiManager.enableNetwork(netId, false)
                && mWifiManager.reconnect());
    }


    private void listenForWifiConnection(final String ssid) {
        Log.e(TAG, "listening for wifi connection");

        // Start the timer
        mConnectStartTime.set(new Date().getTime());

        mConnectWifiHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized (mConnectWifiLock) {

                        // Check if launch timed out
                        if ((new Date().getTime() - mConnectStartTime.get()) > CONNECT_WIFI_TIMEOUT) {
                            throw new Exception("New Connection timed out");

                            // Check if network is connected yet
                        } else if (WifiUtility.isNetworkConnected(mContext, ssid)) {
                            Log.e(TAG, "WE ARE CONNECTED!");
                            mEventManager.emitEvent(EventConstants.EVENT_WIFI_CONNECT, ssid);
                            finishWifiConnectTask();

                            // Run the check again after interval
                        } else {
                            mConnectWifiHandler.postDelayed(this, CONNECT_WIFI_INTERVAL);
                        }
                    }

                } catch (Exception e) {
                    finishWifiConnectTask(e);
                }
            }
        });
    }

    private void finishWifiConnectTask() {
        finishWifiConnectTask(null);
    }

    private void finishWifiConnectTask(Throwable t) {
        Log.e(TAG, "finishing Wifi task");
        Utility.finishTask(mConnectWifiTask, t);
    }
}
