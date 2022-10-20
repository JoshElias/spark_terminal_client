package com.industry.sparkterminalclient.wifi.connect;

import bolts.Task;

/**
 * Created by Two on 2/6/2015.
 */
public interface IWifiConnector {
    public Task<Void> connectToWifi(String ssid, String preSharedKey, String encryptionType);
}
