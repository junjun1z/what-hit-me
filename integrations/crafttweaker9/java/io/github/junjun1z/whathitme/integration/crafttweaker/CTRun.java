package io.github.junjun1z.whathitme.integration.crafttweaker;

/** Script runs of CraftTweaker 9+: the plugin tells when a run has finished */
final class CTRun {

    private static volatile boolean ended;

    private CTRun() {}

    static void ended() {
        ended = true;
    }

    /** true once, for the first registration after a finished run */
    static synchronized boolean newRun() {
        if (!ended) return false;
        ended = false;
        return true;
    }
}
