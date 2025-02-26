package com.invisv.pseudotcpexampleapp;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.VpnService;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import bindings.Bindings;
import bindings.Client;

public class PseudotcpExampleService extends VpnService implements Handler.Callback, Runnable {
    public static final String DISCONNECTION = "StopService";
    private static final String TAG = "PseudotcpExampleService";
    private static final int MAX_PACKET_SIZE = 32000;
    private static final boolean verbose = false;
    private static String proxyIP = "127.0.0.1";
    private static String proxyPort = "8444";

    private static final String LOCAL_TUN_IP = "10.157.163.6";  // an IP unlikely to collide.
    private static Thread mThread;
    private static boolean running;
    private Handler mHandler;
    private ParcelFileDescriptor mInterface;

    /**
     * Returns true if the service is running.
     */
    public static boolean isRunning() {
        return PseudotcpExampleService.mThread != null && running;
    }

    /**
     * Terminates immediately.
     */
    public static void hardStop() {
        Log.d(TAG, "hardStop");
        if (PseudotcpExampleService.mThread != null) {
            PseudotcpExampleService.mThread.interrupt();
            PseudotcpExampleService.mThread = null;
        }
        running = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && DISCONNECTION.equals(intent.getAction())) {
            running = false;
            stopForeground(STOP_FOREGROUND_REMOVE);
            if (mThread != null) {
                mThread.interrupt();
                stopSelf();
            }
            return START_NOT_STICKY;
        }

        if (intent != null) {
            proxyIP = intent.getStringExtra("IP");
            proxyPort = intent.getStringExtra("PORT");
        }
        // The handler is only used to show messages.
        if (mHandler == null) {
            mHandler = new Handler(this);
        }

        // Stop the previous session by interrupting the thread.
        if (mThread != null) {
            mThread.interrupt();
        }

        // Start a new session by creating a new thread.
        mThread = new Thread(this, "PseudotcpExampleService");
        mThread.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        if (mThread != null) {
            mThread.interrupt();
        }
    }

    @Override
    public boolean handleMessage(Message message) {
        if (message != null) {
            Toast.makeText(this, message.what, Toast.LENGTH_SHORT).show();
        }
        return true;
    }


    @Override
    public synchronized void run() {
        try {
            Log.i(TAG, "Starting");
            runVPN();
        } catch (Exception e) {
            Log.e(TAG, "Got " + e);
        } finally {
            try {
                mInterface.close();
            } catch (Exception e) {
                // ignore
            }
            mInterface = null;
            running = false;

            Log.i(TAG, "Exiting");
        }
    }

    private void runVPN() throws Exception {
        running = true;
        final VpnService vpn = this;

        try {
            // Configure a new interface from our VpnService instance. This must be done
            // from inside a VpnService.
            VpnService.Builder builder = new VpnService.Builder();

            for (String dnsServer : getDNSServers()) {
                if (dnsServer.indexOf(':') != -1) {
                    // Skip IPv6 DNS servers.
                    continue;
                }
                builder.addDnsServer(dnsServer);
            }

            // Create a local TUN interface using predetermined address.
            VpnService.Builder vpnbuilder = builder.addAddress(LOCAL_TUN_IP, 24)
                    .addRoute("0.0.0.0", 0).setBlocking(true).setMtu(MAX_PACKET_SIZE);

            mInterface = vpnbuilder.establish();

            // Packets to be sent are queued in this input stream.
            FileInputStream in = new FileInputStream(mInterface.getFileDescriptor());

            // Packets received need to be written to this output stream.
            final FileOutputStream out = new FileOutputStream(mInterface.getFileDescriptor());

            Client client = Bindings.newClient(
                    (bytes, l) -> out.write(bytes, 0, (int) l),
                    l -> {
                        Log.i(TAG, "Protecting connection: " + l);
                        if (!vpn.protect((int) l)) {
                            throw new Exception("Failed to protect socket");
                        }
                    }, proxyIP, proxyPort, verbose);
            client.init();

            ByteBuffer packet = ByteBuffer.allocate(MAX_PACKET_SIZE);
            long packetCount = 0;
            while (running) {
                // Read the outgoing packet from the input stream.
                int length = in.read(packet.array());
                if (length > 0) {
                    packet.limit(length);
                    packet.rewind();
                    client.send(packet.array(), length);
                    packet.clear();

                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Got " + e);
            throw new Exception();
        }
    }

    private List<String> getDNSServers() {
        List<String> dnsServers = new ArrayList<>();
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        try {
            for (Network network : connectivityManager.getAllNetworks()) {
                NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
                assert networkInfo != null;
                if (networkInfo.isConnected()) {
                    LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
                    assert linkProperties != null;
                    for (InetAddress ipAddress : linkProperties.getDnsServers()) {
                        dnsServers.add(ipAddress.getHostAddress());
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Got " + e);
        }
        return dnsServers;
    }

}