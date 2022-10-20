package com.industry.sparkterminalclient.tcp.http.request;

import org.json.JSONObject;

/**
 * Created by Two on 2/10/2015.
 */
public interface IHttpSecureRequest extends IHttpRequest {
    public int getSessionId();
    public int getClientId();
}
