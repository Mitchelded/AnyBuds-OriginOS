package io.github.mitchelded.twspopup;

import java.lang.ref.WeakReference;

/** Correlates a setter input with HeyMelody's immediately following battery event. */
final class VendorBatteryEvent {
    static final int PROTOCOL_PARTS = 1048588;
    static final int VENDOR_PARTS = 1048589;
    static final int AGGREGATE = 1048590;

    private final WeakReference<Object> device;
    final String address;
    final String name;
    final VendorBatteryReport report;
    final long observed;

    VendorBatteryEvent(Object device, String address, String name, VendorBatteryReport report, long observed) {
        this.device = new WeakReference<>(device);
        this.address = address;
        this.name = name;
        this.report = report;
        this.observed = observed;
    }

    boolean matches(int eventId, Object data, long now) {
        return (eventId == PROTOCOL_PARTS || eventId == VENDOR_PARTS)
                && data != null && device.get() == data
                && observed >= 0 && now >= observed && now - observed <= VendorBatteryCache.MAX_AGE_MS;
    }
}
