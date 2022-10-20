package com.industry.sparkterminalclient.event;

/**
 * Created by Two on 2/5/2015.
 */
public class EventConstants {

    // SESSION EVENT CONSTANTS
    public final static String EVENT_USER_SESSION_START = "EVENT_USER_SESSION_START";
    public final static String EVENT_USER_SSESSION_STOP = "EVENT_USER_SSESSION_STOP";
    public final static String EVENT_APP_SESSION_START = "EVENT_APP_SESSION_START";
    public final static String EVENT_APP_SESSION_STOP = "EVENT_APP_SESSION_STOP";


    // LOBBY EVENT CONSTANTS
    public final static String EVENT_LOBBY_DEBUG_MODE = "EVENT_LOBBY_DEBUG_MODE";
    public final static String EVENT_LOBBY_BRIGHTNESS_CHANGE = "EVENT_LOBBY_BRIGHTNESS_CHANGE";
    public final static String EVENT_LOBBY_VOLUME_CHANGE = "EVENT_LOBBY_VOLUME_CHANGE";
    public final static String EVENT_LOBBY_ACCESS_OPTIONS = "EVENT_LOBBY_ACCESS_OPTIONS";
    public final static String EVENT_LOBBY_REQUEST_SECRET_KEY = "EVENT_LOBBY_REQUEST_SECRET_KEY";


    // APP EVENT CONSTANTS
    public final static String EVENT_APP_DOWNLOAD = "EVENT_APP_DOWNLOAD";
    public final static String EVENT_APP_VERIFIED = "EVENT_APP_VERIFIED";
    public final static String EVENT_APP_INSTALL = "EVENT_APP_INSTALL";
    public final static String EVENT_APP_UPDATE = "EVENT_APP_UPDATE";
    public final static String EVENT_APP_UNINSTALL = "EVENT_APP_UNINSTALL";
    public final static String EVENT_APP_START = "EVENT_APP_START";
    public final static String EVENT_APP_STOP = "EVENT_APP_STOP";
    public final static String EVENT_APP_ENABLE = "EVENT_APP_ENABLE";
    public final static String EVENT_APP_DISABLE = "EVENT_APP_DISABLE";


    // STATE CONSTANTS
    public final static String EVENT_STATE_IDLE= "EVENT_STATE_IDLE";
    public final static String EVENT_STATE_BUSY = "EVENT_STATE_BUSY";


    // WIFI EVENT CONSTANTS
    public final static String EVENT_WIFI_CONNECT = "EVENT_WIFI_CONNECT";
    public final static String EVENT_WIFI_DISCONNECT = "EVENT_WIFI_DISCONNECT";
    public final static String EVENT_WIFI_SCAN = "EVENT_WIFI_SCAN";


    // CONTENT SYNC CONSTANTS
    public final static String EVENT_CONTENT_SYNC = "EVENT_CONTENT_SYNC";
}
