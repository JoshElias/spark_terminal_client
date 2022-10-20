package com.industry.sparkterminalclient.tcp.http;

/**
 * Created by Two on 2/10/2015.
 */
public class HttpConstants {

    // DATA PACKAGE


    // Request
    public static final String DATA_PACKAGE_KEY = "DATAP_PACKAGE_KEY";
    public static final String MESSAGE_DATA_KEY = "MESSAGE_DATA_KEY";

    // Response
    public static final String HTTP_RESULT_KEY = "HTTP_RESULT_KEY";
    public static final String HTTP_ERROR_KEY = "HTTP_ERROR_KEY";

    // Secure
    public static final String DATA_SIGNATURE_KEY = "DATA_SIGNATURE_KEY";
    public static final String SESSION_ID_KEY = "SESSION_ID_KEY";
    public static final String UNIQUE_RANDOM_KEY = "UNIQUE_RANDOM_KEY";



    // REQUEST HANDLER ROUTES

    // App
    public static final String ROUTE_LAUNCH_APP = "/launchApp";
    public static final String ROUTE_GET_INSTALLED_APP_LIST= "/getInstalledAppList";
    public static final String ROUTE_ENABLE_APP = "/enableApp";
    public static final String ROUTE_DISABLE_APP = "/disableApp";

    // Launcher
    public static final String ROUTE_ENABLE_SPARK_LAUNCHER = "/enableSparkLauncher";
    public static final String ROUTE_ENABLE_ANDROID_LAUNCHER = "/enableAndroidLauncher";

    // Signage
    public static final String ROUTE_GET_INSTALLED_SIGNAGE_LOOP = "/getInstalledSignageLoop";
    public static final String ROUTE_GET_DEVICE_INFO = "/getDeviceInfo";
    public static final String ROUTE_RESTRICT_DEVICE_PERMISSIONS = "/restrictDevicePermissions";

    // Wifi
    public static final String ROUTE_GET_AVAILABLE_WIFI_NETWORKS = "/getAvailableWifiNetworks";
    public static final String ROUTE_CONNECT_TO_WIFI_NETWORK = "/connectToWifiNetwork";
    public static final String ROUTE_DISCONNECT_FROM_WIFI_NETWORK = "/disconnectFromWifiNetwork";
    public static final String ROUTE_CURRENT_WIFI_SSID = "/currentWifiSSID";
    public static final String ROUTE_IS_WIFI_ENABLED = "/isWifiEnabled";

    // Analytics
    public static final String ROUTE_ANALYTICS_EVENT = "/analyticsEvent";

    // Security
    public static final String ROUTE_REQUEST_UNIQUE_RANDOM = "/requestUniqueRandom";
    public static final String ROUTE_REQUEST_SESSION_ID = "/requestSessionId";
}
