package com.invisv.pseudotcpexampleapp;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    public static final int VPN_PREPARE = 1001;

    private static String currentProxyIP = "127.0.0.1";
    private static String currentProxyPort = "8444";
    private static final String TAG = "MainActivity";

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        if (request == VPN_PREPARE) {
            if (result == RESULT_OK) {
                Log.d(TAG, "VPN prepared");
                Intent intent = new Intent(this, PseudotcpExampleService.class);
                intent.putExtra("IP", currentProxyIP);
                intent.putExtra("PORT", currentProxyPort);
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

        MainActivity activity = this;

        setContentView(R.layout.activity_main);
        EditText proxyIPInput = findViewById(R.id.proxyIPInput);
        EditText proxyPortInput = findViewById(R.id.proxyPortInput);

        Switch toggle = (Switch) findViewById(R.id.relayswitch);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Log.d(TAG, "Toggling Relay on");

                    currentProxyIP = proxyIPInput.getText().toString();
                    currentProxyPort = proxyPortInput.getText().toString();

                    // Disable editing inputs while relay is on
                    proxyIPInput.setInputType(InputType.TYPE_NULL);
                    proxyPortInput.setInputType(InputType.TYPE_NULL);

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

                    // Re-enable editing inputs now that relay is off again
                    proxyIPInput.setInputType(InputType.TYPE_CLASS_TEXT);
                    proxyPortInput.setInputType(InputType.TYPE_CLASS_TEXT);

                    Intent intent = new Intent(activity, PseudotcpExampleService.class);
                    startService(intent.setAction(PseudotcpExampleService.DISCONNECTION));
                }
            }
        });


    }
}