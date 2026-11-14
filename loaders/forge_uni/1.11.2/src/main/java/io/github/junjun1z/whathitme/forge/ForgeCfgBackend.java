package io.github.junjun1z.whathitme.forge;

import java.io.File;

import io.github.junjun1z.whathitme.core.config.ConfigBackend;
import io.github.junjun1z.whathitme.core.config.Setting;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

/** Forge 1.11's Configuration as the storage of a core config file (keeps Forge's in-game config screen working). */
public final class ForgeCfgBackend implements ConfigBackend {

    public final Configuration cfg;

    public ForgeCfgBackend(File file) {
        cfg = new Configuration(file);
    }

    @Override
    public void load() {
        cfg.load();
    }

    @Override
    public void save() {
        cfg.save();
    }

    @Override
    public boolean hasChanged() {
        return cfg.hasChanged();
    }

    @Override
    public String get(Setting s) {
        switch (s.type) {
            case BOOLEAN:
                return String.valueOf(cfg.getBoolean(s.key, s.category, Boolean.parseBoolean(s.def), s.comment));
            case INT:
                return String.valueOf(cfg.getInt(s.key, s.category, Integer.parseInt(s.def), (int) s.min, (int) s.max, s.comment));
            case FLOAT:
                return String.valueOf(cfg.getFloat(s.key, s.category, Float.parseFloat(s.def), (float) s.min, (float) s.max, s.comment));
            case STRING:
                return s.valid != null ? cfg.getString(s.key, s.category, s.def, s.comment, s.valid) : cfg.getString(s.key, s.category, s.def, s.comment);
            default:
                return String.join(", ", getList(s));
        }
    }

    @Override
    public String[] getList(Setting s) {
        return cfg.getStringList(s.key, s.category, new String[0], s.comment);
    }

    private Property prop(Setting s) {
        if (s.isList()) return cfg.get(s.category, s.key, new String[0]);
        if (s.type == Setting.Type.BOOLEAN) return cfg.get(s.category, s.key, Boolean.parseBoolean(s.def));
        if (s.type == Setting.Type.INT) return cfg.get(s.category, s.key, Integer.parseInt(s.def));
        return cfg.get(s.category, s.key, s.def);
    }

    @Override
    public void set(Setting s, String value) {
        prop(s).set(value);
    }

    @Override
    public void setList(Setting s, String[] values) {
        prop(s).set(values);
    }

    @Override
    public void categoryComment(String category, String comment) {
        cfg.setCategoryComment(category, comment);
    }

    @Override
    public void categoryLangKey(String category, String key) {
        cfg.setCategoryLanguageKey(category, key);
    }
}
