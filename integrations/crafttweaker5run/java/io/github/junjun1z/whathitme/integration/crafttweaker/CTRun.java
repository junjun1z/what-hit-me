package io.github.junjun1z.whathitme.integration.crafttweaker;

import com.blamejared.crafttweaker.api.CraftTweakerAPI;

/** Script runs of CraftTweaker 5 / 6 (no plugin API, no run object): every run starts a new scripting engine */
final class CTRun {

    private static Object last;

    private CTRun() {}

    /** true once, for the first registration of a run that differs from the previous one */
    static synchronized boolean newRun() {
        Object run;
        try {
            run = CraftTweakerAPI.getEngine();
        } catch (RuntimeException e) {
            return false;
        }
        if (run == null || run == last) return false;
        last = run;
        return true;
    }
}
