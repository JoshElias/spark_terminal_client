package com.industry.sparkterminalclient.tcp.socket;

import android.util.Log;

import com.industry.sparkterminalclient.Utility;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedList;

import bolts.Task;

/**
 * Created by Two on 11/18/2014.
 */
public class SparkSocketClient {
    private static final String TAG = SparkSocketClient.class.getName();

    // SOCKET CLIENT ID
    private static final String SPARK_CLIENT_ID = "client";

    // SOCKET MESSAGE TYPES
    private static final String SOCKET_MESSAGE_DATA = "SOCKET_MESSAGE_DATA";
    private static final String SOCKET_MESSAGE_HEARTBEAT = "SOCKET_MESSAGE_HEARTBEAT";

    // SOCKET EVENT TYPES
    public static final String SOCKET_EVENT_CONNECT = "SOCKET_EVENT_CONNECT";
    public static final String SOCKET_EVENT_DATA = "SOCKET_EVENT_DATA";
    public static final String SOCKET_EVENT_RECONNECT = "SOCKET_EVENT_RECONNECT";
    public static final String SOCKET_EVENT_DISCONNECT = "SOCKET_EVENT_DISCONNECT";


    // Socket Client
    private Socket mSocket;

    // Data Management
    private String mChunk;
    private BufferedReader mInputReader;
    private PrintWriter mOutputWriter;

    // Connection Management
    private boolean mEverConnected = false;
    private boolean mIsConnected = false;
    private int mLastClientHeartbeat = 0;
    private int mNumOfReconnects = 0;

    // Socket Event Management
    LinkedList<SparkSocketEvent> mConnectEventQueue = new LinkedList<SparkSocketEvent>();
    LinkedList<SparkSocketEvent> mDataEventQueue = new LinkedList<SparkSocketEvent>();
    LinkedList<SparkSocketEvent> mReconnectEventQueue = new LinkedList<SparkSocketEvent>();
    LinkedList<SparkSocketEvent> mDisconnectEventQueue = new LinkedList<SparkSocketEvent>();
    HashMap<String,LinkedList<SparkSocketEvent>> mCustomEventMap = new HashMap<String, LinkedList<SparkSocketEvent>>();

    // Public Getters/Setters
    public boolean isConnected() {
        return mIsConnected;
    }
    public int getNumOfReconnects() {
        return mNumOfReconnects;
    }
    public void incrementReconnects() {
        mNumOfReconnects++;
    }



    // CONSTRUCTOR

    public SparkSocketClient(Socket socket) throws IOException {
        mSocket = socket;
        mInputReader = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
        mOutputWriter = new PrintWriter(new OutputStreamWriter(mSocket.getOutputStream()));
    }



    // SOCKET CLIENT INTERFACE

    public SparkSocketClient on(String eventName, SparkSocketEvent socketEvent) {
        if(eventName.equals(SOCKET_EVENT_CONNECT)) {
            mConnectEventQueue.add(socketEvent);
        } else if(eventName.equals(SOCKET_EVENT_DATA)) {
            mDataEventQueue.add(socketEvent);
        } else if(eventName.equals(SOCKET_EVENT_RECONNECT)) {
            mReconnectEventQueue.add(socketEvent);
        } else if(eventName.equals(SOCKET_EVENT_DISCONNECT)) {
            mDisconnectEventQueue.add(socketEvent);
        } else {
            registerCustomEvent(eventName, socketEvent);
        }
        return this;
    }

    public void emit(String eventName, JSONObject obj) {
        if(eventName != null || obj != null) {
            Log.d(TAG, "Emitting event: "+eventName);
            writeToSocket(SOCKET_MESSAGE_DATA, eventName, obj);
        }
    }



    // EVENT HANDLERS

    public interface SparkSocketEvent {
        public void onEvent(JSONObject obj);
    }

    public void triggerEvent(LinkedList<SparkSocketEvent> queue, JSONObject obj) {
        for(SparkSocketEvent socketEvent : queue) {
            socketEvent.onEvent(obj);
        }
    }

    public void triggerEvent(LinkedList<SparkSocketEvent> queue) {
        JSONObject blankObj = new JSONObject();
        for(SparkSocketEvent socketEvent : queue) {
            socketEvent.onEvent(blankObj);
        }
    }

    public void registerCustomEvent(String eventName, SparkSocketEvent socketEvent) {
        if(!mCustomEventMap.containsKey(eventName)) {
            mCustomEventMap.put(eventName, new LinkedList<SparkSocketEvent>());
        }
        mCustomEventMap.get(eventName).add(socketEvent);
    }



    // DATA MANAGEMENT

    public Task<Boolean> readInput() {
        final Task<Boolean>.TaskCompletionSource done = Task.create();
        try {
            // Check for input
            mChunk = mInputReader.readLine();
            if(mChunk == null) {
                done.setResult(false);
            } else {

                // Register Connection
                receivedConnection();

                // Read new data
                mChunk = URLDecoder.decode(mChunk, "UTF-8");
                JSONObject messageObj = new JSONObject(mChunk);
                if(messageObj.length() < 1) {
                    Log.d(TAG, "Message was empty");
                    done.setResult(false);
                } else {

                    // Delegate message
                    String messageType = messageObj.getString("type");
                    if (messageType.equals(SOCKET_MESSAGE_HEARTBEAT)) {
                        processHeartbeat();
                        done.setResult(true);
                    } else if (messageType.equals(SOCKET_MESSAGE_DATA)) {
                        processData(messageObj);
                        done.setResult(true);
                    } else {
                        Log.d(TAG, "Received socket message of unknown type: " + messageType);
                        done.setResult(false);
                    }
                }
            }
        } catch(Exception e) {
            Utility.finishTask(done, e);
        } finally {
            return done.getTask();
        }
    }

    public void processData(JSONObject obj) {
        JSONObject data = new JSONObject();
        try {
            data = obj.getJSONObject("data");
        } catch (Exception e ) {
            e.printStackTrace();
        } finally {
            triggerEvent(mDataEventQueue, data);
        }

        // If this data is tied to an event then emit that that event
        try {
            String eventName = obj.getString("event");
            if(eventName != null || eventName.length() < 1) {
                if(mCustomEventMap.containsKey(eventName)) {
                    LinkedList<SparkSocketEvent> eventQueue = mCustomEventMap.get(eventName);
                    triggerEvent(eventQueue, obj);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void writeToSocket(String type) {
        writeToSocket(type, null, new JSONObject());
    }

    private void writeToSocket(String type, JSONObject obj) {
        writeToSocket(type, null, obj);
    }

    private void writeToSocket(String type, String eventType, JSONObject obj) {
        JSONObject netObj = new JSONObject();
        try {
            netObj.put("clientID", SPARK_CLIENT_ID);
            netObj.put("type", type);
            netObj.put("event", eventType);
            netObj.put("data", obj);
            String encodedString = URLEncoder.encode(obj.toString(), "UTF-8");
            mOutputWriter.println(encodedString);
            mOutputWriter.flush();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }



    // CONNECTION MANAGEMENT

    public void processHeartbeat() {
        Log.d(TAG, "Received heartbeat");
        mLastClientHeartbeat = 0;
    }

    public void sendHeartbeat() {
        writeToSocket(SOCKET_MESSAGE_HEARTBEAT);
    }

    public void receivedConnection() {
        mNumOfReconnects = 0;
        mLastClientHeartbeat = 0;

        if(!mIsConnected) {
            mIsConnected = true;
            if(mEverConnected) {
                triggerEvent(mReconnectEventQueue);
            }
        }

        if(!mEverConnected) {
            mEverConnected = true;
            triggerEvent(mConnectEventQueue);
        }


    }



    // CLEANUP

    public void close() {
        mIsConnected = true;
        try {
            if(mSocket != null && !mSocket.isClosed())
                mSocket.close();
            if(mInputReader != null)
                mInputReader.close();
            if(mOutputWriter != null)
                mOutputWriter.close();
        } catch( Exception e ) {
            e.printStackTrace();
        } finally {
            triggerEvent(mDisconnectEventQueue, new JSONObject());
        }
    }
}
