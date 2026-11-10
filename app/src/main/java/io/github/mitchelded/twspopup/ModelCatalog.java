package io.github.mitchelded.twspopup;

import android.content.Context;
import android.content.res.AssetManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The earbud models the vivo app knows, read from its own assets/tws_config.json, and the pictures it
 * ships for them in assets/flash_connect/. Mirrors what the app does when it prepares a popup
 * (TwsFastPairBitmapUtils): pick the picture folder, then load one picture set per name.
 *
 * If a single one of the seven picture sets cannot be loaded the app silently refuses to show the
 * popup, so this is checked here first and reported in plain words.
 */
final class ModelCatalog {
    private static final String CONFIG = "tws_config.json";
    private static final String ART_ROOT = "flash_connect";
    /** bitmap_data keys, in the order of the BitmapNameBean setters used by DeviceBean. */
    static final String[] ART_KEYS = {"enter", "change", "circle", "connecting", "left", "right", "box"};

    private static JSONArray models;

    private ModelCatalog() { }

    /** Where a model's pictures come from. */
    static final class Art {
        int fileModel;
        String dir;
        boolean animated;
        final String[] names = new String[ART_KEYS.length];
        final List<String> missing = new ArrayList<>();
    }

    private static synchronized JSONArray models(Context ctx) throws Exception {
        if (models == null) {
            try (InputStream in = ctx.getAssets().open(CONFIG)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[16384];
                for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
                models = new JSONObject(out.toString("UTF-8")).getJSONArray("tws_config");
            }
        }
        return models;
    }

    /** Config entry of a model, or null. Colour variants (model + 1..3) fall back to their base model. */
    static JSONObject find(Context ctx, int model) throws Exception {
        JSONArray all = models(ctx);
        JSONObject base = null;
        for (int i = 0; i < all.length(); i++) {
            JSONObject e = all.getJSONObject(i);
            int m = e.optInt("model", -1);
            if (m == model) return e;
            if (m == model / 4 * 4) base = e;
        }
        return base;
    }

    static Art art(Context ctx, int model, JSONObject entry) throws IOException {
        AssetManager assets = ctx.getAssets();
        JSONObject data = entry.optJSONObject("bitmap_data");
        Art art = new Art();

        int fileModel = data != null ? data.optInt("file_model", model) : model;
        if (fileModel <= 0 && model != 0) fileModel = model;
        if (!contains(assets.list(ART_ROOT), String.valueOf(fileModel))) fileModel = fileModel / 4 * 4;
        art.fileModel = fileModel;

        art.dir = ART_ROOT + "/" + fileModel;
        String[] files = assets.list(art.dir);
        if (files == null || files.length == 0) {
            art.dir = ART_ROOT + "/default";   // generic earbuds, used by the app for models without own pictures
            files = assets.list(art.dir);
        }
        if (files == null) files = new String[0];
        art.animated = files.length > 20;      // the animated models ship dozens of frames

        for (int i = 0; i < ART_KEYS.length; i++) {
            String name = data != null ? data.optString(ART_KEYS[i], "") : "";
            art.names[i] = name;
            if (name.isEmpty() || !anyStartsWith(files, name)) art.missing.add(ART_KEYS[i] + "=" + name);
        }
        return art;
    }

    /** Writes every usable model to logcat (the LIST broadcast). */
    static void logAll(Context ctx) throws Exception {
        JSONArray all = models(ctx);
        L.i("models known to the app (id, name, pictures):");
        for (int i = 0; i < all.length(); i++) {
            JSONObject e = all.getJSONObject(i);
            int model = e.optInt("model", -1);
            Art art = art(ctx, model, e);
            String pictures = !art.missing.isEmpty() ? "none in the APK, cannot be shown"
                    : art.dir.endsWith("/default") ? "generic"
                    : art.animated ? "own, animated" : "own";
            L.i(String.format(Locale.ROOT, "%4d  %-26s %s", model, e.optString("name"), pictures));
        }
    }

    private static boolean contains(String[] list, String value) {
        return list != null && Arrays.asList(list).contains(value);
    }

    private static boolean anyStartsWith(String[] files, String prefix) {
        for (String f : files) if (f.startsWith(prefix)) return true;
        return false;
    }
}
