package io.github.mitchelded.twspopup;

import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import static org.junit.Assert.*;

public class EncoProtocolTest {
    private EncoProtocol.Packet packet(int command, byte... data) {
        return new EncoProtocol.Packet(command, 0, data);
    }

    @Test public void outboundFramesMatchDocumentedWireLayout() {
        assertArrayEquals(new byte[]{(byte) 0xaa, 7, 0, 0, 0, 2, 0, 0, 0},
                EncoProtocol.encode(0x0200, 0, new byte[0]));
        assertArrayEquals(new byte[]{(byte) 0xaa, 8, 0, 0, 1, 2, 1, 1, 0, 1},
                EncoProtocol.encode(0x0201, 1, new byte[]{1}));
    }

    @Test public void fragmentedInputReadsAndConcatenatedFramesStaySeparate() throws Exception {
        byte[] wire = {(byte) 0xaa, 10, 0, 0, 0, (byte) 0x82, 0, 3, 0, 0, 1, 1,
                (byte) 0xaa, 9, 0, 0, 1, (byte) 0x82, 1, 2, 0, 0, 1};
        ByteArrayInputStream stream = new ByteArrayInputStream(wire) {
            @Override public synchronized int read(byte[] data, int offset, int length) {
                return super.read(data, offset, Math.min(length, 1));
            }
        };
        assertTrue(EncoProtocol.supportsBattery(EncoProtocol.read(stream)));
        assertEquals(0x8201, EncoProtocol.read(stream).command);
        assertEquals(-1, stream.read());
    }

    @Test public void capabilityMustBeSuccessfulCorrelatedAndContainBattery() {
        assertTrue(EncoProtocol.supportsBattery(packet(0x8200, (byte) 0, (byte) 2, (byte) 3, (byte) 1)));
        assertFalse(EncoProtocol.supportsBattery(packet(0x8200, (byte) 1, (byte) 1, (byte) 1)));
        assertFalse(EncoProtocol.supportsBattery(packet(0x8200, (byte) 0, (byte) 1, (byte) 2)));
        assertFalse(EncoProtocol.supportsBattery(packet(0x8200, (byte) 0, (byte) 2, (byte) 1)));
        assertFalse(EncoProtocol.supportsBattery(new EncoProtocol.Packet(0x8200, 4, new byte[]{0, 1, 1})));
    }

    @Test public void decodesIndependentPartsAndChargingWithoutRounding() {
        VendorBatteryReport report = EncoProtocol.battery(packet(0x0204,
                (byte) 1, (byte) 3, (byte) 1, (byte) 85, (byte) 2, (byte) 228, (byte) 3, (byte) 1));
        assertArrayEquals(new int[]{85, 100, 1}, report.percent);
        assertEquals(2, report.chargingMask());
        assertEquals(7, report.chargingKnownMask());
    }

    @Test public void singleBudDoesNotPopulateMissingParts() {
        VendorBatteryReport report = EncoProtocol.battery(packet(0x0204, (byte) 1, (byte) 1, (byte) 2, (byte) 77));
        assertArrayEquals(new int[]{-1, 77, -1}, report.percent);
    }

    @Test public void rejectsWrongEventsCountsLengthsAndUnknownValues() {
        assertNull(EncoProtocol.battery(packet(0x8200, (byte) 1, (byte) 1, (byte) 1, (byte) 90)));
        assertNull(EncoProtocol.battery(packet(0x0204, (byte) 2, (byte) 1, (byte) 1, (byte) 90)));
        assertNull(EncoProtocol.battery(packet(0x0204, (byte) 1, (byte) 2, (byte) 1, (byte) 90)));
        assertNull(EncoProtocol.battery(packet(0x0204, (byte) 1, (byte) 1, (byte) 1, (byte) 0)));
        assertNull(EncoProtocol.battery(packet(0x0204, (byte) 1, (byte) 1, (byte) 1, (byte) 127)));
        assertNull(EncoProtocol.battery(packet(0x0204, (byte) 1, (byte) 1, (byte) 9, (byte) 90)));
    }

    @Test public void rejectsDuplicatePartInsteadOfLastValueWinning() {
        assertNull(EncoProtocol.battery(packet(0x0204, (byte) 1, (byte) 2,
                (byte) 1, (byte) 50, (byte) 1, (byte) 90)));
    }

    @Test public void assemblesLinkFragmentsWithSequenceValidation() throws Exception {
        byte[] frames = {(byte) 0xaa, 6, 1, 0, 0, 4, 2, 7,
                (byte) 0xaa, 9, 3, 0, 1, 4, 0, 1, 1, 2, 60};
        assertEquals(60, EncoProtocol.battery(EncoProtocol.read(new ByteArrayInputStream(frames))).percent[1]);
        frames[12] = 2;
        rejected(frames);
    }

    @Test public void rejectsMalformedFramesAndTruncation() throws Exception {
        byte[] frame = {(byte) 0xaa, 7, 0, 0, 0, 2, 0, 0, 0};
        for (int size = 0; size < frame.length; size++) rejected(Arrays.copyOf(frame, size));
        rejected(new byte[]{0, 7});
        rejected(new byte[]{(byte) 0xaa, (byte) 0x80, (byte) 0x80});
        rejected(new byte[]{(byte) 0xaa, (byte) 0xff, 127});
        frame[7] = 1;
        rejected(frame);
    }

    @Test public void supportsTwoByteLinkLength() throws Exception {
        byte[] frame = new byte[133];
        frame[0] = (byte) 0xaa;
        frame[1] = (byte) 0x82;
        frame[2] = 1; // 130 link bytes = 2 control + 5 packet header + 123 payload
        frame[5] = 4;
        frame[6] = 2;
        frame[8] = 123;
        assertEquals(123, EncoProtocol.read(new ByteArrayInputStream(frame)).payload.length);
    }

    private void rejected(byte[] wire) throws Exception {
        try { EncoProtocol.read(new ByteArrayInputStream(wire)); fail("Malformed frame accepted"); }
        catch (IOException expected) { }
    }
}
