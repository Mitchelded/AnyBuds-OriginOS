package io.github.mitchelded.twspopup;

import android.app.AndroidAppHelper;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.provider.Settings;
import android.os.SystemClock;
import android.os.Parcelable;

import java.util.List;
import java.util.Locale;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/** Read-only observer for the exact user-supplied HeyMelody build; never opens a BT socket. */
final class HeyMelodyBatteryHook {
    static final String PACKAGE = "com.heytap.headset";
    private static final String DEVICE = "com.oplus.melody.btsdk.api.data.DeviceInfo";
    private static final String EVENT = "com.oplus.melody.btsdk.api.data.BluetoothReceiveData";
    private static final long SUPPORTED_VERSION = 117006006L;
    private static final ThreadLocal<VendorBatteryEvent> CAPTURED = new ThreadLocal<>();

    static void install(ClassLoader loader) {
        Class<?> device = XposedHelpers.findClass(DEVICE, loader);
        Class<?> event = XposedHelpers.findClass(EVENT, loader);
        XposedHelpers.findAndHookConstructor(event, int.class, Parcelable.class, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                int eventId = (Integer) p.args[0];
                if (eventId != VendorBatteryEvent.PROTOCOL_PARTS && eventId != VendorBatteryEvent.VENDOR_PARTS
                        && eventId != VendorBatteryEvent.AGGREGATE) return;
                VendorBatteryEvent captured = CAPTURED.get();
                CAPTURED.remove();
                if (p.hasThrowable() || captured == null) return;
                if (!captured.matches(eventId, p.args[1], SystemClock.elapsedRealtime())) {
                    L.i("HeyMelody event ignored: aggregate, unmatched or expired; eventId=" + eventId);
                    return;
                }
                try { forward(captured, eventId); }
                catch (Throwable t) { L.e("HeyMelody event observer", t); }
            }
        });
        XposedHelpers.findAndHookMethod(device, "setBatteryInfo", List.class, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                CAPTURED.remove();
            }

            @Override protected void afterHookedMethod(MethodHookParam p) {
                if (p.hasThrowable()) return;
                try { capture(p.thisObject, p.args[0]); }
                catch (Throwable t) { L.e("HeyMelody battery observer", t); }
            }
        });
        L.i("HeyMelody observer installed; supported versionCode=" + SUPPORTED_VERSION);
    }

    private static void capture(Object device, Object value) throws Exception {
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
        // Bluetooth owns opening/closing. Keep reports that precede the vendor's connection flag.
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
        CAPTURED.set(new VendorBatteryEvent(device, address, name, report, SystemClock.elapsedRealtime()));
    }

    private static void forward(VendorBatteryEvent event, int eventId) {
        Application app = AndroidAppHelper.currentApplication();
        if (app == null || !"heymelody".equals(Settings.Global.getString(app.getContentResolver(),
                BtWatcher.KEY_BATTERY_SOURCE))) return;
        VendorBatteryReport report = event.report;
        Intent show = new Intent(PopupController.ACTION_VENDOR)
                .setComponent(new ComponentName(VivoApi.PACKAGE, VivoApi.TRIGGER_RECEIVER))
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES | Intent.FLAG_RECEIVER_FOREGROUND)
                .putExtra("mac", event.address).putExtra("title", event.name)
                .putExtra("state", PopupRequest.STATE_CONNECTED)
                .putExtra("left", report.percent[0]).putExtra("right", report.percent[1])
                .putExtra("box", report.percent[2]).putExtra("charging", report.chargingMask())
                .putExtra("observedElapsedMs", event.observed);
        app.sendBroadcast(show);
        L.i("source=heymelody_observed L=" + report.percent[0] + " R=" + report.percent[1]
                + " case=" + report.percent[2] + " charging=" + report.chargingMask()
                + " chargingKnown=" + report.chargingKnownMask()
                + " eventId=" + eventId + " measurementTime=unknown");
    }
}
