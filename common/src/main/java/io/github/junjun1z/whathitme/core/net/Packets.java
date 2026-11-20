package io.github.junjun1z.whathitme.core.net;

import java.util.ArrayList;
import java.util.List;

import io.github.junjun1z.whathitme.core.data.Entry;
import io.github.junjun1z.whathitme.core.data.Snapshot;

/**
 * The network protocol, independent of the loader. Each packet has a type id (also the 1.12.2 SimpleNetworkWrapper
 * discriminator, so the 1.0.x wire format is unchanged) and a body. Loaders move the bytes: one channel with
 * {@link #encode} / {@link #decode} (type byte + body), or one message per type with {@link #body} / {@link #read}.
 * Packets are only sent on death, totem / heavy hit and commands, never per hit.
 */
public final class Packets {

    /** 1 = What Hit Me? 1.0 / 1.1, 2 = 1.2 (extra record fields, armor break lines, equipment, pack data), 3 = 1.3 (templates, death messages) */
    public static final int PROTOCOL = 3;
    /** Channel name for loaders with namespaced channels ("whathitme:main") */
    public static final String CHANNEL_PATH = "main";

    public static final int HELLO = 0;
    public static final int REQUEST = 1;
    public static final int SERVER_INFO = 2;
    public static final int RECAP = 3;
    public static final int ENTRIES = 4;
    public static final int MESSAGE = 5;

    private Packets() {}

    /** A packet of the protocol */
    public interface Packet {
        int type();

        void write(WireOut out);
    }

    public static byte[] body(Packet p) {
        WireOut o = new WireOut();
        p.write(o);
        return o.toByteArray();
    }

    /** Type byte + body */
    public static byte[] encode(Packet p) {
        WireOut o = new WireOut();
        o.writeByte(p.type());
        p.write(o);
        return o.toByteArray();
    }

    public static Packet decode(byte[] data) {
        WireIn in = new WireIn(data);
        int type = in.readUnsignedByte();
        return read(type, in);
    }

    public static Packet read(int type, byte[] body) {
        return read(type, new WireIn(body));
    }

    public static Packet read(int type, WireIn in) {
        switch (type) {
            case HELLO:
                return Hello.read(in);
            case REQUEST:
                return Request.read(in);
            case SERVER_INFO:
                return ServerInfo.readAny(in);
            case RECAP:
                return Recap.read(in);
            case ENTRIES:
                return Entries.read(in);
            case MESSAGE:
                return Message.read(in);
            default:
                throw new IllegalStateException("Unknown What Hit Me? packet " + type);
        }
    }

    /** Is this a packet the client sends (the server receives it)? */
    public static boolean serverbound(int type) {
        return type == HELLO || type == REQUEST;
    }

    // ================================================================== client -> server

    /** Once per connection: "this client has What Hit Me?" */
    public static final class Hello implements Packet {
        public final int protocol;

        public Hello() {
            this(PROTOCOL);
        }

        public Hello(int protocol) {
            this.protocol = protocol;
        }

        @Override
        public int type() {
            return HELLO;
        }

        @Override
        public void write(WireOut o) {
            o.writeInt(protocol);
        }

        static Hello read(WireIn in) {
            return new Hello(in.readInt());
        }
    }

    /** Data for a /whm command */
    public static final class Request implements Packet {
        public static final int HISTORY = 0;
        public static final int LAST = 1;
        public static final int SNAPSHOT = 2;
        public static final int INSPECT = 3;
        public static final int EFFECTS = 4;
        public static final int SEARCH = 5;
        public static final int DUMP = 6;
        public static final int RELOAD = 7;
        /** Run the server /whm with these arguments ("/whm server ..." on 1.13+; 1.12 sends a chat command instead) */
        public static final int COMMAND = 8;
        /** 1.3: a pack editor message chunk ({@code PackProto}); not cut to 256 characters */
        public static final int PACK = 9;

        public final int kind;
        public final String arg;

        public Request(int kind, String arg) {
            this.kind = kind;
            this.arg = arg == null ? "" : arg;
        }

        @Override
        public int type() {
            return REQUEST;
        }

        @Override
        public void write(WireOut o) {
            o.writeByte(kind);
            o.writeString(kind != PACK && arg.length() > 256 ? arg.substring(0, 256) : arg);
        }

        static Request read(WireIn in) {
            return new Request(in.readByte(), in.readString());
        }
    }

    // ================================================================== server -> client

    /** The server has the mod, plus its common display limits and filters */
    public static final class ServerInfo implements Packet {
        public int mask;
        public int summarySeconds;
        public List<String> ignored = new ArrayList<>();
        public List<String> renames = new ArrayList<>();
        public List<String> hints = new ArrayList<>();
        /** Protocol of the server (1 when it sent no extension) */
        public int protocol = 1;
        /** Write the protocol 2 extension (only for clients that said hello with protocol 2+) */
        public boolean extended;
        /** Config keys the modpack locked (lockedFeatures) */
        public List<String> locked = new ArrayList<>();
        /** renameMap / hintMap of the pack folder (lower priority than renames / hints) */
        public List<String> packRenames = new ArrayList<>();
        public List<String> packHints = new ArrayList<>();
        /** Protocol 3 part: write it (the client said hello with 3+) */
        public boolean v3;
        /** Pack templates: "output\nline\nline..." */
        public List<String> templates = new ArrayList<>();
        /** Pack deathMessages: "match key = template" */
        public List<String> deathMessages = new ArrayList<>();
        /** Script variables for the template editor: "name\ndescription" */
        public List<String> scriptVars = new ArrayList<>();

        @Override
        public int type() {
            return SERVER_INFO;
        }

        /** Entries per list in the single packet format (clients before 1.3) */
        public static final int OLD_LIST_LIMIT = 1000;
        /**
         * Largest single packet body: below the 1 MiB custom payload limit; the 1.7.10 loader lowers it (its S3F length is an
         * unsigned short). Entries that do not fit are left out.
         */
        public static int oldMaxBytes = 1000000;

        /** Lists still to write in {@link #write}: each gets an equal share of the room left */
        private int listsLeft;

        private void writeList(WireOut o, List<String> l) {
            int n = Math.min(l.size(), OLD_LIST_LIMIT);
            List<byte[]> keep = new ArrayList<>(n);
            int room = (oldMaxBytes - 64 - o.size()) / Math.max(1, listsLeft--) - 2;
            for (int i = 0; i < n; i++) {
                byte[] b = l.get(i).getBytes(java.nio.charset.StandardCharsets.UTF_8);
                room -= b.length + 5;
                if (room < 0) break;
                keep.add(b);
            }
            o.writeShort(keep.size());
            for (byte[] b : keep) {
                o.writeVarInt(b.length);
                o.writeBytes(b);
            }
        }

        private static List<String> readList(WireIn in) {
            int n = in.readShort();
            List<String> l = new ArrayList<>(Math.max(0, n));
            for (int i = 0; i < n; i++) l.add(in.readString());
            return l;
        }

        @Override
        public void write(WireOut o) {
            listsLeft = !extended ? 3 : !v3 ? 6 : 9;
            o.writeInt(mask);
            o.writeInt(summarySeconds);
            writeList(o, ignored);
            writeList(o, renames);
            writeList(o, hints);
            if (!extended) return;
            o.writeInt(PROTOCOL);
            writeList(o, locked);
            writeList(o, packRenames);
            writeList(o, packHints);
            if (!v3) return;
            writeList(o, templates);
            writeList(o, deathMessages);
            writeList(o, scriptVars);
        }

        /** Protocol 3: the whole info as {@link InfoPart} packets, no list limit */
        public List<Packet> parts() {
            byte[] b = full();
            int n = Math.max(1, (b.length + InfoPart.BYTES - 1) / InfoPart.BYTES);
            int seq = InfoPart.nextSeq();
            List<Packet> out = new ArrayList<>(n);
            for (int k = 0; k < n; k++) {
                int from = k * InfoPart.BYTES;
                out.add(new InfoPart(seq, k, n, java.util.Arrays.copyOfRange(b, from, Math.min(b.length, from + InfoPart.BYTES))));
            }
            return out;
        }

        /** Body of the protocol 3 parts: int list sizes, every entry */
        public byte[] full() {
            WireOut o = new WireOut();
            o.writeInt(mask);
            o.writeInt(summarySeconds);
            o.writeInt(PROTOCOL);
            for (List<String> l : lists(this)) {
                o.writeInt(l.size());
                for (String x : l) o.writeString(x);
            }
            return o.toByteArray();
        }

        private static List<List<String>> lists(ServerInfo i) {
            return java.util.Arrays.asList(i.ignored, i.renames, i.hints, i.locked, i.packRenames, i.packHints, i.templates, i.deathMessages, i.scriptVars);
        }

        public static ServerInfo readFull(byte[] b) {
            WireIn in = new WireIn(b);
            ServerInfo i = new ServerInfo();
            i.mask = in.readInt();
            i.summarySeconds = in.readInt();
            i.protocol = in.readInt();
            i.extended = true;
            i.v3 = true;
            for (List<String> l : lists(i)) {
                int n = in.readInt();
                // every entry takes at least one byte
                if (n < 0 || n > in.remaining()) throw new IllegalStateException("bad list size");
                for (int k = 0; k < n; k++) l.add(in.readString());
            }
            return i;
        }

        static Packet readAny(WireIn in) {
            int mask = in.readInt();
            if (mask == InfoPart.MARK) return InfoPart.readRest(in);
            return read(in, mask);
        }

        static ServerInfo read(WireIn in, int mask) {
            ServerInfo i = new ServerInfo();
            i.mask = mask;
            i.summarySeconds = in.readInt();
            i.ignored = readList(in);
            i.renames = readList(in);
            i.hints = readList(in);
            if (in.remaining() >= 4) {
                i.protocol = in.readInt();
                i.extended = true;
                i.locked = readList(in);
                i.packRenames = readList(in);
                i.packHints = readList(in);
                if (in.remaining() >= 4) {
                    i.v3 = true;
                    i.templates = readList(in);
                    i.deathMessages = readList(in);
                    if (in.remaining() > 0) i.scriptVars = readList(in);
                }
            }
            return i;
        }
    }

    /**
     * Protocol 3: one part of a {@link ServerInfo} (same packet type; the first int is {@link #MARK}, never a real mask).
     * The client applies the info once all parts of one sequence number have arrived ({@link InfoAssembler}).
     */
    public static final class InfoPart implements Packet {
        static final int MARK = 0x8057484D;
        /** Body bytes per part: far below the smallest payload limit (1.7.10 S3F length is an unsigned short) */
        public static final int BYTES = 24000;
        /** Largest assembled info */
        public static final int MAX_TOTAL = 32 * 1024 * 1024;

        private static int lastSeq;

        static synchronized int nextSeq() {
            lastSeq = (lastSeq + 1) & 0x3FFFFFFF;
            return lastSeq;
        }

        public final int seq;
        public final int index;
        public final int count;
        public final byte[] data;

        public InfoPart(int seq, int index, int count, byte[] data) {
            this.seq = seq;
            this.index = index;
            this.count = count;
            this.data = data;
        }

        @Override
        public int type() {
            return SERVER_INFO;
        }

        @Override
        public void write(WireOut o) {
            o.writeInt(MARK);
            o.writeInt(seq);
            o.writeInt(index);
            o.writeInt(count);
            o.writeVarInt(data.length);
            o.writeBytes(data);
        }

        static InfoPart readRest(WireIn in) {
            int seq = in.readInt();
            int index = in.readInt();
            int count = in.readInt();
            int n = in.readVarInt();
            if (n < 0 || n > BYTES || n > in.remaining()) throw new IllegalStateException("bad info part");
            byte[] b = new byte[n];
            for (int k = 0; k < n; k++) b[k] = (byte) in.readByte();
            return new InfoPart(seq, index, count, b);
        }
    }

    /** Collects {@link InfoPart}s (client thread); only a complete set of the newest sequence is returned */
    public static final class InfoAssembler {
        private int seq = -1;
        private byte[][] parts;
        private int have;
        private int size;

        public void reset() {
            seq = -1;
            parts = null;
        }

        /** @return the info when its last part arrived, else null; parts of an older sequence are dropped */
        public ServerInfo add(InfoPart p) {
            if (p.count < 1 || p.count > InfoPart.MAX_TOTAL / InfoPart.BYTES + 1 || p.index < 0 || p.index >= p.count) throw new IllegalStateException("bad info part");
            if (seq >= 0 && p.seq < seq) return null;
            if (p.seq != seq) {
                seq = p.seq;
                parts = new byte[p.count][];
                have = 0;
                size = 0;
            }
            if (parts == null) return null; // this sequence is done or was refused
            if (parts.length != p.count) {
                parts = null;
                throw new IllegalStateException("bad info part count");
            }
            if (parts[p.index] == null) {
                parts[p.index] = p.data;
                have++;
                size += p.data.length;
                if (size > InfoPart.MAX_TOTAL) {
                    parts = null;
                    throw new IllegalStateException("server info too large");
                }
            }
            if (have < parts.length) return null;
            byte[] all = new byte[size];
            int at = 0;
            for (byte[] b : parts) {
                System.arraycopy(b, 0, all, at, b.length);
                at += b.length;
            }
            parts = null;
            return ServerInfo.readFull(all);
        }
    }

    /** A history / death recap / snapshot (sent only on death, totem, heavy hit or a command) */
    public static final class Recap implements Packet {
        public static final int HISTORY = 0;
        public static final int LAST = 1;
        public static final int DEATH = 2;
        public static final int ALERT = 3;
        public static final int SNAPSHOT = 4;
        public static final int INSPECT_DEATH = 5;
        public static final int INSPECT_HISTORY = 6;
        /** Protocol 3: a custom death message of player "who" (snapshot: the killing blow, deathMessage = the vanilla message) */
        public static final int DEATH_MESSAGE = 7;


        public final int kind;
        public final String who;
        /** Snapshot number (1 = newest) */
        public final int index;
        /** Number of saved snapshots */
        public final int count;
        /** null = nothing */
        public final Snapshot snapshot;
        /** Protocol of the receiver: 2+ gets the full snapshot */
        public final int protocol;

        public Recap(int kind, String who, int index, int count, Snapshot snapshot) {
            this(kind, who, index, count, snapshot, 1);
        }

        public Recap(int kind, String who, int index, int count, Snapshot snapshot, int protocol) {
            this.protocol = protocol;
            this.kind = kind;
            this.who = who == null ? "" : who;
            this.index = index;
            this.count = count;
            this.snapshot = snapshot;
        }

        @Override
        public int type() {
            return RECAP;
        }

        @Override
        public void write(WireOut o) {
            long now = System.currentTimeMillis();
            boolean v2 = protocol >= 2;
            boolean v3 = protocol >= 3;
            o.writeByte(v3 ? kind | V2 | V3 : v2 ? kind | V2 : kind);
            o.writeString(who);
            o.writeInt(index);
            o.writeInt(count);
            o.writeBoolean(snapshot != null);
            if (snapshot != null) {
                if (v2) snapshot.write2(o, now, v3);
                else snapshot.write(o, now);
            }
        }

        /** Marks the protocol 2 body in the kind byte (old clients never receive it) */
        private static final int V2 = 0x40;
        /** Protocol 3 body: rule results of the records */
        private static final int V3 = 0x20;

        static Recap read(WireIn in) {
            long now = System.currentTimeMillis();
            int raw = in.readByte();
            boolean v2 = (raw & V2) != 0;
            boolean v3 = (raw & V3) != 0;
            int kind = raw & ~(V2 | V3);
            String who = in.readString();
            int index = in.readInt();
            int count = in.readInt();
            Snapshot s = in.readBoolean() ? (v2 ? Snapshot.read2(in, now, v3) : Snapshot.read(in, now)) : null;
            return new Recap(kind, who, index, count, s, v3 ? 3 : v2 ? 2 : 1);
        }
    }

    /** Search results, active effects or the damage catalog for a dump */
    public static final class Entries implements Packet {
        public static final int SEARCH = 0;
        public static final int EFFECTS = 1;
        public static final int DUMP = 2;

        public final int kind;
        public final String query;
        public final int total;
        public final List<Entry> entries;

        public Entries(int kind, String query, int total, List<Entry> entries) {
            this.kind = kind;
            this.query = query == null ? "" : query;
            this.total = total;
            this.entries = entries;
        }

        @Override
        public int type() {
            return ENTRIES;
        }

        @Override
        public void write(WireOut o) {
            o.writeByte(kind);
            o.writeString(query);
            o.writeInt(total);
            o.writeInt(entries.size());
            for (Entry e : entries) e.write(o);
        }

        static Entries read(WireIn in) {
            int kind = in.readByte();
            String q = in.readString();
            int total = in.readInt();
            int n = in.readInt();
            if (n < 0 || n > 100000) throw new IllegalStateException("bad entry count");
            List<Entry> l = new ArrayList<>(n);
            for (int i = 0; i < n; i++) l.add(Entry.read(in));
            return new Entries(kind, q, total, l);
        }
    }

    /** A short translated reply (errors, confirmations) */
    public static final class Message implements Packet {
        public final boolean error;
        public final String key;
        public final String[] args;

        public Message(boolean error, String key, String... args) {
            this.error = error;
            this.key = key;
            this.args = args;
        }

        @Override
        public int type() {
            return MESSAGE;
        }

        @Override
        public void write(WireOut o) {
            o.writeBoolean(error);
            o.writeString(key);
            o.writeByte(args.length);
            for (String a : args) o.writeString(a);
        }

        static Message read(WireIn in) {
            boolean err = in.readBoolean();
            String key = in.readString();
            int n = in.readByte();
            String[] a = new String[Math.max(0, n)];
            for (int i = 0; i < a.length; i++) a[i] = in.readString();
            return new Message(err, key, a);
        }
    }
}
