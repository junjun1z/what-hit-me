package io.github.junjun1z.whathitme.integration.crafttweaker;

import com.blamejared.crafttweaker.api.CraftTweakerAPI;

/** Script runs of CraftTweaker 8 (no plugin API): every run is a new ScriptRun object */
final class CTRun {

    private static Object last;

    private CTRun() {}

    /** true once, for the first registration of a run that differs from the previous one */
    static synchronized boolean newRun() {
        Object run;
        try {
            run = CraftTweakerAPI.getCurrentRun();
        } catch (RuntimeException e) {
            return false; // no run yet
        }
        if (run == null || run == last) return false;
        last = run;
        return true;
    }
}
