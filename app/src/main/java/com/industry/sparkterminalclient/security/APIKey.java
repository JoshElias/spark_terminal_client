package com.industry.sparkterminalclient.security;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * Created by Two on 1/15/2015.
 */
public class APIKey {
    private static final String TAG = APIKey.class.getName();

    static private final String SECURE_RANDOM_ALG = "SHA1PRNG";
    private static final String CHAR_ENCODING = "UTF-8";
    private final static String ENVIRONMENT_DIRECTORY = Environment.DIRECTORY_DOWNLOADS;
    private final static String KEY_DIRECTORY_NAME = "sparkKeys";
    private final static String API_KEY_NAME = "SparkAPI.key";

    MessageDigest mMessageDigest;
    SecureRandom mSecureRandom;

    private byte[] mKeyBytes;
    private File mKeyFile;


    public APIKey() {
        refreshKey();
    }

    public byte[] getKeyBytes() {
        return mKeyBytes;
    }

    private MessageDigest getMessageDigest() {
        if( mMessageDigest == null ) {
            try {
                mMessageDigest = MessageDigest.getInstance("SHA-256");
            } catch(Exception e) {
                Log.e(TAG, "Unable to generate secure random");
                e.printStackTrace();
            }
        }
        return mMessageDigest;
    }

    private SecureRandom getSecureRandom() {
        if( mSecureRandom == null ) {
            try {
                mSecureRandom = SecureRandom.getInstance(SECURE_RANDOM_ALG);
            } catch(Exception e) {
                Log.e(TAG, "Unable to generate secure random");
                e.printStackTrace();
            }
        }
        return mSecureRandom;
    }

    private void refreshKeyBytes() {
        byte[] randomBytes = new byte[256];
        getSecureRandom().nextBytes(randomBytes);
        getMessageDigest().update(randomBytes);
        mKeyBytes = getMessageDigest().digest();
    }

    private void writeKeyToFile() {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream( getKeyFile() );
            fos.write(mKeyBytes);
        } catch( Exception e) {
            Log.e(TAG, "Unable to write API Key to File");
            e.printStackTrace();
        } finally {
            if(fos != null) {
                try {
                    fos.close();
                } catch (Exception e) {
                    Log.e(TAG, "Unable close fileOutputStream to file: "+getKeyFile().getPath());
                    e.printStackTrace();
                }
            }
        }
    }

    public File getKeyFile() {
        if(mKeyFile == null) {
            try {
                File keyDir = new File(Environment.getExternalStoragePublicDirectory(
                        ENVIRONMENT_DIRECTORY)+File.separator+KEY_DIRECTORY_NAME);
                if(!keyDir.exists() && !keyDir.mkdirs()) {
                    throw new Exception("Unable to get Spark API Key File");
                }
                mKeyFile = new File(keyDir, API_KEY_NAME);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                e.printStackTrace();
            }
        }
        return mKeyFile;
    }

    private void refreshKey() {
        refreshKeyBytes();
        writeKeyToFile();
    }
}
