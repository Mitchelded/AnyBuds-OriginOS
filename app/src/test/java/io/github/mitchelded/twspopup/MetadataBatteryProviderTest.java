package io.github.mitchelded.twspopup;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class MetadataBatteryProviderTest {
    private static final String DEVICE = "AA:BB:CC:DD:EE:FF";

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    public void readsSeparatePartsAndChargingFromAospKeys() {
        Map<Integer, byte[]> values = new HashMap<>();
        values.put(10, bytes("15"));
        values.put(11, bytes("83"));
        values.put(12, bytes("0"));
        values.put(13, bytes("true"));
        values.put(14, bytes("false"));
        values.put(15, bytes("true"));
        BatterySnapshot snapshot = MetadataBatteryProvider.read(DEVICE, 1234, 60, values::get);
        assertEquals(15, snapshot.left.percent);
        assertEquals(83, snapshot.right.percent);
        assertEquals(0, snapshot.chargingCase.percent);
        assertEquals(60, snapshot.aggregatePercent);
        assertEquals(5, snapshot.chargingMask());
        assertEquals(7, snapshot.chargingKnownMask());
        assertEquals(DEVICE, snapshot.deviceAddress);
        assertEquals(1234, snapshot.readElapsedRealtimeMs);
        assertEquals(BatterySnapshot.Source.ANDROID_METADATA_CACHE, snapshot.left.source);
    }

    @Test
    public void aggregateDoesNotFillMissingParts() {
        BatterySnapshot snapshot = MetadataBatteryProvider.read(DEVICE, 100, 77, key -> null);
        assertEquals(77, snapshot.aggregatePercent);
        assertEquals(-1, snapshot.left.percent);
        assertEquals(-1, snapshot.right.percent);
        assertEquals(-1, snapshot.chargingCase.percent);
        assertNull(snapshot.left.charging);
        assertEquals(0, snapshot.chargingKnownMask());
        assertEquals(BatterySnapshot.Source.UNKNOWN, snapshot.left.source);
    }

    @Test
    public void unavailableRightBudDoesNotEraseLeftOrCase() {
        BatterySnapshot snapshot = MetadataBatteryProvider.read(DEVICE, 200, -1, key -> {
            if (key == 11 || key == 14) throw new SecurityException("denied");
            if (key == 10) return bytes("25");
            if (key == 12) return bytes("90");
            return null;
        });
        assertEquals(25, snapshot.left.percent);
        assertEquals(-1, snapshot.right.percent);
        assertEquals(90, snapshot.chargingCase.percent);
        assertNull(snapshot.right.charging);
        assertEquals(2, snapshot.failedReads);
    }

    @Test
    public void rejectsMalformedAndOutOfRangeBatteryValues() {
        for (String value : new String[]{"-1", "101", "999", "+5", "5.0", "NaN", "", "25%", "1e2", "\uFF11", "x".repeat(17)}) {
            BatterySnapshot snapshot = MetadataBatteryProvider.read(DEVICE, 0, 101, key -> bytes(value));
            assertEquals(value, -1, snapshot.left.percent);
            assertEquals(value, -1, snapshot.right.percent);
            assertEquals(value, -1, snapshot.chargingCase.percent);
            assertEquals(-1, snapshot.aggregatePercent);
            assertNull(snapshot.left.charging);
        }
    }

    @Test
    public void handlesZeroFullBatteryAndWhitespace() {
        for (int level : new int[]{0, 1, 99, 100}) {
            BatterySnapshot snapshot = MetadataBatteryProvider.read(DEVICE, 0, level, key -> bytes(" " + level + " "));
            assertEquals(level, snapshot.left.percent);
            assertEquals(level, snapshot.aggregatePercent);
            assertNull(snapshot.left.charging);
        }
    }

    @Test
    public void unknownChargingRemainsDistinctFromFalse() {
        BatterySnapshot snapshot = MetadataBatteryProvider.read(DEVICE, 0, -1, key -> {
            if (key == 13) return bytes("false");
            if (key == 14) return bytes("1");
            return null;
        });
        assertEquals(Boolean.FALSE, snapshot.left.charging);
        assertNull(snapshot.right.charging);
        assertNull(snapshot.chargingCase.charging);
        assertEquals(0, snapshot.chargingMask());
        assertEquals(1, snapshot.chargingKnownMask());
    }

    @Test
    public void apiFailureReturnsUnknownWithDiagnostics() {
        BatterySnapshot snapshot = MetadataBatteryProvider.read(DEVICE, 0, 30, key -> {
            throw new UnsupportedOperationException("API unavailable");
        });
        assertEquals(6, snapshot.failedReads);
        assertEquals(30, snapshot.aggregatePercent);
        assertEquals(-1, snapshot.left.percent);
        assertEquals(0, snapshot.chargingKnownMask());
    }

    @Test
    public void doesNotReuseDataBetweenDevicesOrReads() {
        BatterySnapshot first = MetadataBatteryProvider.read(DEVICE, 100, -1, key -> bytes("10"));
        BatterySnapshot second = MetadataBatteryProvider.read("11:22:33:44:55:66", 200, -1, key -> null);
        BatterySnapshot third = MetadataBatteryProvider.read(DEVICE, 300, -1, key -> null);
        assertEquals(10, first.left.percent);
        assertEquals(-1, second.left.percent);
        assertEquals(-1, third.left.percent);
        assertEquals(100, first.readElapsedRealtimeMs);
        assertEquals(300, third.readElapsedRealtimeMs);
    }
}
