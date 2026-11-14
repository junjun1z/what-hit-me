package io.github.junjun1z.whathitme.core.net;

import java.nio.charset.StandardCharsets;

/** Reader for {@link WireOut} data. Throws IllegalStateException on truncated or malformed input. */
public final class WireIn {

    private final byte[] buf;
    private int pos;
    private final int end;

    public WireIn(byte[] buf) {
        this(buf, 0, buf.length);
    }

    public WireIn(byte[] buf, int offset, int length) {
        this.buf = buf;
        this.pos = offset;
        this.end = offset + length;
    }

    private void need(int n) {
        if (n < 0 || pos + n > end) throw new IllegalStateException("What Hit Me? packet too short");
    }

    public int readByte() {
        need(1);
        return buf[pos++];
    }

    public int readUnsignedByte() {
        return readByte() & 0xFF;
    }

    public boolean readBoolean() {
        return readByte() != 0;
    }

    public short readShort() {
        need(2);
        int v = ((buf[pos] & 0xFF) << 8) | (buf[pos + 1] & 0xFF);
        pos += 2;
        return (short) v;
    }

    public int readInt() {
        need(4);
        int v = ((buf[pos] & 0xFF) << 24) | ((buf[pos + 1] & 0xFF) << 16) | ((buf[pos + 2] & 0xFF) << 8) | (buf[pos + 3] & 0xFF);
        pos += 4;
        return v;
    }

    public long readLong() {
        long hi = readInt() & 0xFFFFFFFFL;
        long lo = readInt() & 0xFFFFFFFFL;
        return (hi << 32) | lo;
    }

    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    public int readVarInt() {
        int v = 0;
        int shift = 0;
        while (true) {
            int b = readByte();
            v |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) return v;
            shift += 7;
            if (shift > 28) throw new IllegalStateException("VarInt too big");
        }
    }

    public String readString() {
        int n = readVarInt();
        if (n > 262144) throw new IllegalStateException("String too long");
        need(n);
        String s = new String(buf, pos, n, StandardCharsets.UTF_8);
        pos += n;
        return s;
    }

    public int remaining() {
        return end - pos;
    }
}
