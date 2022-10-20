package com.industry.sparkterminalclient.tcp.socket;

import android.util.Log;

import org.json.JSONObject;

/**
 * Created by Two on 11/25/2014.
 */
public class LobbySocketInterface {
    private static final String TAG = LobbySocketInterface.class.getName();

    SparkSocketServer mSocketServer;

    private static LobbySocketInterface mInstance;
    public static LobbySocketInterface getInstance() {
        if(mInstance == null) {
            mInstance = new LobbySocketInterface();
        }
        return mInstance;
    }
    private LobbySocketInterface() {
        setupHandlers();
    }



    // SOCKET INTERFACE

    public void setupHandlers() {
        mSocketServer = SparkSocketServer.getInstance( new ISparkSocketServerEmitter() {
            @Override
            public void onConnection(SparkSocketClient client) {
                Log.d(TAG, "Spark Socket Server Connected");

                client.on("setVolume", setVolumeHandler);
                client.on("setBrightness", setBrightnessHandler);
                client.on("disableApp", disableAppHandler);

            }
            @Override
            public void onError(Exception e) {
                Log.d(TAG, "Spark Socket Server on Error: " + e.getMessage());
            }
            @Override
            public void onClose() {
                Log.d(TAG, "Spark Socket Server Closing");
            }
        });
    }

    SparkSocketClient.SparkSocketEvent setVolumeHandler = new SparkSocketClient.SparkSocketEvent() {
        @Override
        public void onEvent(JSONObject obj) {
            Log.d(TAG, "VolumeHandler Triggered");
            Log.d(TAG, obj.toString());
        }
    };

    SparkSocketClient.SparkSocketEvent setBrightnessHandler = new SparkSocketClient.SparkSocketEvent() {
        @Override
        public void onEvent(JSONObject obj) {
            Log.d(TAG, "BrightnessHandler Triggered");
            Log.d(TAG, obj.toString());
        }
    };

    SparkSocketClient.SparkSocketEvent disableAppHandler = new SparkSocketClient.SparkSocketEvent() {
        @Override
        public void onEvent(JSONObject obj) {
            Log.d(TAG, "DisableAppHandler Triggered");
            Log.d(TAG, obj.toString());
        }
    };

    public void close() {
        if(mSocketServer != null) {
            mSocketServer.close();
        }
    }

    /**
     * Created by Two on 1/8/2015.
     */
    public static interface ISparkSocketServerEmitter {
        void onConnection(SparkSocketClient socketClient);
        void onError(Exception e);
        void onClose();
    }
}
