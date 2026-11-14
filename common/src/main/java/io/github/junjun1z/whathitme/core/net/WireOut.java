package io.github.junjun1z.whathitme.core.net;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Growable big-endian byte buffer. Strings are a VarInt byte length + UTF-8, the same layout as Forge's
 * ByteBufUtils.writeUTF8String and vanilla FriendlyByteBuf, so the 1.12.2 wire format of What Hit Me? 1.0.x is kept.
 */
public final class WireOut {

    private byte[] buf = new byte[256];
    private int len;

    private void ensure(int n) {
        if (len + n > buf.length) buf = Arrays.copyOf(buf, Math.max(buf.length * 2, len + n));
    }

    public WireOut writeByte(int v) {
        ensure(1);
        buf[len++] = (byte) v;
        return this;
    }

    public WireOut writeBoolean(boolean v) {
        return writeByte(v ? 1 : 0);
    }

    public WireOut writeShort(int v) {
        ensure(2);
        buf[len++] = (byte) (v >>> 8);
        buf[len++] = (byte) v;
        return this;
    }

    public WireOut writeInt(int v) {
        ensure(4);
        buf[len++] = (byte) (v >>> 24);
        buf[len++] = (byte) (v >>> 16);
        buf[len++] = (byte) (v >>> 8);
        buf[len++] = (byte) v;
        return this;
    }

    public WireOut writeLong(long v) {
        writeInt((int) (v >>> 32));
        return writeInt((int) v);
    }

    public WireOut writeFloat(float v) {
        return writeInt(Float.floatToIntBits(v));
    }

    public WireOut writeVarInt(int v) {
        while ((v & ~0x7F) != 0) {
            writeByte((v & 0x7F) | 0x80);
            v >>>= 7;
        }
        return writeByte(v);
    }

    public WireOut writeString(String s) {
        byte[] b = (s == null ? "" : s).getBytes(StandardCharsets.UTF_8);
        writeVarInt(b.length);
        return writeBytes(b);
    }

    public WireOut writeBytes(byte[] b) {
        ensure(b.length);
        System.arraycopy(b, 0, buf, len, b.length);
        len += b.length;
        return this;
    }

    public int size() {
        return len;
    }

    public byte[] toByteArray() {
        return Arrays.copyOf(buf, len);
    }
}
