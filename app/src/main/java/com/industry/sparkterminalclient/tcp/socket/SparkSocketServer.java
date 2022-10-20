package com.industry.sparkterminalclient.tcp.socket;

import android.os.Looper;
import android.util.Log;
import android.os.Handler;

import com.industry.sparkterminalclient.Utility;
import com.industry.sparkterminalclient.thread.ThreadManager;

import org.json.JSONObject;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Vector;

import bolts.Capture;
import bolts.Continuation;
import bolts.Task;

/**
 * Created by Two on 11/17/2014.
 */
public class SparkSocketServer {
    private static final String TAG = SparkSocketServer.class.getName();

    // CONNECTION CONSTANTS
    private static final int SOCKET_PORT = 3000;
    private static final String HOST_ADDRESS = "127.0.0.1";
    public static final int HEARTBEAT_TIMEOUT = 6500;
    public static final int HEARTBEAT_INTERVAL = 3000;
    public static final int RECONNECT_LIMIT = 5;
    public static int RECONNECT_INTERVAL = 4000;

    // SOCKET VARS
    private ServerSocket mServerSocket;
    private Vector mMessageQueue = new Vector();
    private Vector<SparkSocketClient> mSocketClients = new Vector<SparkSocketClient>();

    // EVENT EMITTER
    private static ArrayList<LobbySocketInterface.ISparkSocketServerEmitter> mServerEventEmitters = new ArrayList<LobbySocketInterface.ISparkSocketServerEmitter>();

    // THREADS AND TIMERS
    private volatile boolean mIsServerListening = true;
    private volatile boolean mIsReceivingInput = true;
    private Handler mSocketServerHandler = new Handler(Looper.getMainLooper());
    private Runnable mReadingInputRunnable;
    private Runnable mHeartbeatMonitor;
    private Runnable mReconnectionMonitor;



    // SINGLETON
    private static SparkSocketServer mInstance;
    public static SparkSocketServer getInstance(LobbySocketInterface.ISparkSocketServerEmitter emitter) {
        if(mInstance == null) {
            mInstance = new SparkSocketServer();
        }
        if(emitter != null) {
            mServerEventEmitters.add(emitter);
        }
        return mInstance;
    }
    public SparkSocketServer() {
        try {
            mServerSocket = new ServerSocket(SOCKET_PORT);
            Log.d(TAG, "Server Socket Listening: "+ mServerSocket.getInetAddress()+":"+mServerSocket.getLocalPort());
            startListening();
            startReadingInput();
            startHeartbeatMonitor();
            startReconnectionMonitor();
        } catch(Exception e) {
            e.printStackTrace();
            for(LobbySocketInterface.ISparkSocketServerEmitter emitter : mServerEventEmitters) {
                emitter.onError(e);
            }
        }
    }


    // SOCKET SERVER INTERFACE

    public SparkSocketClient getGetClient() throws Exception {
        return addSocketClient(new Socket(HOST_ADDRESS, SOCKET_PORT));
    }

    public SparkSocketClient addSocketClient(Socket socket) throws IOException {
        Log.e(TAG, "Adding new socket connection");
        SparkSocketClient socketClient = new SparkSocketClient(socket);
        mSocketClients.add(socketClient);
        for(LobbySocketInterface.ISparkSocketServerEmitter emitter : mServerEventEmitters) {
            emitter.onConnection(socketClient);
        }
        return socketClient;
    }

    public void removeSocketClient(SparkSocketClient client) {
        client.close();
        int index = mSocketClients.indexOf(client);
        if( index != -1) {
            mSocketClients.removeElementAt(index);
        }
    }

    public void emit(String eventName, JSONObject obj) {
        for(SparkSocketClient socketClient : mSocketClients) {
            socketClient.emit(eventName, obj);
        }
    }



    // CONNECTION MANAGEMENT

    private void startHeartbeatMonitor() {
        mHeartbeatMonitor = new Runnable() {
            @Override
            public void run() {
                for(SparkSocketClient socket : mSocketClients) {
                    socket.sendHeartbeat();
                }
                mSocketServerHandler.postDelayed(this, HEARTBEAT_INTERVAL);
            }
        };
        mSocketServerHandler.post(mHeartbeatMonitor);
    }

    private void startReconnectionMonitor() {
        mReconnectionMonitor = new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "Reconnection monitor tick");
                // Find which reconnecting clients must be deleted
                Vector<SparkSocketClient> clientsToBeDeleted = new Vector<SparkSocketClient>();
                for(SparkSocketClient socket : mSocketClients) {
                    socket.incrementReconnects();
                    if(socket.getNumOfReconnects() > SparkSocketServer.RECONNECT_LIMIT) {
                        clientsToBeDeleted.add(socket);
                    }
                }
                // Delete those old and tired clients RIP
                for(SparkSocketClient socket : clientsToBeDeleted) {
                    removeSocketClient(socket);
                }
                mSocketServerHandler.postDelayed(this, RECONNECT_INTERVAL);
            }
        };
        mSocketServerHandler.postDelayed(mReconnectionMonitor, 5000);
    }

    private void startListening() {
        ThreadManager.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                while (mIsServerListening) {
                    try {
                        Log.e(TAG, "Accepting incoming socket connection");
                        addSocketClient(mServerSocket.accept());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void startReadingInput() {
        mReadingInputRunnable = new Runnable() {
            @Override
            public void run() {
                if(mIsReceivingInput) {
                    readInput().continueWith(new Continuation<Boolean, Object>() {
                        @Override
                        public Object then(Task<Boolean> task) throws Exception {
                            if (task.isFaulted()) {
                                task.getError().printStackTrace();
                            } else {
                                if (task.getResult()) {
                                    mSocketServerHandler.postDelayed(mReadingInputRunnable, 10);
                                } else {
                                    mSocketServerHandler.postDelayed(mReadingInputRunnable, 500);
                                }
                            }
                            return null;
                        }
                    }, ThreadManager.getInstance());
                }
            }
        };
        mSocketServerHandler.post(mReadingInputRunnable);
    }

    public Task<Boolean> readInput() {
        final Task<Boolean>.TaskCompletionSource done = Task.create();
        try {

            final Capture<Boolean> socketsHaveData = new Capture<Boolean>(false);

            // Iterate over all socket clients and see if there is any data
            // Set socketsHaveData if there was any
            Task<Void> task = Task.forResult(null);
            for (final SparkSocketClient client : mSocketClients) {
                task = task.continueWithTask(new Continuation<Void, Task<Boolean>>() {
                    public Task<Boolean> then(Task<Void> ignored) throws Exception {
                        return client.readInput();
                    }
                }, ThreadManager.getInstance()).continueWith(new Continuation<Boolean, Void>() {
                    @Override
                    public Void then(Task<Boolean> task) throws Exception {
                        if (task.getResult()) {
                            socketsHaveData.set(true);
                        }
                        return null;
                    }
                }, ThreadManager.getInstance());
            }
            task.continueWith(new Continuation<Void, Object>() {
                @Override
                public Object then(Task<Void> task) throws Exception {
                    if (task.isFaulted()) {
                        done.setError(task.getError());
                    } else {
                        done.setResult(socketsHaveData.get());
                    }
                    return null;
                }
            }, ThreadManager.getInstance());

        } catch (Throwable t ) {
            Utility.finishTask(done, t);
        } finally {
            return done.getTask();
        }
    }


    // CLEANUP
    protected void close() {
        for(LobbySocketInterface.ISparkSocketServerEmitter emitter : mServerEventEmitters) {
            emitter.onClose();
        }
        mServerEventEmitters.clear();

        if(mSocketServerHandler != null) {
            mSocketServerHandler.removeCallbacks(mHeartbeatMonitor);
            mSocketServerHandler.removeCallbacks(mReconnectionMonitor);
        }

        mIsServerListening = false;
        mIsReceivingInput = false;
    }
}
