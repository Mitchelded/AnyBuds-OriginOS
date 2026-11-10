package io.github.mitchelded.twspopup;

import android.util.Log;

import java.lang.reflect.InvocationTargetException;

/** Logging: everything goes to logcat under one tag, so `logcat -d -s TwsPopup` shows the whole story. */
final class L {
    static final String TAG = "TwsPopup";

    private L() { }

    static void i(String msg) {
        Log.i(TAG, msg);
    }

    static void w(String msg) {
        Log.w(TAG, msg);
    }

    static void e(String msg, Throwable t) {
        // Reflection wraps the real error; log the cause, that is the useful part.
        Throwable cause = t instanceof InvocationTargetException && t.getCause() != null ? t.getCause() : t;
        Log.e(TAG, msg + ": " + cause, cause);
    }
}
