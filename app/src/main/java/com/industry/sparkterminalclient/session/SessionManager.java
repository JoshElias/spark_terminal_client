package com.industry.sparkterminalclient.session;

import android.content.Context;
import android.util.Log;

import com.industry.sparkterminalclient.event.EventManager;
import com.industry.sparkterminalclient.event.IEventListener;

import org.json.JSONObject;

import java.util.UUID;
import java.util.Vector;

/**
 * Created by Two on 2/3/2015.
 */
public class SessionManager {
    private final static String TAG = SessionManager.class.getName();


    // Dependencies
    private Context mContext;
    private EventManager mEventManager;


    // Members
    private UUID mLastUUID = null;
    private UUID mCurrentUUID = null;


    // Singleton
    private static SessionManager mInstance;
    public static synchronized SessionManager getInstance(Context context) {
        if(mInstance == null) {
            mInstance = new SessionManager(context);
        }
        return mInstance;
    }


    // Constructor
    private SessionManager(Context context) {
        mContext = context;
        mEventManager = EventManager.getInstance(context);
        listenForRuntimeEvents();
    }

    private UUID getNextSessionID() {
        if(mCurrentUUID != null) {
            mLastUUID = mCurrentUUID;
        }
        return mCurrentUUID = UUID.randomUUID();
    }


    private void listenForRuntimeEvents() {
        mRuntimeMonitor.addAppRuntimeListener( new IEventListener() {
            @Override
            public void onAppStart(String packageName) {

            }

            @Override
            public void onAppStop(String packageName) {

            }

            @Override
            public void onAppInstall(String packageName) {

            }

            @Override
            public void onAppUninstall(String packageName) {

            }

            @Override
            public void onIdle() {

            }

            @Override
            public void onBusy() {

            }

            @Override
            public void onSignageStart() {

            }

            @Override
            public void onSignageStop() {

            }
        });
    }


    // SESSION LOGIC ----- different class?

    private void startSession() {

    }

    private void endSession() {

    }


    // LISTENER LOGIC

    public void addListener(ISessionManager listener) {
        mAnalyticsEventListeners.add(listener);
    }

    public void removeListener(ISessionManager listener) {
        int index = mAnalyticsEventListeners.indexOf(listener);
        if(index != -1) {
            mAnalyticsEventListeners.remove(index);
        }
    }

    private void emitAnalyticsEvent(String eventName, JSONObject obj) {
        try {
            // Create message
            JSONObject message = new JSONObject();
            message.put(ANALYTICS_EVENT_NAME_KEY, eventName);
            message.put(ANALYTICS_DATA_KEY, obj);

            // Emit to all listeners
            for (ISessionManager listener : mAnalyticsEventListeners) {
                listener.onAnalyticsEvent(obj);
            }
        } catch(Exception e) {
            Log.e(TAG, "FAILED TO EMIT ANALYTICS EVENT");
            e.printStackTrace();
        }
    }
}
