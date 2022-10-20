package com.industry.sparkterminalclient.app.uninstall;

import com.industry.sparkterminalclient.Utility;

import java.util.ArrayList;

import bolts.Task;

/**
 * Created by Two on 2/6/2015.
 */
public interface IAppUninstaller {
    public Task<Void> uninstallApp(String packageName);
    public Task<Void> uninstallApps(ArrayList<String> packageNames);
}
