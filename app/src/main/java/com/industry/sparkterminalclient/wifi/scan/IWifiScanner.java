package com.industry.sparkterminalclient.wifi.scan;

import android.net.wifi.ScanResult;

import org.json.JSONArray;

import java.util.List;

import bolts.Continuation;
import bolts.Task;

/**
 * Created by Two on 2/6/2015.
 */
public interface IWifiScanner {
    public Task<JSONArray> getAvailableWifiNetworksJSON();
    public Task<List<ScanResult>> getAvailableWifiNetworks();
}
