package io.github.junjun1z.whathitme.core;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

import io.github.junjun1z.whathitme.core.text.Lang;

/**
 * Services of the running game, set once by the loader entry point before anything else runs. Everything here is
 * loader / version specific; the core only talks to Minecraft through this class and the port interfaces
 * ({@code server.ServerPort}, {@code client.ClientPort}, ...).
 */
public final class Platform {

    /** Mod names, mod ids and which mod a class comes from */
    public interface Mods {
        /** Display name of a mod id ("Minecraft" for minecraft, the id itself when unknown) */
        String modName(String modId);

        /** Ids of every loaded mod (for @modid tab completion) */
        Collection<String> modIds();

        /** Version of a loaded mod ("" = unknown); part of the catalog cache hash */
        default String version(String modId) {
            return "";
        }

        default boolean isLoaded(String modId) {
            return modIds().contains(modId);
        }
    }

    public interface Log {
        void info(String msg);

        void warn(String msg, Throwable t);
    }

    public static final String MODID = "whathitme";

    public static volatile Mods mods = new Mods() {
        @Override
        public String modName(String modId) {
            return "minecraft".equals(modId) ? "Minecraft" : modId;
        }

        @Override
        public Collection<String> modIds() {
            return Collections.singletonList("minecraft");
        }
    };

    public static volatile Log log = new Log() {
        @Override
        public void info(String msg) {
            System.out.println("[WhatHitMe] " + msg);
        }

        @Override
        public void warn(String msg, Throwable t) {
            System.out.println("[WhatHitMe] " + msg + (t == null ? "" : ": " + t));
        }
    };

    /** Language of the local client (null on a dedicated server) */
    public static volatile Lang clientLang;
    /** Translations the logical server knows besides ours (vanilla / mod English), may be null */
    public static volatile Lang serverLang;

    /** config/ directory of the game or server */
    public static volatile File configDir = new File("config");

    private Platform() {}

    public static String modName(String modId) {
        if (modId == null || modId.isEmpty()) return "";
        if ("minecraft".equals(modId)) return "Minecraft";
        try {
            String n = mods.modName(modId);
            return n == null || n.isEmpty() ? modId : n;
        } catch (Throwable t) {
            return modId;
        }
    }

    /** config/whathitme/ */
    public static File whmConfigDir() {
        return new File(configDir, MODID);
    }
}
