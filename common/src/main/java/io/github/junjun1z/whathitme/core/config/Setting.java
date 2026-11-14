package io.github.junjun1z.whathitme.core.config;

/** One configuration key: category, name, type, default, range / valid values and comment. Immutable. */
public final class Setting {

    public enum Type { BOOLEAN, INT, FLOAT, STRING, LIST }

    public final String category;
    public final String key;
    public final Type type;
    /** Default as text (lists: "") */
    public final String def;
    public final double min;
    public final double max;
    /** Allowed values of a STRING setting (null = any) */
    public final String[] valid;
    public final String comment;

    private Setting(String category, String key, Type type, String def, double min, double max, String[] valid, String comment) {
        this.category = category;
        this.key = key;
        this.type = type;
        this.def = def;
        this.min = min;
        this.max = max;
        this.valid = valid;
        this.comment = comment;
    }

    public static Setting bool(String cat, String key, boolean def, String comment) {
        return new Setting(cat, key, Type.BOOLEAN, String.valueOf(def), 0, 0, null, comment);
    }

    public static Setting integer(String cat, String key, int def, int min, int max, String comment) {
        return new Setting(cat, key, Type.INT, String.valueOf(def), min, max, null, comment);
    }

    public static Setting decimal(String cat, String key, float def, float min, float max, String comment) {
        return new Setting(cat, key, Type.FLOAT, Float.toString(def), min, max, null, comment);
    }

    public static Setting string(String cat, String key, String def, String[] valid, String comment) {
        return new Setting(cat, key, Type.STRING, def, 0, 0, valid, comment);
    }

    public static Setting list(String cat, String key, String comment) {
        return new Setting(cat, key, Type.LIST, "", 0, 0, null, comment);
    }

    public boolean isList() {
        return type == Type.LIST;
    }

    /** Range text as Forge prints it in the file ("[range: 5 ~ 500, default: 50]") */
    public String rangeText() {
        switch (type) {
            case INT:
                return "[range: " + (int) min + " ~ " + (int) max + ", default: " + def + "]";
            case FLOAT:
                return "[range: " + (float) min + " ~ " + (float) max + ", default: " + def + "]";
            case STRING:
                if (valid != null && valid.length > 0) return "[default: " + def + "]";
                return "[default: " + def + "]";
            case LIST:
                return "[default: ]";
            default:
                return "[default: " + def + "]";
        }
    }
}
