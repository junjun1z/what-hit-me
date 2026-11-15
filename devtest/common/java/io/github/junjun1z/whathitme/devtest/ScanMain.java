package io.github.junjun1z.whathitme.devtest;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import io.github.junjun1z.whathitme.core.data.DamageIdScanner;

/**
 * Runs the damage id scanner outside the game: java -cp whathitme-selftest.jar io.github.junjun1z.whathitme.devtest.ScanMain
 * &lt;mods dir&gt; [base classes, comma separated]. Mod ids come from mcmod.info (else the file name).
 */
public final class ScanMain {

    private static final Pattern MODID = Pattern.compile("\"modid\"\\s*:\\s*\"([^\"]+)\"");

    private ScanMain() {}

    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        List<String> base = Arrays.asList((args.length > 1 ? args[1]
            : "net/minecraft/util/DamageSource,net/minecraft/util/EntityDamageSource,net/minecraft/util/EntityDamageSourceIndirect").split(","));
        Map<File, String> files = new LinkedHashMap<>();
        List<File> all = new ArrayList<>();
        collect(dir, all);
        for (File f : all) files.put(f, modId(f));
        DamageIdScanner.Input in = new DamageIdScanner.Input(base, files, files.values(), DamageIdScanner.OLD_FORGE_SETTERS);
        long t0 = System.nanoTime();
        DamageIdScanner.Result r = DamageIdScanner.scan(in);
        long ms = (System.nanoTime() - t0) / 1000000L;
        Runtime rt = Runtime.getRuntime();
        for (DamageIdScanner.Found f : r.found) System.out.println(f.id + "\t" + f.flags + "\t" + f.owner);
        System.out.println("RESULT " + r + ", wall " + ms + " ms, heap used " + (rt.totalMemory() - rt.freeMemory()) / (1 << 20) + " MB");
    }

    private static void collect(File d, List<File> out) {
        File[] list = d.listFiles();
        if (list == null) return;
        Arrays.sort(list);
        for (File f : list) {
            if (f.isDirectory()) collect(f, out);
            else if (f.getName().endsWith(".jar") || f.getName().endsWith(".zip")) out.add(f);
        }
    }

    private static String modId(File f) {
        try (ZipFile z = new ZipFile(f)) {
            ZipEntry e = z.getEntry("mcmod.info");
            if (e != null) {
                try (InputStream is = z.getInputStream(e)) {
                    byte[] b = new byte[(int) Math.min(e.getSize() < 0 ? 65536 : e.getSize(), 1 << 20)];
                    int n = 0;
                    int r;
                    while (n < b.length && (r = is.read(b, n, b.length - n)) > 0) n += r;
                    Matcher m = MODID.matcher(new String(b, 0, n, StandardCharsets.UTF_8));
                    if (m.find()) return m.group(1);
                }
            }
        } catch (Exception ignored) {
            // not a zip
        }
        String n = f.getName().replaceAll("\\.(jar|zip)$", "");
        return n.replaceAll("[-_ ]?[0-9].*$", "").toLowerCase(java.util.Locale.ROOT);
    }
}
