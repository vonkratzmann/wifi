package mysites.com.au.scanwifi;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final static String TAG = MainActivity.class.getSimpleName();

    WifiManager mWifiManager;
    BroadcastReceiver mWifiScanReceiver;
    ListView mListView;
    Button mButtonScan;

    ArrayList<String> mArraylist = new ArrayList<>();
    ArrayAdapter mAdapter;

    final int PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // Check we have permissions to access coarse location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

                // The callback method gets the result of the request.
            }
        } else {
            // Permission has already been granted
        }

        mButtonScan = (Button) findViewById(R.id.scan);
        mButtonScan.setOnClickListener(this);
        mListView = (ListView) findViewById(R.id.wifilist);

        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (mWifiManager.isWifiEnabled() == false) {
            Toast.makeText(getApplicationContext(), "wifi is disabled..making it enabled",
                    Toast.LENGTH_LONG).show();
            mWifiManager.setWifiEnabled(true);
        }

        // Set up broadcast receiver
        mWifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                Log.d(TAG, "onReceive()");

                boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
                if (success) {
                    scanSuccess();
                } else {
                    // scan failure handling
                    scanFailure();
                }
            }
        };
        this.mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mArraylist);
        mListView.setAdapter(this.mAdapter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                } else {
                    // permission denied, boo!
                    finish();
                }
                return;
            }
        }
    }

    private void scanSuccess() {
        Log.d(TAG, "scanSuccess()");
        Toast.makeText(this, "Scan Success", Toast.LENGTH_SHORT).show();

        List<ScanResult> mResults = mWifiManager.getScanResults();
        int size = mResults.size();
        Log.d(TAG, "mResults.size(): " + size);
        unregisterReceiver(mWifiScanReceiver);

        try {
            while (size > 0) {
                size--;
                String ssid = mResults.get(size).SSID;
                String bssid = mResults.get(size).BSSID;
                String capabilities = mResults.get(size).capabilities;
                int level = mResults.get(size).level;
                int frequency = mResults.get(size).frequency;

                mArraylist.add("ssid: " + ssid + " bssid: " + bssid + " capabilities: " + capabilities
                + " level: " + level + " frequency: " + frequency);
                mAdapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
            Log.w("WifScanner", "Exception: " + e);
        }
    }

    private void scanFailure() {
        Log.d(TAG, "scanFailure()");

        Toast.makeText(this, "Scan Failure", Toast.LENGTH_SHORT).show();

        // handle failure: new scan did NOT succeed
        // consider using old scan mResults: these are the OLD mResults!
        List<ScanResult> results = mWifiManager.getScanResults();
        //...potentially use older scan mResults ...
    }

    public void onClick(View view) {
        scanWifiNetworks();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void scanWifiNetworks() {
        Log.d(TAG, "scanWifiNetworks()");
        mArraylist.clear();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        this.registerReceiver(mWifiScanReceiver, intentFilter);

        Toast.makeText(this, "Scanning....", Toast.LENGTH_SHORT).show();

        boolean success = mWifiManager.startScan();
        if (!success) {
            // scan failure handling
            scanFailure();
        }
    }
}
