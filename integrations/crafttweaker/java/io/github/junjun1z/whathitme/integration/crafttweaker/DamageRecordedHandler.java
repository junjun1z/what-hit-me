package io.github.junjun1z.whathitme.integration.crafttweaker;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import org.openzen.zencode.java.ZenCodeType;

/** (event as mods.whathitme.DamageRecordedEvent) => { ... } */
@FunctionalInterface
@ZenRegister
@ZenCodeType.Name("mods.whathitme.DamageRecordedHandler")
public interface DamageRecordedHandler {

    @ZenCodeType.Method
    void handle(CTDamageRecordedEvent event);
}
