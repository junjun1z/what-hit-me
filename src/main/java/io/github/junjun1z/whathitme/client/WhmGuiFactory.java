package io.github.junjun1z.whathitme.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.github.junjun1z.whathitme.WhatHitMeMod;
import io.github.junjun1z.whathitme.config.WhmConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.DummyConfigElement;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** The "Config" button of the Mods list: both config files, edited in game (saved and applied on Done). */
@SideOnly(Side.CLIENT)
public final class WhmGuiFactory implements IModGuiFactory {

    @Override
    public void initialize(Minecraft minecraftInstance) {}

    @Override
    public boolean hasConfigGui() {
        return true;
    }

    @Override
    public GuiScreen createConfigGui(GuiScreen parentScreen) {
        return new GuiConfig(parentScreen, elements(), WhatHitMeMod.MODID, false, false, I18n.format("whathitme.config.title"));
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }

    private static List<IConfigElement> categories(Configuration cfg, String... names) {
        List<IConfigElement> out = new ArrayList<>();
        for (String n : names) {
            if (cfg.hasCategory(n)) out.add(new ConfigElement(cfg.getCategory(n)));
        }
        return out;
    }

    public static List<IConfigElement> elements() {
        List<IConfigElement> out = new ArrayList<>();
        out.add(new DummyConfigElement.DummyCategoryElement("client", "whathitme.config.client",
            categories(WhmConfig.client.cfg, WhmConfig.CAT_DISPLAY, WhmConfig.CAT_FILTERS, WhmConfig.CAT_LAYOUT)));
        out.add(new DummyConfigElement.DummyCategoryElement("common", "whathitme.config.common",
            categories(WhmConfig.common.cfg, WhmConfig.CAT_DISPLAY, WhmConfig.CAT_FILTERS, WhmConfig.CAT_TRACKING)));
        return out;
    }
}
