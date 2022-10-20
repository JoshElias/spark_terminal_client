package com.industry.sparkterminalclient.tcp.http.parser;

import org.json.JSONObject;

/**
 * Created by Two on 2/10/2015.
 */
public interface IHttpBodyParser {
    public JSONObject parseBody(Object body) throws Exception;
}
