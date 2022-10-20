package com.industry.sparkterminalclient.app;

import android.os.Environment;

import java.io.File;

/**
 * Created by Two on 2/4/2015.
 */
public class AppConstants {

    public final static String ENVIRONMENT_DIRECTORY = Environment.DIRECTORY_DOWNLOADS;

    public final static String APPS_DIRECTORY_NAME = "apps";
    public final static String APP_FILE_EXTENSION = ".apk";
    public final static String TEMP_UNZIP_DIRECTORY_NAME = "tempUnzip";


    public final static String ROOT_DIRECTORY = "/storage/emulated/0/";
    public final static String TEMP_UNZIP_DIRECTORY = new File(Environment.getExternalStoragePublicDirectory(
    ENVIRONMENT_DIRECTORY), TEMP_UNZIP_DIRECTORY_NAME).getPath();

    // FILE STATES
    public static final String APP_DOWNLOADING_FILE = "APP_DOWNLOADING_FILE";
    public static final String APP_INSTALLED_FILE = "APP_INSTALLED_FILE";
    public static final String APP_REVERT_FILE = "APP_REVERT_FILE";

}
