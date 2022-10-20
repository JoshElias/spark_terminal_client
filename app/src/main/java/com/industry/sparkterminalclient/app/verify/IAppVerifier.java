package com.industry.sparkterminalclient.app.verify;

import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;

import bolts.Continuation;
import bolts.Task;

/**
 * Created by Two on 2/6/2015.
 */
public interface IAppVerifier {
    public Task<Void> verifyApp(final File file, final String hid);
    public Task<Void> verifyApps(final HashMap<File, String> verifyAppDict);
}
