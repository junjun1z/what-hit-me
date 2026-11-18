package io.github.junjun1z.whathitme.integration.crafttweaker;

import com.blamejared.crafttweaker.api.annotations.ZenRegister;
import org.openzen.zencode.java.ZenCodeType;

/** (event as mods.whathitme.AlertEvent) => { ... } */
@FunctionalInterface
@ZenRegister
@ZenCodeType.Name("mods.whathitme.AlertHandler")
public interface AlertHandler {

    @ZenCodeType.Method
    void handle(CTAlertEvent event);
}
