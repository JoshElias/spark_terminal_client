package com.industry.sparkterminalclient.app.download;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.industry.sparkterminalclient.Utility;
import com.industry.sparkterminalclient.app.AppModule;
import com.industry.sparkterminalclient.app.AppUtility;
import com.industry.sparkterminalclient.event.EventManager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import bolts.Task;

/**
 * Created by Two on 11/26/2014.
 */
public class AppDownloader extends AppModule implements IAppDownloader {
    private static final String TAG = AppDownloader.class.getName();

    // Constants
    protected static final String APP_EVENT_NAME = "EVENT_APP_UNINSTALL";
    private static final int QUEUE_LIMIT = 200;
    private static final int DOWNLOAD_LIMIT = 5;


    // Dependencies
    private DownloadManager mDownloadManager;


    // Members
    private ConcurrentHashMap<Long, DownloadItem> mDownloadRequestMap = new ConcurrentHashMap<Long, DownloadItem>();
    private ConcurrentLinkedQueue<DownloadItem> mDownloadQueue = new ConcurrentLinkedQueue<DownloadItem>();
    private AtomicInteger mConcurrentDownloads = new AtomicInteger(0);
    private AtomicBoolean mIsDownloading = new AtomicBoolean(false);


    // Inner Classes
    private class DownloadItem {
        File destinationFile;
        String apkUrl;
        Task<Void>.TaskCompletionSource task;
        DownloadItem(File _destinationFile, String _apkUrl, Task<Void>.TaskCompletionSource _task) {
            destinationFile = _destinationFile;
            apkUrl = _apkUrl;
            task = _task;
        }
    }


    // Singleton
    private static AppDownloader mInstance;
    public static synchronized AppDownloader getInstance(Context context) {
        if(mInstance == null) {
            mInstance = new AppDownloader(context);
        }
        return null;
    }


    // Constructor
    public AppDownloader(Context context) {
        super(context);

        mDownloadManager = (DownloadManager)context.getSystemService(Context.DOWNLOAD_SERVICE);

        context.registerReceiver(mDownloadActionReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }


    // Event Listeners
    private BroadcastReceiver mDownloadActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context _context, Intent intent) {
            String action = intent.getAction();

            // Handle DownloadManager Complete
            if (action.equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
                // Get the Download ID from the intent
                Long downloadID = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                checkDownloadStatus(downloadID);
            }
        }
    };


    // Methods
    public synchronized Task<Void> downloadApp(final File destinationFile, final String apkUrl) {
        final Task<Void>.TaskCompletionSource done = Task.create();
        try {
            // Verify Inputs
            if(destinationFile == null || apkUrl == null || apkUrl.isEmpty()) {
                throw new Exception("Invalid arguments passed to downloadApp");
            }

            // Check Queue Limit
            if(mDownloadQueue.size() > QUEUE_LIMIT) {
                throw new Exception("Download Queue limit reached");
            }

            // Add Download to the Queue
            mDownloadQueue.add(new DownloadItem(destinationFile, apkUrl, done));

            if(!mIsDownloading.get()) {
                performNextDownloadBatch();
            }

        } catch( Throwable t ) {
            finishTask(done, t);
        } finally {
            return done.getTask();
        }
    }

    public synchronized Task<Void> downloadApps(final HashMap<File, String> downloadRequestDict) {
        ArrayList<Task<Void>> tasks = new ArrayList<Task<Void>>();
        for(Map.Entry<File,String> entry : downloadRequestDict.entrySet()) {
            tasks.add(downloadApp(entry.getKey(), entry.getValue()));
        }
        return Task.whenAll(tasks);
    }

    public void performNextDownloadBatch() {
        Log.d(TAG, "Performing next Download Batch");
        // Do we have any pending Install Requests?
        int queueSize = mDownloadQueue.size();
        if(queueSize > 0) {
            mIsDownloading.set(true);

            // We are allowed to download 5 things at once
            int positionsAvailable = DOWNLOAD_LIMIT - mConcurrentDownloads.get();
            int nextBatchSize = (positionsAvailable > queueSize) ? queueSize : positionsAvailable;
            for(int i=0; i < nextBatchSize; i++) {

                // Get the next ready install file from the queue
                DownloadItem downloadItem = mDownloadQueue.poll();
                try {

                    // Check if all this stuff is good
                    if (!Utility.isNetworkAvailable(mContext)) {
                        throw new Exception("Network is not available");
                    }
                    if (!Utility.isExternalStorageWritable()) {
                        throw new Exception("External storage is not writable");
                    }
                    if (!AppUtility.isAppStorageDirectoryAvailable()) {
                        throw new Exception("APKStorageDirectory is not available");
                    }

                    // Set up download options
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadItem.apkUrl));
                    request.setDescription("Downloading some test image");
                    request.setTitle("Downloading Server Image");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        request.allowScanningByMediaScanner();
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION);
                    }
                    request.setDestinationUri(Uri.fromFile(downloadItem.destinationFile));

                    // Delete old apk if it exists
                    if (downloadItem.destinationFile.exists() && !downloadItem.destinationFile.delete()) {
                        Log.e(TAG, "Unable to delete old APK");
                        throw new Exception("Unable to delete old downloaded file");
                    }

                    // Start download
                    Long downloadID = mDownloadManager.enqueue(request);

                    // Map Download Request
                    mDownloadRequestMap.put(downloadID, downloadItem);
                    mConcurrentDownloads.getAndIncrement();
                    Log.e(TAG, "Downloads Remaining in Queue: "+mDownloadQueue.size());

                } catch (Throwable t) {
                    Log.d(TAG, "Error where there shouldnt be00");
                    finishTask(downloadItem.task, t);
                }
            }
        } else {
            Log.e(TAG, "Finished All Downloads");
            Log.e(TAG, "Downloads in REquestMap: "+mDownloadRequestMap.size());
            mIsDownloading.set(false);
        }
    }

    private void checkDownloadStatus(Long downloadID) {
        final String errMessage = "Unable to download APK";

        // Reunite downloadID with action intent and task
        Task<Void>.TaskCompletionSource task = mDownloadRequestMap.get(downloadID).task;

        try {
            // Query the DownloadManager
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadID);
            Cursor cursor = mDownloadManager.query(query);
            if(cursor.moveToFirst()){
                int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                int status = cursor.getInt(columnIndex);
                int columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                int reason = cursor.getInt(columnReason);

                switch(status) {
                    case DownloadManager.STATUS_FAILED:
                    /*
                    String failedReason = "";
                    switch(reason) {
                        case DownloadManager.ERROR_CANNOT_RESUME:
                            failedReason = "ERROR_CANNOT_RESUME";
                            break;
                        case DownloadManager.ERROR_DEVICE_NOT_FOUND:
                            failedReason = "ERROR_DEVICE_NOT_FOUND";
                            break;
                        case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
                            failedReason = "ERROR_FILE_ALREADY_EXISTS";
                            break;
                        case DownloadManager.ERROR_FILE_ERROR:
                            failedReason = "ERROR_FILE_ERROR";
                            break;
                        case DownloadManager.ERROR_HTTP_DATA_ERROR:
                            failedReason = "ERROR_HTTP_DATA_ERROR";
                            break;
                        case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                            failedReason = "ERROR_INSUFFICIENT_SPACE";
                            break;
                        case DownloadManager.ERROR_TOO_MANY_REDIRECTS:
                            failedReason = "ERROR_TOO_MANY_REDIRECTS";
                            break;
                        case DownloadManager.ERROR_UNHANDLED_HTTP_CODE:
                            failedReason = "ERROR_UNHANDLED_HTTP_CODE";
                            break;
                        case DownloadManager.ERROR_UNKNOWN:
                            failedReason = "ERROR_UNKNOWN";
                            break;
                    }
                    */
                        throw new Exception(errMessage);
                /*
                case DownloadManager.STATUS_PAUSED:
                    String pausedReason = "";

                    switch(reason){
                        case DownloadManager.PAUSED_QUEUED_FOR_WIFI:
                            pausedReason = "PAUSED_QUEUED_FOR_WIFI";
                            break;
                        case DownloadManager.PAUSED_UNKNOWN:
                            pausedReason = "PAUSED_UNKNOWN";
                            break;
                        case DownloadManager.PAUSED_WAITING_FOR_NETWORK:
                            pausedReason = "PAUSED_WAITING_FOR_NETWORK";
                            break;
                        case DownloadManager.PAUSED_WAITING_TO_RETRY:
                            pausedReason = "PAUSED_WAITING_TO_RETRY";
                            break;
                    }

                    Toast.makeText(AndroidDownloadManagerActivity.this,
                            "PAUSED: " + pausedReason,
                            Toast.LENGTH_LONG).show();
                    break;
                case DownloadManager.STATUS_PENDING:
                    Toast.makeText(AndroidDownloadManagerActivity.this,
                            "PENDING",
                            Toast.LENGTH_LONG).show();
                    break;
                case DownloadManager.STATUS_RUNNING:
                    Toast.makeText(AndroidDownloadManagerActivity.this,
                            "RUNNING",
                            Toast.LENGTH_LONG).show();
                    break;
                */
                    case DownloadManager.STATUS_SUCCESSFUL:
                        finishTask(task);
                        break;
                }
            }
        } catch( Throwable t ) {
            finishTask(task, t);
        } finally {
            Log.d(TAG, "Download Finished");
            mDownloadRequestMap.remove(downloadID);
            mConcurrentDownloads.getAndDecrement();
            performNextDownloadBatch();
        }
    }
}
