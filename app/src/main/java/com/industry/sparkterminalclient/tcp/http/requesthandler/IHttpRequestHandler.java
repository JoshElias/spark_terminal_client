package com.industry.sparkterminalclient.tcp.http.requesthandler;

import com.industry.sparkterminalclient.tcp.http.request.IHttpRequest;
import com.industry.sparkterminalclient.tcp.http.response.IHttpResponse;

/**
 * Created by Two on 2/10/2015.
 */
public interface IHttpRequestHandler {
    public void onRequest(IHttpRequest request, IHttpResponse response);
}
