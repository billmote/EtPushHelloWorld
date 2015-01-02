package com.example.helloworld.ui.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.exacttarget.etpushsdk.ETException;
import com.exacttarget.etpushsdk.ETLocationManager;
import com.exacttarget.etpushsdk.ETPush;
import com.example.helloworld.HelloWorldApplication;
import com.example.helloworld.R;
import com.radiusnetworks.ibeacon.BleNotAvailableException;


public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String KEY_FIRST_LAUNCH = "first_launch";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor preferencesEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPreferences = getPreferences(MODE_PRIVATE);
        preferencesEditor = sharedPreferences.edit();
        try {
            /*
                We must call enablePush() at least once for the application.
             */
            if (sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true) || true) {
                // forcing the enablePush() for v3.4.x
                Log.i(TAG, String.format("%1$s is true.", KEY_FIRST_LAUNCH));
                ETPush.pushManager().enablePush();
                /*
                    Set this after the call to enablePush() so we don't prematurely record that
                    we've made it past our first_launch.
                 */
                preferencesEditor.putBoolean(KEY_FIRST_LAUNCH, false).apply();
                Log.i(TAG, String.format("Updated %1$s to false", KEY_FIRST_LAUNCH));
            }
            /*
                If we have Location enabled then we must start/stop watching as the default state
                at least once.
             */
            if (HelloWorldApplication.LOCATION_ENABLED) {
                Log.i(TAG, "Location is enabled.");
                ETLocationManager.locationManager().startWatchingLocation();
                /*
                    If we're watching for location then also watch for beacons if possible.
                 */
                try {
                    if (!ETLocationManager.locationManager().startWatchingProximity()) {
                        Log.i(TAG, "BLE is available.");
                        promptForBluetoothSettings();
                    }
                } catch (BleNotAvailableException e) {
                    Log.w(TAG, "BLE is not available on this device");
                    //sharedPreferences.edit().putBoolean("pref_proximity", false).commit();
                    ETLocationManager.locationManager().stopWatchingProximity();
                }
            }
            /*
                Add First & Last Name Attributes & a Subscriber Key
             */
            Log.i(TAG, "Adding attributes.");
            ETPush.pushManager().addAttribute("FirstName", "Hello");
            ETPush.pushManager().addAttribute("LastName", getString(R.string.gcm_sender_id));
            Log.i(TAG, "Adding subscriber key.");
            ETPush.pushManager().setSubscriberKey("bmote@exacttarget.com");
            Log.i(TAG, "Enabling push.");
            ETPush.pushManager().enablePush();
        } catch (ETException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    private void promptForBluetoothSettings() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle("Enable Bluetooth?")
                .setMessage("Beacon alerts require that you have Bluetooth enabled. Enable it now?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Enable Bluetooth", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                        if (!mBluetoothAdapter.isEnabled()) {
                            mBluetoothAdapter.enable();
                        }
                        try {
                            ETLocationManager.locationManager().startWatchingProximity();
                        } catch (BleNotAvailableException e) {
                            Log.e(TAG, e.getMessage(), e);
                        } catch (ETException e) {
                            Log.e(TAG, e.getMessage(), e);
                        }
                    }
                }).show();
    }

}