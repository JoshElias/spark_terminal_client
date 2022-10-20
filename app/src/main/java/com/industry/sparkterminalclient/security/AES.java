package com.industry.sparkterminalclient.security;

import android.os.Environment;
import android.util.Log;

import com.industry.sparkterminalclient.Utility;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;


/**
 * Created by Tim on 12/10/2014.
 */
public class AES {
    static private final String TAG = AES.class.getName();

	static private final String CRYPTO_PROVIDER = "BC";
	static private final String CIPHER_ALG = "AES/CBC/PKCS7Padding";
	static private final String SECURE_RANDOM_ALG = "SHA1PRNG";
    static private final String SECRET_KEY_ALG = "PBKDF2WithHmacSHA1";
    static public final String CHAR_ENCODING = "UTF-8";
    static public final int NUM_OF_ITERATIONS = 128000;
    static public final int KEY_BIT_LENGTH = 256;
    public final static String ENVIRONMENT_DIRECTORY = Environment.DIRECTORY_DOWNLOADS;
    public final static String KEY_DIRECTORY_NAME = "sparkKeys";
    public final static String DEFAULT_KEY_NAME = "AES.key";

    SecureRandom mSecureRandom;
    SecretKeyFactory mKeyFactory;

    private String mName;
    private byte[] mSalt;
    private String mPassword;
    public byte[] mSecretKeyBytes;
    private SecretKey mSecretKey;
    private File mKeyFile;

    Charset mCharset = Charset.forName("UTF-8");
    private CharsetEncoder mCharsetEncoder = mCharset.newEncoder();
    private CharsetDecoder mCharsetDecoder = mCharset.newDecoder();

    public AES(String name) {
        mName = name;
        refreshKey();
    }

    public AES() {
        this(DEFAULT_KEY_NAME);
    }

    public JSONObject encrypt( JSONObject json ) {
        try {
            byte[] data = mCharsetEncoder.encode( CharBuffer.wrap( Utility.stringifyJSON(json).toCharArray() ) ).array();
            Cipher c = Cipher.getInstance(CIPHER_ALG, CRYPTO_PROVIDER);
            c.init(Cipher.ENCRYPT_MODE, mSecretKey);
            byte[] encrypted = c.doFinal( data );
            byte[] iv = c.getIV();
            JSONObject encryptedMessage = new JSONObject();
            encryptedMessage.put("encryptedHex", Utility.toHex(encrypted));
            encryptedMessage.put("ivHex", Utility.toHex(iv));
            return encryptedMessage;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public JSONObject decrypt( JSONObject encryptedMessage ) {
		try {
            Log.e(TAG, "Encrypted Hex: "+encryptedMessage.getString("encryptedHex"));
            Log.e(TAG, "IV Hex: "+encryptedMessage.getString("ivHex"));
			byte[] iv = Utility.fromHex(encryptedMessage.getString("ivHex"));
			byte[] encrypted = Utility.fromHex(encryptedMessage.getString("encryptedHex"));
			Cipher c = Cipher.getInstance(CIPHER_ALG, CRYPTO_PROVIDER);
			IvParameterSpec ips = new IvParameterSpec(iv);
			c.init(Cipher.DECRYPT_MODE, mSecretKey, ips);
			byte[] decrypted = c.doFinal(encrypted);
            Log.e(TAG, "PLEASE GOD: "+Utility.toHex(decrypted));
			String stringifiedJsonMessage = mCharsetDecoder.decode(ByteBuffer.wrap(decrypted)).toString();
            Log.e(TAG, "Stringified JOSN Message: "+stringifiedJsonMessage);
			JSONObject jsonMessage = Utility.parseJSON(stringifiedJsonMessage);
			return jsonMessage;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

    public SecureRandom getSecureRandom() {
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

    public SecretKeyFactory getKeyFactory() {
        if( mKeyFactory == null ) {
            try {
                mKeyFactory = SecretKeyFactory.getInstance(SECRET_KEY_ALG);
            } catch( Exception e ) {
                Log.e(TAG, "Unable to get PBKDF2 Factory");
                e.printStackTrace();
            }
        }
        return mKeyFactory;
    }

    private void refreshSalt() {
        mSalt = new byte[512];
        getSecureRandom().nextBytes(mSalt);
    }

    private void refreshPassword() {
        byte[] passwordBytes = new byte[512];
        getSecureRandom().nextBytes(passwordBytes);
        mPassword = Utility.toHex(passwordBytes);
    }

    private void refreshKeyBytes() {
        try {
            KeySpec spec = new PBEKeySpec(mPassword.toCharArray(), mSalt, NUM_OF_ITERATIONS, KEY_BIT_LENGTH);

            mSecretKeyBytes = getKeyFactory().generateSecret(spec).getEncoded();
        } catch (Exception e) {
            Log.e(TAG, "Unable to generate secret key bytes");
            e.printStackTrace();
        }
    }

    private void refreshSecretKey() {
        mSecretKey = new SecretKeySpec(mSecretKeyBytes, "AES");
    }


    private void writeKeyToFile() {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream( getKeyFile() );
            fos.write(mSecretKeyBytes);
        } catch( Exception e) {
            Log.e(TAG, "Unable to write AES Key to File");
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
                        ENVIRONMENT_DIRECTORY)+ File.separator+KEY_DIRECTORY_NAME);
                if(!keyDir.exists() && !keyDir.mkdirs()) {
                    throw new Exception("Unable to get Spark AES Key File");
                }
                mKeyFile = new File(keyDir, mName);
                Log.e(TAG, mKeyFile.getPath());
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                e.printStackTrace();
            }
        }
        return mKeyFile;
    }

    private void refreshKey() {
        refreshSalt();
        refreshPassword();
        refreshKeyBytes();
        refreshSecretKey();
        writeKeyToFile();
    }
}
