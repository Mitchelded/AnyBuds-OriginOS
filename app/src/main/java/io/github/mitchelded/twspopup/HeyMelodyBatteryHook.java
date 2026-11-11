package io.github.mitchelded.twspopup;

import android.app.AndroidAppHelper;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.provider.Settings;
import android.os.SystemClock;

import java.util.List;
import java.util.Locale;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/** Read-only observer for the exact user-supplied HeyMelody build; never opens a BT socket. */
final class HeyMelodyBatteryHook {
    static final String PACKAGE = "com.heytap.headset";
    private static final String DEVICE = "com.oplus.melody.btsdk.api.data.DeviceInfo";
    private static final long SUPPORTED_VERSION = 117006006L;

    static void install(ClassLoader loader) {
        Class<?> device = XposedHelpers.findClass(DEVICE, loader);
        XposedHelpers.findAndHookMethod(device, "setBatteryInfo", List.class, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                if (p.hasThrowable()) return;
                try { report(p.thisObject, p.args[0]); }
                catch (Throwable t) { L.e("HeyMelody battery observer", t); }
            }
        });
        L.i("HeyMelody observer installed; supported versionCode=" + SUPPORTED_VERSION);
    }

    private static void report(Object device, Object value) throws Exception {
        Application app = AndroidAppHelper.currentApplication();
        if (app == null) { L.w("HeyMelody report skipped: application unavailable"); return; }
        String selected = Settings.Global.getString(app.getContentResolver(), BtWatcher.KEY_BATTERY_SOURCE);
        if (!"heymelody".equals(selected)) {
            L.i("HeyMelody report skipped: source=" + selected);
            return;
        }
        L.i("HeyMelody battery callback received");
        long version = app.getPackageManager().getPackageInfo(PACKAGE, 0).getLongVersionCode();
        if (version != SUPPORTED_VERSION) {
            L.w("HeyMelody unsupported versionCode=" + version + "; no battery forwarded");
            return;
        }
        String address = (String) XposedHelpers.callMethod(device, "getDeviceAddress");
        String name = (String) XposedHelpers.callMethod(device, "getDeviceName");
        if (address == null || !BluetoothAdapter.checkBluetoothAddress(address.toUpperCase(Locale.ROOT))) return;
        address = address.toUpperCase(Locale.ROOT);
        if (!Boolean.TRUE.equals(XposedHelpers.callMethod(device, "isConnected"))) {
            L.i("HeyMelody report skipped: vendor device is disconnected");
            return;
        }
        String filter = Settings.Global.getString(app.getContentResolver(), BtWatcher.KEY_FILTER);
        if (filter == null || filter.trim().isEmpty()) { L.w("HeyMelody report skipped: auto filter unset"); return; }
        filter = filter.trim();
        if (!filter.equalsIgnoreCase(address) && (name == null
                || !name.toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT)))) {
            L.i("HeyMelody report skipped: device does not match auto filter");
            return;
        }
        if (!(value instanceof List)) return;
        List<?> entries = (List<?>) value;
        if (entries.size() > 16) { L.w("HeyMelody oversized battery report ignored"); return; }
        VendorBatteryReport report = new VendorBatteryReport();
        for (Object entry : entries) {
            if (entry == null) continue;
            report.add(XposedHelpers.getIntField(entry, "mDeviceType"),
                    XposedHelpers.getIntField(entry, "mLevel"),
                    XposedHelpers.getIntField(entry, "mCharging"));
        }
        if (!report.hasLevel()) {
            L.i("HeyMelody report skipped: no usable per-part battery levels");
            return;
        }
        Intent show = new Intent(PopupController.ACTION_VENDOR)
                .setComponent(new ComponentName(VivoApi.PACKAGE, VivoApi.TRIGGER_RECEIVER))
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES | Intent.FLAG_RECEIVER_FOREGROUND)
                .putExtra("mac", address).putExtra("title", name)
                .putExtra("state", PopupRequest.STATE_CONNECTED)
                .putExtra("left", report.percent[0]).putExtra("right", report.percent[1])
                .putExtra("box", report.percent[2]).putExtra("charging", report.chargingMask())
                .putExtra("observedElapsedMs", SystemClock.elapsedRealtime());
        app.sendBroadcast(show);
        L.i("source=heymelody_observed L=" + report.percent[0] + " R=" + report.percent[1]
                + " case=" + report.percent[2] + " charging=" + report.chargingMask()
                + " chargingKnown=" + report.chargingKnownMask()
                + " measurementTime=unknown");
    }
}
