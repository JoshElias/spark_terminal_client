package com.industry.sparkterminalclient.tcp.http.requesthandler;

import com.industry.sparkterminalclient.tcp.http.request.IHttpRequest;
import com.industry.sparkterminalclient.tcp.http.request.IHttpSecureRequest;
import com.industry.sparkterminalclient.tcp.http.response.IHttpResponse;
import com.industry.sparkterminalclient.tcp.http.response.IHttpSecureResponse;

/**
 * Created by Two on 2/10/2015.
 */
public interface IHttpSecureRequestHandler {
    public void onRequest(IHttpSecureRequest request, IHttpSecureResponse response);
}
