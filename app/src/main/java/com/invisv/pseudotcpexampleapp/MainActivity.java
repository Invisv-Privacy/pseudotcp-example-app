package com.invisv.pseudotcpexampleapp;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    public static final int VPN_PREPARE = 1001;

    private static final String TAG = "MainActivity";

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        if (request == VPN_PREPARE) {
            if (result == RESULT_OK) {
                Log.d(TAG, "VPN prepared");
                Intent intent = new Intent(this, PseudotcpExampleService.class);
                startService(intent);
            } else {
                Log.d(TAG, "VPN not prepared, got result: $result");
            }
        } else {
            super.onActivityResult(request, result, data);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Switch toggle = (Switch) findViewById(R.id.relayswitch);

        MainActivity activity = this;
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Log.d(TAG, "Toggling Relay on");

                    // TODO: Add check for private DNS, which needs to be set to auto.
                    // TODO: Add check for Internet access, if desired.

                    Intent intent = VpnService.prepare(activity);
                    if (intent != null) {
                        startActivityForResult(intent, VPN_PREPARE);
                    } else {
                        onActivityResult(VPN_PREPARE, Activity.RESULT_OK, null);
                    }
                } else {
                    // The toggle is disabled
                    Log.d(TAG, "Toggling Relay off");
                    Intent intent = new Intent(activity, PseudotcpExampleService.class);
                    startService(intent.setAction(PseudotcpExampleService.DISCONNECTION));
                }
            }
        });
    }
}