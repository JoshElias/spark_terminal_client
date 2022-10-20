package com.industry.sparkterminalclient.tcp.http.koush;

import com.industry.sparkterminalclient.tcp.http.HttpConstants;
import com.industry.sparkterminalclient.tcp.http.response.IHttpResponse;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;

import org.json.JSONObject;
import org.json.simple.JSONValue;

/**
 * Created by Two on 2/9/2015.
 */
public class KoushHttpResponseAdapter implements IHttpResponse {
    private final static String TAG = KoushHttpResponseAdapter.class.getName();


    // Members
    private AsyncHttpServerResponse mServerResponse;


    // Constructor
    public KoushHttpResponseAdapter(AsyncHttpServerResponse response) {
        mServerResponse = response;
    }


    // Methods
    public void sendSuccess(JSONValue obj) {
        try {
            JSONObject message = new JSONObject();
            message.put(HttpConstants.HTTP_RESULT_KEY, obj);
        } catch (Exception ee) {
            ee.printStackTrace();
        }
    }

    public void sendError(Exception e) {
        try {
            JSONObject message = new JSONObject();
            message.put(HttpConstants.HTTP_ERROR_KEY, e.getMessage());
            mServerResponse.send(message);
        } catch (Exception ee) {
            ee.printStackTrace();
        }
    }
}
