package io.github.mitchelded.twspopup;

import android.util.Log;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Debug aid, off unless `setprop debug.twspopup.applog 1` (the property is gone after a reboot).
 *
 * The vivo app has detailed logging of the whole popup flow, but its logger is switched off in
 * release builds. This copies every call to it into logcat under the tag "TwsApp", which shows
 * exactly why the app did or did not draw something.
 *
 * Unlike the rest of the module this depends on an obfuscated class name, valid for VivoTws 9.2.41.2.
 */
final class AppLogMirror {
    private static final String LOGGER = "ld.r";

    private AppLogMirror() { }

    static void install(ClassLoader cl, String process) {
        final String where = process.contains(":") ? process.substring(process.indexOf(':')) : "main";
        try {
            Class<?> logger = XposedHelpers.findClass(LOGGER, cl);
            int hooked = 0;
            for (Method m : logger.getDeclaredMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (!Modifier.isStatic(m.getModifiers()) || p.length < 2 || p[0] != String.class || p[1] != String.class) continue;
                XposedBridge.hookMethod(m, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        Log.i("TwsApp", "[" + where + "] " + param.args[0] + ": " + param.args[1]);
                    }
                });
                hooked++;
            }
            L.i("app log mirror on in " + where + " (" + hooked + " methods), tag TwsApp");
        } catch (Throwable t) {
            L.w("app log mirror unavailable for this app version: " + t);
        }
    }
}
