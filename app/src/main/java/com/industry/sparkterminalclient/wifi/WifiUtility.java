package com.industry.sparkterminalclient.wifi;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by Two on 2/6/2015.
 */
public class WifiUtility {
    private static final String TAG = WifiUtility.class.getName();


    public synchronized static boolean isWifiEnabled(Context context) {
        try {
            return ((android.net.wifi.WifiManager)context.getSystemService(Context.WIFI_SERVICE)).isWifiEnabled();
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    public synchronized static boolean isNetworkAvailable(Context context) {
        try {
            NetworkInfo activeNetworkInfo = ((ConnectivityManager)(context.getSystemService(Context.CONNECTIVITY_SERVICE))).getActiveNetworkInfo();
            return (activeNetworkInfo != null && activeNetworkInfo.isConnected());
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    public synchronized static boolean isConnectedViaWifi(Context context) {
        try {
            return ((ConnectivityManager)(context.getSystemService(Context.CONNECTIVITY_SERVICE)))
                    .getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    public synchronized static String getCurrentSSID(Context context) {
        try {
            return ((android.net.wifi.WifiManager)context.getSystemService(Context.WIFI_SERVICE))
                    .getConnectionInfo().getSSID();
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    public synchronized  static boolean isNetworkConnected(Context context, String ssid) {
        try {
            return getCurrentSSID(context).equals(ssid);
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }
}
