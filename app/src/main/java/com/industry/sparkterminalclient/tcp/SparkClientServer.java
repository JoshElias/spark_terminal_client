package com.industry.sparkterminalclient.tcp;

import com.industry.sparkterminalclient.tcp.http.handlers.app.AppStartHandler;
import com.industry.sparkterminalclient.tcp.http.requesthandler.IHttpRequestHandler;

/**
 * Created by Two on 2/8/2015.
 */
public class SparkClientServer {
    //Singleton class Manages Client Requests


    // Members
    private IHttpRequestHandler mStartHandler = new AppStartHandler();


    // http posts and socket ons


    public void setupHandlers() {
        mHttpServer.post("url", mStartHandler);
    }
}
