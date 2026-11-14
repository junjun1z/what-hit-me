package io.github.junjun1z.whathitme.mc;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * The small API differences inside 26.x (one source compiled against 26.1.2, 26.2 and 26.3; 26.x is not obfuscated,
 * so these names are the same on every loader). Looked up once, by reflection.
 */
public final class Compat26 {

    private Compat26() {}

    private static Method screenM, chatHolderM, targetM, swingM;
    private static Field screenF, chatHolderF, invulF, swingF;
    private static boolean screenInit, chatInit, targetInit, invulInit, swingInit;

    /** Current screen: the field Minecraft.screen (26.1) or Minecraft.gui.screen() (26.2+) */
    public static Screen screen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return null;
        try {
            if (!screenInit) {
                screenInit = true;
                try {
                    screenF = Minecraft.class.getField("screen");
                } catch (NoSuchFieldException e) {
                    screenM = net.minecraft.client.gui.Gui.class.getMethod("screen");
                }
            }
            return (Screen) (screenF != null ? screenF.get(mc) : screenM.invoke(mc.gui));
        } catch (Exception e) {
            return null;
        }
    }

    /** The chat: Minecraft.gui.getChat() (26.1) or Minecraft.gui.hud.getChat() (26.2+) */
    public static ChatComponent chat() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gui == null) return null;
        try {
            if (!chatInit) {
                chatInit = true;
                try {
                    chatHolderM = net.minecraft.client.gui.Gui.class.getMethod("getChat");
                } catch (NoSuchMethodException e) {
                    chatHolderF = net.minecraft.client.gui.Gui.class.getField("hud");
                    chatHolderM = chatHolderF.getType().getMethod("getChat");
                }
            }
            Object holder = chatHolderF == null ? mc.gui : chatHolderF.get(mc.gui);
            return holder == null ? null : (ChatComponent) chatHolderM.invoke(holder);
        } catch (Exception e) {
            return null;
        }
    }

    /** The main render target: GameRenderer.mainRenderTarget() (26.2+) or Minecraft.getMainRenderTarget() (26.1) */
    public static Object mainRenderTarget() {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (!targetInit) {
                targetInit = true;
                try {
                    targetM = mc.gameRenderer.getClass().getMethod("mainRenderTarget");
                } catch (NoSuchMethodException e) {
                    targetM = Minecraft.class.getMethod("getMainRenderTarget");
                }
            }
            return targetM.getDeclaringClass() == Minecraft.class ? targetM.invoke(mc) : targetM.invoke(mc.gameRenderer);
        } catch (Exception e) {
            return null;
        }
    }

    private static void initInvul() {
        if (invulInit) return;
        invulInit = true;
        try {
            // 26.3: the damage cooldown moved to LivingEntity.damageCooldownTime (Entity.invulnerableTime is something else there)
            invulF = LivingEntity.class.getField("damageCooldownTime");
        } catch (NoSuchFieldException e) {
            try {
                invulF = Entity.class.getField("invulnerableTime");
            } catch (NoSuchFieldException e2) {
                invulF = null;
            }
        }
    }

    /** Damage cooldown ("invulnerability frames"): LivingEntity.damageCooldownTime (26.3) or Entity.invulnerableTime (26.1, 26.2) */
    public static int invulnerableTime(Entity e) {
        initInvul();
        try {
            return invulF == null || !invulF.getDeclaringClass().isInstance(e) ? 0 : invulF.getInt(e);
        } catch (Exception ex) {
            return 0;
        }
    }

    /** Is the entity swinging its arm? The field swinging (26.1, 26.2) or isSwinging() (26.3) */
    public static boolean swinging(LivingEntity e) {
        try {
            if (!swingInit) {
                swingInit = true;
                try {
                    swingF = LivingEntity.class.getField("swinging");
                } catch (NoSuchFieldException ex) {
                    try {
                        swingM = LivingEntity.class.getMethod("isSwinging");
                    } catch (NoSuchMethodException ex2) {
                        swingM = null;
                    }
                }
            }
            if (swingF != null) return swingF.getBoolean(e);
            return swingM != null && (Boolean) swingM.invoke(e);
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Mouse button of a 26.x input event in the classic numbering of the core (left = 0, right = 1, middle = 2):
     * 26.x numbers buttons like SDL (left = 1, middle = 2, right = 3).
     */
    public static int button(int sdlButton) {
        if (sdlButton == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) return 0;
        if (sdlButton == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT) return 1;
        if (sdlButton == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_MIDDLE) return 2;
        return sdlButton - 1;
    }

    /** Minecraft.setScreen (26.1) or Minecraft.gui.setScreen (26.2+) */
    public static void setScreen(Screen screen) {
        Minecraft mc = Minecraft.getInstance();
        try {
            try {
                Minecraft.class.getMethod("setScreen", Screen.class).invoke(mc, screen);
            } catch (NoSuchMethodException e) {
                Object gui = Minecraft.class.getField("gui").get(mc);
                gui.getClass().getMethod("setScreen", Screen.class).invoke(gui, screen);
            }
        } catch (Exception e) {
            // no screen change
        }
    }
}
