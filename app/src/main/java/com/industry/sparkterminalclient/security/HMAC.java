package com.industry.sparkterminalclient.security;


import com.industry.sparkterminalclient.SJSON;
import com.industry.sparkterminalclient.Utility;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


/**
 * Created by Tim on 16/10/2014.
 */
public class HMAC {
	static private final String TAG="HMAC";
	static private final String SIGNING_ALGORITHM = "HMAC-SHA256";
    static private final String SECRET_KEY_ALG = "PBKDF2WithHmacSHA1";
    static public final String CHAR_ENCODING = "UTF-8";

	private HMAC(){
	}

	static public byte[] sign( JSONObject data, byte[] key ) throws NoSuchAlgorithmException, InvalidKeyException, JSONException, UnsupportedEncodingException, NoSuchProviderException {
		SJSON sjson = new SJSON();
		String canonicalJSON = sjson.getCanonicalJSON(data);
		byte[] bytes = canonicalJSON.getBytes(CHAR_ENCODING);
		return sign(bytes, key);
	}

	static public byte[] sign( byte[] data, byte[] key ) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException {
		Mac mac = Mac.getInstance(SIGNING_ALGORITHM, "BC");
		mac.init(new SecretKeySpec(key,SIGNING_ALGORITHM));
		return mac.doFinal(data);
	}

	static public Boolean verify( JSONObject data, byte[] key, String signature ) throws JSONException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
		String _signature = Utility.toBase64(sign(data, key));
		return _signature.equals(signature);
	}

}
