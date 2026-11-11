package io.github.mitchelded.twspopup;

import org.junit.Test;
import static org.junit.Assert.*;

public class VendorBatteryCacheTest {
    private VendorBatteryCache.Entry entry(long time) {
        return new VendorBatteryCache.Entry(time, 90, 100, 100, 2);
    }

    @Test public void reportBeforePopupIsAvailableWithoutOpeningAnything() {
        VendorBatteryCache c = new VendorBatteryCache();
        assertTrue(c.put("a", entry(100), 200));
        assertEquals(90, c.get("a", 300).left);
        assertEquals(100, c.get("a", 300).right);
    }

    @Test public void expiredAndFutureReportsAreRejected() {
        VendorBatteryCache c = new VendorBatteryCache();
        assertFalse(c.put("a", entry(100), 10101));
        assertFalse(c.put("a", entry(201), 200));
        assertNull(c.get("a", 200));
    }

    @Test public void expiryUsesObservationTimeNotReceiptTime() {
        VendorBatteryCache c = new VendorBatteryCache();
        assertTrue(c.put("a", entry(100), 10000));
        assertNotNull(c.get("a", 10100));
        assertNull(c.get("a", 10101));
    }

    @Test public void delayedReportCannotOverwriteNewerReading() {
        VendorBatteryCache c = new VendorBatteryCache();
        assertTrue(c.put("a", entry(200), 200));
        assertFalse(c.put("a", entry(100), 300));
        assertEquals(200, c.get("a", 300).observed);
    }

    @Test public void disconnectAndDevicesAreIsolated() {
        VendorBatteryCache c = new VendorBatteryCache();
        c.put("a", entry(100), 200); c.put("b", entry(100), 200);
        c.disconnect("a", 200);
        assertNull(c.get("a", 200));
        assertNotNull(c.get("b", 200));
    }

    @Test public void invalidValuesAreUnknownAndCapacityIsBounded() {
        VendorBatteryCache c = new VendorBatteryCache();
        c.put("bad", new VendorBatteryCache.Entry(100, -5, 101, 50, 255), 100);
        assertEquals(-1, c.get("bad", 100).left);
        assertEquals(-1, c.get("bad", 100).right);
        assertEquals(0, c.get("bad", 100).charging);
        for (int i = 0; i < 16; i++) c.put("device" + i, entry(100), 100);
        assertNull(c.get("bad", 100));
        assertNotNull(c.get("device15", 100));
    }

    @Test public void queuedPreDisconnectReportCannotReturnAfterReset() {
        VendorBatteryCache c = new VendorBatteryCache();
        c.put("a", entry(100), 100);
        c.disconnect("a", 200);
        assertFalse(c.put("a", entry(150), 250));
        assertFalse(c.put("a", entry(200), 250));
        assertNull(c.get("a", 250));
        assertTrue(c.put("a", entry(201), 250));
        assertEquals(201, c.get("a", 250).observed);
    }

    @Test public void delayedDisconnectPreservesNewerConnectionReport() {
        VendorBatteryCache c = new VendorBatteryCache();
        c.put("a", entry(300), 300);
        c.disconnect("a", 200);
        assertEquals(300, c.get("a", 350).observed);
        c.disconnect("a", 100);
        assertFalse(c.put("a", entry(150), 350));
    }

    @Test public void negativeObservationIsRejectedEvenJustAfterBoot() {
        VendorBatteryCache c = new VendorBatteryCache();
        assertFalse(c.put("a", entry(-1), 0));
        assertFalse(c.put("a", entry(Long.MIN_VALUE), 10));
        assertNull(c.get("a", 10));
    }

    @Test public void unusableReportCannotEraseKnownValues() {
        VendorBatteryCache c = new VendorBatteryCache();
        c.put("a", entry(100), 100);
        assertFalse(c.put("a", new VendorBatteryCache.Entry(200, 0, -1, 101, 0), 200));
        assertEquals(90, c.get("a", 200).left);
    }
}
