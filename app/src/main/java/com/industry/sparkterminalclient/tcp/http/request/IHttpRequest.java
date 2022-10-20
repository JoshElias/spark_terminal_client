package com.industry.sparkterminalclient.tcp.http.request;

import org.json.JSONObject;

/**
 * Created by Two on 2/8/2015.
 */
public interface IHttpRequest {
    public JSONObject getMessageData() throws Exception;
}
