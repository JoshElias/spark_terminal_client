package com.industry.sparkterminalclient.app.stop;

import java.util.ArrayList;
import java.util.Arrays;

import bolts.Task;

/**
 * Created by Two on 2/6/2015.
 */
public interface IAppStopper {
    public Task<Void> stopApp(String packageName);
    public Task<Void> stopApps(ArrayList<String> packageNames);
}
