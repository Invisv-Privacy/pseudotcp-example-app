package com.invisv.pseudotcpexampleapp;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

/**
 * End2End test, which will execute on an (probably emulated) Android device and can exercise actual
 * networking connectivity
 */
@RunWith(AndroidJUnit4.class)
public class End2EndTest {
    private static final String RELAY_SAMPLE_PACKAGE
            = "com.invisv.pseudotcpexampleapp";

    private static final String PROXY_IP = "172.25.0.3";
    private static final String PROXY_PORT = "8444";
    private static final String ECHO_SERVER_URL = "http://172.25.0.4:8080";
    private static final String VPN_DIALOG_PACKAGE = "com.android.vpndialogs";
    private static final int LAUNCH_TIMEOUT = 5000;
    private static final int TRANSITION_TIMEOUT = 5000;

    private UiDevice device;

    private static final String TAG = "End2EndTest";

    private String initialIP;

    @Before
    public void startMainActivityFromHomeScreen() throws IOException {
        // Initialize UiDevice instance
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Get IP of device before running VPN
        initialIP = getIP();

        // Start from the home screen
        device.pressHome();

        // Wait for launcher
        final String launcherPackage = device.getLauncherPackageName();
        assertThat(launcherPackage, notNullValue());
        device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)),
                LAUNCH_TIMEOUT);

        // Launch the app
        Context context = ApplicationProvider.getApplicationContext();

        final Intent intent = context.getPackageManager()
                .getLaunchIntentForPackage(RELAY_SAMPLE_PACKAGE);

        // Clear out any previous instances
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);

        // Wait for the app to appear
        device.wait(Until.hasObject(By.pkg(RELAY_SAMPLE_PACKAGE).depth(0)),
                LAUNCH_TIMEOUT);
    }

    private String getIP() throws IOException {
        // Open a browser window to the echo server
        String res = device.executeShellCommand("am start -a android.intent.action.VIEW -d " + ECHO_SERVER_URL);
        Log.d(TAG, "res: " + res);

        device.wait(Until.hasObject(By.textContains("headers")), LAUNCH_TIMEOUT);

        // The echo server output includes the string "headers", this is a little lazy but good enough for our use case
        UiObject2 browserText = device.findObject(By.textContains("headers"));

        String browserContent = browserText.getText();

        // The echo server output is a JSON string
        JsonObject rootObject = JsonParser.parseString(browserContent).getAsJsonObject();

        // The "ip" key contains the ip of the request as seen by the echo server
        return rootObject.get("ip").getAsString();
    }

    @Test
    public void enableRelay() throws IOException {
        // Set the proxy IP and port text fields to the docker proxy we've spun up
        UiObject2 proxyIPInput = device.findObject(
                By.desc("proxyIPInput")
        );
        UiObject2 proxyPortInput = device.findObject(
                By.desc("proxyPortInput")
        );
        proxyIPInput.setText(PROXY_IP);
        proxyPortInput.setText(PROXY_PORT);

        // Turn on relay
        UiObject2 enableButton = device.findObject(
                By.text("Enable Relay").clazz("android.widget.Switch")
        );
        enableButton.clickAndWait(Until.newWindow(), TRANSITION_TIMEOUT);

        // Click "OK" on the VPN dialog
        UiObject2 vpnDialogOK = device.findObject(By.text("OK").clickable(true).pkg(VPN_DIALOG_PACKAGE));

        if (vpnDialogOK != null) {
            vpnDialogOK.clickAndWait(Until.newWindow(), TRANSITION_TIMEOUT);
        }

        device.wait(Until.gone(By.pkg(VPN_DIALOG_PACKAGE)), LAUNCH_TIMEOUT);

        // Get our new IP from the echo server
        String newIP = getIP();

        // Assert that it's now changed
        assertNotEquals(newIP, initialIP);
    }
}