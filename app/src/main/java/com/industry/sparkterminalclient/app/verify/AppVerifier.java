package com.industry.sparkterminalclient.app.verify;

import android.content.Context;
import android.provider.ContactsContract;
import android.util.Log;

import com.industry.sparkterminalclient.Utility;
import com.industry.sparkterminalclient.app.AppConstants;
import com.industry.sparkterminalclient.app.AppUtility;
import com.industry.sparkterminalclient.event.EventConstants;
import com.industry.sparkterminalclient.event.EventManager;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpPost;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.koushikdutta.async.http.body.UrlEncodedFormBody;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;
import org.zeroturnaround.zip.ZipUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import bolts.Continuation;
import bolts.Task;

/**
 * Created by Two on 2/6/2015.
 */
public class AppVerifier {
    private static final String TAG = AppVerifier.class.getName();


    // Constants
    private static final String MANIFEST_CHECKSUM_LINE_PREFIX = "SHA1-Digest:";
    public final static String APK_META_DIRECTORY = "META-INF";
    public final static String VALIDATION_SERVER_URL = "http://192.168.2.14:3000/";


    // Dependencies
    private Context mContext;
    private EventManager mEventManager;


    // Singleton
    private static AppVerifier mInstance;
    public static synchronized AppVerifier getInstance(Context context) {
        if(mInstance == null) {
            mInstance = new AppVerifier(context);
        }
        return mInstance;
    }


    // Constructor
    private AppVerifier(Context context) {
        mContext = context;
        mEventManager = EventManager.getInstance(context);
    }


    // Methods
    public synchronized Task<Void> verifyApp(final File file, final String hid) {
        final Task<Void>.TaskCompletionSource done = Task.create();
        try {
            // Verify inputs
            if(file == null || !file.exists() || hid == null || hid.isEmpty()) {
                new Exception("Invalid args passed to verifyApp");
            }

            // Perform Verification Process
            verifyAPKChecksum(file, hid).onSuccessTask(new Continuation<JSONObject, Task<JSONObject>>() {
                public Task<JSONObject> then(Task<JSONObject> task) throws Exception {
                    return verifyManifestChecksum(file, hid);
                }
            }).onSuccessTask(new Continuation<JSONObject, Task<Void>>() {
                public Task<Void> then(Task<JSONObject> task) throws Exception {
                    return verifyAPKFilesAndChecksums(hid);
                }
            }).continueWith(new Continuation<Void, Void>() {
                public Void then(Task<Void> task) throws Exception {
                    if (task.isFaulted()) {
                        done.setError(task.getError());
                    } else {
                        mEventManager.emitEvent(EventConstants.EVENT_APP_VERIFIED, hid);
                        done.setResult(null);
                    }
                    return null;
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Unable to verify to APK Checksum");
            done.setError(e);
        } finally {
            return done.getTask();
        }
    }

    public synchronized Task<Void> verifyApps(final HashMap<File, String> verifyAppDict) {
        ArrayList<Task<Void>> tasks = new ArrayList<Task<Void>>();
        for(Map.Entry<File,String> entry : verifyAppDict.entrySet()) {
            tasks.add(verifyApp(entry.getKey(), entry.getValue()));
        }
        return Task.whenAll(tasks);
    }

    private synchronized Task<JSONObject> verifyAPKChecksum(final File file, final String hid) {
        final Task<JSONObject>.TaskCompletionSource done = Task.create();
        final String errMessage = "Unable to verify to APK Checksum";
        try {

            // Get SHA-256 Checksum
            String checksum = Utility.getFileBase64Checksum(file, "SHA-256");

            // Verify the checksum with the server
            ArrayList<NameValuePair> mDeviceInfo = new ArrayList<NameValuePair>();
            mDeviceInfo.add(new BasicNameValuePair("hid", hid));
            mDeviceInfo.add(new BasicNameValuePair("apkChecksum", checksum));
            AsyncHttpPost post = new AsyncHttpPost(VALIDATION_SERVER_URL+"verify/apk");
            UrlEncodedFormBody writer = new UrlEncodedFormBody(mDeviceInfo);
            post.setBody(writer);
            AsyncHttpClient.getDefaultInstance().executeJSONObject(post, new AsyncHttpClient.JSONObjectCallback() {
                @Override
                public void onCompleted(Exception e, AsyncHttpResponse response, JSONObject result) {
                    if (e != null) {
                        done.setError(new Exception(errMessage));
                    } else {
                        Boolean err = result.has("err");
                        if (err) {
                            done.setError(new Exception(errMessage));
                        } else {
                            done.setResult(null);
                        }

                    }
                }
            });
        } catch (Throwable t) {
            Utility.finishTask(done, t);
        } finally {
            return done.getTask();
        }
    }

    private Task<JSONObject> verifyManifestChecksum(final File file, final String hid) {
        final Task<JSONObject>.TaskCompletionSource done = Task.create();
        final String errMessage = "Failed to verify the APK's manifest checksum and public key";
        FileInputStream fis = null;
        try {
            File tempUnzipDir = AppUtility.getTempUnzipDir(hid);
            if (!tempUnzipDir.exists() && !tempUnzipDir.mkdir()) {
                Log.d(TAG, "Unable to create tempUnzipDir");
                throw new Exception(errMessage);
            } else {

                //Extract APK into temp dir
                fis = new FileInputStream(file);
                ZipUtil.unpack(fis, tempUnzipDir);
                fis.close();

                // Get the encrypted manifest checksum
                File manifest = new File(file.getPath() + File.separator +APK_META_DIRECTORY + File.separator + "MANIFEST.MF");
                String manifestChecksum = Utility.getFileBase64Checksum(manifest, "SHA-256");

                // Check if they match with the server
                // Verify the checksum with the server
                ArrayList<NameValuePair> mDeviceInfo = new ArrayList<NameValuePair>();
                mDeviceInfo.add(new BasicNameValuePair("hid", hid));
                mDeviceInfo.add(new BasicNameValuePair("manifestChecksum", manifestChecksum));
                AsyncHttpPost post = new AsyncHttpPost(VALIDATION_SERVER_URL+"verify/manifest");
                UrlEncodedFormBody writer = new UrlEncodedFormBody(mDeviceInfo);
                post.setBody(writer);
                AsyncHttpClient.getDefaultInstance().executeJSONObject(post, new AsyncHttpClient.JSONObjectCallback() {
                    @Override
                    public void onCompleted(Exception e, AsyncHttpResponse response, JSONObject result) {
                        if (e != null) {
                            done.setError(new Exception(errMessage));
                        } else {
                            Boolean err = result.has("err");
                            if (err) {
                                done.setError(new Exception(errMessage));
                            } else {
                                done.setResult(null);
                            }
                        }
                    }
                });
            }
        } catch (Throwable t) {
            Log.e(TAG, "trouble HID with manifest check: "+hid);
            Utility.finishTask(done, t);
        } finally {
            try {
                fis.close();
            } catch(Throwable t) {
                t.printStackTrace();
            }

            return done.getTask();
        }
    }

    private Task<Void> verifyAPKFilesAndChecksums(String hid) {
        final Task<Void>.TaskCompletionSource done = Task.create();
        final String errMessage = "Malicious file found inside APK";

        FileInputStream is = null;
        BufferedReader reader = null;

        try {
            // Build a File List
            File unzipDir = AppUtility.getTempUnzipDir(hid);
            File manifest = new File(unzipDir.getPath() + File.separator + APK_META_DIRECTORY + File.separator + "MANIFEST.MF");

            // Get Raw File line list
            is = new FileInputStream(manifest);
            reader = new BufferedReader(new InputStreamReader(is));
            ArrayList<String> rawManifestLines = new ArrayList<String>();
            String line;
            while ((line = reader.readLine()) != null) {
                rawManifestLines.add(line);
            }

            // Get the starting line of the relevant manifest info
            int startingIndex;
            for (startingIndex = 0; startingIndex < rawManifestLines.size(); startingIndex++) {
                if(rawManifestLines.get(startingIndex).length() <= 0) {
                    startingIndex++;
                    break;
                }
            }

            // Clean file line list
            HashMap<String, String> manifestFileMap = new HashMap<String, String>();
            for (int i = startingIndex; i < rawManifestLines.size(); i+=3) {
                String manifestFilenameLine = rawManifestLines.get(i).substring(6);

                // Check if the Filename was so long that it wrapped to the next line
                if(rawManifestLines.get(i+1).length() < MANIFEST_CHECKSUM_LINE_PREFIX.length()
                        || !rawManifestLines.get(i+1).substring(0, MANIFEST_CHECKSUM_LINE_PREFIX.length()).equals(MANIFEST_CHECKSUM_LINE_PREFIX)) {
                    manifestFilenameLine += rawManifestLines.get(++i).substring(1, rawManifestLines.get(i).length());
                }
                //Log.d(TAG, "Filename Line: "+manifestFilenameLine);
                String manifestChecksumLine = rawManifestLines.get(i+1).substring(13);
                //Log.d(TAG, "Checksum Line: "+manifestChecksumLine);
                manifestFileMap.put(manifestFilenameLine.trim(), manifestChecksumLine.trim());
            }

            // Get file list of apk
            ArrayList<File> apkFileList = new ArrayList<File>();
            HashMap<String, File> apkFileMap = new HashMap<String, File>();
            Utility.recursivelyGetFiles(unzipDir, apkFileList);
            for (int i = 0; i < apkFileList.size(); i++) {

                // Get the position of the meta dir name
                String filePath = apkFileList.get(i).getAbsolutePath();
                //Log.d(TAG, "FilePath: "+filePath);
                int startIndex = (AppConstants.TEMP_UNZIP_DIRECTORY+File.separator
                        +hid+File.separator).length();
                int endIndex = startIndex + APK_META_DIRECTORY.length();

                // Check that the file path isn't too small to
                // Check that the root of this file is not the meta dir
                //Log.d(TAG, "File Key: "+filePath.substring(startIndex, endIndex));
                if(filePath.length() <= endIndex || !filePath.substring(startIndex, endIndex).equals(APK_META_DIRECTORY)) {
                    //Log.d(TAG, "Adding key to file map: "+filePath.substring(startIndex, filePath.length()).trim());
                    //Log.d(TAG, "Substringing filepath: "+filePath);
                    apkFileMap.put(filePath.substring(startIndex, filePath.length()).trim(), apkFileList.get(i));
                }
            }

            for (HashMap.Entry<String, File> entry : apkFileMap.entrySet()) {
                // Make sure there is nothing in the file list that isn't included in the manifest
                if (!manifestFileMap.containsKey(entry.getKey())) {
                    Log.e(TAG, "Unknown File Found: " + entry.getKey());
                    Log.e(TAG, hid);
                    throw new Exception(errMessage);
                    // Calculate each file checksum and compare it to what's in the manifest
                } else {

                    // Get the SHA1 Digest of the File
                    String fileChecksumString = Utility.getFileBase64Checksum(entry.getValue(), "SHA1");

                    // Check that the encrypted Checksums match
                    if (!manifestFileMap.get(entry.getKey()).equals(fileChecksumString)) {
                        Log.e(TAG, "Edited file found : " + entry.getKey());
                        Log.e(TAG, hid);
                        throw new Exception(errMessage);
                    }
                }
            }

            done.setResult(null);

        } catch (Throwable t) {
            t.printStackTrace();
            done.setError(new Exception(t));
        } finally {
            // Clean up streams
            try {
                if(is != null) {
                    is.close();
                }
                if(reader != null) {
                    reader.close();
                }
            } catch(Exception e) {
                e.printStackTrace();
            } finally {
                return done.getTask();
            }
        }
    }
}
