package io.github.mitchelded.twspopup;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

import org.json.JSONObject;

import de.robv.android.xposed.XposedHelpers;

/**
 * Builds the app's TwsFastPairDeviceBean, the object its BLE scanner normally produces when it sees
 * vivo earbuds, from a PopupRequest.
 */
final class DeviceBean {
    /** Setter names on EarbudFastPair$BitmapNameBean, same order as ModelCatalog.ART_KEYS. */
    private static final String[] ART_SETTERS =
            {"setEnter", "setChange", "setCircle", "setConnecting", "setLeft", "setRight", "setBox"};

    final Object raw;

    private DeviceBean(Object raw) {
        this.raw = raw;
    }

    /** Returns null (after logging why) when this request cannot be shown. */
    static DeviceBean create(VivoApi api, Context ctx, PopupRequest req) throws Exception {
        JSONObject entry = ModelCatalog.find(ctx, req.model);
        if (entry == null) {
            L.w("model " + req.model + " is not in the app's config; send the LIST action to see the valid ids");
            return null;
        }
        ModelCatalog.Art art = ModelCatalog.art(ctx, req.model, entry);
        if (!art.missing.isEmpty()) {
            L.w("model " + req.model + " (" + entry.optString("name") + ") has no pictures in the APK for "
                    + art.missing + " (looked in assets/" + art.dir + "); the app would show nothing, pick another model");
            return null;
        }
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            L.w("no Bluetooth adapter");
            return null;
        }

        Object raw = api.newBean(adapter.getRemoteDevice(req.mac));

        Object names = api.newBitmapNames();
        XposedHelpers.callMethod(names, "setFileModel", art.fileModel);
        for (int i = 0; i < ART_SETTERS.length; i++) XposedHelpers.callMethod(names, ART_SETTERS[i], art.names[i]);
        XposedHelpers.callMethod(raw, "setBitmapNameBean", names);

        String title = req.title != null ? req.title : entry.optString("name", "TWS");
        XposedHelpers.callMethod(raw, "setModel", req.model);
        XposedHelpers.callMethod(raw, "setDeviceType", entry.optInt("device_type", 1));
        XposedHelpers.callMethod(raw, "setTitle", title);
        XposedHelpers.callMethod(raw, "setDeviceName", title);

        DeviceBean bean = new DeviceBean(raw);
        bean.apply(req, req.state);
        return bean;
    }

    /** The fields that may change while the popup is open. */
    void apply(PopupRequest req, int state) {
        XposedHelpers.callMethod(raw, "setViewState", state);
        // 0 = not connected, 1 = pairing, 2 = connected
        XposedHelpers.callMethod(raw, "setPairState",
                state >= PopupRequest.STATE_CONNECTED ? 2 : state >= PopupRequest.STATE_CONNECTING ? 1 : 0);
        XposedHelpers.callMethod(raw, "setLeftBattery", req.left);
        XposedHelpers.callMethod(raw, "setRightBattery", req.right);
        XposedHelpers.callMethod(raw, "setBaseBattery", req.box);
        XposedHelpers.callMethod(raw, "setChargeState", req.charging);
        XposedHelpers.callMethod(raw, "setBaseStateEarbudsCount", req.inCase);
        // The constructor sets this from the bond state of the (usually fake) address, i.e. to true,
        // which makes the connected card start the "first connection" feature tour.
        XposedHelpers.setBooleanField(raw, "mFirstConnected", req.firstPair);
    }
}
