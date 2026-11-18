package io.github.junjun1z.whathitme.integration.crafttweaker;

import com.blamejared.crafttweaker.api.annotations.ZenRegister;
import org.openzen.zencode.java.ZenCodeType;

/** A register hook: () => { ... } */
@FunctionalInterface
@ZenRegister
@ZenCodeType.Name("mods.whathitme.RegisterHandler")
public interface RegisterHandler {

    @ZenCodeType.Method
    void register();
}
