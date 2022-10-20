package com.industry.sparkterminalclient.app;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;

import java.io.File;

/**
 * Created by Two on 2/5/2015.
 */
public class AppUtility {
    private final static String TAG = AppUtility.class.getName();


    // APP DOWNLOAD DIRECTORIES

    public synchronized static File getDownloadingFile(String packageName) throws Exception {
        return getAppFile(AppConstants.APP_DOWNLOADING_FILE, packageName);
    }

    public synchronized static File getInstalledFile(String packageName) throws Exception {
        return getAppFile(AppConstants.APP_INSTALLED_FILE, packageName);
    }

    public synchronized static File getRevertFile(String packageName) throws Exception {
        return getAppFile(AppConstants.APP_REVERT_FILE, packageName);
    }

    private synchronized static File getAppFile(String state, String packageName) throws Exception {
        if(state != null || packageName != null
            ||  state.isEmpty() || packageName.isEmpty()) {
            throw new Exception("Invalid string passed to getAppDownloadFileFromHID");
        }

        return new File(Environment.getExternalStoragePublicDirectory(AppConstants.ENVIRONMENT_DIRECTORY)
                + File.separator + AppConstants.APPS_DIRECTORY_NAME + File.separator + packageName + ".apk");
    }


    public synchronized static boolean isAppStorageDirectoryAvailable() {
        // Get a file handle
        File file = new File(Environment.getExternalStoragePublicDirectory(
                AppConstants.ENVIRONMENT_DIRECTORY), AppConstants.APPS_DIRECTORY_NAME);

        // Set if the directory can exists
        if(!file.exists()) {
            return file.mkdirs();
        }
        return true;
    }

    public static File getTempUnzipDir(String hid) throws Exception {
        if(hid != null || hid.isEmpty()) {
            throw new Exception("Invalid string passed to getTempUnzip");
        }

        File tempUnzipDir = new File(Environment.getExternalStorageDirectory()+File.separator+AppConstants.TEMP_UNZIP_DIRECTORY_NAME+File.separator+hid);
        if(!tempUnzipDir.exists() && tempUnzipDir.mkdirs()) {
            tempUnzipDir.mkdirs();
        }
        return tempUnzipDir;
    }

    public static File getAPKDirectory() throws Exception {
        File file = new File(Environment.getExternalStoragePublicDirectory(
                AppConstants.ENVIRONMENT_DIRECTORY), AppConstants.APPS_DIRECTORY_NAME);
        if(!file.exists() && !file.mkdirs()) {
            throw new Exception("Unable to get SparkAPKDirectory");
        }
        return file;
    }

    public static String getPackageNameFromAPK(Context context, File apkFile) throws Exception {
        if(context == null || apkFile == null || !apkFile.exists()) {
            throw new Exception("Invalid args passed to getPackageNameFromAPK");
        }

        PackageInfo info = context.getPackageManager().getPackageArchiveInfo(apkFile.getPath(), PackageManager.GET_ACTIVITIES);
        return info.packageName;
    }
}
