package io.github.mitchelded.twspopup;

import org.junit.Test;
import static org.junit.Assert.*;

public class VendorBatteryReportTest {
    @Test public void distinctPartsMatchVendorExample() {
        VendorBatteryReport r = new VendorBatteryReport();
        r.add(1, 90, 0); r.add(2, 100, 1); r.add(3, 100, -1);
        assertArrayEquals(new int[]{90, 100, 100}, r.percent);
        assertEquals(2, r.chargingMask());
        assertEquals(3, r.chargingKnownMask());
    }

    @Test public void absentCaseAndOtherBudStayUnknown() {
        VendorBatteryReport r = new VendorBatteryReport();
        r.add(2, 45, -1);
        assertArrayEquals(new int[]{-1, 45, -1}, r.percent);
        assertTrue(r.hasLevel());
        assertEquals(0, r.chargingKnownMask());
    }

    @Test public void zeroInvalidAndAggregateAreNotPerPartReadings() {
        VendorBatteryReport r = new VendorBatteryReport();
        r.add(1, 0, -1); r.add(2, 101, 2); r.add(3, -1, -1); r.add(255, 100, 1);
        assertArrayEquals(new int[]{-1, -1, -1}, r.percent);
        assertFalse(r.hasLevel());
        assertEquals(0, r.chargingKnownMask());
    }

    @Test public void conflictingDuplicatesRemainUnknown() {
        VendorBatteryReport r = new VendorBatteryReport();
        r.add(1, 90, 0); r.add(1, 100, 1); r.add(1, 80, 1);
        assertEquals(-1, r.percent[0]);
        assertNull(r.charging[0]);
    }

    @Test public void newReportNeverRetainsMissingParts() {
        VendorBatteryReport first = new VendorBatteryReport();
        first.add(1, 90, 1); first.add(3, 100, 0);
        VendorBatteryReport second = new VendorBatteryReport();
        second.add(2, 30, 0);
        assertArrayEquals(new int[]{-1, 30, -1}, second.percent);
        assertEquals(0, second.chargingMask());
    }
}
