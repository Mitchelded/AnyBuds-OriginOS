package io.github.mitchelded.twspopup;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Module entry point. It only decides which part of the module runs in which process:
 *
 *   com.android.vivo.tws.vivotws (main process) - PopupController: shows / updates / closes the popup
 *   com.android.vivo.tws.vivotws (any process)  - AppLogMirror: optional debug mirror of the app's own log
 *   com.android.bluetooth                       - BtWatcher: optional "show when my earbuds connect"
 */
public class Hook implements IXposedHookLoadPackage {
    static final String BLUETOOTH = "com.android.bluetooth";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        try {
            if (VivoApi.PACKAGE.equals(lp.packageName)) {
                if (Props.flag("debug.twspopup.applog")) AppLogMirror.install(lp.classLoader, lp.processName);
                // The popup manager and its state live in the main process; ":ui" only draws.
                if (VivoApi.PACKAGE.equals(lp.processName)) PopupController.install(lp.classLoader);
            } else if (HeyMelodyBatteryHook.PACKAGE.equals(lp.packageName)) {
                L.i("module=1.4-dev HeyMelody process=" + lp.processName);
                if (HeyMelodyBatteryHook.PACKAGE.equals(lp.processName)) HeyMelodyBatteryHook.install(lp.classLoader);
            } else if (BLUETOOTH.equals(lp.packageName)) {
                BtWatcher.install();
            }
        } catch (Throwable t) {
            L.e("install failed in " + lp.processName, t);
        }
    }
}
