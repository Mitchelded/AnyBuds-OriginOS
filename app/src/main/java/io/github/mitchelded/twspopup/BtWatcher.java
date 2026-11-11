package io.github.mitchelded.twspopup;

import android.annotation.SuppressLint;
import android.app.AndroidAppHelper;
import android.app.Application;
import android.app.Instrumentation;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * Optional: runs inside the Bluetooth process (always alive, never frozen) and asks PopupController
 * to show the "connected" card when chosen earbuds connect.
 *
 * Off by default. To use it, add com.android.bluetooth to the module's scope and name the earbuds:
 *   settings put global twspopup_auto "Enco"       part of the Bluetooth name, or the full MAC
 *   settings delete global twspopup_auto            turns it off again
 * The setting is read on every connection, so changing it needs no restart.
 */
final class BtWatcher extends BroadcastReceiver {
    static final String KEY_FILTER = "twspopup_auto";
    static final String KEY_BATTERY_SOURCE = "twspopup_battery_source";
    private static final BatteryProvider METADATA_BATTERY = new AndroidMetadataBatteryProvider();

    private static final String HEADSET_STATE = "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED";
    private static final String A2DP_STATE = "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED";
    private static final String BATTERY_CHANGED = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED";
    private static final String EXTRA_BATTERY = "android.bluetooth.device.extra.BATTERY_LEVEL";

    /** Headset and A2DP connect within a second or two of each other: one popup for both. */
    private static final long SAME_CONNECTION_MS = 15_000;
    /** The battery level usually arrives a moment after the connection; refresh the popup when it does. */
    private static final long BATTERY_FOLLOW_UP_MS = 20_000;
    private static final long SETTLE_MS = 1_500;

    private static boolean installed;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final Map<String, Long> shownAt = new HashMap<>();
    private final EncoBatteryClient enco = new EncoBatteryClient();

    static void install() {
        if (installed) return;
        installed = true;
        Application app = AndroidAppHelper.currentApplication();
        if (app != null) {
            register(app);
            return;
        }
        XposedHelpers.findAndHookMethod(Instrumentation.class, "callApplicationOnCreate", Application.class,
                new XC_MethodHook() {
                    private boolean done;

                    @Override
                    protected void afterHookedMethod(MethodHookParam p) {
                        if (done) return;
                        done = true;
                        try {
                            register((Application) p.args[0]);
                        } catch (Throwable t) {
                            L.e("Bluetooth watcher could not start", t);
                        }
                    }
                });
    }

    private static void register(Context app) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(HEADSET_STATE);
        filter.addAction(A2DP_STATE);
        filter.addAction(BATTERY_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        BtWatcher watcher = new BtWatcher();
        if (Build.VERSION.SDK_INT >= 33) app.registerReceiver(watcher, filter, Context.RECEIVER_EXPORTED);
        else app.registerReceiver(watcher, filter);
        L.i("Bluetooth watcher ready (" + KEY_FILTER + "=" + filterOf(app) + ")");
    }

    @Override
    public void onReceive(Context ctx, Intent in) {
        try {
            String filter = filterOf(ctx);
            BluetoothDevice device = in.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (!EncoBatteryClient.SOURCE.equals(Settings.Global.getString(ctx.getContentResolver(), KEY_BATTERY_SOURCE))
                    || filter == null) enco.cancel(null);
            if (device != null && BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(in.getAction())) {
                enco.cancel(device.getAddress());
            }
            if (filter == null || device == null || !matches(device, filter)) return;

            String address = device.getAddress();
            long now = SystemClock.elapsedRealtime();
            Long last = shownAt.get(address);

            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(in.getAction())) {
                shownAt.remove(address);
                ctx.sendBroadcast(new Intent(PopupController.ACTION_VENDOR_RESET)
                        .setComponent(new ComponentName(VivoApi.PACKAGE, VivoApi.TRIGGER_RECEIVER))
                        .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES | Intent.FLAG_RECEIVER_FOREGROUND)
                        .putExtra("mac", address).putExtra("disconnectedElapsedMs", now));
                return;
            }

            if (BATTERY_CHANGED.equals(in.getAction())) {
                if (last != null && now - last < BATTERY_FOLLOW_UP_MS) {
                    request(ctx, device, in.getIntExtra(EXTRA_BATTERY, -1), true, last);
                }
                return;
            }
            if (in.getIntExtra(BluetoothProfile.EXTRA_STATE, -1) != BluetoothProfile.STATE_CONNECTED) return;
            if (last != null && now - last < SAME_CONNECTION_MS) return;
            shownAt.put(address, now);
            if (EncoBatteryClient.SOURCE.equals(Settings.Global.getString(ctx.getContentResolver(), KEY_BATTERY_SOURCE))) {
                enco.start(ctx, device);
            }
            main.postDelayed(() -> {
                Long current = shownAt.get(address);
                if (current != null && current == now) request(ctx, device, batteryOf(device), false, now);
            }, SETTLE_MS);
        } catch (Throwable t) {
            L.e("Bluetooth watcher", t);
        }
    }

    private static void request(Context ctx, BluetoothDevice device, int battery, boolean updateOnly, long connectionAt) {
        try {
            String source = Settings.Global.getString(ctx.getContentResolver(), KEY_BATTERY_SOURCE);
            boolean vendor = "heymelody".equals(source) || EncoBatteryClient.SOURCE.equals(source);
            // Aggregate changes must not erase vendor reports or keep old cache values alive.
            if (vendor && updateOnly) return;
            int level = battery < 0 ? -1 : battery;   // Android has one level for the pair; the case is unknown
            Intent show = new Intent(PopupController.ACTION_SHOW)
                    .setComponent(new ComponentName(VivoApi.PACKAGE, VivoApi.TRIGGER_RECEIVER))
                    .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES | Intent.FLAG_RECEIVER_FOREGROUND)
                    .putExtra("mac", device.getAddress())
                    .putExtra("title", nameOf(device))
                    .putExtra("state", PopupRequest.STATE_CONNECTED)
                    .putExtra("left", level)
                    .putExtra("right", level)
                    .putExtra("box", -1)
                    .putExtra("update", updateOnly);
            show.putExtra("connectionElapsedMs", connectionAt);
            if (vendor) {
                show.putExtra("left", -1).putExtra("right", -1).putExtra("vendorMode", true);
                L.i("source=" + source + "_waiting; opening popup independently of vendor reports");
            }
            if ("metadata".equals(Settings.Global.getString(ctx.getContentResolver(), KEY_BATTERY_SOURCE))) {
                BatterySnapshot snapshot = METADATA_BATTERY.read(device, battery, SystemClock.elapsedRealtime());
                show.putExtra("left", snapshot.left.percent)
                        .putExtra("right", snapshot.right.percent)
                        .putExtra("box", snapshot.chargingCase.percent)
                        .putExtra("charging", snapshot.chargingMask());
                L.i(snapshot.toString());
            }
            ctx.sendBroadcast(show);
            L.i((updateOnly ? "battery update: " : "asked for a popup: ") + nameOf(device) + " battery=" + level);
        } catch (Throwable t) {
            L.e("could not ask for a popup", t);
        }
    }

    private static String filterOf(Context ctx) {
        String v = Settings.Global.getString(ctx.getContentResolver(), KEY_FILTER);
        return v == null || v.trim().isEmpty() ? null : v.trim();
    }

    private static boolean matches(BluetoothDevice device, String filter) {
        if (filter.equalsIgnoreCase(device.getAddress())) return true;
        String name = nameOf(device);
        return name != null && name.toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT));
    }

    // Runs in the Bluetooth host, with its permissions rather than the module APK's.
    // Permission denial on a different firmware is handled by the existing catch below.
    @SuppressLint("MissingPermission")
    private static String nameOf(BluetoothDevice device) {
        try {
            String alias = Build.VERSION.SDK_INT >= 30 ? device.getAlias() : null;
            return alias != null && !alias.isEmpty() ? alias : device.getName();
        } catch (Throwable t) {
            return null;
        }
    }

    /** BluetoothDevice.getBatteryLevel() is a hidden API: -1 unknown, 0..100 otherwise. */
    private static int batteryOf(BluetoothDevice device) {
        try {
            return (Integer) XposedHelpers.callMethod(device, "getBatteryLevel");
        } catch (Throwable t) {
            return -1;
        }
    }
}
