package com.industry.sparkterminalclient.event;

/**
 * Created by Two on 2/5/2015.
 */
public interface IEventEmitter {
    public void emitEvent(String eventName, Object... args);
}
