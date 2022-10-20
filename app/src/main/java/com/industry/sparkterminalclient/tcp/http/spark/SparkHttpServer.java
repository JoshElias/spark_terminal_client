package com.industry.sparkterminalclient.tcp.http.spark;

import com.industry.sparkterminalclient.tcp.http.requesthandler.IHttpRequestHandler;
import com.industry.sparkterminalclient.tcp.http.server.IHttpServer;
import com.industry.sparkterminalclient.tcp.http.koush.KoushHttpServerAdapter;
import com.industry.sparkterminalclient.tcp.http.server.SecureHttpServer;

/**
 * Created by Two on 2/10/2015.
 */
public class SparkHttpServer extends SecureHttpServer implements IHttpServer {
    private static final String TAG = SparkHttpServer.class.getName();


    // Members
    private KoushHttpServerAdapter mKoushServerAdapter = new KoushHttpServerAdapter();


    public void listen(final int port) {
        mKoushServerAdapter.listen(port);
    }

    public void post(final String url, final IHttpRequestHandler handler) {
       mKoushServerAdapter.post(url, handler);
    }
}
