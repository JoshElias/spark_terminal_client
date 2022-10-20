package com.industry.sparkterminalclient.app.download;

import com.industry.sparkterminalclient.Utility;

import java.io.File;
import java.util.HashMap;

import bolts.Task;

/**
 * Created by Two on 2/6/2015.
 */
public interface IAppDownloader {
    public Task<Void> downloadApp(final File destinationFile, final String apkUrl);
    public Task<Void> downloadApps(final HashMap<File, String> downloadRequestDict);
}
