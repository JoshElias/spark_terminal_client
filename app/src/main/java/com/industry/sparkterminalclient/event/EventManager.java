package com.industry.sparkterminalclient.event;


import android.content.Context;
import android.util.Log;

import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Two on 1/5/2015.
 */
public class EventManager implements IEventManager {
    private final static String TAG = EventManager.class.getName();


    // Dependencies
    private Context mContext;


    // Members
    private ConcurrentHashMap<String,Vector<IEventListener>> mEventEmitters =
            new ConcurrentHashMap<String,Vector<IEventListener>>();


    // Singleton
    private static EventManager mInstance;
    public static synchronized EventManager getInstance(Context context) {
        if(mInstance == null) {
            mInstance = new EventManager(context);
        }
        return mInstance;
    }


    // Constructor
    public EventManager(Context context) {
        mContext = context;
    }


    // Methods
    public synchronized void emitEvent(String eventName, Object... args) {
        if(eventName == null || eventName.isEmpty() || !mEventEmitters.containsKey(eventName)) {
            Log.e(TAG, "Did not emit invalid event");
            return;
        }

        Vector<IEventListener> listeners = mEventEmitters.get(eventName);
        for( IEventListener listener : listeners) {
            listener.onEvent(eventName, args);
        }
    }

    public synchronized void registerListener(String eventName, IEventListener listener) {
        if(eventName == null || eventName.isEmpty() || listener == null) {
            Log.e(TAG, "Did not register invalid event");
            return;
        }

        if(!mEventEmitters.containsKey(eventName)) {
            mEventEmitters.put(eventName, new Vector<IEventListener>());
        }
        Vector<IEventListener> listeners = mEventEmitters.get(eventName);
        listeners.add(listener);
    }

    public synchronized void unregisterListener(String eventName, IEventListener listener) {
        if(eventName == null || eventName.isEmpty() || listener == null) {
            Log.e(TAG, "Did not unregister invalid event");
            return;
        }

        if(mEventEmitters.containsKey(eventName)) {
            Vector<IEventListener> listeners = mEventEmitters.get(eventName);
            int index = listeners.indexOf(listener);
            if(index != -1) {
                listeners.remove(index);
            }
            if(listeners.size() < 1) {
                mEventEmitters.remove(eventName);
            }
        }
    }
}
