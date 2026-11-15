package io.github.junjun1z.whathitme.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import io.github.junjun1z.whathitme.mc.ConfigScreen1202;
import net.minecraft.client.gui.screens.Screen;

/** Optional Mod Menu entrypoint (com.terraformersmc.modmenu.api): the config button of the mods list. Only loaded by Mod Menu. */
public final class WhmModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen1202::new;
    }

    /** /whm config: the screen of the same factory */
    static Screen open(Screen parent) {
        return new WhmModMenu().getModConfigScreenFactory().create(parent);
    }
}
