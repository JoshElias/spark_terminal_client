package com.industry.sparkterminalclient.app.install;

import java.io.File;
import java.util.ArrayList;

import bolts.Task;

/**
 * Created by Two on 2/6/2015.
 */
public interface IAppInstaller {
    public Task<Void> installApps(ArrayList<File> files);
    public Task<Void> installApp(File file);
}
