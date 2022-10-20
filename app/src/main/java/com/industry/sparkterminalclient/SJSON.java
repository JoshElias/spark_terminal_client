package com.industry.sparkterminalclient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

/**
 * Created by Tim on 17/10/2014.
 */
public class SJSON {
	public static final String SORTED_KEYLIST_KEY = "__SORTED_KEYS";
	private static final String NEWLINE_GLUE = System.getProperty("line.separator");

	private JSONObject sjsonObject;
	private ArrayList<String> keyList;

	public SJSON() {}


	public String getCanonicalJSON( JSONObject srcObject ) throws JSONException {
		StringBuilder canonicalJSON = new StringBuilder();
		JSONObject sjson = convertToSJSON(srcObject);
		Iterator<String> keys = keyList.iterator();
		while(keys.hasNext()) {
			String key = keys.next();
			canonicalJSON.append(key);
			canonicalJSON.append(NEWLINE_GLUE);
			canonicalJSON.append(sjson.get(key));
			if(keys.hasNext()) {
				canonicalJSON.append(NEWLINE_GLUE);
			}
		}
		return canonicalJSON.toString();
	}

	public JSONObject convertToSJSON( JSONObject srcObject ) throws JSONException {
		if( this.sjsonObject != null ) {
			this.sjsonObject = null;
		}
		this.sjsonObject = new JSONObject();
		if( this.keyList != null ) {
			this.keyList = null;
		}
		this.keyList = new ArrayList<String>();
		toSJSON(srcObject, "");
		Collections.sort(this.keyList);
		this.sjsonObject.put(SORTED_KEYLIST_KEY,this.keyList);
		return this.sjsonObject;
	}


	private void toSJSON( JSONObject srcObject, String keyPath ) throws JSONException {
		Iterator<String> keys = srcObject.keys();
		if(!keys.hasNext()) {
			this.sjsonObject.put(keyPath,srcObject);
			this.keyList.add(keyPath);
			return;
		}

		while(keys.hasNext()) {
			String key = keys.next();
			String sjsonKey = ( keyPath.length() == 0 ) ? key : keyPath.concat(".").concat(key);
			Object obj = srcObject.get(key);
			handleObject(obj,sjsonKey);
		}
	}

	private void toSJSON( JSONArray srcObject, String keyPath ) throws JSONException {
		if( srcObject.length() == 0 ) {
			this.sjsonObject.put(keyPath,srcObject);
			this.keyList.add(keyPath);
			return;
		}

		for (int i = 0; i < srcObject.length(); i++) {
			String sjsonKey = ( keyPath.length() == 0 ) ? Integer.toString(i) : keyPath.concat(".").concat(Integer.toString(i));
			Object obj = srcObject.get(i);
			handleObject(obj,sjsonKey);
		}
	}

	private void handleObject( Object obj, String keyPath ) throws JSONException {
		if( obj instanceof String
				|| obj instanceof Long
				|| obj instanceof Boolean
				|| obj instanceof Double
				|| obj instanceof Integer ) {
			this.sjsonObject.put(keyPath,obj);
			this.keyList.add(keyPath);
		} else if( obj instanceof JSONObject ) {
			toSJSON((JSONObject)obj, keyPath);
		} else if( obj instanceof JSONArray ) {
			toSJSON((JSONArray)obj, keyPath);
		} else if( obj.equals(JSONObject.NULL) ) {
			this.sjsonObject.put(keyPath,"__NULL__");
			this.keyList.add(keyPath);
		} else {
			throw new RuntimeException("Unsupported class type, class=" + obj.getClass().getName() + ". Only allowed: Null, String, Long, Boolean, Double, Integer, JSONObject, JSONArray.");
		}
	}



}
