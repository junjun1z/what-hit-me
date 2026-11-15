package io.github.junjun1z.whathitme.fabric;

import java.util.function.Function;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.mc.ConfigScreen115;
import io.github.prospector.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;

/** Optional Mod Menu entrypoint (Mod Menu 1.8 - 1.10 API): the config button of the mods list. Only loaded by Mod Menu. */
public final class WhmModMenu implements ModMenuApi {

    @Override
    public String getModId() {
        return Platform.MODID;
    }

    @Override
    public Function<Screen, ? extends Screen> getConfigScreenFactory() {
        return ConfigScreen115::new;
    }

    /** /whm config: the screen of the same factory */
    static Screen open(Screen parent) {
        return new WhmModMenu().getConfigScreenFactory().apply(parent);
    }
}
