package com.industry.sparkterminalclient.tcp.http.handlers.handshake;

import com.industry.sparkterminalclient.tcp.http.HttpConstants;
import com.industry.sparkterminalclient.tcp.http.request.IHttpSecureRequest;
import com.industry.sparkterminalclient.tcp.http.requesthandler.IHttpHandshakeRequestHandler;
import com.industry.sparkterminalclient.tcp.http.requesthandler.IHttpSecureRequestHandler;
import com.industry.sparkterminalclient.tcp.http.response.IHttpSecureResponse;

import org.json.JSONObject;

import java.util.UUID;

/**
 * Created by Two on 2/10/2015.
 */
public class GetUniqueRandomHandler implements IHttpHandshakeRequestHandler {
    @Override
    public void onRequest(final IHttpSecureRequest request, final IHttpSecureResponse response) {
        JSONObject requestData = request.getData();

        // Keep track which client is requesting which unique random
        int uniqueRandom = UUID.randomUUID().hashCode();
        int clientId = request.getClientId();
        mRandomUniqueMap.put(clientId, uniqueRandom);

        // Package Message Data
        JSONObject responseData = new JSONObject();
        responseData.put(HttpConstants.UNIQUE_RANDOM_KEY, uniqueRandom);
        JSONObject responseObj = packageData(messageData, 0);

        // Respond
        response.send(responseObj);
    }
}
