package com.industry.sparkterminalclient.tcp.http.server;

import com.industry.sparkterminalclient.tcp.http.koush.KoushHttpServerAdapter;
import com.industry.sparkterminalclient.tcp.http.requesthandler.IHttpRequestHandler;

/**
 * Created by Two on 2/10/2015.
 */
public abstract class BaseHttpServer {

    // Members
    private IHttpServer mHttpServer = new KoushHttpServerAdapter();


    // Properties
    protected IHttpServer getHttpServer() {
        return mHttpServer;
    }


    // Methods
    public void listen(final int port) {
        mHttpServer.listen(port);
    }

    public void post(final String url, final IHttpRequestHandler handler) {
        mHttpServer.post(url, handler);
    }
}
