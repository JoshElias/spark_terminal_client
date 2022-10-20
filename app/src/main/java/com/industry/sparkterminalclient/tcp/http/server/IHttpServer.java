package com.industry.sparkterminalclient.tcp.http.server;


import com.industry.sparkterminalclient.tcp.http.requesthandler.IHttpHandshakeRequestHandler;
import com.industry.sparkterminalclient.tcp.http.requesthandler.IHttpRequestHandler;
import com.industry.sparkterminalclient.tcp.http.requesthandler.IHttpSecureRequestHandler;

/**
 * Created by Two on 2/8/2015.
 */
public interface IHttpServer {
    public void listen(final int port);
    public void post(String url, IHttpRequestHandler handler);
    public void post(String url, IHttpHandshakeRequestHandler handler);
    public void post(String url, IHttpSecureRequestHandler handler);
}
