package io.github.junjun1z.whathitme.core.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

/**
 * Settings of players without the mod (respawn summary off, personal ignore list), stored per world in
 * &lt;world&gt;/whathitme/players/&lt;uuid&gt;.properties. For loaders without per-player persistent data (Fabric); Forge keeps
 * them in the player's persistent NBT instead. Read and written only when a command changes or needs them.
 */
public final class FilePlayerPrefs {

    private final File file;

    public FilePlayerPrefs(File worldDir, UUID player) {
        this.file = new File(new File(new File(worldDir, "whathitme"), "players"), player + ".properties");
    }

    private Properties load() {
        Properties p = new Properties();
        if (file.isFile()) {
            try (InputStream in = new FileInputStream(file)) {
                p.load(in);
            } catch (Exception ignored) {
                // defaults
            }
        }
        return p;
    }

    private void save(Properties p) {
        try {
            file.getParentFile().mkdirs();
            try (OutputStream out = new FileOutputStream(file)) {
                p.store(out, "What Hit Me? settings of this player (players without the mod)");
            }
        } catch (Exception ignored) {
            // not saved
        }
    }

    public boolean chatOff() {
        return Boolean.parseBoolean(load().getProperty("chatOff", "false"));
    }

    /** @return true when the summary is on now */
    public boolean toggleChat() {
        Properties p = load();
        boolean off = !Boolean.parseBoolean(p.getProperty("chatOff", "false"));
        p.setProperty("chatOff", String.valueOf(off));
        save(p);
        return !off;
    }

    public List<String> ignored() {
        String v = load().getProperty("ignored", "");
        List<String> out = new ArrayList<>();
        for (String s : v.split("\n")) if (!s.trim().isEmpty()) out.add(s.trim());
        return out;
    }

    /** @return true when the id is ignored now */
    public boolean toggleIgnore(String id) {
        Properties p = load();
        List<String> cur = new ArrayList<>(Arrays.asList(p.getProperty("ignored", "").split("\n")));
        cur.removeIf(s -> s.trim().isEmpty());
        boolean now = !cur.remove(id);
        if (now) cur.add(id);
        p.setProperty("ignored", String.join("\n", cur));
        save(p);
        return now;
    }
}
