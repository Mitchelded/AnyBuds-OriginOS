package io.github.mitchelded.twspopup;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import java.util.Locale;

/**
 * What to show, parsed from the extras of the SHOW broadcast.
 * Every number may be sent as an int (--ei) or as a string (--es), whichever is easier to type.
 */
final class PopupRequest {
    /*
     * View states understood by the popup (TwsFastPairDeviceBean.mViewState). This is the field that
     * decides what the popup draws; with 0 it draws an empty card.
     *   1  new earbuds: opening animation + "Connect" button
     *  60  connecting animation
     *  80  connected: left / right / case pictures with battery levels, closes itself after 5 s
     * (20, 30, 40, 50 are the account / already-paired / waiting variants of 1.)
     */
    static final int STATE_CONNECT = 1;
    static final int STATE_CONNECTING = 60;
    static final int STATE_CONNECTED = 80;

    /** `settings put global twspopup_model 68` changes the model used when the broadcast names none. */
    static final String KEY_DEFAULT_MODEL = "twspopup_model";
    private static final int FALLBACK_MODEL = 32; // vivo TWS 2e: full animation is bundled in the app
    private static final String FAKE_MAC = "AA:BB:CC:DD:EE:FF";

    /** Bit masks for "charging" and "incase". */
    static final int LEFT = 1, RIGHT = 2, CASE = 4;

    final int model;
    final int state;
    final int left, right, box;   // battery percent, -1 shows "--"
    final int charging;           // LEFT | RIGHT | CASE
    final int inCase;             // LEFT | RIGHT
    final boolean firstPair;      // true makes the app treat it as a first connection (feature tour)
    final int timeoutSec;         // 0: the popup's own timing decides
    final boolean updateOnly;     // refresh an open popup, never open a new one (late battery reports)
    final boolean vendorMode;
    final long connectionElapsedMs;
    final String title;           // null: the model's name from the app's config
    final String mac;

    private PopupRequest(Context ctx, Intent in) {
        Bundle x = in.getExtras() != null ? in.getExtras() : Bundle.EMPTY;
        model = number(x, "model", defaultModel(ctx));
        state = state(x.get("state"));
        left = battery(x, "left", 80);
        right = battery(x, "right", 80);
        box = battery(x, "box", 100);
        charging = number(x, "charging", 0) & (LEFT | RIGHT | CASE);
        inCase = number(x, "incase", LEFT | RIGHT) & (LEFT | RIGHT);
        firstPair = flag(x, "first");
        timeoutSec = Math.max(0, number(x, "timeout", 0));
        updateOnly = flag(x, "update");
        vendorMode = flag(x, "vendorMode");
        connectionElapsedMs = in.getLongExtra("connectionElapsedMs", -1);
        title = text(x, "title");
        mac = mac(text(x, "mac"));
    }

    static PopupRequest from(Context ctx, Intent in) {
        return new PopupRequest(ctx, in);
    }

    private PopupRequest(PopupRequest original, VendorBatteryCache.Entry entry) {
        model = original.model;
        state = original.state;
        left = entry != null ? entry.left : -1;
        right = entry != null ? entry.right : -1;
        box = entry != null ? entry.box : -1;
        charging = entry != null ? entry.charging : 0;
        inCase = original.inCase;
        firstPair = original.firstPair;
        timeoutSec = original.timeoutSec;
        updateOnly = original.updateOnly;
        vendorMode = original.vendorMode;
        connectionElapsedMs = original.connectionElapsedMs;
        title = original.title;
        mac = original.mac;
    }

    PopupRequest withVendorBattery(VendorBatteryCache.Entry entry) {
        return new PopupRequest(this, entry);
    }

    /** Same earbuds: the open popup can be updated in place instead of being reopened. */
    boolean sameDevice(PopupRequest o) {
        return model == o.model && mac.equals(o.mac);
    }

    @Override
    public String toString() {
        return "model=" + model + " state=" + state + " L=" + left + " R=" + right + " case=" + box
                + (title != null ? " title=\"" + title + '"' : "") + " mac=" + mac;
    }

    private static int defaultModel(Context ctx) {
        try {
            String v = Settings.Global.getString(ctx.getContentResolver(), KEY_DEFAULT_MODEL);
            if (v != null) return Integer.parseInt(v.trim());
        } catch (Exception ignored) {
            // unset or not a number: use the built-in default
        }
        return FALLBACK_MODEL;
    }

    private static int state(Object v) {
        if (v instanceof Number) return ((Number) v).intValue();
        if (v == null) return STATE_CONNECTED;
        String s = v.toString().trim().toLowerCase(Locale.ROOT);
        switch (s) {
            case "connect":
            case "pair":
                return STATE_CONNECT;
            case "connecting":
                return STATE_CONNECTING;
            case "connected":
            case "battery":
                return STATE_CONNECTED;
            default:
                try {
                    return Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    L.w("unknown state \"" + s + "\", using connected");
                    return STATE_CONNECTED;
                }
        }
    }

    private static int battery(Bundle x, String key, int def) {
        int v = number(x, key, def);
        return v < 0 ? -1 : Math.min(v, 100);
    }

    private static int number(Bundle x, String key, int def) {
        Object v = x.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        if (v instanceof String) {
            try {
                return Integer.parseInt(((String) v).trim());
            } catch (NumberFormatException e) {
                L.w(key + "=\"" + v + "\" is not a number, using " + def);
            }
        }
        return def;
    }

    private static boolean flag(Bundle x, String key) {
        Object v = x.get(key);
        if (v instanceof Boolean) return (Boolean) v;
        return v != null && ("1".equals(v.toString()) || "true".equalsIgnoreCase(v.toString()));
    }

    private static String text(Bundle x, String key) {
        Object v = x.get(key);
        return v == null || v.toString().trim().isEmpty() ? null : v.toString().trim();
    }

    private static String mac(String v) {
        if (v == null) return FAKE_MAC;
        String m = v.toUpperCase(Locale.ROOT);
        if (BluetoothAdapter.checkBluetoothAddress(m)) return m;
        L.w("mac \"" + v + "\" is not a valid address, using a placeholder");
        return FAKE_MAC;
    }
}
