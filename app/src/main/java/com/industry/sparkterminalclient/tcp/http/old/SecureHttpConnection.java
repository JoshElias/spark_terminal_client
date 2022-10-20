package com.industry.sparkterminalclient.tcp.http.old;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.industry.sparkterminalclient.tcp.http.HttpException;
import com.industry.sparkterminalclient.tcp.interfaces.ISparkSecureHttpConnection;
import com.industry.sparkterminalclient.security.AES;
import com.industry.sparkterminalclient.security.APIKey;
import com.industry.sparkterminalclient.security.HMAC;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import org.json.JSONObject;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Two on 1/14/2015.
 */
public class SecureHttpConnection implements ISparkSecureHttpConnection {
    private static final String TAG = SecureHttpConnection.class.getName();

    static private final String SECURE_RANDOM_ALG = "SHA1PRNG";

    //static private final String UNIQUE_RANDOM_REQUEST = "UNIQUE_RANDOM_REQUEST";
    //static private final String SESSION_ID_REQUEST = "SESSION_ID_REQUEST";

    static private final String SESSION_ID_KEY = "SESSION_ID_KEY";
    static private final String MESSAGE_TYPE_KEY = "MESSAGE_TYPE_KEY";
    static private final String MESSAGE_DATA_KEY = "MESSAGE_DATA_KEY";
    static private final String DATA_PACKAGE_KEY = "DATA_PACKAGE_KEY";
    static private final String DATA_SIGNATURE_KEY = "DATA_SIGNATURE_KEY";
    static private final String CLIENT_ID_KEY = "CLIENT_ID_KEY";
    static private final String UNIQUE_RANDOM_KEY = "UNIQUE_RANDOM_KEY";
    static private final String TOP_STRING_KEY = "TOP_STRING_KEY";

    static private final int MONITOR_SESSION_INTERVAL = 60000; // 1 minute
    static private final int SESSION_ID_CURFEW = 300000; // 5 minutes

    private static final int CLIENT_LOBBY = 42;



    private AES mAES = new AES();
    private APIKey mApiKey = new APIKey();
    private ConcurrentHashMap<Integer, Integer> mRandomUniqueMap = new ConcurrentHashMap<Integer, Integer>();
    private ConcurrentHashMap<Integer, Session> mSessionMap = new ConcurrentHashMap<Integer, Session>();
    private Handler mMonitorSessionsHandler = new Handler(Looper.getMainLooper());
    private AtomicBoolean mIsMonitoringSessions = new AtomicBoolean(false);

    class Session {
        private int id;
        private int clientId;
        private long timestamp;

        public Session(int _clientId) {
            id = UUID.randomUUID().hashCode();
            clientId = _clientId;
            timestamp = new Date().getTime();
        }
    }




    // HTTP Request

    // DataPackage
        // MessageType
        // MessageData
        // SessionID
    // Signature

    public JSONObject packageData(JSONObject messageData, int sessionId) {
        try {
            // Package the data
            JSONObject dataPackage = new JSONObject();
            //dataPackage.put(MESSAGE_TYPE_KEY, messageType);
            dataPackage.put(MESSAGE_DATA_KEY, messageData);
            dataPackage.put(SESSION_ID_KEY, sessionId);

            // Sign Data
            byte[] dataSignature = HMAC.sign(dataPackage, mApiKey.getKeyBytes());

            // Prepare final package
            JSONObject finalPackage = new JSONObject();
            finalPackage.put(DATA_SIGNATURE_KEY, dataSignature);
            finalPackage.put(DATA_PACKAGE_KEY, dataPackage);

            // Encrypt Final Package
            return mAES.encrypt(finalPackage);

        } catch(Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    private JSONObject unpackDataNoSession(AsyncHttpServerRequest request) throws Exception {

        // Get data from request
        JSONObject requestData = LobbyHttpInterface.getJSONFromRequest(request);
        JSONObject packedData = new JSONObject( requestData.getString(TOP_STRING_KEY) );

        // Decrypt the message with AES
        JSONObject decryptedMessage = mAES.decrypt(packedData);

        // Verify HMAC
        JSONObject dataPackage = decryptedMessage.getJSONObject(DATA_PACKAGE_KEY);
        if(!HMAC.verify(dataPackage, mApiKey.getKeyBytes(), dataPackage.getString(DATA_SIGNATURE_KEY))) {
            throw new HttpException.InvalidDataSignatureException();
        }

       return dataPackage;
    }

    public JSONObject unpackData(AsyncHttpServerRequest request) throws Exception {
        JSONObject unpackedData = unpackDataNoSession(request);
        verifySession(unpackedData);
        return unpackedData;
    }

    private void verifySession(JSONObject obj) throws Exception {
        // Do we even have this session id cached?
        int sessionId = obj.getInt(SESSION_ID_KEY);
        if (!mSessionMap.containsKey(sessionId)) {
            Log.e(TAG, "REQUEST DROPPED - DID NOT RECOGNIZE SESSION ID");
            throw new HttpException.InvalidSessionIdException();
        }

        // Is it associated with the correct client?
        Session session = mSessionMap.get(sessionId);
        int clientId = obj.getInt(CLIENT_ID_KEY);
        if(clientId != session.clientId) {
            Log.e(TAG, "REQUEST DROPPED - DIDNT MATCH CLIENT");
            throw new HttpException.InvalidSessionIdException();
        }
    }

    public HttpServerRequestCallback getRequestUniqueRandomHandler() {
        return new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                Log.e(TAG, "Requesting Unique Id HAndler");
                try {
                    JSONObject dataPackage = unpackDataNoSession(request);

                    // Keep track which client is requesting which unique random
                    int clientId = dataPackage.getInt(CLIENT_ID_KEY);
                    int uniqueRandom = UUID.randomUUID().hashCode();
                    mRandomUniqueMap.put(clientId, uniqueRandom);

                    // Package Message Data
                    JSONObject messageData = new JSONObject();
                    messageData.put(UNIQUE_RANDOM_KEY, uniqueRandom);
                    JSONObject responseObj = packageData(messageData, 0);

                    // Respond
                    response.send(responseObj);

                } catch(HttpException e) {
                    LobbyHttpInterface.respondWithError(response, e.getCode());
                } catch (Exception e) {
                    e.printStackTrace();
                    LobbyHttpInterface.respondWithError(response, HttpException.GENERAL_HTTP_EXCEPTION);
                }

            }
        };
    }

    public HttpServerRequestCallback getRequestSessionIdHandler() {
        return new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                Log.e(TAG, "Requesting SessionId HAndler");
                try {
                    JSONObject dataPackage = unpackDataNoSession(request);

                    // Verify unique random
                    int clientId = dataPackage.getInt(CLIENT_ID_KEY);
                    JSONObject messageData = dataPackage.getJSONObject(MESSAGE_DATA_KEY);
                    int uniqueRandom = messageData.getInt(UNIQUE_RANDOM_KEY);
                    if (!mRandomUniqueMap.containsKey(clientId) || !mRandomUniqueMap.get(clientId).equals(uniqueRandom)) {
                        throw new HttpException.InvalidUniqueRandomException();
                    }

                    // Assign this client a session and monitor it
                    Session session = new Session(clientId);
                    mSessionMap.put(session.id, session);
                    startMonitoringSessions();

                    // Package Data and Respond
                    JSONObject responseObj = packageData(new JSONObject(), session.id);
                    response.send(responseObj);

                } catch(HttpException e) {
                    LobbyHttpInterface.respondWithError(response, e.getCode());
                } catch (Throwable t) {
                    LobbyHttpInterface.respondWithError(response, HttpException.GENERAL_HTTP_EXCEPTION);
                }
            }
        };
    }

    private void startMonitoringSessions() {
        if (!mIsMonitoringSessions.get() || !mSessionMap.isEmpty()) {
            mIsMonitoringSessions.set(true);

            mMonitorSessionsHandler.post(new Runnable() {
                @Override
                public void run() {

                    // Clear out expired sessions
                    for (Map.Entry<Integer, Session> entry : mSessionMap.entrySet()) {
                        Session session = entry.getValue();
                        if ((new Date().getTime() - session.timestamp) > SESSION_ID_CURFEW) {
                            mSessionMap.remove(entry.getKey());
                        }
                    }

                    // Any more sessions to check
                    if(mSessionMap.isEmpty()) {
                       mIsMonitoringSessions.set(false);
                    } else {
                        mMonitorSessionsHandler.postDelayed(this, MONITOR_SESSION_INTERVAL);
                    }
                }
            });
        }
    }
}
