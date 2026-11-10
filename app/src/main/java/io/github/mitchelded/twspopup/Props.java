package io.github.mitchelded.twspopup;

import de.robv.android.xposed.XposedHelpers;

/** System properties: the only switch that is readable before the app has a Context. */
final class Props {
    private Props() { }

    /** True when the property is set to 1 / true, e.g. `setprop debug.twspopup.applog 1`. */
    static boolean flag(String key) {
        try {
            Class<?> sp = XposedHelpers.findClass("android.os.SystemProperties", null);
            String v = String.valueOf(XposedHelpers.callStaticMethod(sp, "get", key, ""));
            return "1".equals(v) || "true".equalsIgnoreCase(v);
        } catch (Throwable t) {
            return false;
        }
    }
}
