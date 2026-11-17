package io.github.junjun1z.whathitme.integration.crafttweaker;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import org.openzen.zencode.java.ZenCodeType;

/** (event as mods.whathitme.DeathEvent) => { ... } */
@FunctionalInterface
@ZenRegister
@ZenCodeType.Name("mods.whathitme.DeathHandler")
public interface DeathHandler {

    @ZenCodeType.Method
    void handle(CTDeathEvent event);
}
