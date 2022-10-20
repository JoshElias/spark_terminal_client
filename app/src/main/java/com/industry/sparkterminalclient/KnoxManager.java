package com.industry.sparkterminalclient;

import android.app.Activity;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.app.enterprise.AppControlInfo;
import android.app.enterprise.ApplicationPolicy;
import android.app.enterprise.DeviceInventory;
import android.app.enterprise.EnterpriseDeviceManager;
import android.app.enterprise.NetworkStats;
import android.app.enterprise.RestrictionPolicy;
import android.app.enterprise.SecurityPolicy;
import android.app.enterprise.kioskmode.KioskMode;
import android.app.enterprise.license.EnterpriseLicenseManager;
import android.app.enterprise.multiuser.MultiUserManager;
import android.app.enterprise.remotecontrol.RemoteInjection;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpPost;
import com.koushikdutta.async.http.AsyncHttpResponse;

import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutionException;

import bolts.Continuation;
import bolts.Task;

/**
 * Created by Two on 10/24/2014.
 */
public class KnoxManager {
    private static final String TAG = "KnoxManager";

    private final static String NODE_SERVER = "http://192.168.2.66:3000/";
    public static final int RESULT_ADMIN_ENABLE = 666;
    private static final String RESULT_LICENSE_STATUS = "edm.intent.extra.license.status";
    private static final String RESULT_LICENSE_FAILED = "fail";
    private static final String RESULT_LICENSE_SUCCESS = "success";

    // Knox Vars
    private EnterpriseLicenseManager mEnterpriseLicenseManager;
    private ComponentName mDeviceAdmin;
    private DevicePolicyManager mDevicePolicyManager;
    private EnterpriseDeviceManager mEnterpriseDeviceManager;
    private DeviceInventory mDeviceInventory;
    private ApplicationPolicy mApplicationPolicy;
    private KioskMode mKioskMode;
    private RestrictionPolicy mRestrictionPolicy;
    private MultiUserManager mMultiUserManager;
    private RemoteInjection mRemoteInjection;

    // Task Vars
    static Task<Void>.TaskCompletionSource mEnabledAdminTask;
    static Task<String>.TaskCompletionSource mActivatedLicenseTask;

    static String mKnoxKey;



    /**
     * SINGLETON
     */

    private static KnoxManager mInstance;

    public static KnoxManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new KnoxManager(context);
        }
        return mInstance;
    }

    private KnoxManager(Context context) {
        mEnterpriseLicenseManager = EnterpriseLicenseManager.getInstance(context);
        mDeviceAdmin = new ComponentName(context, KnoxManager.AndroidDeviceAdminReceiver.class);
        mDevicePolicyManager = (DevicePolicyManager)context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        mEnterpriseDeviceManager = new EnterpriseDeviceManager(context);
        mDeviceInventory = mEnterpriseDeviceManager.getDeviceInventory();
        mApplicationPolicy = mEnterpriseDeviceManager.getApplicationPolicy();
        mKioskMode = KioskMode.getInstance(context);
        mRestrictionPolicy = mEnterpriseDeviceManager.getRestrictionPolicy();
        mMultiUserManager = MultiUserManager.getInstance(context);
        mRemoteInjection = RemoteInjection.getInstance();
    }


    /**
     * INNER KNOX CLASSES
     */

    public static class AndroidDeviceAdminReceiver extends DeviceAdminReceiver {

        @Override
        public void onEnabled(Context context, Intent intent) {
            Log.d(TAG, "Device Admin Receiver onEnabled fired");
        }

        @Override
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            Log.d(TAG, "DeviceAdminReceiver onReceive fired");
        }

        @Override
        public CharSequence onDisableRequested(Context context, Intent intent) {
            Log.d(TAG, "This is an optional message to warn the user about disabling.");
            return "This is an optional message to warn the user about disabling.";
        }

        @Override
        public void onDisabled(Context context, Intent intent) {
            Log.d(TAG, "Sample Device Admin: disabled");
        }

        @Override
        public void onPasswordChanged(Context context, Intent intent) {
            Log.d(TAG, "Sample Device Admin: pw changed");
        }

        @Override
        public void onPasswordFailed(Context context, Intent intent) {
            Log.d(TAG, "Sample Device Admin: pw failed");
        }

        @Override
        public void onPasswordSucceeded(Context context, Intent intent) {
            Log.d(TAG, "Sample Device Admin: pw succeeded");
        }
    }

    public static void onAdminActivityResult(int resultCode, Intent data) {
        Log.d(TAG, "RESULT ADMIN ENABLED RETURNED: "+resultCode);
        if (mEnabledAdminTask != null) {
            if (resultCode == Activity.RESULT_OK) {
                mEnabledAdminTask.setResult(null);
            } else {
                mEnabledAdminTask.setError(new Exception("User declined to activate admin"));
            }
        }
    }

    public static class KnoxAdminReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Knox Admin Receiver onReceiver fired");
            if (mActivatedLicenseTask != null) {
                if(intent.getStringExtra(RESULT_LICENSE_STATUS).equals(RESULT_LICENSE_SUCCESS)) {
                    mActivatedLicenseTask.setResult(mKnoxKey);
                    mKnoxKey = null;
                } else {
                    mActivatedLicenseTask.setError(new Exception("User declined to activate knox license"));
                }
            }
        }
    }


    /**
     * KNOX FUNCTIONS
     */

    public boolean isLicenseActive() {
        try {
            mApplicationPolicy.disableAndroidMarket();
            mApplicationPolicy.enableAndroidMarket();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isAdminActive() {
        return mEnterpriseDeviceManager.isAdminActive(mDeviceAdmin);
    }

    public Task<String> activateKnox(Activity activity) {
        final Task<String>.TaskCompletionSource done = Task.create();
        try {

            if( mActivatedLicenseTask != null ) {
                done.setError(new Exception("Knox is already trying to activate"));
                return done.getTask();
            }

            activateLicense(activity).continueWith(new Continuation<String, Void>() {
                public Void then(Task<String> task) throws Exception {
                    if (task.isFaulted()) {
                        done.setError(task.getError());
                        mActivatedLicenseTask = null;
                    } else {
                        done.setResult(task.getResult());
                        mActivatedLicenseTask = null;
                    }
                    return null;
                }
            });
        } catch( Exception e) {
            done.setError(e);
        } finally {
            return done.getTask();
        }
    }

    public Task<String> activateKnox(final String knoxKey, Activity activity) {
        final Task<String>.TaskCompletionSource done = Task.create();
        try {

            if( mActivatedLicenseTask != null ) {
                done.setError(new Exception("Knox is already trying to activate"));
                return done.getTask();
            }

            activateLicense(knoxKey, activity).continueWith(new Continuation<String, Void>() {
                public Void then(Task<String> task) throws Exception {
                    if (task.isFaulted()) {
                        done.setError(task.getError());
                        mActivatedLicenseTask = null;
                    } else {
                        done.setResult(task.getResult());
                        mActivatedLicenseTask = null;
                    }
                    return null;
                }
            });
        } catch( Exception e) {
            done.setError(e);
        } finally {
            return done.getTask();
        }
    }

    private Task<String> getKnoxKey() {
        final Task<String>.TaskCompletionSource done = Task.create();
        try {
            AsyncHttpPost post = new AsyncHttpPost(NODE_SERVER+"loader/knoxKey");
            AsyncHttpClient.getDefaultInstance().executeJSONObject(post, new AsyncHttpClient.JSONObjectCallback() {
                @Override
                public void onCompleted(Exception e, AsyncHttpResponse response, JSONObject result) {
                    if (e != null || result.has("err")) {
                        done.setError(new Exception("Call to server failed"));
                    } else {
                        try {
                            done.setResult(result.getString("result"));
                        } catch( Exception err ) {
                            done.setError(new Exception("Server responded with no result"));
                        }
                    }
                }
            });
        } catch( Exception e) {
            done.setError(e);
        } finally {
            return done.getTask();
        }
    }

    private Task<Void> enableAdmin(Activity activity) {
        mEnabledAdminTask = Task.create();
        try {

            if (isLicenseActive()) {
                mEnabledAdminTask.setResult(null);
                return mEnabledAdminTask.getTask();
            }

            if (!isAdminActive()) {
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mDeviceAdmin);
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Additional text explaining why this needs to be added.");
                activity.startActivityForResult(intent, RESULT_ADMIN_ENABLE);
            } else {
                mEnabledAdminTask.setResult(null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while trying to enable admin");
            e.printStackTrace();
            mEnabledAdminTask.setError(e);
        } finally {
            return mEnabledAdminTask.getTask();
        }
    }

    private Task<String> activateLicense(Activity activity) {
        mActivatedLicenseTask = Task.create();
        try {
            enableAdmin(activity).onSuccessTask(new Continuation<Void, Task<String>>() {
                public Task<String> then(Task<Void> task) throws Exception {
                    return getKnoxKey();
                }
            }).continueWith(new Continuation<String, Void>() {
                public Void then(Task<String> task) throws Exception {
                    if (task.isFaulted()) {
                        mActivatedLicenseTask.setError(task.getError());
                    } else {
                        Log.d(TAG, "Admin is active. Activating license.");
                        mKnoxKey = task.getResult();
                        mEnterpriseLicenseManager.activateLicense(mKnoxKey);
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error while trying to activating License");
            mActivatedLicenseTask.setError(e);
        } finally {
            return mActivatedLicenseTask.getTask();
        }
    }

    private Task<String> activateLicense(final String knoxKey, Activity activity) {
        mActivatedLicenseTask = Task.create();
        try {

            if (isLicenseActive()) {
                mActivatedLicenseTask.setResult(knoxKey);
                return mActivatedLicenseTask.getTask();
            }

            enableAdmin(activity).continueWith(new Continuation<Void, Void>() {
                public Void then(Task<Void> task) throws Exception {
                    if (task.isFaulted()) {
                        mActivatedLicenseTask.setError(task.getError());
                    } else {
                        Log.d(TAG, "Admin is active. Activating license.");
                        mEnterpriseLicenseManager.activateLicense(knoxKey);
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error while trying to activating License");
            mActivatedLicenseTask.setError(e);
        } finally {
            return mActivatedLicenseTask.getTask();
        }
    }

    public Task<Void> disableKnox() {
        Task<Void>.TaskCompletionSource done = Task.create();
        try {
            if ( mDevicePolicyManager.isAdminActive(mDeviceAdmin) ) {
                mDevicePolicyManager.removeActiveAdmin(mDeviceAdmin);
            }
            done.setResult(null);

        } catch (Exception e) {
            Log.e(TAG, "Error while trying to disable admin");
            done.setError(e);
        } finally {
            return done.getTask();
        }
    }


    /**
     *  UTILITY FUNCS
     */


    // APPLICATION METHODS

    public boolean installApplication(final String apkFilePath, boolean keepDataAndCache) {
        try {
            return mApplicationPolicy.installApplication(apkFilePath, keepDataAndCache);
        } catch(Exception e) {
            Log.e(TAG, "Knox unable to install app: "+e);
            return false;
        }
    }

    public boolean uninstallApplication(final String packageName, boolean keepDataAndCache) {
        try {
            return mApplicationPolicy.uninstallApplication(packageName, keepDataAndCache);
        } catch(Exception e) {
            Log.e(TAG, "Knox unable to uninstall app: "+e);
            return false;
        }
    }

    public boolean updateApplication(final String apkFilePath) throws SecurityException {
        try {
            return mApplicationPolicy.updateApplication(apkFilePath);
        } catch(Exception e) {
            Log.e(TAG, "Knox unable to update application");
            return false;
        }
    }

    public boolean isApplicationInstalled(final String packageName) {
        return mApplicationPolicy.isApplicationInstalled(packageName);
    }

    public boolean isApplicationRunning(final String packageName) {
        return mApplicationPolicy.isApplicationRunning(packageName);
    }

    public boolean stopApp(String packageName) {
        try {
            return mApplicationPolicy.stopApp(packageName);
        } catch (SecurityException e) {
            Log.e(TAG, "Knox is unable to enableApp: " + e);
            return false;
        }
    }

    public boolean startApp(String packageName, String className) {
        try {
            return mApplicationPolicy.startApp(packageName, className);
        } catch (SecurityException e) {
            Log.e(TAG, "Knox is unable to enableApp: " + e);
            return false;
        }
    }

    public boolean wipeAppData(String packageName) {
        try {
           return mApplicationPolicy.wipeApplicationData(packageName);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to wipe app data: "+e);
            return false;
        }
    }

    public boolean removePackagesFromPreventStartBlackList(List<String> packageList) {
        try {
            return mApplicationPolicy.removePackagesFromPreventStartBlackList(packageList);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to remove packages from prevent start blacklist: "+e);
            return false;
        }
    }

    public List<String> addPackagesToPreventStartBlackList(List<String> packageList) throws Exception {
        try {
            return mApplicationPolicy.addPackagesToPreventStartBlackList(packageList);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to add packages to prevent start blacklist: "+e);
            throw e;
        }
    }

    public List<AppControlInfo> getAppPackageNamesAllBlackLists() throws Exception {
        try {AppControlInfo e = new AppControlInfo();
            return mApplicationPolicy.getAppPackageNamesAllBlackLists();

        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to add packages to prevent start blacklist: "+e);
            throw e;
        }
    }

    public boolean clearAppPackageNameFromList() throws Exception {
        try {
            return mApplicationPolicy.clearAppPackageNameFromList();
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to add packages to prevent start blacklist: "+e);
            throw e;
        }
    }

    public List<AppControlInfo> getAppPackageNamesAllWhiteLists() throws Exception {
        try {AppControlInfo e = new AppControlInfo();
            return mApplicationPolicy.getAppPackageNamesAllWhiteLists();

        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to add packages to prevent start blacklist: "+e);
            throw e;
        }
    }

    public boolean setDisableApplication(String packageName) {
        try {
            return mApplicationPolicy.setDisableApplication(packageName);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to set disable application: "+e);
            return false;
        }
    }

    public boolean setEnableApplication(String packageName) {
        try {
            return mApplicationPolicy.setEnableApplication(packageName);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to set enable application: "+e);
            return false;
        }
    }

    public boolean getApplicationStateEnabled(String packageName) {
        return mApplicationPolicy.getApplicationStateEnabled(packageName);
    }

    public String[] getInstalledApplicationsIDList() throws Exception {
        try {
            return mApplicationPolicy.getInstalledApplicationsIDList();
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to get installed application id list: "+e);
            throw e;
        }
    }

    public boolean addAppPackageNameToBlackList(String packageNamne) {
        try {
            return mApplicationPolicy.addAppPackageNameToBlackList(packageNamne);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to add app package name to black list: "+e);
            return false;
        }
    }

    public boolean removeAppPackageNameFromBlackList(String packageName) {
        try {
            return mApplicationPolicy.removeAppPackageNameFromBlackList(packageName);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to remove app package name from black list: "+e);
            return false;
        }
    }

    public boolean addAppPackageNameToWhiteList(String packageName) {
        try {
            return mApplicationPolicy.addAppPackageNameToWhiteList(packageName);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to add app package name to white list: "+e);
            return false;
        }
    }

    public boolean removeAppPackageNameFromWhiteList(String packageName) {
        try {
            return mApplicationPolicy.removeAppPackageNameFromWhiteList(packageName);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to remove app package name from whitelist: "+e);
            return false;
        }
    }

    public boolean enableAndroidMarket() {
        try {
            mEnterpriseDeviceManager.getApplicationPolicy().enableAndroidMarket();
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean disableAndroidMarket() {
        try {
            mEnterpriseDeviceManager.getApplicationPolicy().disableAndroidMarket();
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String[] getApplicationStateList(boolean state) {
        try {
            return mApplicationPolicy.getApplicationStateList(state);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to get the list of apps: "+e);
            return null;
        }
    }

    public Long getApplicationCodeSize(String packageName) {
        try {
            return mApplicationPolicy.getApplicationCodeSize(packageName);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to determine the code size of " + packageName + ": "+e);
            return null;
        }
    }

    public List<NetworkStats> getApplicationNetworkStats(){
        try{
            return mApplicationPolicy.getApplicationNetworkStats();
        } catch (Exception e){
            Log.e(TAG, "Knox is unable to get a list of NetworkStats: "+e);
            return null;
        }
    }




    // DEVICE METHODS

    public String getDeviceMaker() {
        return mDeviceInventory.getDeviceMaker();
    }

    public String getDeviceName() {
        return mDeviceInventory.getDeviceName();
    }

    public String getDeviceOS() {
        return mDeviceInventory.getDeviceOS();
    }

    public String getDeviceOSVersion() {
        return mDeviceInventory.getDeviceOSVersion();
    }

    public String getDevicePlatform() {
        return mDeviceInventory.getDevicePlatform();
    }

    public String getModelName() {
        return mDeviceInventory.getModelName();
    }

    public String getModelNumber() {
        return mDeviceInventory.getModelNumber();
    }

    public String getModemFirmware() {
        return mDeviceInventory.getModemFirmware();
    }

    public String getPlatformVersion() {
        return mDeviceInventory.getPlatformVersion();
    }

    public String getPlatformVersionName() {
        return mDeviceInventory.getPlatformVersionName();
    }

    public String getSerialNumber() {
        return mDeviceInventory.getSerialNumber();
    }

    public void resetDataUsage() {
        try{
            mDeviceInventory.resetDataUsage();
        } catch (SecurityException se) {
            Log.e(TAG, "Security Exception : " + se);
        }
    }



    // KIOSK METHODS

    public List<Integer> allowHardwareKeys(List<Integer> hwKeyId, boolean allow) throws Exception {
        try {
            return mKioskMode.allowHardwareKeys(hwKeyId, allow);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to allow hardware keys: "+e);
            throw e;
        }
    }

    public boolean allowTaskManager(boolean allow) {
        try {
            return mKioskMode.allowTaskManager(allow);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to allow task manager: "+e);
            return false;
        }
    }

    public boolean hideSystemBar (boolean hide) {
        try {
            return mKioskMode.hideSystemBar(hide);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to hide system bar: "+e);
            return false;
        }
    }

    public boolean hideNavigationBar(boolean hide) {
        try {
            return mKioskMode.hideNavigationBar(hide);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to hide navigation bar: "+e);
            return false;
        }
    }

    public boolean allowMultiWindowMode(boolean allow) {
        try {
            return mKioskMode.allowMultiWindowMode(allow);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to allow multi window mode: "+e);
            return false;
        }
    }

    public boolean wipeRecentTasks() {
        try {
            return mKioskMode.wipeRecentTasks();
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to wipe recent tasks: "+e);
            return false;
        }
    }

    public boolean enableKioskMode(String packageName) {
        try {
            mKioskMode.enableKioskMode(packageName);
            return true;
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to enable Kiosk Mode");
            return false;
        }
    }

    public void disableKioskMode(){
        try {
            mKioskMode.disableKioskMode();
        } catch (SecurityException se) {
            Log.e(TAG, "Security Exception : " + se);
        }
    }



    // Restriction Policy

    public boolean allowAndroidBeam(boolean allow) {
        try {
            return mRestrictionPolicy.allowAndroidBeam(allow);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to allow android beam: "+e);
            return false;
        }
    }

    public boolean allowAudioRecord(boolean allow) {
        try {
            return mRestrictionPolicy.allowAudioRecord(allow);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to allow audio record: "+e);
            return false;
        }
    }

    public boolean allowBluetooth(boolean allow) {
        try {
            return mRestrictionPolicy.allowBluetooth(allow);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to allow bluetooth: "+e);
            return false;
        }
    }

    public boolean allowFactoryReset(boolean allow) {
        try {
            return mRestrictionPolicy.allowFactoryReset(allow);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to allow factory reset: "+e);
            return false;
        }
    }

    public boolean allowGoogleAccountsAutoSync(boolean allow) {
        try {
            return mRestrictionPolicy.allowGoogleAccountsAutoSync(allow);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to allow google accounts auto sync: "+e);
            return false;
        }
    }


    public boolean allowGoogleCrashReport(boolean allow) {
        try {
            return mRestrictionPolicy.allowGoogleCrashReport(allow);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to allow google crash report: "+e);
            return false;
        }
    }


    public boolean allowLockScreenWidgetView(boolean allow) {
        try {
            return mRestrictionPolicy.allowLockScreenView(mRestrictionPolicy.LOCKSCREEN_MULTIPLE_WIDGET_VIEW, allow);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to allow the lock screen widget view: "+e);
            return false;
        }
    }

    public boolean allowLockScreenShortcutsView(boolean allow) {
        try {
            return mRestrictionPolicy.allowLockScreenView(mRestrictionPolicy.LOCKSCREEN_SHORTCUTS_VIEW, allow);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to allow lock screen shortcut view: "+e);
            return false;
        }
    }

    public boolean allowOTAUpgrade(boolean allow) {
        try {
            return mRestrictionPolicy.allowOTAUpgrade(allow);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to allow OTA Upgrade: "+e);
            return false;
        }
    }

    public boolean allowSBeam(boolean allow) {
        try {
            return mRestrictionPolicy.allowSBeam(allow);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to allow S Beam: "+e);
            return false;
        }
    }

    public boolean allowSafeMode(boolean allow) {
        try {
            return mRestrictionPolicy.allowSafeMode(allow);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to allow: "+e);
            return false;
        }
    }

    public boolean allowSVoice(boolean allow) {
        try {
            return mRestrictionPolicy.allowSVoice(allow);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to allow s voice: "+e);
            return false;
        }
    }

    public boolean allowShareList(boolean allow) {
        try {
            return mRestrictionPolicy.allowShareList(allow);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to allow share list: "+e);
            return false;
        }
    }

    public boolean allowStatusBarExpansion(boolean allow) {
        try {
            return mRestrictionPolicy.allowStatusBarExpansion(allow);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to allow status bar expansion: "+e);
            return false;
        }
    }

    public boolean allowUSbHostStorage(boolean allow) {
        try {
            return mRestrictionPolicy.allowUsbHostStorage(allow);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to allow Usb Host Storage: "+e);
            return false;
        }
    }

    public boolean allowVideoRecord(boolean allow) {
        try {
            return mRestrictionPolicy.allowVideoRecord(allow);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to allow video Record: "+e);
            return false;
        }
    }

    public boolean allowWifiDirect(boolean allow) {
        try {
            return mRestrictionPolicy.allowWifiDirect(allow);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to allow wifi direct: "+e);
            return false;
        }
    }

    public boolean setBackup(boolean allow) {
        try {
            return mRestrictionPolicy.setBackup(allow);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to set backup: "+e);
            return false;
        }
    }

    public boolean setBluetoothTethering(boolean allow) {
        try {
            return mRestrictionPolicy.setBluetoothTethering(allow);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to set bluetooth tethering: "+e);
            return false;
        }
    }

    public boolean setCameraState(boolean allow) {
        try {
            return mRestrictionPolicy.setCameraState(allow);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to set camera state: "+e);
            return false;
        }
    }

    public boolean setEnableNFC(boolean allow) {
        try {
            return mRestrictionPolicy.setEnableNFC(allow);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to set enable NFC: "+e);
            return false;
        }
    }

    public boolean setLockScreenState(boolean allow) {
        try {
            return mRestrictionPolicy.setBackup(allow);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to set lock screen state: "+e);
            return false;
        }
    }

    public boolean setMockLocation(boolean allow) {
        try {
            return mRestrictionPolicy.setBackup(allow);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to set mock location: "+e);
            return false;
        }
    }

    public boolean setTethering(boolean allow) {
        try {
            return mRestrictionPolicy.setTethering(allow);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to set tethering: "+e);
            return false;
        }
    }

    public boolean setUsbTethering(boolean allow) {
        try {
            return mRestrictionPolicy.setUsbTethering(allow);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to set usb tethering: "+e);
            return false;
        }
    }

    public boolean setWifiTethering(boolean allow) {
        try {
            return mRestrictionPolicy.setWifiTethering(allow);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to set wifi tethering: "+e);
            return false;
        }
    }

    public boolean setSdCardState(boolean allow) {
        try {
            return mRestrictionPolicy.setSdCardState(allow);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to set sd card state: "+e);
            return false;
        }
    }



    // MUTI USER MANAGEMENT

    public boolean allowMultipleUsers(boolean allow) {
        try {
            return mMultiUserManager.allowMultipleUsers(allow);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to allow multiple users: "+e);
            return false;
        }
    }

    public boolean allowUserCreation(boolean allow) {
        try {
            return mMultiUserManager.allowUserCreation(allow);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to allow user creation: "+e);
            return false;
        }
    }

    public boolean allowUserRemoval(boolean allow) {
        try {
            return mMultiUserManager.allowUserRemoval(allow);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to allow user removal: "+e);
            return false;
        }
    }

    public Long getApplicationCpuUsage(String pkgName) {
        try {
            return mApplicationPolicy.getApplicationCpuUsage(pkgName);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to get CPU usage: "+e);
            return -1L;
        }
    }

    public Long getApplicationMemoryUsage(String pkgName) {
        try {
            return mApplicationPolicy.getApplicationMemoryUsage(pkgName);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to get memory usage: "+e);
            return -1L;
        }
    }

    public Long getApplicationCacheSize(String pkgName) {
        try {
            return mApplicationPolicy.getApplicationCacheSize(pkgName);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to get cache size: "+e);
            return -1L;
        }
    }


    // INJECT INPUT EVENTS

    public boolean injectKeyEvent(KeyEvent ev, boolean sync) {
        try {
            return mRemoteInjection.injectKeyEvent(ev, sync);
        } catch (Exception e) {
            Log.e(TAG, "Knox unable to inject key event: "+e);
            return false;
        }
    }

    public boolean injectPointerEvent(MotionEvent ev, boolean sync) {
        try {
            return mRemoteInjection.injectPointerEvent(ev, sync);
        } catch (Exception e) {
            Log.e(TAG, "Knox unable to inject pointer event: "+e);
            return false;
        }
    }

    public boolean injectTrackballEvent(MotionEvent ev, boolean sync) {
        try {
            return mRemoteInjection.injectTrackballEvent(ev, sync);
        } catch (Exception e) {
            Log.e(TAG, "Knox unable to inject key event: "+e);
            return false;
        }
    }



    // POWER METHODS

    public void reboot(String reason) {
        try {
            mEnterpriseDeviceManager.reboot(reason);
        } catch(Exception e) {
            Log.e(TAG, "Knox is unable to reboot the system: "+e);
        }
    }
}
