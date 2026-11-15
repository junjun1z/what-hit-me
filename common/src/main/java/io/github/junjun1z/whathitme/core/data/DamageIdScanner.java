package io.github.junjun1z.whathitme.core.data;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Finds the damage ids of mods without loading them: reads the class files of the mod jars (constant pool and method
 * code) and collects the string constants given to the damage source constructors, e.g. {@code new DamageSource("acid")}
 * or {@code super("acid")} in a subclass. For versions without a damage type registry (before 1.19.4). Plain Java, no
 * Minecraft classes; the loader gives the class names and the mod files.
 */
public final class DamageIdScanner {

    /** Setters of DamageSource on Forge 1.7.10 - 1.12.2 (MCP and SRG names) */
    public static final Map<String, Integer> OLD_FORGE_SETTERS;

    static {
        Map<String, Integer> m = new HashMap<>();
        put(m, DamageRecord.BYPASS_ARMOR, "setDamageBypassesArmor", "func_76348_h");
        put(m, DamageRecord.ABSOLUTE, "setDamageIsAbsolute", "func_151518_m");
        put(m, DamageRecord.BYPASS_CREATIVE, "setDamageAllowedInCreativeMode", "func_76359_i");
        put(m, DamageRecord.FIRE, "setFireDamage", "func_76361_j");
        put(m, DamageRecord.MAGIC, "setMagicDamage", "func_82726_p");
        put(m, DamageRecord.EXPLOSION, "setExplosion", "func_94540_d");
        put(m, DamageRecord.PROJECTILE, "setProjectile", "func_76349_b");
        put(m, 0, "setDifficultyScaled", "func_76351_m");
        OLD_FORGE_SETTERS = Collections.unmodifiableMap(m);
    }

    private static void put(Map<String, Integer> m, int flag, String... names) {
        for (String n : names) m.put(n, flag);
    }

    /** What the loader gives the scanner */
    public static final class Input {
        /** Runtime internal names (a/b/C) of the damage source class and its vanilla subclasses */
        public final List<String> baseClasses;
        /** Mod jars or directories with their mod id */
        public final Map<File, String> sources;
        /** Ids of the loaded mods ("modid.name" prefixes) */
        public final Set<String> modIds;
        /** Setter name to DamageRecord flag (setter calls right after the constructor) */
        public final Map<String, Integer> setters;

        public Input(List<String> baseClasses, Map<File, String> sources, Collection<String> modIds, Map<String, Integer> setters) {
            this.baseClasses = new ArrayList<>(baseClasses);
            this.sources = new LinkedHashMap<>(sources);
            this.modIds = modIds == null ? Collections.<String>emptySet() : new HashSet<>(modIds);
            this.setters = setters == null ? Collections.<String, Integer>emptyMap() : setters;
        }
    }

    /** One damage id found */
    public static final class Found {
        public final String id;
        public final String type;
        public final String modId;
        /** Class (a.b.C) that creates it */
        public final String owner;
        public final int flags;

        Found(String id, String type, String modId, String owner, int flags) {
            this.id = id;
            this.type = type;
            this.modId = modId;
            this.owner = owner;
            this.flags = flags;
        }
    }

    public static final class Result {
        public final List<Found> found = new ArrayList<>();
        public int sources;
        public int classes;
        public int walked;
        public int subclasses;
        public int errors;
        public long millis;
        public boolean cancelled;

        @Override
        public String toString() {
            return found.size() + " ids in " + millis + " ms (" + sources + " mod files, " + classes + " classes, " + walked + " read in full, "
                + subclasses + " mod damage classes, " + errors + " errors)";
        }
    }

    private static final int LIMIT = 10000;
    private static final int MAX_CLASS = 8 << 20;

    private DamageIdScanner() {}

    public static Result scan(Input in) {
        return scan(in, null);
    }

    /** Never throws; a broken jar or class is skipped. cancel is checked between classes. */
    public static Result scan(Input in, BooleanSupplier cancel) {
        long t0 = System.nanoTime();
        Scan s = new Scan(in, cancel);
        try {
            s.run();
        } catch (Throwable t) {
            s.res.errors++;
        }
        s.finish();
        s.res.millis = (System.nanoTime() - t0) / 1000000L;
        return s.res;
    }

    static long hash(byte[] b, int off, int len) {
        long h = 0xcbf29ce484222325L;
        for (int i = off; i < off + len; i++) {
            h ^= b[i] & 0xff;
            h *= 0x100000001b3L;
        }
        return h;
    }

    static long hash(String s) {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        return hash(b, 0, b.length);
    }

    private static final class Raw {
        final String type;
        final long ctor;
        final int mod;
        final String owner;
        int flags;

        Raw(String type, long ctor, int mod, String owner) {
            this.type = type;
            this.ctor = ctor;
            this.mod = mod;
            this.owner = owner;
        }
    }

    private static final class Scan {
        final Input in;
        final BooleanSupplier cancel;
        final Result res = new Result();
        final Set<Long> base = new HashSet<>();
        final Set<Long> known = new HashSet<>();
        /** known minus base: damage classes of mods */
        final Set<Long> modClasses = new HashSet<>();
        final List<String> mods = new ArrayList<>();
        long[] pThis = new long[4096];
        long[] pSuper = new long[4096];
        int[] pMod = new int[4096];
        int pN;
        final Map<Long, Integer> subMod = new HashMap<>();
        final Map<Long, Integer> classFlags = new HashMap<>();
        final Map<String, Raw> raws = new LinkedHashMap<>();
        final long objectHash = hash("java/lang/Object");
        int pass;
        byte[] buf = new byte[1 << 16];

        Scan(Input in, BooleanSupplier cancel) {
            this.in = in;
            this.cancel = cancel;
            for (String c : in.baseClasses) {
                if (c == null || c.isEmpty()) continue;
                long h = hash(c.replace('.', '/'));
                base.add(h);
                known.add(h);
            }
        }

        boolean cancelled() {
            if (cancel != null && cancel.getAsBoolean()) {
                res.cancelled = true;
                return true;
            }
            return false;
        }

        void run() {
            if (base.isEmpty()) return;
            int i = 0;
            for (Map.Entry<File, String> e : in.sources.entrySet()) {
                mods.add(e.getValue());
                res.sources++;
            }
            pass = 1;
            for (File f : in.sources.keySet()) {
                if (cancelled()) return;
                source(f, i++);
            }
            closure();
            if (modClasses.isEmpty()) return;
            pass = 2;
            i = 0;
            for (File f : in.sources.keySet()) {
                if (cancelled()) return;
                source(f, i++);
            }
        }

        /** Mod damage classes: every class whose super class chain reaches a base class */
        void closure() {
            boolean changed = true;
            while (changed) {
                changed = false;
                for (int i = 0; i < pN; i++) {
                    if (!known.contains(pThis[i]) && known.contains(pSuper[i])) {
                        known.add(pThis[i]);
                        modClasses.add(pThis[i]);
                        subMod.put(pThis[i], pMod[i]);
                        changed = true;
                    }
                }
            }
            res.subclasses = modClasses.size();
            pThis = pSuper = null;
            pMod = null;
        }

        void source(File f, int mod) {
            try {
                if (f.isDirectory()) dir(f, mod, 0);
                else if (f.isFile()) jar(f, mod);
            } catch (Throwable t) {
                res.errors++;
            }
        }

        void dir(File d, int mod, int depth) throws IOException {
            File[] list = d.listFiles();
            if (list == null || depth > 32) return;
            for (File f : list) {
                if (cancelled()) return;
                if (f.isDirectory()) dir(f, mod, depth + 1);
                else if (f.getName().endsWith(".class")) {
                    try (InputStream is = new java.io.FileInputStream(f)) {
                        klass(is, mod);
                    } catch (Throwable t) {
                        res.errors++;
                    }
                }
            }
        }

        void jar(File f, int mod) throws IOException {
            try (ZipFile z = new ZipFile(f)) {
                Enumeration<? extends ZipEntry> en = z.entries();
                while (en.hasMoreElements()) {
                    if (cancelled()) return;
                    ZipEntry e = en.nextElement();
                    String n = e.getName();
                    if (e.isDirectory()) continue;
                    try {
                        if (n.endsWith(".class")) {
                            try (InputStream is = z.getInputStream(e)) {
                                klass(is, mod);
                            }
                        } else if (n.endsWith(".jar")) {
                            try (ZipInputStream zin = new ZipInputStream(z.getInputStream(e))) {
                                nested(zin, mod);
                            }
                        }
                    } catch (Throwable t) {
                        res.errors++;
                    }
                }
            }
        }

        /** A jar inside a jar, streamed (one level) */
        void nested(ZipInputStream zin, int mod) throws IOException {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                if (cancelled()) return;
                if (!e.isDirectory() && e.getName().endsWith(".class")) klass(zin, mod);
            }
        }

        void klass(InputStream is, int mod) throws IOException {
            int n = 0;
            while (true) {
                if (n == buf.length) {
                    if (buf.length >= MAX_CLASS) return;
                    buf = Arrays.copyOf(buf, buf.length * 2);
                }
                int r = is.read(buf, n, buf.length - n);
                if (r < 0) break;
                n += r;
            }
            if (pass == 1) res.classes++;
            try {
                new ClassFile(this, buf, n, mod).read();
            } catch (RuntimeException ex) {
                res.errors++;
            }
        }

        void pair(long t, long s, int mod) {
            if (pN == pThis.length) {
                pThis = Arrays.copyOf(pThis, pN * 2);
                pSuper = Arrays.copyOf(pSuper, pN * 2);
                pMod = Arrays.copyOf(pMod, pN * 2);
            }
            pThis[pN] = t;
            pSuper[pN] = s;
            pMod[pN] = mod;
            pN++;
        }

        Raw record(String type, long ctor, int mod, String owner) {
            if (type == null || type.isEmpty() || type.length() > 128) return null;
            for (int i = 0; i < type.length(); i++) {
                char c = type.charAt(i);
                if (c <= ' ' || c == 0x7f) return null;
            }
            String key = type + '\u0000' + ctor + '\u0000' + mod;
            Raw r = raws.get(key);
            if (r == null) {
                if (raws.size() >= LIMIT) return null;
                r = new Raw(type, ctor, mod, owner);
                raws.put(key, r);
            }
            return r;
        }

        void finish() {
            Map<String, Found> out = new LinkedHashMap<>();
            for (Raw r : raws.values()) {
                // melee types become "mob:<attacker>" ids in play
                if ("mob".equals(r.type) || "player".equals(r.type)) continue;
                String mod = modOf(r);
                String id = r.type.indexOf(':') > 0 ? r.type : mod + ":" + r.type;
                int flags = r.flags;
                Integer cf = classFlags.get(r.ctor);
                if (cf != null) flags |= cf;
                Found old = out.get(id);
                if (old != null) flags |= old.flags;
                out.put(id, new Found(id, r.type, mod, old != null ? old.owner : r.owner, flags & DamageRecord.PROPERTY_MASK));
            }
            res.found.addAll(out.values());
        }

        /** Same order as TypeModResolver: namespace, vanilla name, mod subclass, "modid." prefix, the jar */
        String modOf(Raw r) {
            int c = r.type.indexOf(':');
            if (c > 0) return r.type.substring(0, c);
            if (base.contains(r.ctor) && TypeModResolver.VANILLA_TYPES.contains(r.type)) return "minecraft";
            Integer sm = subMod.get(r.ctor);
            if (sm != null && sm >= 0 && sm < mods.size()) return mods.get(sm);
            for (String sep : new String[] {".", "_"}) {
                int i = r.type.indexOf(sep);
                if (i > 0) {
                    String p = r.type.substring(0, i).toLowerCase(Locale.ROOT);
                    if (in.modIds.contains(p)) return p;
                }
            }
            return r.mod >= 0 && r.mod < mods.size() ? mods.get(r.mod) : "unknown";
        }
    }

    /** Minimal class file reader: constant pool, super class, method code */
    private static final class ClassFile {
        private static final int ALOAD_0 = 0x2a;
        private static final int DUP = 0x59;
        private static final int LDC = 0x12;
        private static final int LDC_W = 0x13;
        private static final int INVOKEVIRTUAL = 0xb6;
        private static final int INVOKESPECIAL = 0xb7;
        private static final int INVOKESTATIC = 0xb8;
        private static final int INVOKEINTERFACE = 0xb9;
        private static final int INVOKEDYNAMIC = 0xba;
        private static final int NEW = 0xbb;
        private static final int BAD = 9;
        private static final long STRING = hash("java/lang/String");
        private static final long BUILDER = hash("java/lang/StringBuilder");
        private static final long BUFFER = hash("java/lang/StringBuffer");

        final Scan s;
        final byte[] b;
        final int len;
        final int mod;
        int[] off;
        byte[] tag;
        long thisHash;
        long superHash;
        String thisName;

        ClassFile(Scan s, byte[] b, int len, int mod) {
            this.s = s;
            this.b = b;
            this.len = len;
            this.mod = mod;
        }

        int u1(int p) {
            if (p >= len) throw new IndexOutOfBoundsException();
            return b[p] & 0xff;
        }

        int u2(int p) {
            return (u1(p) << 8) | u1(p + 1);
        }

        int s4(int p) {
            return (u2(p) << 16) | u2(p + 2);
        }

        long utfHash(int idx) {
            int p = off[idx];
            return hash(b, p + 2, u2(p));
        }

        String utf(int idx) {
            int p = off[idx];
            int n = u2(p);
            p += 2;
            int end = p + n;
            if (end > len) throw new IndexOutOfBoundsException();
            char[] out = new char[n];
            int k = 0;
            while (p < end) {
                int c = b[p] & 0xff;
                if (c < 0x80) {
                    out[k++] = (char) c;
                    p++;
                } else if ((c & 0xe0) == 0xc0) {
                    out[k++] = (char) (((c & 0x1f) << 6) | (u1(p + 1) & 0x3f));
                    p += 2;
                } else {
                    out[k++] = (char) (((c & 0x0f) << 12) | ((u1(p + 1) & 0x3f) << 6) | (u1(p + 2) & 0x3f));
                    p += 3;
                }
            }
            return new String(out, 0, k);
        }

        /** Hash of the name of a CONSTANT_Class */
        long classHash(int idx) {
            if (idx <= 0 || idx >= tag.length || tag[idx] != 7) return 0;
            return utfHash(u2(off[idx]));
        }

        void read() {
            if (len < 10 || s4(0) != 0xCAFEBABE) return;
            int count = u2(8);
            off = new int[count];
            tag = new byte[count];
            int p = 10;
            for (int i = 1; i < count; i++) {
                int t = u1(p);
                tag[i] = (byte) t;
                off[i] = p + 1;
                switch (t) {
                    case 1:
                        p += 3 + u2(p + 1);
                        break;
                    case 3:
                    case 4:
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    case 17:
                    case 18:
                        p += 5;
                        break;
                    case 5:
                    case 6:
                        p += 9;
                        i++;
                        break;
                    case 7:
                    case 8:
                    case 16:
                    case 19:
                    case 20:
                        p += 3;
                        break;
                    case 15:
                        p += 4;
                        break;
                    default:
                        return;
                }
            }
            int thisIdx = u2(p + 2);
            int superIdx = u2(p + 4);
            thisHash = classHash(thisIdx);
            superHash = classHash(superIdx);
            if (s.pass == 1 && superHash != 0 && superHash != s.objectHash) s.pair(thisHash, superHash, mod);
            // pre-filter: only classes that name a damage class are read in full
            Set<Long> want = s.pass == 1 ? s.base : s.modClasses;
            boolean hit = false;
            for (int i = 1; i < tag.length && !hit; i++) {
                if (tag[i] == 7 && want.contains(classHash(i))) hit = true;
            }
            if (!hit) return;
            s.res.walked++;
            thisName = utf(u2(off[thisIdx])).replace('/', '.');
            p += 6;
            int ifaces = u2(p);
            p += 2 + ifaces * 2;
            p = skipMembers(p);
            int methods = u2(p);
            p += 2;
            boolean superKnown = s.known.contains(superHash);
            for (int m = 0; m < methods; m++) {
                int name = u2(p + 2);
                int attrs = u2(p + 6);
                p += 8;
                for (int a = 0; a < attrs; a++) {
                    int an = u2(p);
                    int alen = s4(p + 2);
                    if (tag[an] == 1 && "Code".equals(utf(an))) {
                        int codeLen = s4(p + 10);
                        int start = p + 14;
                        if (start + codeLen <= len) {
                            try {
                                walk(start, start + codeLen, "<init>".equals(utf(name)), superKnown);
                            } catch (RuntimeException ignored) {
                                // odd code: skip this method
                            }
                        }
                    }
                    p += 6 + alen;
                }
            }
        }

        int skipMembers(int p) {
            int n = u2(p);
            p += 2;
            for (int i = 0; i < n; i++) {
                int attrs = u2(p + 6);
                p += 8;
                for (int a = 0; a < attrs; a++) p += 6 + s4(p + 2);
            }
            return p;
        }

        String ldcString(int idx) {
            if (idx <= 0 || idx >= tag.length || tag[idx] != 8) return null;
            return utf(u2(off[idx]));
        }

        void walk(int start, int end, boolean ctor, boolean superKnown) {
            final int max = 64;
            long[] fClass = new long[max];
            boolean[] fDs = new boolean[max];
            boolean[] fPseudo = new boolean[max];
            int[] fPos = new int[max];
            String[] fStr = new String[max];
            int[] fArg = new int[max];
            int fN = 0;
            Raw armed = null;
            int prev = -1;
            int pc = start;
            if (ctor && superKnown && u1(pc) == ALOAD_0) {
                // super("id") in a constructor of a damage class
                fClass[0] = superHash;
                fDs[0] = true;
                fPseudo[0] = true;
                fPos[0] = 1;
                fArg[0] = 0;
                fN = 1;
                prev = ALOAD_0;
                pc++;
            }
            while (pc < end) {
                int op = u1(pc);
                int size = size(op, pc, start);
                if (size <= 0) return;
                boolean keepArmed = false;
                if (fN > 0 && fDs[fN - 1]) {
                    int t = fN - 1;
                    if (fPos[t] == 0) {
                        fPos[t] = op == DUP ? 1 : BAD;
                    } else if (fPos[t] == 1) {
                        // simple pushes (outer instance of an inner class, null, numbers) may come before the string
                        String str = op == LDC ? ldcString(u1(pc + 1)) : op == LDC_W ? ldcString(u2(pc + 1)) : null;
                        if (str != null) {
                            fStr[t] = str;
                            fPos[t] = 2;
                        } else if (op >= 0x01 && op <= 0x2d && fArg[t] < 4) {
                            fArg[t]++;
                        } else {
                            fPos[t] = BAD;
                        }
                    } else if (fPos[t] == 2) {
                        if (op == INVOKEDYNAMIC) fStr[t] = null;
                        else if (op == INVOKEVIRTUAL || op == INVOKESTATIC || op == INVOKESPECIAL || op == INVOKEINTERFACE) {
                            long owner = classHash(refClass(u2(pc + 1)));
                            if (owner == STRING || owner == BUILDER || owner == BUFFER) fStr[t] = null;
                        }
                    }
                }
                if (op == NEW) {
                    if (fN == max) return;
                    long h = classHash(u2(pc + 1));
                    fClass[fN] = h;
                    fDs[fN] = s.known.contains(h);
                    fPseudo[fN] = false;
                    fPos[fN] = 0;
                    fStr[fN] = null;
                    fArg[fN] = 0;
                    fN++;
                } else if (op == INVOKESPECIAL) {
                    int ref = u2(pc + 1);
                    if ("<init>".equals(refName(ref))) {
                        long owner = classHash(refClass(ref));
                        int k = fN - 1;
                        while (k >= 0 && !(fClass[k] == owner || (fPseudo[k] && owner == thisHash))) k--;
                        if (k >= 0) {
                            if (fDs[k] && fPos[k] == 2 && fStr[k] != null && k == fN - 1 && isString(refDesc(ref), fArg[k])) {
                                armed = s.record(fStr[k], fPseudo[k] ? thisHash : fClass[k], mod, thisName);
                                keepArmed = armed != null;
                            }
                            fN = k;
                        }
                    }
                } else if (op == INVOKEVIRTUAL && !s.in.setters.isEmpty()) {
                    int ref = u2(pc + 1);
                    Integer f = s.in.setters.get(refName(ref));
                    if (f != null && refDesc(ref).startsWith("()L")) {
                        if (armed != null) {
                            armed.flags |= f;
                            keepArmed = true;
                        } else if (ctor && superKnown && prev == ALOAD_0) {
                            Integer old = s.classFlags.get(thisHash);
                            s.classFlags.put(thisHash, (old == null ? 0 : old) | f);
                        }
                    }
                }
                if (!keepArmed) armed = null;
                prev = op;
                pc += size;
            }
        }

        /** True when parameter idx of a method descriptor is a String */
        static boolean isString(String desc, int idx) {
            int p = 1;
            for (int i = 0; p < desc.length() && desc.charAt(p) != ')'; i++) {
                int q = p;
                while (desc.charAt(q) == '[') q++;
                int e = desc.charAt(q) == 'L' ? desc.indexOf(';', q) + 1 : q + 1;
                if (e <= 0) return false;
                if (i == idx) return desc.startsWith("Ljava/lang/String;", p) && q == p;
                p = e;
            }
            return false;
        }

        int refClass(int ref) {
            if (ref <= 0 || ref >= tag.length || (tag[ref] != 9 && tag[ref] != 10 && tag[ref] != 11)) return 0;
            return u2(off[ref]);
        }

        int nat(int ref) {
            if (ref <= 0 || ref >= tag.length || (tag[ref] != 9 && tag[ref] != 10 && tag[ref] != 11)) return 0;
            int n = u2(off[ref] + 2);
            return n > 0 && n < tag.length && tag[n] == 12 ? n : 0;
        }

        String refName(int ref) {
            int n = nat(ref);
            return n == 0 ? "" : utf(u2(off[n]));
        }

        String refDesc(int ref) {
            int n = nat(ref);
            return n == 0 ? "" : utf(u2(off[n] + 2));
        }

        /** Length of the instruction at pc, 0 when unknown */
        int size(int op, int pc, int start) {
            if (op <= 0x0f) return 1;
            if (op == 0x10) return 2;
            if (op == 0x11) return 3;
            if (op == LDC) return 2;
            if (op <= 0x14) return 3;
            if (op <= 0x19) return 2;
            if (op <= 0x35) return 1;
            if (op <= 0x3a) return 2;
            if (op <= 0x83) return 1;
            if (op == 0x84) return 3;
            if (op <= 0x98) return 1;
            if (op <= 0xa8) return 3;
            if (op == 0xa9) return 2;
            if (op == 0xaa) {
                int p = pc + 1;
                p += (4 - ((p - start) & 3)) & 3;
                int lo = s4(p + 4);
                int hi = s4(p + 8);
                long n = (long) hi - lo + 1;
                if (n < 0 || n > 65536) return 0;
                return (int) (p + 12 + n * 4 - pc);
            }
            if (op == 0xab) {
                int p = pc + 1;
                p += (4 - ((p - start) & 3)) & 3;
                int n = s4(p + 4);
                if (n < 0 || n > 65536) return 0;
                return p + 8 + n * 8 - pc;
            }
            if (op <= 0xb1) return 1;
            if (op <= 0xb8) return 3;
            if (op <= 0xba) return 5;
            if (op == NEW) return 3;
            if (op == 0xbc) return 2;
            if (op == 0xbd) return 3;
            if (op <= 0xbf) return 1;
            if (op <= 0xc1) return 3;
            if (op <= 0xc3) return 1;
            if (op == 0xc4) return u1(pc + 1) == 0x84 ? 6 : 4;
            if (op == 0xc5) return 4;
            if (op <= 0xc7) return 3;
            if (op <= 0xc9) return 5;
            return 0;
        }
    }
}
