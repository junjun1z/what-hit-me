package io.github.junjun1z.whathitme.integration.crafttweaker;

import com.blamejared.crafttweaker.api.plugin.CraftTweakerPlugin;
import com.blamejared.crafttweaker.api.plugin.ICraftTweakerPlugin;
import com.blamejared.crafttweaker.api.plugin.IListenerRegistrationHandler;
import com.blamejared.crafttweaker.api.zencode.scriptrun.ScriptRunConfiguration;

/**
 * CraftTweaker plugin (found by CraftTweaker's annotation scan only): marks the end of each script run, so the next run
 * replaces what the previous one registered.
 */
@CraftTweakerPlugin("whathitme:main")
public class WhatHitMeCTPlugin implements ICraftTweakerPlugin {

    @Override
    public void registerListeners(IListenerRegistrationHandler handler) {
        handler.onExecuteRun(cfg -> {
            if (cfg.runKind() == ScriptRunConfiguration.RunKind.EXECUTE && "crafttweaker".equals(cfg.loader().name())) CTRun.ended();
        });
    }
}
