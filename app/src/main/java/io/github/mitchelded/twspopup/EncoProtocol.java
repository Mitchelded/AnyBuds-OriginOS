package io.github.mitchelded.twspopup;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/** Small, bounded OPOv1 codec reconstructed from the supplied APK. See docs/ENCO_PROTOCOL.md. */
final class EncoProtocol {
    static final int CAPABILITIES = 0x0200;
    static final int REGISTER = 0x0201;
    static final int NOTIFICATION = 0x0204;
    private static final int MAX_PACKET = 4096;

    static final class Packet {
        final int command, sequence;
        final byte[] payload;
        Packet(int command, int sequence, byte[] payload) {
            this.command = command;
            this.sequence = sequence;
            this.payload = payload;
        }
    }

    static byte[] encode(int command, int sequence, byte[] payload) {
        if (payload.length > 120 || sequence < 0 || sequence > 255) throw new IllegalArgumentException();
        byte[] frame = new byte[payload.length + 9];
        frame[0] = (byte) 0xaa;
        frame[1] = (byte) (payload.length + 7);
        // Two zero link-control bytes, followed by the little-endian command packet.
        frame[4] = (byte) command;
        frame[5] = (byte) (command >>> 8);
        frame[6] = (byte) sequence;
        frame[7] = (byte) payload.length;
        System.arraycopy(payload, 0, frame, 9, payload.length);
        return frame;
    }

    static Packet read(InputStream input) throws IOException {
        ByteArrayOutputStream packet = new ByteArrayOutputStream();
        int expectedFragment = 0;
        boolean fragmented = false;
        for (int frames = 0; frames < 64; frames++) {
            if (one(input) != 0xaa) throw new IOException("Invalid OPO frame marker");
            int first = one(input);
            int length = first & 0x7f;
            if ((first & 0x80) != 0) {
                int second = one(input);
                if ((second & 0x80) != 0 || second == 0) throw new IOException("Invalid OPO length");
                length |= second << 7;
            }
            if (length < 3 || length > MAX_PACKET) throw new IOException("OPO frame outside limits");
            byte[] frame = new byte[length];
            for (int offset = 0; offset < length;) {
                int count = input.read(frame, offset, length - offset);
                if (count < 0) throw new EOFException("Truncated OPO frame");
                if (count == 0) frame[offset++] = (byte) one(input);
                else offset += count;
            }
            int kind = frame[0] & 3;
            int offset = kind == 0 ? 2 : 3;
            if (kind == 0 && fragmented) throw new IOException("Interrupted OPO fragments");
            if (kind != 0) {
                if (kind == 1 ? fragmented : !fragmented) throw new IOException("Unexpected OPO fragment");
                if ((frame[2] & 255) != expectedFragment++) throw new IOException("OPO fragment order");
                fragmented = true;
            }
            if (packet.size() + length - offset > MAX_PACKET) throw new IOException("OPO packet too large");
            packet.write(frame, offset, length - offset);
            if (kind == 0 || kind == 3) return decode(packet.toByteArray());
        }
        throw new IOException("Too many OPO fragments");
    }

    private static int one(InputStream in) throws IOException {
        int value = in.read();
        if (value < 0) throw new EOFException("OPO stream ended");
        return value;
    }

    private static Packet decode(byte[] packet) throws IOException {
        if (packet.length < 5 || u16(packet, 3) != packet.length - 5) throw new IOException("OPO packet length mismatch");
        return new Packet(u16(packet, 0), packet[2] & 255, Arrays.copyOfRange(packet, 5, packet.length));
    }

    private static int u16(byte[] data, int offset) {
        return (data[offset] & 255) | ((data[offset + 1] & 255) << 8);
    }

    static boolean supportsBattery(Packet packet) {
        byte[] data = packet.payload;
        if (packet.command != 0x8200 || packet.sequence != 0 || data.length < 2 || data[0] != 0
                || (data[1] & 255) != data.length - 2) return false;
        for (int i = 2; i < data.length; i++) if (data[i] == 1) return true;
        return false;
    }

    static VendorBatteryReport battery(Packet packet) {
        // Only actual notification packets; aggregate Android battery never enters this codec.
        byte[] data = packet.payload;
        if (packet.command != NOTIFICATION || data.length < 4 || data[0] != 1) return null;
        int count = data[1] & 255;
        if (count == 0 || count > 16 || data.length != 2 + count * 2) return null;
        VendorBatteryReport report = new VendorBatteryReport();
        for (int i = 0; i < count; i++) {
            int packed = data[3 + i * 2] & 255;
            report.add(data[2 + i * 2] & 255, packed & 127, packed >>> 7);
        }
        return report.hasLevel() ? report : null;
    }
}
