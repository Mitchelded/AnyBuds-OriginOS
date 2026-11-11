package io.github.mitchelded.twspopup;

import org.junit.Test;
import static org.junit.Assert.*;

public class VendorBatteryEventTest {
    private VendorBatteryEvent capture(Object device, long time) {
        VendorBatteryReport report = new VendorBatteryReport();
        report.add(1, 90, 0);
        return new VendorBatteryEvent(device, "a", "Enco", report, time);
    }

    @Test public void aggregateEventCannotBecomeLeftBattery() {
        Object device = new Object();
        assertFalse(capture(device, 100).matches(VendorBatteryEvent.AGGREGATE, device, 101));
    }

    @Test public void verifiedPartEventsMatchTheirOwnDeviceInput() {
        Object device = new Object();
        VendorBatteryEvent event = capture(device, 100);
        assertTrue(event.matches(VendorBatteryEvent.PROTOCOL_PARTS, device, 101));
        assertTrue(event.matches(VendorBatteryEvent.VENDOR_PARTS, device, 101));
        assertFalse(event.matches(VendorBatteryEvent.PROTOCOL_PARTS, new Object(), 101));
        assertFalse(event.matches(VendorBatteryEvent.PROTOCOL_PARTS, null, 101));
        assertFalse(event.matches(42, device, 101));
    }

    @Test public void captureExpiresWithoutRefreshingObservationTime() {
        Object device = new Object();
        VendorBatteryEvent event = capture(device, 100);
        assertTrue(event.matches(VendorBatteryEvent.PROTOCOL_PARTS, device, 10100));
        assertFalse(event.matches(VendorBatteryEvent.PROTOCOL_PARTS, device, 10101));
        assertFalse(event.matches(VendorBatteryEvent.PROTOCOL_PARTS, device, 99));
        assertFalse(capture(device, -1).matches(VendorBatteryEvent.PROTOCOL_PARTS, device, 0));
    }
}
