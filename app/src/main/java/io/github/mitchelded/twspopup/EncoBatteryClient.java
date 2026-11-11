package io.github.mitchelded.twspopup;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.provider.Settings;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/** Opt-in independent RFCOMM battery subscription. No pairing, retries, audio or device-setting commands. */
final class EncoBatteryClient {
    static final String SOURCE = "enco";
    private static final UUID SERVICE = UUID.fromString("00001107-D102-11E1-9B23-00025B00A5A5");
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "AnyBuds-Enco-timeout");
        thread.setDaemon(true);
        return thread;
    });
    private Job current;

    @SuppressLint("MissingPermission") // Runs with Bluetooth host permissions; failure is caught.
    synchronized void start(Context context, BluetoothDevice device) {
        cancel(null);
        try {
            if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                L.w("Enco direct skipped: device is not already bonded");
                return;
            }
            Job job = new Job(context, device.getAddress(), device.getName(),
                    device.createRfcommSocketToServiceRecord(SERVICE));
            current = job;
            job.timeout = timer.schedule(() -> stop(job, "20-second acquisition window ended"), 20, TimeUnit.SECONDS);
            Thread worker = new Thread(() -> run(job), "AnyBuds-Enco-read");
            worker.setDaemon(true);
            worker.start();
        } catch (Exception e) {
            cancel(null);
            L.e("Enco direct start", e);
        }
    }

    synchronized void cancel(String address) {
        if (current != null && (address == null || current.address.equalsIgnoreCase(address))) stop(current, "cancelled");
    }

    private synchronized void stop(Job job, String reason) {
        if (job.closed) return;
        job.closed = true;
        if (job.timeout != null) job.timeout.cancel(false);
        try { job.socket.close(); } catch (IOException ignored) { }
        if (current == job) current = null;
        L.i("Enco direct stopped: " + reason);
    }

    @SuppressLint("MissingPermission") // Host permission, not module manifest; SecurityException is logged below.
    private void run(Job job) {
        try {
            L.i("Enco direct connecting to vendor RFCOMM service");
            job.socket.connect();
            if (job.closed) return;
            L.i("Enco direct connected; requesting notification capabilities");
            OutputStream out = job.socket.getOutputStream();
            InputStream in = job.socket.getInputStream();
            out.write(EncoProtocol.encode(EncoProtocol.CAPABILITIES, 0, new byte[0]));
            out.flush();
            boolean subscribed = false;
            for (int packets = 0; packets < 128 && !job.closed; packets++) {
                EncoProtocol.Packet packet = EncoProtocol.read(in);
                if (!selected(job)) break;
                L.i("Enco direct packet command=0x" + Integer.toHexString(packet.command)
                        + " bytes=" + packet.payload.length);
                if (packet.command == 0x8200 && packet.sequence == 0 && !subscribed) {
                    if (!EncoProtocol.supportsBattery(packet)) {
                        L.w("Enco direct: battery notification not advertised or capability response invalid");
                        break;
                    }
                    out.write(EncoProtocol.encode(EncoProtocol.REGISTER, 1, new byte[]{1}));
                    out.flush();
                    subscribed = true;
                    L.i("Enco direct: battery notification subscription sent");
                }
                if (packet.command == 0x8201 && packet.sequence == 1 && subscribed) {
                    if (packet.payload.length < 2 || packet.payload[0] != 0 || packet.payload[1] != 1) {
                        L.w("Enco direct: battery subscription rejected");
                        break;
                    }
                    L.i("Enco direct: battery subscription accepted");
                }
                VendorBatteryReport report = subscribed ? EncoProtocol.battery(packet) : null;
                if (report != null) publish(job, report);
            }
        } catch (Exception e) {
            if (!job.closed) L.e("Enco direct acquisition", e);
        } finally { stop(job, "session finished"); }
    }

    private synchronized void publish(Job job, VendorBatteryReport report) {
        if (current != job || job.closed || !selected(job)) return;
        job.context.sendBroadcast(new Intent(PopupController.ACTION_VENDOR)
                .setComponent(new ComponentName(VivoApi.PACKAGE, VivoApi.TRIGGER_RECEIVER))
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES | Intent.FLAG_RECEIVER_FOREGROUND)
                .putExtra("batterySource", SOURCE).putExtra("mac", job.address).putExtra("title", job.name)
                .putExtra("state", PopupRequest.STATE_CONNECTED)
                .putExtra("left", report.percent[0]).putExtra("right", report.percent[1]).putExtra("box", report.percent[2])
                .putExtra("charging", report.chargingMask()).putExtra("observedElapsedMs", SystemClock.elapsedRealtime()));
        L.i("source=enco_rfcomm L=" + report.percent[0] + " R=" + report.percent[1] + " case=" + report.percent[2]
                + " charging=" + report.chargingMask() + " chargingKnown=" + report.chargingKnownMask());
    }

    private static boolean selected(Job job) {
        return SOURCE.equals(Settings.Global.getString(job.context.getContentResolver(), BtWatcher.KEY_BATTERY_SOURCE))
                && java.util.Objects.equals(job.filter,
                Settings.Global.getString(job.context.getContentResolver(), BtWatcher.KEY_FILTER));
    }

    private static final class Job {
        final Context context;
        final String address, name, filter;
        final BluetoothSocket socket;
        volatile boolean closed;
        ScheduledFuture<?> timeout;
        Job(Context context, String address, String name, BluetoothSocket socket) {
            this.context = context.getApplicationContext();
            this.address = address;
            this.name = name;
            this.socket = socket;
            this.filter = Settings.Global.getString(context.getContentResolver(), BtWatcher.KEY_FILTER);
        }
    }
}
