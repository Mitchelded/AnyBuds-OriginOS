package io.github.mitchelded.twspopup;

import java.nio.charset.StandardCharsets;

/** Decodes the AOSP BluetoothDevice metadata contract without depending on Android at test time. */
final class MetadataBatteryProvider {
    // AOSP BluetoothDevice keys, verified in docs/BATTERY_RESEARCH.md.
    private static final int[] KEYS = {10, 11, 12, 13, 14, 15};

    interface Reader {
        byte[] read(int key) throws Exception;
    }

    static BatterySnapshot read(String address, long readElapsedMs, int aggregate, Reader reader) {
        byte[][] values = new byte[KEYS.length][];
        int failed = 0;
        for (int i = 0; i < KEYS.length; i++) {
            try {
                values[i] = reader.read(KEYS[i]);
            } catch (Exception e) {
                // A failed part must not discard other independently available values.
                failed++;
            }
        }
        return new BatterySnapshot(address, readElapsedMs, aggregate,
                new BatterySnapshot.Part(percent(values[0]), charging(values[3])),
                new BatterySnapshot.Part(percent(values[1]), charging(values[4])),
                new BatterySnapshot.Part(percent(values[2]), charging(values[5])), failed);
    }

    private static int percent(byte[] value) {
        String text = text(value);
        if (text == null || !text.matches("[0-9]{1,3}")) return -1;
        return BatterySnapshot.validPercent(Integer.parseInt(text));
    }

    private static Boolean charging(byte[] value) {
        String text = text(value);
        if ("true".equalsIgnoreCase(text)) return Boolean.TRUE;
        if ("false".equalsIgnoreCase(text)) return Boolean.FALSE;
        return null;
    }

    private static String text(byte[] value) {
        if (value == null || value.length == 0 || value.length > 16) return null;
        return new String(value, StandardCharsets.UTF_8).trim();
    }
}
