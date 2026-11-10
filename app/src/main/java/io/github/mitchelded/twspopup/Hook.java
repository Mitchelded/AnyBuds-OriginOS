package io.github.mitchelded.twspopup;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

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

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lp) {
        if (!PKG.equals(lp.packageName) || !PKG.equals(lp.processName)) return;
        XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam p) {
                if (registered) return;
                registered = true;
                final Application app = (Application) p.thisObject;
                final ClassLoader cl = lp.classLoader;
                BroadcastReceiver r = new BroadcastReceiver() {
                    @Override
                    public void onReceive(final Context c, final Intent i) {
                        new Handler(Looper.getMainLooper()).post(() -> show(app, cl, i));
                    }
                };
                IntentFilter f = new IntentFilter(ACTION);
                if (Build.VERSION.SDK_INT >= 33) app.registerReceiver(r, f, Context.RECEIVER_EXPORTED);
                else app.registerReceiver(r, f);
                XposedBridge.log("TwsPopup: receiver registered");
            }
        });
    }

    static void show(Application app, ClassLoader cl, Intent i) {
        try {
            int model = i.getIntExtra("model", 32);   // 32 = vivo TWS 2e (animations bundled in the APK)
            int left = i.getIntExtra("left", 80);
            int right = i.getIntExtra("right", 80);
            int box = i.getIntExtra("box", 100);
            String name = i.hasExtra("name") ? i.getStringExtra("name") : "vivo TWS 2e";

            Class<?> beanC = XposedHelpers.findClass(BEAN, cl);
            Object bean;
            try {
                Constructor<?> k = beanC.getDeclaredConstructor(BluetoothDevice.class, BluetoothDevice.class);
                k.setAccessible(true);
                bean = k.newInstance((Object) null, (Object) null);
            } catch (Throwable t) {
                XposedBridge.log("TwsPopup: ctor(null,null) failed, using Unsafe: " + t);
                Field uf = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
                uf.setAccessible(true);
                Object u = uf.get(null);
                bean = u.getClass().getMethod("allocateInstance", Class.class).invoke(u, beanC);
            }

            XposedHelpers.callMethod(bean, "setModel", model);
            XposedHelpers.callMethod(bean, "setDeviceType", 1);
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
            XposedBridge.log("TwsPopup: onFoundTwsOptionView called, model=" + model);
        } catch (Throwable t) {
            XposedBridge.log("TwsPopup: show failed: " + t);
            XposedBridge.log(t);
        }
    }
}
