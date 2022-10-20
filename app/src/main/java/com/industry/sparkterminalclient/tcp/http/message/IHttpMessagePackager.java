package com.industry.sparkterminalclient.tcp.http.message;

import org.json.JSONObject;

/**
 * Created by Two on 2/10/2015.
 */
public interface IHttpMessagePackager {
    public JSONObject packageMessage(JSONObject obj);
}
