package io.github.junjun1z.whathitme.core.config;

/**
 * Storage of one configuration file. {@link CfgFile} writes the Forge 1.12 ".cfg" format on every version; 1.12.2 uses
 * Forge's own Configuration class (so its in-game config screen keeps working).
 */
public interface ConfigBackend {

    void load();

    void save();

    boolean hasChanged();

    /** Creates the entry (default, comment, range) when missing, fixes invalid values, returns the current value */
    String get(Setting s);

    String[] getList(Setting s);

    void set(Setting s, String value);

    void setList(Setting s, String[] values);

    void categoryComment(String category, String comment);

    /** Translation key of a category for the config screen */
    default void categoryLangKey(String category, String key) {}
}
