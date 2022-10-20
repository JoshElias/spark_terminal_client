package com.industry.sparkterminalclient.tcp.http;

/**
 * Created by Two on 1/19/2015.
 */
public abstract class HttpException extends Exception {

    static public final String GENERAL_HTTP_EXCEPTION = "GENERAL_HTTP_EXCEPTION";
    static public final String INVALID_SESSION_ID = "INVALID_SESSION_ID";
    static public final String INVALID_DATA_SIGNATURE = "INVALID_DATA_SIGNATURE";
    static public final String INVALID_UNIQUE_RANDOM = "INVALID_UNIQUE_RANDOM";

    protected String mErrorCode = "Abstract Code Called";
    public String getCode() {
        return mErrorCode;
    }

    public static class InvalidSessionIdException extends HttpException {
        public InvalidSessionIdException() {
            mErrorCode = INVALID_SESSION_ID;
        }
    }

    public static class InvalidDataSignatureException extends HttpException {
        public InvalidDataSignatureException() {
            mErrorCode = INVALID_DATA_SIGNATURE;
        }
    }

    public static class InvalidUniqueRandomException extends HttpException {
        public InvalidUniqueRandomException() {
            mErrorCode = INVALID_UNIQUE_RANDOM;
        }
    }

    public static class GeneralHttpException extends HttpException {
        public GeneralHttpException() {
            mErrorCode = GENERAL_HTTP_EXCEPTION;
        }
    }
}
