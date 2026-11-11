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
            this.left = left > 0 ? BatterySnapshot.validPercent(left) : -1;
            this.right = right > 0 ? BatterySnapshot.validPercent(right) : -1;
            this.box = box > 0 ? BatterySnapshot.validPercent(box) : -1;
            this.charging = charging >= 0 && charging <= 7 ? charging : 0;
        }
    }

    private static final class Slot {
        Entry entry;
        long disconnectedAt = -1;
    }

    private final Map<String, Slot> slots = new LinkedHashMap<>();

    void disconnect(String address, long observed) {
        if (address == null || observed < 0) return;
        Slot slot = slots.get(address);
        if (slot == null) slot = new Slot();
        slot.disconnectedAt = Math.max(slot.disconnectedAt, observed);
        if (slot.entry != null && slot.entry.observed <= slot.disconnectedAt) slot.entry = null;
        remember(address, slot);
    }

    boolean put(String address, Entry entry, long now) {
        if (address == null || entry.observed < 0 || now < entry.observed
                || now - entry.observed > MAX_AGE_MS) return false;
        if (entry.left < 0 && entry.right < 0 && entry.box < 0) return false;
        Slot slot = slots.get(address);
        if (slot == null) slot = new Slot();
        if (entry.observed <= slot.disconnectedAt) return false;
        if (slot.entry != null && entry.observed < slot.entry.observed) return false;
        slot.entry = entry;
        remember(address, slot);
        return true;
    }

    Entry get(String address, long now) {
        Slot slot = slots.get(address);
        Entry entry = slot != null ? slot.entry : null;
        if (entry != null && (now < entry.observed || now - entry.observed > MAX_AGE_MS)) {
            slot.entry = null;
            return null;
        }
        return entry;
    }

    private void remember(String address, Slot slot) {
        slots.remove(address);
        slots.put(address, slot);
        if (slots.size() > 16) slots.remove(slots.keySet().iterator().next());
    }
}
