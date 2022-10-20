package com.industry.sparkterminalclient.tcp.http.response;

import org.json.simple.JSONValue;

/**
 * Created by Two on 2/8/2015.
 */
public interface IHttpResponse {
    public void sendError(Exception e);
    public void sendSuccess(JSONValue val);
}
