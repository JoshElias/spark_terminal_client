package com.industry.sparkterminalclient.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.industry.sparkterminalclient.KnoxManager;
import com.industry.sparkterminalclient.R;


public class SparkStartClientActivity extends Activity {
    private final static String TAG = SparkStartClientActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "Puppies???");
        startService(new Intent(SparkStartClientActivity.this, SparkClientService.class));
        SparkStartClientActivity.this.moveTaskToBack(true);
        //Log.e(TAG, SparkAppManager.getAPKDownloadFileFromHID("hid2").getPath());
        //Log.e(TAG, "Puppies!");
        //httpInterface = LobbyHttpInterface.getInstance(getApplicationContext());
        //Log.e(TAG, "Current Network: "+SparkWifiManager.getInstance(getApplicationContext()).getCurrentNetwork());

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // KNOX DEPENDENCY
        if(requestCode == KnoxManager.RESULT_ADMIN_ENABLE) {
            KnoxManager.onAdminActivityResult(resultCode, data);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, SparkClientService.class));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.spark_terminal_client, menu);
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
}