package com.industry.sparkterminalclient.tcp.http.koush;

import com.industry.sparkterminalclient.tcp.http.message.IHttpMessageUnpacker;
import com.industry.sparkterminalclient.tcp.http.parser.IHttpBodyParser;
import com.industry.sparkterminalclient.tcp.http.request.IHttpRequest;
import com.koushikdutta.async.http.body.JSONObjectBody;
import com.koushikdutta.async.http.body.MultipartFormDataBody;
import com.koushikdutta.async.http.body.StringBody;
import com.koushikdutta.async.http.body.UrlEncodedFormBody;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;

import org.apache.http.NameValuePair;
import org.json.JSONObject;

/**
 * Created by Two on 2/9/2015.
 */
public class KoushHttpRequestAdapter implements IHttpRequest {
    private static final String TAG = KoushHttpRequestAdapter.class.getName();


    // Members
    private AsyncHttpServerRequest mServerRequest;
    private IHttpBodyParser mBodyParser = new KoushBodyParserAdapter();
    private IHttpMessageUnpacker mMessageUnpacker = new


    // Constructor
    public KoushHttpRequestAdapter(AsyncHttpServerRequest request) {
        mServerRequest = request;
    }


    // Methods
    public JSONObject getMessageData() throws Exception {
        JSONObject fullMessage = mBodyParser.parseBody( mServerRequest.getBody() );
        // unpackmessage


    }
}
