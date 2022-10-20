package com.industry.sparkterminalclient.activities;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.industry.sparkterminalclient.KnoxManager;
import com.industry.sparkterminalclient.tcp.http.old.LobbyHttpInterface;
import com.industry.sparkterminalclient.state.StateMonitor;
import com.industry.sparkterminalclient.tcp.socket.LobbySocketInterface;

public class SparkClientService extends Service {
    private static final String TAG = SparkClientService.class.getName();

    private static Thread.UncaughtExceptionHandler mUncaughtExceptionHandler = null;
    private static boolean mIsRunning = false;

    private KnoxManager mKnox;
    private LobbyHttpInterface mHttpInterface;
    private LobbySocketInterface mSocketInterface;

    private Handler mHeartbeatHandler = new Handler(Looper.getMainLooper());

    private StateMonitor mSparkInputMonitor;

    public SparkClientService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "Spark Client Service onStart()");
        listenForExceptions();
        mHttpInterface = LobbyHttpInterface.getInstance(getApplicationContext());
        mHeartbeatHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "Spark Client Service is ALIVE!");
                mHeartbeatHandler.postDelayed(this, 2000);
            }
        });
        return START_REDELIVER_INTENT;
        //return START_STICKY;
    }

    private void listenForExceptions() {
        mIsRunning = true;
        if(mUncaughtExceptionHandler == null) {
            mUncaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable throwable) {
                    Log.e(TAG, "Error in SparkClientService");
                    throwable.printStackTrace();
                }
            };
            Thread.setDefaultUncaughtExceptionHandler(mUncaughtExceptionHandler);
        }
    }


    @Override
    public void onCreate() {
        Log.e(TAG, "Super Puppies!");
        // Start Client Terminal process
        //mHttpInterface = SparkLobbyHttpInterface.getInstance(getApplicationContext());
        /*
        try {
            //startSparkKioskMode();
            mKnox = KnoxManager.getInstance(getApplicationContext());
            //mHttpInterface = LobbyHttpInterface.getInstance(getApplicationContext());
            mSocketInterface = LobbySocketInterface.getInstance();
        } catch(Throwable t) {
            Log.e(TAG, "Unable to start kiosk mode");
            t.printStackTrace();
            this.stopSelf();
        }

        mSparkInputMonitor = new SparkInputMonitor(getApplicationContext(), new ISparkInputListener() {
            @Override
            public void onIdle() {
                Log.e(TAG, "ON ze idle");
            }

            @Override
            public void onBusy() {
                Log.e(TAG, "ON ze BUZY");
            }
        });
        */
    }

    @Override
    public void onDestroy() {
        mIsRunning = false;

    }

    public static boolean getIsRunning() {
        return mIsRunning;
    }

    // START ON STARTUP
    public class StartSparkClientReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Restarting Spark Client Service");
            context.startService(new Intent(context, SparkClientService.class));
        }
    }


    // NEVER SURRENDER
    public void startSparkKioskMode() throws Exception {
        try {
            // Disable features
            mKnox.allowSVoice(false);
            mKnox.allowTaskManager(false);
            mKnox.hideSystemBar(true);
            mKnox.allowMultiWindowMode(false);
            mKnox.setDisableApplication("com.google.android.googlequicksearchbox");
            mKnox.enableKioskMode("com.IndustryCorp.SparkLobby");

            // Limit the installation of everything excluding the admin activity
            mKnox.addAppPackageNameToBlackList("*");
            mKnox.addAppPackageNameToWhiteList("com.industry.sparkterminalclient.*");
        } catch(Exception e) {
            Log.e(TAG, "Unable to start Spark Kiosk Mode");
        }
    }
}

