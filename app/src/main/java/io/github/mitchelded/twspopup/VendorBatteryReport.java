package io.github.mitchelded.twspopup;

/** A single HeyMelody report. Missing parts never inherit another report/device's values. */
final class VendorBatteryReport {
    final int[] percent = {-1, -1, -1};
    final Boolean[] charging = new Boolean[3];
    private final boolean[] seen = new boolean[3];

    void add(int type, int level, int state) {
        if (type < 1 || type > 3) return;
        int part = type - 1;
        // Ambiguous duplicate records must not silently win by list order.
        if (seen[part]) {
            percent[part] = -1;
            charging[part] = null;
            return;
        }
        seen[part] = true;
        // HeyMelody uses zero for absent/unavailable; it cannot prove measured empty here.
        percent[part] = level >= 1 && level <= 100 ? level : -1;
        charging[part] = state == 0 ? Boolean.FALSE : state == 1 ? Boolean.TRUE : null;
    }

    boolean hasLevel() {
        return percent[0] >= 0 || percent[1] >= 0 || percent[2] >= 0;
    }

    int chargingMask() {
        int mask = 0;
        for (int i = 0; i < 3; i++) if (Boolean.TRUE.equals(charging[i])) mask |= 1 << i;
        return mask;
    }

    int chargingKnownMask() {
        int mask = 0;
        for (int i = 0; i < 3; i++) if (charging[i] != null) mask |= 1 << i;
        return mask;
    }
}
