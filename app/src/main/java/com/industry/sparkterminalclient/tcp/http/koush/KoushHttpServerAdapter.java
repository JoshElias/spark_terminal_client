package com.industry.sparkterminalclient.tcp.http.koush;

import com.industry.sparkterminalclient.tcp.http.request.IHttpRequest;
import com.industry.sparkterminalclient.tcp.http.requesthandler.IHttpHandshakeRequestHandler;
import com.industry.sparkterminalclient.tcp.http.requesthandler.IHttpRequestHandler;
import com.industry.sparkterminalclient.tcp.http.requesthandler.IHttpSecureRequestHandler;
import com.industry.sparkterminalclient.tcp.http.response.IHttpResponse;
import com.industry.sparkterminalclient.tcp.http.server.IHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

/**
 * Created by Two on 2/9/2015.
 */
public class KoushHttpServerAdapter implements IHttpServer {
    private static final String TAG = KoushHttpServerAdapter.class.getName();


    // Members
    private AsyncHttpServer mHttpServer = new AsyncHttpServer();


    // Methods
    public void listen(final int port) {
        mHttpServer.listen(port);
    }

    public void post(final String url, final IHttpRequestHandler handler) {
        mHttpServer.post(url, new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {

                IHttpResponse responseAdapter = new KoushHttpResponseAdapter(response);
                IHttpRequest requestAdapter;
                try {
                    requestAdapter = new KoushHttpRequestAdapter(request);
                    handler.onRequest(requestAdapter, responseAdapter);
                } catch(Exception e) {
                    // Respond with general error
                    responseAdapter.sendError(e);
                }
            }
        });
    }

    public void post(final String url, final IHttpHandshakeRequestHandler handler) {
        mHttpServer.post(url, new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {

                IHttpResponse responseAdapter = new KoushHttpResponseAdapter(response);
                IHttpRequest requestAdapter;
                try {
                    requestAdapter = new KoushHttpRequestAdapter(request);
                    handler.onRequest(requestAdapter, responseAdapter);
                } catch( Exception e ) {
                    // Respond general error
                    // Respond if AES failed
                    responseAdapter.sendError(e);
                }
            }
        });
    }

    public void post(final String url, final IHttpSecureRequestHandler handler) {
        mHttpServer.post(url, new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                IHttpResponse responseAdapter = new KoushHttpResponseAdapter(response);
                IHttpRequest requestAdapter;
                try {
                    requestAdapter = new KoushHttpRequestAdapter(request);
                    handler.onRequest(requestAdapter, responseAdapter);
                } catch(Exception e) {
                    // Respond general error
                    // Respond if AES failed
                    // Respond Session Id expired
                    responseAdapter.sendError(e);
                }
            }
        });
    }
}
