package com.industry.sparkterminalclient.tcp.http.requesthandler;


import com.industry.sparkterminalclient.tcp.http.request.IHttpSecureRequest;
import com.industry.sparkterminalclient.tcp.http.response.IHttpSecureResponse;

/**
 * Created by Two on 2/10/2015.
 */
public interface IHttpHandshakeRequestHandler {
    public void onRequest(IHttpSecureRequest request, IHttpSecureResponse response);
}
