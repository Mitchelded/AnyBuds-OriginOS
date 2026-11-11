package io.github.mitchelded.twspopup;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.bluetooth.BluetoothAdapter;
import android.provider.Settings;
import java.util.Locale;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Runs in the main process of the vivo TWS app and drives its quick-pair popup.
 *
 * Trigger: an explicit broadcast to one of the app's own manifest receivers, e.g.
 *   am broadcast -n com.android.vivo.tws.vivotws/com.vivo.service.dongle.DongleBroadcastReceiver \
 *       -a io.github.mitchelded.twspopup.SHOW --ei model 32 --ei left 60 --ei right 90 --ei box 40
 * A manifest receiver is started by the system even when the app is frozen or not running, so there
 * is nothing to "wake up" first.
 *
 * Everything here runs on the main thread: the receiver, the manager's click handler and the timers.
 */
final class PopupController {
    static final String ACTION_PREFIX = "io.github.mitchelded.twspopup.";
    static final String ACTION_SHOW = ACTION_PREFIX + "SHOW";
    static final String ACTION_DISMISS = ACTION_PREFIX + "DISMISS";
    static final String ACTION_LIST = ACTION_PREFIX + "LIST";
    static final String ACTION_VENDOR = ACTION_PREFIX + "VENDOR_BATTERY";
    static final String ACTION_VENDOR_RESET = ACTION_PREFIX + "VENDOR_RESET";
    private static final VendorBatteryCache VENDOR_CACHE = new VendorBatteryCache();

    /** Click codes the popup sends back to the manager. */
    private static final int CLICK_CLOSE = 1;   // X, tap outside, or the popup's own timeout

    /** Unbinding the popup service is asynchronous; give the old popup time to go away before rebinding. */
    private static final long REBIND_DELAY_MS = 700;
    /** How long the "connecting" animation plays after "Connect" is tapped. */
    private static final long CONNECTING_MS = 1800;

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static VivoApi api;
    /** The popup this module is showing right now, or null. */
    private static Session session;
    /** Bumped whenever a popup is opened or closed, so timers of an older popup do nothing. */
    private static int generation;

    private PopupController() { }

    private static final class Session {
        final Object manager;
        final DeviceBean bean;
        final Object data;      // the manager's mData while this popup is the one on screen
        PopupRequest request;
        int state;

        Session(Object manager, DeviceBean bean, Object data, PopupRequest request) {
            this.manager = manager;
            this.bean = bean;
            this.data = data;
            this.request = request;
            this.state = request.state;
        }
    }

    static void install(ClassLoader cl) throws ReflectiveOperationException {
        if (api != null) return;
        api = new VivoApi(cl);

        XposedHelpers.findAndHookMethod(api.triggerReceiver, "onReceive", Context.class, Intent.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam p) {
                        Context ctx = (Context) p.args[0];
                        Intent in = (Intent) p.args[1];
                        String action = in != null ? in.getAction() : null;
                        if (ctx == null || action == null || !action.startsWith(ACTION_PREFIX)) return;
                        p.setResult(null);   // ours: the receiver's own USB handling must not see it
                        try {
                            onCommand(ctx, action, in);
                        } catch (Throwable t) {
                            L.e(action + " failed", t);
                        }
                    }
                });

        XposedBridge.hookMethod(api.onClick, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam p) {
                try {
                    onPopupClick(p.thisObject, String.valueOf(p.args[0]));
                } catch (Throwable t) {
                    L.e("click handling failed", t);
                }
            }
        });
        L.i("ready (send " + ACTION_SHOW + " to " + VivoApi.TRIGGER_RECEIVER + ")");
    }

    private static void onCommand(Context ctx, String action, Intent in) throws Exception {
        Application app = (Application) ctx.getApplicationContext();
        switch (action) {
            case ACTION_SHOW:
                if (in.getBooleanExtra("vendorMode", false)) {
                    VendorBatteryCache.Entry cached = VENDOR_CACHE.get(
                            in.getStringExtra("mac"), SystemClock.elapsedRealtime());
                    if (cached != null) applyVendor(in, cached);
                    L.i(cached == null ? "HeyMelody popup: waiting for vendor report"
                            : "HeyMelody popup: using recent observed report");
                }
                show(app, PopupRequest.from(app, in));
                break;
            case ACTION_VENDOR:
                if (!"heymelody".equals(Settings.Global.getString(app.getContentResolver(),
                        BtWatcher.KEY_BATTERY_SOURCE))) return;
                String address = in.getStringExtra("mac");
                if (address == null) return;
                address = address.toUpperCase(Locale.ROOT);
                if (!BluetoothAdapter.checkBluetoothAddress(address)) return;
                VendorBatteryCache.Entry entry = new VendorBatteryCache.Entry(
                        in.getLongExtra("observedElapsedMs", -1), in.getIntExtra("left", -1),
                        in.getIntExtra("right", -1), in.getIntExtra("box", -1),
                        in.getIntExtra("charging", 0));
                if (!VENDOR_CACHE.put(address, entry, SystemClock.elapsedRealtime())) {
                    L.w("HeyMelody report rejected: expired or out of order");
                    return;
                }
                L.i("HeyMelody report received L=" + entry.left + " R=" + entry.right + " case=" + entry.box);
                // Reports never open a popup. Bluetooth connection events own the window lifecycle.
                if (session != null && session.request.vendorMode && session.request.mac.equals(address)) {
                    in.putExtra("mac", address).putExtra("update", true).putExtra("vendorMode", true)
                            .putExtra("model", session.request.model);
                    show(app, PopupRequest.from(app, in));
                }
                break;
            case ACTION_VENDOR_RESET:
                String disconnected = in.getStringExtra("mac");
                VENDOR_CACHE.remove(disconnected);
                if (session != null && session.request.vendorMode && session.request.mac.equals(disconnected)) {
                    close("Bluetooth disconnected");
                }
                break;
            case ACTION_DISMISS:
                session = null;
                generation++;
                api.close(api.manager(app));
                L.i("closed on request");
                break;
            case ACTION_LIST:
                ModelCatalog.logAll(app);
                break;
            default:
                L.w("unknown action " + action);
        }
    }

    private static void applyVendor(Intent intent, VendorBatteryCache.Entry entry) {
        intent.putExtra("left", entry.left).putExtra("right", entry.right)
                .putExtra("box", entry.box).putExtra("charging", entry.charging);
    }

    private static void show(Application app, PopupRequest request) throws Exception {
        Object manager = api.manager(app);
        Object data = api.data(manager);
        boolean bound = api.isBound(manager);

        // Our popup for the same earbuds is still open in the same state: just refresh its numbers.
        Session s = session;
        if (s != null && bound && data == s.data && s.state == request.state && s.request.sameDevice(request)) {
            s.request = request;
            s.bean.apply(request, s.state);
            api.push(manager, s.bean.raw);
            L.i("updated: " + request);
            return;
        }
        if (request.updateOnly) return;   // the popup this update was meant for is already gone

        // Anything else starts from a clean manager. The manager keeps mData after a popup was closed by
        // hand, and with stale mData it neither rebinds nor shows: that is why a second popup never came.
        final int gen = ++generation;
        session = null;
        if (data != null || bound) api.reset(manager);
        Runnable task = () -> {
            if (gen == generation) open(app, manager, request, gen);
        };
        if (bound) MAIN.postDelayed(task, REBIND_DELAY_MS);
        else task.run();
    }

    private static void open(Application app, Object manager, PopupRequest request, int gen) {
        try {
            DeviceBean bean = DeviceBean.create(api, app, request);
            if (bean == null) return;
            api.push(manager, bean.raw);
            Object data = api.data(manager);
            if (data == null) {
                L.w("the app did not accept the popup data");
                return;
            }
            session = new Session(manager, bean, data, request);
            L.i("shown: " + request);
            if (request.timeoutSec > 0) {
                MAIN.postDelayed(() -> {
                    if (gen == generation) close("timeout " + request.timeoutSec + " s");
                }, request.timeoutSec * 1000L);
            }
        } catch (Throwable t) {
            L.e("could not show " + request, t);
        }
    }

    /**
     * The popup reports clicks to the manager, which forwards them to the app's BLE state machine. That
     * state machine knows nothing about our popup and ignores them, so without this the X button, a tap
     * outside and the popup's own 5 s timeout would all do nothing and the popup would stay forever.
     */
    private static void onPopupClick(Object manager, String code) throws Exception {
        Session s = session;
        if (s == null || api.data(manager) != s.data) return;   // a real vivo popup: leave it to the app
        int click;
        try {
            click = Integer.parseInt(code);
        } catch (NumberFormatException e) {
            return;
        }
        if (click != CLICK_CLOSE && s.state < PopupRequest.STATE_CONNECTING) {
            playConnect(s);   // "Connect" tapped: connecting animation, then the battery card
        } else {
            close(click == CLICK_CLOSE ? "closed from the popup" : "button tapped");
        }
    }

    private static void playConnect(Session s) throws Exception {
        final int gen = generation;
        setState(s, PopupRequest.STATE_CONNECTING);
        MAIN.postDelayed(() -> {
            if (gen != generation || session != s) return;
            try {
                setState(s, PopupRequest.STATE_CONNECTED);   // the popup closes itself 5 s later
            } catch (Throwable t) {
                L.e("could not switch to connected", t);
            }
        }, CONNECTING_MS);
    }

    private static void setState(Session s, int state) throws Exception {
        s.state = state;
        s.bean.apply(s.request, state);
        api.push(s.manager, s.bean.raw);
        L.i("state -> " + state);
    }

    private static void close(String why) {
        Session s = session;
        if (s == null) return;
        session = null;
        generation++;
        try {
            api.close(s.manager);
            L.i("closed: " + why);
        } catch (Throwable t) {
            L.e("close failed", t);
        }
    }
}
