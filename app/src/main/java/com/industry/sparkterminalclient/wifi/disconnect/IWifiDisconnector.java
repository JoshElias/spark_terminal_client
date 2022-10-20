package com.industry.sparkterminalclient.wifi.disconnect;

import bolts.Task;

/**
 * Created by Two on 2/6/2015.
 */
public interface IWifiDisconnector {
    public Task<Void> disconnectFromWifi();
}
