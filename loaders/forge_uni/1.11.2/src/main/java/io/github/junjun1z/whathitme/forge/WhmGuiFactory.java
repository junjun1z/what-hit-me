package io.github.junjun1z.whathitme.forge;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.github.junjun1z.whathitme.core.Platform;
import io.github.junjun1z.whathitme.core.config.WhmConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.DummyConfigElement;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.GuiConfigEntries;
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
        return new WithTemplates(parentScreen);
    }

    /** Forge's config screen plus a "Templates..." button that opens the template editor */
    public static final class WithTemplates extends GuiConfig {
        private static final int TEMPLATES = 3013;
        private static final int PACK = 3014;

        public WithTemplates(GuiScreen parent) {
            super(parent, elements(), Platform.MODID, false, false, I18n.format("whathitme.config.title"));
        }

        @Override
        public void initGui() {
            super.initGui();
            String label = I18n.format("whathitme.config.templates");
            int w = Math.max(70, io.github.junjun1z.whathitme.mc.Gfx111.INSTANCE.width(label) + 12);
            buttonList.add(new net.minecraft.client.gui.GuiButton(TEMPLATES, width - w - 4, 4, w, 20, label));
            String pl = I18n.format("whathitme.config.packeditor");
            int pw = Math.max(70, io.github.junjun1z.whathitme.mc.Gfx111.INSTANCE.width(pl) + 12);
            buttonList.add(new net.minecraft.client.gui.GuiButton(PACK, width - w - 4 - pw - 2, 4, pw, 20, pl));
        }

        /** Same as clicking "Pack editor..." (self-test); false when the button is missing */
        public boolean pressPackEditor() {
            for (net.minecraft.client.gui.GuiButton b : buttonList) {
                if (b.id == PACK) {
                    actionPerformed(b);
                    return true;
                }
            }
            return false;
        }

        /** Same as clicking "Templates..." (self-test); false when the button is missing */
        public boolean pressTemplates() {
            for (net.minecraft.client.gui.GuiButton b : buttonList) {
                if (b.id == TEMPLATES) {
                    actionPerformed(b);
                    return true;
                }
            }
            return false;
        }

        @Override
        protected void actionPerformed(net.minecraft.client.gui.GuiButton button) {
            if (button.id == TEMPLATES) {
                mc.displayGuiScreen(new GuiTemplateEditor(this));
                return;
            }
            if (button.id == PACK) {
                // pack mode for operators of a server with the mod, otherwise the personal lists
                mc.displayGuiScreen(new GuiTemplateEditor(this, new io.github.junjun1z.whathitme.core.client.PackEditor(false)));
                return;
            }
            super.actionPerformed(button);
        }
    }

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return null;
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }

    @Override
    public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element) {
        return null;
    }

    private static Configuration cfg(WhmConfig.Base b) {
        return ((ForgeCfgBackend) b.backend).cfg;
    }

    private static List<IConfigElement> categories(Configuration cfg, String... names) {
        List<IConfigElement> out = new ArrayList<>();
        for (String n : names) {
            if (!cfg.hasCategory(n)) continue;
            ConfigCategory c = cfg.getCategory(n);
            for (Property p : c.values()) p.setConfigEntryClass(WhmConfig.isLocked(p.getName()) ? lockedEntry(p) : null);
            out.add(new ConfigElement(c));
        }
        return out;
    }

    /** lockedFeatures of the pack folder: shown greyed out, not editable */
    private static Class<? extends GuiConfigEntries.IConfigEntry> lockedEntry(Property p) {
        if (p.isList()) return LockedArray.class;
        switch (p.getType()) {
            case INTEGER:
                return LockedInteger.class;
            case DOUBLE:
                return LockedDouble.class;
            default:
                return LockedString.class;
        }
    }

    public static final class LockedInteger extends GuiConfigEntries.IntegerEntry {
        public LockedInteger(GuiConfig s, GuiConfigEntries l, IConfigElement e) {
            super(s, l, e);
        }

        @Override
        public boolean enabled() {
            return false;
        }
    }

    public static final class LockedDouble extends GuiConfigEntries.DoubleEntry {
        public LockedDouble(GuiConfig s, GuiConfigEntries l, IConfigElement e) {
            super(s, l, e);
        }

        @Override
        public boolean enabled() {
            return false;
        }
    }

    public static final class LockedString extends GuiConfigEntries.StringEntry {
        public LockedString(GuiConfig s, GuiConfigEntries l, IConfigElement e) {
            super(s, l, e);
        }

        @Override
        public boolean enabled() {
            return false;
        }
    }

    public static final class LockedArray extends GuiConfigEntries.ArrayEntry {
        public LockedArray(GuiConfig s, GuiConfigEntries l, IConfigElement e) {
            super(s, l, e);
        }

        @Override
        public boolean enabled() {
            return false;
        }
    }

    public static List<IConfigElement> elements() {
        List<IConfigElement> out = new ArrayList<>();
        out.add(new DummyConfigElement.DummyCategoryElement("client", "whathitme.config.client",
            categories(cfg(WhmConfig.client), WhmConfig.CAT_DISPLAY, WhmConfig.CAT_FILTERS, WhmConfig.CAT_LAYOUT)));
        out.add(new DummyConfigElement.DummyCategoryElement("common", "whathitme.config.common",
            categories(cfg(WhmConfig.common), WhmConfig.CAT_DISPLAY, WhmConfig.CAT_FILTERS, WhmConfig.CAT_TRACKING)));
        return out;
    }
}
