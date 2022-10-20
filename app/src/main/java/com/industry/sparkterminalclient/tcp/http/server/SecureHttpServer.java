package com.industry.sparkterminalclient.tcp.http.server;

import com.industry.sparkterminalclient.tcp.http.HttpConstants;
import com.industry.sparkterminalclient.tcp.http.handlers.handshake.GetSessionIdHandler;
import com.industry.sparkterminalclient.tcp.http.handlers.handshake.GetUniqueRandomHandler;
import com.industry.sparkterminalclient.tcp.http.requesthandler.IHttpHandshakeRequestHandler;

/**
 * Created by Two on 2/10/2015.
 */
public abstract class SecureHttpServer extends BaseHttpServer {
    // listen on secure channels
    // handle message back and forth


    // Members
    private IHttpHandshakeRequestHandler mGetSessionIdHandler = new GetSessionIdHandler();
    private IHttpHandshakeRequestHandler mGetUniqueRandomHandler = new GetUniqueRandomHandler();


    // Constructor
    public SecureHttpServer() {
        setupSecurityHandlers();
    }


    // Methods
    private void setupSecurityHandlers() {
        getHttpServer().post(HttpConstants.ROUTE_REQUEST_UNIQUE_RANDOM, mGetSessionIdHandler);
        getHttpServer().post(HttpConstants.ROUTE_REQUEST_SESSION_ID, mGetUniqueRandomHandler);
    }
}
