package com.industry.sparkterminalclient.event;

import android.util.Log;

import java.util.Vector;

/**
 * Created by Two on 2/6/2015.
 */
public interface IEventManager {
    public void emitEvent(String eventName, Object... args);
    public void registerListener(String eventName, IEventListener listener);
    public void unregisterListener(String eventName, IEventListener listener);
}
