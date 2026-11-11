package io.github.mitchelded.twspopup;

import java.util.LinkedHashMap;
import java.util.Map;

/** Short-lived delivery cache, not a measurement freshness guarantee. */
final class VendorBatteryCache {
    static final long MAX_AGE_MS = 10_000;
    static final class Entry {
        final long observed;
        final int left, right, box, charging;
        Entry(long observed, int left, int right, int box, int charging) {
            this.observed = observed;
            this.left = BatterySnapshot.validPercent(left);
            this.right = BatterySnapshot.validPercent(right);
            this.box = BatterySnapshot.validPercent(box);
            this.charging = charging & 7;
        }
    }
    private final Map<String, Entry> entries = new LinkedHashMap<>();

    void remove(String address) { entries.remove(address); }

    boolean put(String address, Entry entry, long now) {
        if (now < entry.observed || now - entry.observed > MAX_AGE_MS) return false;
        Entry previous = entries.get(address);
        if (previous != null && entry.observed < previous.observed) return false;
        entries.remove(address);
        entries.put(address, entry);
        if (entries.size() > 16) entries.remove(entries.keySet().iterator().next());
        return true;
    }

    Entry get(String address, long now) {
        Entry entry = entries.get(address);
        if (entry != null && (now < entry.observed || now - entry.observed > MAX_AGE_MS)) {
            entries.remove(address);
            return null;
        }
        return entry;
    }
}
