package com.industry.sparkterminalclient.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.industry.sparkterminalclient.KnoxManager;
import com.industry.sparkterminalclient.R;
import com.industry.sparkterminalclient.Utility;

import org.json.JSONObject;
import org.spongycastle.util.encoders.Base64;

import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import bolts.Continuation;
import bolts.Task;

public class ActivateKnoxActivity extends Activity {
    private static final String TAG = ActivateKnoxActivity.class.getName();

    KnoxManager mKnox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activate_knox);
        Log.d(TAG, "Starting the Spark ActivateKnoxActivity");
        // Init Knox
        mKnox = KnoxManager.getInstance(ActivateKnoxActivity.this);

        authorizeLauncher().continueWith(new Continuation<Void, Void>() {
            public Void then(Task<Void> task) throws Exception {
                if (task.isFaulted()) {
                    Log.d(TAG, "Failed to Authenticate Knox");
                    ActivateKnoxActivity.this.setResult(Activity.RESULT_CANCELED);
                } else {
                    Log.d(TAG, "Successfully Authenticated Knox");
                    ActivateKnoxActivity.this.setResult(Activity.RESULT_OK);
                }
                ActivateKnoxActivity.this.finish();
                return null;
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activate_knox, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // KNOX DEPENDENCY
        if(requestCode == KnoxManager.RESULT_ADMIN_ENABLE) {
            KnoxManager.onAdminActivityResult(resultCode, data);
        }
    }


    /**
     *  AUTHORIZE FUNCTIONS
     */

    private Task<Void> authorizeLauncher() {
        final Task<Void>.TaskCompletionSource done = Task.create();
        try {
            Log.d(TAG, "Running Authorize Launcher");
            // Verify that we should authorize
            if(mKnox.isLicenseActive()) {
                Log.d(TAG, "Exiting because license is already active");
                done.setResult(null);
                return done.getTask();
            }
            if(!isAuthenticateIntent()) {
                Log.d(TAG, "Exiting because Intent did not have the proper components");
                done.setError(new Exception("Unable to authorize Knox due to inproper credentials in the intent"));
                return done.getTask();
            }

            // Authorize Knox
            Log.d(TAG, "Getting Knox Key");
            mKnox.activateKnox(ActivateKnoxActivity.this).onSuccessTask(new Continuation<String, Task<Void>>() {
                public Task<Void> then(Task<String> task) throws Exception {
                    Log.d(TAG, "Validating Authentication Intent");
                    return validateAuthenticationIntent(task.getResult());
                }
            }).continueWith(new Continuation<Void, Void>() {
                public Void then(Task<Void> task) throws Exception {
                    if (task.isFaulted()) {
                        done.setError(task.getError());
                    } else {
                        done.setResult(null);
                    }
                    return null;
                }
            });

        } catch (Throwable t) {
            Utility.finishTask(done, t);
        } finally {
            return done.getTask();
        }
    }

    private boolean isAuthenticateIntent() {
        try {
            Intent intent = getIntent();

            // Check to see if we have all the necessary keys
            if( !intent.hasExtra("timestamp") || !intent.hasExtra("hash") ) {
                return false;
            }

            // Check that the timestamp is no older than 3 minutes old.
            long timestamp = intent.getLongExtra("timestamp", 0);
            long currTime = new Date().getTime();
            if( (currTime-timestamp) > 180000 ) {
                return false;
            }

            return true;
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private Task<Void> validateAuthenticationIntent(String knoxKey) {
        final Task<Void>.TaskCompletionSource done = Task.create();
        try {
            Intent intent = getIntent();

            // Get values from intent
            long timestamp = intent.getLongExtra("timestamp", 0);
            String oldHash = intent.getStringExtra("hash");

            // Re-create signature for the intent
            JSONObject signature = new JSONObject();
            signature.put("timestamp", timestamp);

            // Hash the signature with the mKnox key
            String alg = "HmacSHA256";
            Mac hmac = Mac.getInstance(alg);
            SecretKeySpec keySpec = new SecretKeySpec(knoxKey.getBytes("UTF-8"), alg);
            hmac.init(keySpec);
            String newHash = new String(Base64.encode(hmac.doFinal(signature.toString().getBytes("UTF-8"))));

            // Validate the signature of the intent
            if(oldHash.equals(newHash)) {
                done.setResult(null);
            } else {
                throw new Exception("Signature from Loader is invalid");
            }

        } catch( Exception e) {
            e.printStackTrace();
            done.setError(new Exception(""));
        } finally {
            return done.getTask();
        }
    }
}
