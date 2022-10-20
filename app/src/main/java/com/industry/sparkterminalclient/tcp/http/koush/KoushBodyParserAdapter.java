package com.industry.sparkterminalclient.tcp.http.koush;

import com.industry.sparkterminalclient.tcp.http.parser.IHttpBodyParser;
import com.koushikdutta.async.http.body.AsyncHttpRequestBody;
import com.koushikdutta.async.http.body.JSONObjectBody;
import com.koushikdutta.async.http.body.MultipartFormDataBody;
import com.koushikdutta.async.http.body.StringBody;
import com.koushikdutta.async.http.body.UrlEncodedFormBody;

import org.apache.http.NameValuePair;
import org.json.JSONObject;

/**
 * Created by Two on 2/10/2015.
 */
public class KoushBodyParserAdapter implements IHttpBodyParser {
    private static final String TAG = KoushBodyParserAdapter.class.getName();


    // Members
    AsyncHttpRequestBody mRequestBody;


    // Members
    public JSONObject parseBody(Object bodyObj) throws Exception {
        AsyncHttpRequestBody requestBody = (AsyncHttpRequestBody)bodyObj;
        JSONObject json = new JSONObject();
        if (mRequestBody instanceof UrlEncodedFormBody) {
            UrlEncodedFormBody body = (UrlEncodedFormBody)mRequestBody;
            for (NameValuePair pair: body.get()) {
                json.put(pair.getName(), pair.getValue());
            }
        }
        else if (mRequestBody instanceof JSONObjectBody) {
            json = ((JSONObjectBody)mRequestBody).get();
        }
        else if (mRequestBody instanceof StringBody) {
            json.put("foo", ((StringBody)mRequestBody).get());
        }
        else if (mRequestBody instanceof MultipartFormDataBody) {
            MultipartFormDataBody body = (MultipartFormDataBody)mRequestBody;
            for (NameValuePair pair: body.get()) {
                json.put(pair.getName(), pair.getValue());
            }
        }
        return json;
    }
}
