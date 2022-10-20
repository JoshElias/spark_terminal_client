package com.industry.sparkterminalclient.tests;

import android.util.Log;

import com.industry.sparkterminalclient.tcp.socket.LobbySocketInterface;
import com.industry.sparkterminalclient.tcp.socket.SparkSocketClient;
import com.industry.sparkterminalclient.tcp.socket.SparkSocketServer;
import com.industry.sparkterminalclient.tests.base.SparkClientTestCase;

import org.json.JSONObject;

/**
 * Created by Two on 12/4/2014.
 */
public class SocketConnectionTest extends SparkClientTestCase {
    private static final String TAG = SocketConnectionTest.class.getName();

    private SparkSocketServer mSocketServer;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        final SparkSocketClient.SparkSocketEvent setVolumeHandler = new SparkSocketClient.SparkSocketEvent() {
            @Override
            public void onEvent(JSONObject obj) {
                Log.d(TAG, "VolumeHandler Triggered");
                Log.d(TAG, obj.toString());
                countDownAsyncLatch();
            }
        };

        final SparkSocketClient.SparkSocketEvent setBrightnessHandler = new SparkSocketClient.SparkSocketEvent() {
            @Override
            public void onEvent(JSONObject obj) {
                Log.d(TAG, "BrightnessHandler Triggered");
                Log.d(TAG, obj.toString());
                countDownAsyncLatch();
            }
        };

        final SparkSocketClient.SparkSocketEvent disableAppHandler = new SparkSocketClient.SparkSocketEvent() {
            @Override
            public void onEvent(JSONObject obj) {
                Log.d(TAG, "DisableAppHandler Triggered");
                Log.d(TAG, obj.toString());
                countDownAsyncLatch();
            }
        };

        mSocketServer = SparkSocketServer.getInstance( new LobbySocketInterface.ISparkSocketServerEmitter() {
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


    // Test Functions

    public void testVolumeHandler() throws Exception {
        resetAsyncLatch();

        mSocketServer.getGetClient().emit("setVolume", new JSONObject());

        if(!awaitForAsyncLatch()) {
            throw new Exception("Failed to testVolumeHandler");
        }
    }

    public void testBrightnessHandler() throws Exception {
        resetAsyncLatch();

        mSocketServer.getGetClient().emit("setBrightness", new JSONObject());

        if(!awaitForAsyncLatch()) {
            throw new Exception("Failed to testBrightnessHandler");
        }
    }

    public void testDisableApp() throws Exception {
        resetAsyncLatch();

        mSocketServer.getGetClient().emit("disableApp", new JSONObject());

        if(!awaitForAsyncLatch()) {
            throw new Exception("Failed to testDisableApp");
        }
    }
}
