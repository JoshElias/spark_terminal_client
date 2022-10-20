package com.industry.sparkterminalclient.tcp.http.old;

import android.content.Context;
import android.util.Log;

import com.industry.sparkterminalclient.tcp.http.HttpException;
import com.industry.sparkterminalclient.tcp.interfaces.ISparkLobbyHttpHandlers;
import com.industry.sparkterminalclient.tcp.interfaces.ISparkSecureHttpConnection;
import com.industry.sparkterminalclient.wifi.WifiUtility;
import com.koushikdutta.async.http.body.JSONObjectBody;
import com.koushikdutta.async.http.body.MultipartFormDataBody;
import com.koushikdutta.async.http.body.StringBody;
import com.koushikdutta.async.http.body.UrlEncodedFormBody;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import bolts.Continuation;
import bolts.Task;

/**
 * Created by Two on 11/25/2014.
 */

public class LobbyHttpInterface implements ISparkLobbyHttpHandlers {
    private static final String TAG = LobbyHttpInterface.class.getName();


    // Constants
    private static final int HTTP_PORT = 4000;
    static private final String MESSAGE_DATA_KEY = "MESSAGE_DATA_KEY";


    // Dependencies
    private Context mContext;
    private IAppStartHandler mAppStartHandler;
    //private IAppStarter mAppStarter;
    //private IAppToggler mAppToggler;
    //private IWifiConnector mWifiConnector;
    //private IWifiDisconnector mWifiDisconnector;
    //private IWifiScanner mWifiScanner;


    // Members
    private AsyncHttpServer mServer = new AsyncHttpServer();
    private ISparkSecureHttpConnection mSecureLocalConnection = new SecureHttpConnection();


    // Singleton
    private static LobbyHttpInterface mInstance;
    public static LobbyHttpInterface getInstance(Context context) {
        if(mInstance == null) {
            mInstance = new LobbyHttpInterface(context);
        }
        return mInstance;
    }


    // Constructor
    private LobbyHttpInterface(Context context) {
        mContext = context;
        mAppStartHandler = new AppStartHandler();
        //mAppStarter = AppStarter.getInstance(context);
        //mAppToggler = AppToggler.getInstance(context);
        //mWifiConnector = WifiConnector.getInstance(context);
        //mWifiDisconnector = WifiDisconnector.getInstance(context);
        //mWifiScanner = WifiScanner.getInstance(context);

        setupHandlers();
        mServer.listen(HTTP_PORT);
    }



    // Methods
    public void setupHandlers() {

        // App
        mServer.post("/launchApp", launchAppHandler());
        mServer.post("/getInstalledAppList", getInstalledAppListHandler());
        mServer.post("/enableApp", enableAppHandler());
        mServer.post("/disableApp", disableAppHandler());

        // Launcher
        mServer.post("/enableSparkLauncher", enableSparkLauncherHandler());
        mServer.post("/enableAndroidLauncher", enableAndroidLauncherHandler());

        // Signage
        mServer.post("/getInstalledSignageLoop", getInstalledSignageLoopHandler());
        mServer.post("/getDeviceInfo", getDeviceInfoHandler());
        mServer.post("/restrictDevicePermissions", restrictDevicePermissionsHandler());

        // Wifi
        mServer.post("/getAvailableWifiNetworks", getAvailableWifiNetworksHandler());
        mServer.post("/connectToWifiNetwork", connectToWifiNetworkHandler());
        mServer.post("/disconnectFromWifiNetwork", disconnectFromWifiNetworkHandler());
        mServer.post("/currentWifiSSID", currentWifiHandler());
        mServer.post("/isWifiEnabled", isWifiEnabledHandler());

        // Analytics
        mServer.post("/analyticsEvent", analyticsEventHandler());

        // Security
        mServer.post("/requestUniqueRandom", mSecureLocalConnection.getRequestUniqueRandomHandler());
        mServer.post("/requestSessionId", mSecureLocalConnection.getRequestSessionIdHandler());
    }


    // Handlers
    public HttpServerRequestCallback launchAppHandler() {
        return new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
                Log.d(TAG, "LaunchAppHandler Triggered");

                try {
                    JSONObject json = getMessageData(request);
                    String packageName = json.getString("packageName");
                    Log.e(TAG, "Package name from client: ");
                    mAppStarter.startApp(packageName).continueWith(new Continuation<Void, Void>() {
                        @Override
                        public Void then(Task<Void> task) throws Exception {
                            Log.d(TAG, "Launching did complete");
                            JSONObject data = new JSONObject();
                            if (task.isFaulted()) {
                                data.put("error", task.getError().getMessage());
                                task.getError().printStackTrace();
                            }
                            Log.d(TAG, "Sending success launch http");
                            response.send(data);
                            return null;
                        }
                    });

                } catch(Exception e) {
                    // Respond with error
                    e.printStackTrace();
                    JSONObject data = new JSONObject();
                    try {
                        data.put("error", e.getMessage());
                    } catch (Exception ee) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "Sending fail launch http");
                    response.send(data);
                }

            }
        };
    }

    // NEEDS KE
    public HttpServerRequestCallback getInstalledAppListHandler() {
        return new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                Log.d(TAG, "Inside getInstalledAppListHandler");
                JSONObject data = new JSONObject();
                try {
                    //String[] packageNames = mKnoxManager.getInstalledApplicationsIDList();
                    //JSONArray array = new JSONArray(Arrays.asList(packageNames));
                    //data.put("result", array);
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        data.put("error", e.getMessage());
                    } catch (Exception ee) {
                        ee.printStackTrace();
                    }
                } finally {
                    Log.d(TAG, "SENDING DATA TO CLIENT");
                    response.send(data);
                }
            }
        };
    }

    public HttpServerRequestCallback enableAppHandler() {
        return new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
                Log.d(TAG, "EnableAppHandler Triggered");

                try {
                    JSONObject json = getJSONFromRequest(request);
                    String packageName = json.getString("packageName");

                    mAppToggler.enableApp(packageName).continueWith(new Continuation<Void, Void>() {
                        @Override
                        public Void then(Task<Void> task) throws Exception {
                            Log.d(TAG, "Enabling did complete");
                            JSONObject data = new JSONObject();
                            if(task.isFaulted()) {
                                data.put("error", task.getError().getMessage());
                                task.getError().printStackTrace();
                            }
                            Log.d(TAG, "Sending success enable http");
                            response.send(data);
                            return null;
                        }
                    });

                } catch(Exception e) {
                    // Respond with error
                    JSONObject data = new JSONObject();
                    try {
                        data.put("error", e.getMessage());
                    } catch (Exception ee) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "Sending fail launch http");
                    response.send(data);
                }

            }
        };
    }

    public HttpServerRequestCallback disableAppHandler() {
        return new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
                Log.d(TAG, "DisableAppHandler Triggered");

                try {
                    JSONObject json = getJSONFromRequest(request);
                    String packageName = json.getString("packageName");

                    mAppToggler.disableApp(packageName).continueWith(new Continuation<Void, Void>() {
                        @Override
                        public Void then(Task<Void> task) throws Exception {
                            Log.d(TAG, "Disabling did complete");
                            JSONObject data = new JSONObject();
                            if (task.isFaulted()) {
                                data.put("error", task.getError().getMessage());
                                task.getError().printStackTrace();
                            }
                            Log.d(TAG, "Sending success disable http");
                            response.send(data);
                            return null;
                        }
                    });

                } catch (Exception e) {
                    // Respond with error
                    JSONObject data = new JSONObject();
                    try {
                        data.put("error", e.getMessage());
                    } catch (Exception ee) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "Sending fail launch http");
                    response.send(data);
                }

            }
        };
    }

    public HttpServerRequestCallback getInstalledSignageLoopHandler() {
        return new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                Log.d(TAG, "GetInstalledSignageLoopHandler Triggered");

            }
        };
    }

    public HttpServerRequestCallback getAvailableWifiNetworksHandler() {
        return new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
                Log.d(TAG, "GetAvailableWifiNetworksHandler Triggered");

                final JSONObject responseObj = new JSONObject();
                try {

                    mWifiScanner.getAvailableWifiNetworksJSON().continueWith(new Continuation<JSONArray, Void>() {
                        @Override
                        public Void then(Task<JSONArray> task) throws Exception {

                            if (task.isFaulted()) {
                                responseObj.put("error", task.getError().getMessage());
                                task.getError().printStackTrace();
                            } else {
                                responseObj.put("result", task.getResult());
                            }
                            response.send(responseObj);
                            return null;
                        }
                    });
                } catch (Exception e) {
                    // Respond with error
                    try {
                        responseObj.put("error", e.getMessage());
                    } catch (Exception ee) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "Sending fail launch http");
                    response.send(responseObj);
                }
            }
        };
    }

    public HttpServerRequestCallback connectToWifiNetworkHandler() {
        return new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
                Log.d(TAG, "ConnectToWifiNetworkHandler Triggered");

                final JSONObject responseObj = new JSONObject();
                try {

                    JSONObject requestJson = getJSONFromRequest(request);
                    JSONObject messageData = new JSONObject(requestJson.getString(MESSAGE_DATA_KEY));
                    String ssid = messageData.getString("ssid");
                    String password = messageData.getString("password");
                    String encryptionType = messageData.getString("encryptionType");

                    mWifiConnector.connectToWifi(ssid, password, encryptionType).continueWith(new Continuation<Void, Void>() {
                        @Override
                        public Void then(Task<Void> task) throws Exception {
                            if (task.isFaulted()) {
                                Log.e(TAG, "connected to WIFI FAULTED");
                                responseObj.put("error", task.getError().getMessage());
                                task.getError().printStackTrace();
                            } else {
                                Log.e(TAG, "connected to WIFI SUCCESS");
                            }
                            response.send(responseObj);
                            return null;
                        }
                    });
                } catch (Exception e) {
                    // Respond with error
                    try {
                        responseObj.put("error", e.getMessage());
                    } catch (Exception ee) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "Sending fail launch http");
                    response.send(responseObj);
                }
            }
        };
    }

    public HttpServerRequestCallback disconnectFromWifiNetworkHandler() {
        return new HttpServerRequestCallback() {
            @Override
            public void onRequest(final AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
                Log.d(TAG, "DisconnectFromWifiNetworkHandler Triggered");
                Log.e(TAG, "Pre Response: "+response.getHeaders().toString());

                final JSONObject responseObj = new JSONObject();
                try {

                    mWifiDisconnector.disconnectFromWifi().continueWith(new Continuation<Void, Void>() {
                        @Override
                        public Void then(Task<Void> task) throws Exception {
                            if (task.isFaulted()) {
                                Log.e(TAG, "disconnect from WIFI FAULTED");
                                responseObj.put("error", task.getError().getMessage());
                                task.getError().printStackTrace();
                            } else {
                                Log.e(TAG, "disconnect from WIFI SUCCESS");

                            }

                            Log.e(TAG, "Post Response: " + response.getHeaders().toString());
                            response.send(responseObj);
                            return null;
                        }
                    });
                } catch (Exception e) {
                    // Respond with error
                    try {
                        responseObj.put("error", e.getMessage());
                    } catch (Exception ee) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "Sending fail launch http");
                    response.send(responseObj);
                }
            }
        };
    }


    public HttpServerRequestCallback currentWifiHandler() {
        return new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                Log.d(TAG, "currentWifiHandler Triggered");

                final JSONObject responseObj = new JSONObject();
                try {

                    String currentNetwork = WifiUtility.getCurrentSSID(mContext);
                    responseObj.put("result", currentNetwork);
                    response.send(responseObj);

                } catch (Exception e) {
                    // Respond with error
                    try {
                        responseObj.put("error", e.getMessage());
                    } catch (Exception ee) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "Sending fail launch http");
                    response.send(responseObj);
                }
            }
        };
    }

    public HttpServerRequestCallback isWifiEnabledHandler() {
        return new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                Log.d(TAG, "isWifienabled Triggered");

                final JSONObject responseObj = new JSONObject();
                try {

                    responseObj.put("result", WifiUtility.isWifiEnabled(mContext));
                    response.send(responseObj);

                } catch (Exception e) {
                    // Respond with error
                    try {
                        responseObj.put("error", e.getMessage());
                    } catch (Exception ee) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "Sending fail launch http");
                    response.send(responseObj);
                }
            }
        };
    }


    public HttpServerRequestCallback getDeviceInfoHandler() {
        return new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                Log.d(TAG, "GetDeviceInfoHandler Triggered");
                //response.send(getTestObj());
            }
        };
    }

    public HttpServerRequestCallback restrictDevicePermissionsHandler() {
        return new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                Log.d(TAG, "RestrictDevicePermissionsHandler Triggered");
                //response.send(getTestObj());
            }
        };
    }

    public HttpServerRequestCallback analyticsEventHandler() {
        return new HttpServerRequestCallback() {
            @Override
            public void onRequest(final AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
                Log.d(TAG, "Analytics Event Handler Triggered");

                final JSONObject responseObj = new JSONObject();
                try {
                    JSONObject requestJson = getJSONFromRequest(request);
                    JSONObject messageData = new JSONObject(requestJson.getString(MESSAGE_DATA_KEY));



                } catch (Exception e) {
                    // Respond with error
                    try {
                        responseObj.put("error", e.getMessage());
                    } catch (Exception ee) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "Sending fail launch http");
                    response.send(responseObj);
                }
            }
        };
    }



    // SECURE HANDLERS

    public  HttpServerRequestCallback enableSparkLauncherHandler() {
        return new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                Log.d(TAG, "Inside enableSparkLauncherHandler");
                try {
                    mSecureLocalConnection.unpackData(request);


                } catch (HttpException e) {
                    respondWithError(response, e.getCode());
                } catch(Exception e) {
                    respondWithError(response, e.getMessage());
                }
            }
        };
    }

    public HttpServerRequestCallback enableAndroidLauncherHandler() {
        return new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                Log.d(TAG, "Inside getInstalledAppListHandler");
                JSONObject serverResponse = new JSONObject();
                try {
                    mSecureLocalConnection.unpackData(request);
                    // ENABLE ANDROID LAUNCHER
                    Log.e(TAG, "Wait what? Success?");

                } catch (HttpException e) {
                    respondWithError(response, e.getCode());
                } catch(Exception e) {
                    respondWithError(response, e.getMessage());
                }
            }
        };
    }

    private void enableSparkHandler() {
        /*
        if (!mKnox.allowSVoice(false)
                || !mKnox.allowTaskManager(false)
                || !mKnox.hideSystemBar(true)
                || !mKnox.allowMultiWindowMode(false)
                || !mKnox.enableKioskMode("com.IndustryCorp.SparkLobby")) {
            data.put("error", new Exception("Unable to set spark lobby as default launcher"));
        }

    } catch (Exception e) {
        e.printStackTrace();
        try {
            data.put("error", e.getMessage());
        } catch (Exception ee) {
            ee.printStackTrace();
        }
        */
    }



    public static JSONObject getMessageData(AsyncHttpServerRequest request) throws Exception {
        JSONObject requestData = getJSONFromRequest(request);
        String messageString = requestData.getString(MESSAGE_DATA_KEY);
        return new JSONObject(messageString);
    }

    public static JSONObject getJSONFromRequest(AsyncHttpServerRequest request) throws Exception {
        JSONObject json = new JSONObject();
        if (request.getBody() instanceof UrlEncodedFormBody) {
            UrlEncodedFormBody body = (UrlEncodedFormBody)request.getBody();
            for (NameValuePair pair: body.get()) {
                json.put(pair.getName(), pair.getValue());
            }
        }
        else if (request.getBody() instanceof JSONObjectBody) {
            json = ((JSONObjectBody)request.getBody()).get();
        }
        else if (request.getBody() instanceof StringBody) {
            json.put("foo", ((StringBody)request.getBody()).get());
        }
        else if (request.getBody() instanceof MultipartFormDataBody) {
            MultipartFormDataBody body = (MultipartFormDataBody)request.getBody();
            for (NameValuePair pair: body.get()) {
                json.put(pair.getName(), pair.getValue());
            }
        }
        return json;
    }

    public static void respondWithError(AsyncHttpServerResponse response, String errorCode) {
        try {
            JSONObject responseObj = new JSONObject();
            Log.e(TAG, "Responding with errorcOde: "+errorCode);
            responseObj.put("error", errorCode);
            response.send(responseObj);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}