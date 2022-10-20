package com.industry.sparkterminalclient.wifi.scan;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.industry.sparkterminalclient.Utility;
import com.industry.sparkterminalclient.event.EventConstants;
import com.industry.sparkterminalclient.event.EventManager;
import com.industry.sparkterminalclient.wifi.WifiConstants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import bolts.Continuation;
import bolts.Task;

/**
 * Created by Two on 1/27/2015.
 */
public class WifiScanner implements IWifiScanner {
    private static final String TAG = WifiScanner.class.getName();


    // Constants
    private final static int NUM_WIFI_LEVELS = 4;


    // Dependencies
    private Context mContext;
    private WifiManager mWifiManager;
    private EventManager mEventManager;


    // Members
    AvailableWifiReceiver mAvailableWifiReceiver;


    // Inner Classes
    public AvailableWifiReceiver getAvailableWifiReceiver() {
        if(mAvailableWifiReceiver == null) {
            mAvailableWifiReceiver = new AvailableWifiReceiver();
        }
        return mAvailableWifiReceiver;
    }


    class AvailableWifiReceiver extends BroadcastReceiver {
        public Task<List<ScanResult>>.TaskCompletionSource task;

        @SuppressLint("UseValueOf")
        @Override
        public void onReceive(Context context, Intent intent) {
            List<ScanResult> scanResults = mWifiManager.getScanResults();
            task.setResult(scanResults);
            mEventManager.emitEvent(EventConstants.EVENT_WIFI_SCAN, mWifiManager.getScanResults());
            mContext.unregisterReceiver(mAvailableWifiReceiver);
        }
    };


    // Singleton
    private static WifiScanner mInstance;
    public static synchronized WifiScanner getInstance(Context context) {
        if(mInstance == null ) {
            mInstance = new WifiScanner(context);
        }
        return mInstance;
    }


    // Constructor
    private WifiScanner(Context context) {
        mContext = context;
        mWifiManager = ((WifiManager)context.getSystemService(Context.WIFI_SERVICE));
        mEventManager = EventManager.getInstance(context);
    }


    // Methods
    public Task<JSONArray> getAvailableWifiNetworksJSON() {
        final Task<JSONArray>.TaskCompletionSource done = Task.create();
        getAvailableWifiNetworks().continueWith(new Continuation<List<ScanResult>, Object>() {
            @Override
            public Object then(Task<List<ScanResult>> task) throws Exception {
                if(task.isFaulted()) {
                    done.setError(task.getError());
                } else {
                    try {
                        done.setResult(ScanResultsToJSONArray(task.getResult()));
                    } catch(Throwable t) {
                        done.setError(new Exception(t));
                    }
                }
                return null;
            }
        });
        return done.getTask();
    }

    public Task<List<ScanResult>> getAvailableWifiNetworks() {
        final Task<List<ScanResult>>.TaskCompletionSource done = Task.create();
        try {
            // Set up filter
            IntentFilter wifiFilter = new IntentFilter();
            wifiFilter.addAction(android.net.wifi.WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

            // Hand off the task to be finished by the available wifi event
            getAvailableWifiReceiver().task = done;

            // Scan for networks
            mContext.registerReceiver(getAvailableWifiReceiver(), wifiFilter);
            mWifiManager.startScan();

        } catch (Throwable t) {
            Utility.finishTask(done, t);
        } finally {
            return done.getTask();
        }
    }

    private JSONObject ScanResultToJSONObject(ScanResult scanResult) throws Throwable {
        JSONObject jsonScanResult = new JSONObject();
        jsonScanResult.put("SSID", scanResult.SSID);
        jsonScanResult.put("encryptionType",  getEncryptionTypeFromCapabilities(scanResult.capabilities));
        jsonScanResult.put("level", android.net.wifi.WifiManager.calculateSignalLevel(scanResult.level, NUM_WIFI_LEVELS));
        return jsonScanResult;
    }

    private JSONArray ScanResultsToJSONArray(List<ScanResult> scanResults) throws Throwable{
        JSONArray jsonScanObjects = new JSONArray();
        for(ScanResult scanResult : scanResults) {
            jsonScanObjects.put(ScanResultToJSONObject(scanResult));
        }
        return jsonScanObjects;
    }

    private String getEncryptionTypeFromCapabilities(String capabilities) {
        String rawEncryptionType = capabilities.substring(1, 4);
        if(rawEncryptionType.equals("ESS")) {
            return WifiConstants.WIFI_ENCRYPTION_OPEN;
        } else if(rawEncryptionType.equals("WEP")) {
            return WifiConstants.WIFI_ENCRYPTION_WEP;
        } else if(rawEncryptionType.equals("WPA")) {
            return WifiConstants.WIFI_ENCRYPTION_WPA;
        } else {
            return WifiConstants.WIFI_ENCRYPTION_UNKNOWN;
        }
    }
}
