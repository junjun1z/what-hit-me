package io.github.junjun1z.whathitme.integration.crafttweaker;

import com.blamejared.crafttweaker.api.annotations.ZenRegister;
import org.openzen.zencode.java.ZenCodeType;

/** (event as mods.whathitme.RespawnEvent) => { ... } */
@FunctionalInterface
@ZenRegister
@ZenCodeType.Name("mods.whathitme.RespawnHandler")
public interface RespawnHandler {

    @ZenCodeType.Method
    void handle(CTRespawnEvent event);
}
