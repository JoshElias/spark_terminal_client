package com.industry.sparkterminalclient.app.toggle;

import java.util.ArrayList;
import java.util.Arrays;

import bolts.Task;

/**
 * Created by Two on 2/6/2015.
 */
public interface IAppToggler {
    public Task<Void> enableApp(String packageName);
    public Task<Void> disableApp(String packageName);
    public Task<Void> enableApps(ArrayList<String> packageNames);
    public Task<Void> disableApps(ArrayList<String> packageNames);
}
