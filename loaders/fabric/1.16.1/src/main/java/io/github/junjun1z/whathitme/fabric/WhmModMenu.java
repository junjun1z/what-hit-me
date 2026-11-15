package io.github.junjun1z.whathitme.fabric;

import io.github.prospector.modmenu.api.ConfigScreenFactory;
import io.github.prospector.modmenu.api.ModMenuApi;

import io.github.junjun1z.whathitme.mc.ConfigScreen116;
import net.minecraft.client.gui.screens.Screen;

/** Optional Mod Menu entrypoint (Mod Menu 1.14 API): the config button of the mods list. Only loaded by Mod Menu. */
public final class WhmModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen116::new;
    }

    /** /whm config: the screen of the same factory */
    static Screen open(Screen parent) {
        return new WhmModMenu().getModConfigScreenFactory().create(parent);
    }
}
