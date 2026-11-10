package io.github.mitchelded.twspopup;

import android.app.Application;
import android.bluetooth.BluetoothDevice;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import de.robv.android.xposed.XposedHelpers;

/**
 * Everything the module touches inside the vivo TWS app, in one place.
 * All members are resolved once at start-up, so an incompatible app version fails immediately with a
 * precise message instead of somewhere in the middle of showing a popup.
 *
 * Verified against VivoTws 9.2.41.2. Only names that survive obfuscation are used
 * (manifest components, bean getters/setters, the manager's public methods).
 *
 * How the app works:
 *   main process:  TwsFastPairViewServerManager keeps one EarbudFastPair ("mData") and pushes it as JSON
 *                  to TwsFastPairViewService in the ":ui" process, which draws the popup.
 *   The popup sends clicks back (1 = close / its own timeout, 2 = main button); normally the app's
 *   BLE state machine reacts to them. Our popup has no entry in that state machine, so
 *   PopupController reacts instead.
 */
final class VivoApi {
    static final String PACKAGE = "com.android.vivo.tws.vivotws";
    /** Exported manifest receiver in the main process: an explicit broadcast to it wakes the app even when frozen. */
    static final String TRIGGER_RECEIVER = "com.vivo.service.dongle.DongleBroadcastReceiver";

    private static final String MANAGER = "com.android.vivo.tws.fastpair.vipc.TwsFastPairViewServerManager";
    private static final String BEAN = "com.android.vivo.tws.fastpair.bean.TwsFastPairDeviceBean";
    private static final String BITMAP_NAMES = "com.vivo.commonbase.bean.EarbudFastPair$BitmapNameBean";

    final Class<?> triggerReceiver;
    final Method onClick;

    private final Method getInstance;
    private final Method onFoundTwsOptionView;
    private final Method onFoundTwsNeedDismissView;
    private final Method unbindPairViewService;
    private final Method clearPairData;
    private final Constructor<?> newBean;
    private final Class<?> bitmapNames;

    VivoApi(ClassLoader cl) throws ReflectiveOperationException {
        Class<?> manager = cl.loadClass(MANAGER);
        Class<?> bean = cl.loadClass(BEAN);
        bitmapNames = cl.loadClass(BITMAP_NAMES);
        triggerReceiver = cl.loadClass(TRIGGER_RECEIVER);

        getInstance = method(manager, "getInstance", Application.class);
        onFoundTwsOptionView = method(manager, "onFoundTwsOptionView", bean);
        onFoundTwsNeedDismissView = method(manager, "onFoundTwsNeedDismissView", bean);
        unbindPairViewService = method(manager, "unbindPairViewService");
        clearPairData = method(manager, "clearPairData");
        onClick = method(manager, "onClick", String.class);
        newBean = bean.getDeclaredConstructor(BluetoothDevice.class, BluetoothDevice.class);
        newBean.setAccessible(true);
    }

    private static Method method(Class<?> c, String name, Class<?>... params) throws NoSuchMethodException {
        Method m = c.getDeclaredMethod(name, params);
        m.setAccessible(true);
        return m;
    }

    /** The app's own singleton; this is the object the real BLE flow talks to. */
    Object manager(Application app) throws ReflectiveOperationException {
        return getInstance.invoke(null, app);
    }

    /** What the manager is currently showing (an EarbudFastPair), or null. */
    Object data(Object manager) {
        return XposedHelpers.getObjectField(manager, "mData");
    }

    /** IDLE, BINDING or CONNECTED: the state of the connection to the popup service. */
    String bindState(Object manager) {
        return String.valueOf(XposedHelpers.getObjectField(manager, "mServiceBindState"));
    }

    boolean isBound(Object manager) {
        return !"IDLE".equals(bindState(manager));
    }

    /** First call opens the popup, later calls with the same bean push its changed fields to it. */
    void push(Object manager, Object bean) throws ReflectiveOperationException {
        onFoundTwsOptionView.invoke(manager, bean);
    }

    /**
     * Closes whatever popup is open. When the popup is up it is asked to close itself (with its own
     * animation); otherwise the connection is simply dropped and the manager's data cleared.
     */
    void close(Object manager) throws ReflectiveOperationException {
        if ("CONNECTED".equals(bindState(manager)) && data(manager) != null) {
            onFoundTwsNeedDismissView.invoke(manager, (Object) null);
        } else {
            reset(manager);
        }
    }

    /** Hard reset: afterwards the manager is in the same state as right after the app started. */
    void reset(Object manager) throws ReflectiveOperationException {
        unbindPairViewService.invoke(manager);
        clearPairData.invoke(manager);
    }

    Object newBean(BluetoothDevice device) throws ReflectiveOperationException {
        return newBean.newInstance(device, null);
    }

    Object newBitmapNames() throws ReflectiveOperationException {
        return bitmapNames.getDeclaredConstructor().newInstance();
    }
}
