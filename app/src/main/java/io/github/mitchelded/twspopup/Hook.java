package io.github.mitchelded.twspopup;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.app.Service;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Runs inside com.android.vivo.tws.vivotws (main process).
 * Registers a receiver; on ACTION it builds a TwsFastPairDeviceBean and hands it to
 * TwsFastPairViewServerManager.onFoundTwsOptionView(), i.e. the same entry point
 * the BLE scan uses when real vivo earbuds open their case.
 */
public class Hook implements IXposedHookLoadPackage {
    static final String PKG = "com.android.vivo.tws.vivotws";
    static final String ACTION = "io.github.mitchelded.twspopup.SHOW";
    static final String BEAN = "com.android.vivo.tws.fastpair.bean.TwsFastPairDeviceBean";
    static final String MGR = "com.android.vivo.tws.fastpair.vipc.TwsFastPairViewServerManager";
    static boolean registered;

    static void L(String m) {
        XposedBridge.log("TwsPopup: " + m);
        Log.i("TwsPopup", m);
    }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lp) {
        if (!PKG.equals(lp.packageName)) return;
        L("handleLoadPackage process=" + lp.processName);
        // Direct trigger: starting this service (am start-foreground-service ... DongleDeviceViewService)
        // now shows the popup with default values.
        try {
            XposedHelpers.findAndHookMethod("com.android.vivo.tws.fastpair.service.DongleDeviceViewService",
                    lp.classLoader, "onCreate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam p) {
                    final Service sv = (Service) p.thisObject;
                    L("DongleDeviceViewService.onCreate in " + lp.processName);
                    // The real manager lives in the main process, so forward the request there.
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        sv.sendBroadcast(new Intent(ACTION).setPackage(PKG));
                        L("forwarded broadcast to main process");
                    }, 2000);
                }
            });
        } catch (Throwable t) {
            L("dongle hook failed: " + t);
        }
        if (!PKG.equals(lp.processName)) return;
        try {
            XposedHelpers.findAndHookMethod(MGR, lp.classLoader, "getInstance", Application.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam p) {
                    L("getInstance in main process");
                    register((Application) p.args[0], lp.classLoader);
                }
            });
        } catch (Throwable t) {
            L("getInstance hook failed: " + t);
        }
        XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam p) {
                register((Application) p.thisObject, lp.classLoader);
            }
        });
    }


    static void register(final Application app, final ClassLoader cl) {
        if (registered) return;
        registered = true;
        BroadcastReceiver r = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context c, final Intent i) {
                L("broadcast received");
                new Handler(Looper.getMainLooper()).post(() -> show(app, cl, i));
            }
        };
        IntentFilter f = new IntentFilter(ACTION);
        if (Build.VERSION.SDK_INT >= 33) app.registerReceiver(r, f, Context.RECEIVER_EXPORTED);
        else app.registerReceiver(r, f);
        L("receiver registered");
    }

    static void show(Application app, ClassLoader cl, Intent i) {
        try {
            int model = i.getIntExtra("model", 32);   // 32 = vivo TWS 2e (animations bundled in the APK)
            int left = i.getIntExtra("left", 80);
            int right = i.getIntExtra("right", 80);
            int box = i.getIntExtra("box", 100);
            String name = i.hasExtra("name") ? i.getStringExtra("name") : "vivo TWS";

            Class<?> beanC = XposedHelpers.findClass(BEAN, cl);
            Object bean;
            try {
                Constructor<?> k = beanC.getDeclaredConstructor(BluetoothDevice.class, BluetoothDevice.class);
                k.setAccessible(true);
                bean = k.newInstance((Object) null, (Object) null);
            } catch (Throwable t) {
                L("ctor(null,null) failed, using Unsafe: " + t);
                Field uf = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
                uf.setAccessible(true);
                Object u = uf.get(null);
                bean = u.getClass().getMethod("allocateInstance", Class.class).invoke(u, beanC);
            }

            // onFoundTwsOptionView() returns early unless device and bitmapNameBean are non-null.
            String mac = i.hasExtra("mac") ? i.getStringExtra("mac") : "AA:BB:CC:DD:EE:FF";
            BluetoothDevice dev = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mac);
            XposedHelpers.setObjectField(bean, "mDevice", dev);

            // Per-model data comes from the app's own assets/tws_config.json
            String json;
            try (InputStream in = app.getAssets().open("tws_config.json")) {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                byte[] buf = new byte[8192]; int n;
                while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
                json = bo.toString("UTF-8");
            }
            JSONArray arr = new JSONObject(json).getJSONArray("tws_config");
            JSONObject entry = null;
            for (int k = 0; k < arr.length(); k++) {
                if (arr.getJSONObject(k).getInt("model") == model) { entry = arr.getJSONObject(k); break; }
            }
            if (entry == null) { L("model " + model + " not found in tws_config.json"); return; }
            JSONObject bd = entry.getJSONObject("bitmap_data");
            Object bnb = XposedHelpers.newInstance(XposedHelpers.findClass(
                    "com.vivo.commonbase.bean.EarbudFastPair$BitmapNameBean", cl));
            XposedHelpers.callMethod(bnb, "setEnter", bd.getString("enter"));
            XposedHelpers.callMethod(bnb, "setChange", bd.getString("change"));
            XposedHelpers.callMethod(bnb, "setCircle", bd.getString("circle"));
            XposedHelpers.callMethod(bnb, "setConnecting", bd.getString("connecting"));
            XposedHelpers.callMethod(bnb, "setLeft", bd.getString("left"));
            XposedHelpers.callMethod(bnb, "setRight", bd.getString("right"));
            XposedHelpers.callMethod(bnb, "setBox", bd.getString("box"));
            XposedHelpers.callMethod(bnb, "setFileModel", bd.getInt("file_model"));
            XposedHelpers.callMethod(bean, "setBitmapNameBean", bnb);
            try {
                Class<?> cfgC = XposedHelpers.findClass("com.vivo.commonbase.bean.TwsConfig", cl);
                Object gson = XposedHelpers.newInstance(XposedHelpers.findClass("com.google.gson.Gson", cl));
                Object cfg = XposedHelpers.callMethod(gson, "fromJson",
                        new Class<?>[]{String.class, Class.class}, json, cfgC);
                XposedHelpers.callMethod(bean, "setTwsConfig", cfg);
            } catch (Throwable t) {
                L("setTwsConfig skipped: " + t);
            }

            XposedHelpers.callMethod(bean, "setModel", model);
            XposedHelpers.callMethod(bean, "setDeviceType", entry.getInt("device_type"));
            XposedHelpers.callMethod(bean, "setLeftBattery", left);
            XposedHelpers.callMethod(bean, "setRightBattery", right);
            XposedHelpers.callMethod(bean, "setBaseBattery", box);
            XposedHelpers.callMethod(bean, "setDeviceName", name);
            XposedHelpers.callMethod(bean, "setTitle", name);
            XposedHelpers.callMethod(bean, "setPairState",
                    XposedHelpers.getStaticIntField(beanC, "PAIR_STATE_NOT_PAIRING_NOT_CONNECTED"));
            XposedHelpers.callMethod(bean, "setBaseStateEarbudsCount", 2);

            Object mgr = XposedHelpers.callStaticMethod(XposedHelpers.findClass(MGR, cl), "getInstance", app);
            XposedHelpers.callMethod(mgr, "onFoundTwsOptionView", bean);
            L("onFoundTwsOptionView called, model=" + model);
        } catch (Throwable t) {
            L("show failed: " + t);
            Log.e("TwsPopup", "trace", t);
        }
    }
}
