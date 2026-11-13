package io.github.junjun1z.whathitme.devtools;

import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import io.github.junjun1z.whathitme.client.ClientCommandWhm;
import io.github.junjun1z.whathitme.client.ClientState;
import io.github.junjun1z.whathitme.client.DeathPanel;
import io.github.junjun1z.whathitme.config.WhmConfig;
import io.github.junjun1z.whathitme.data.DamageTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.common.config.Property;

/**
 * Exploratory test with real mouse / keyboard input (java.awt.Robot): death panel drag, minimize, wheel, copy, chat
 * buttons, typed commands, Tab completion, GUI scales, window resize, repeated deaths, other dimensions, rejoin.
 * "explore2" (second launch) checks that the dragged panel position survived the restart.
 */
final class ExplorePlan {

    private final SelfTest t;
    private Robot robot;
    /** Screen position of the game client area */
    private int ox;
    private int oy;
    /** client pixels per screen (Robot) pixel; differs from 1 with Windows display scaling */
    private double kx = 1;
    private double ky = 1;
    private int sx1;
    private int sy1;
    private int cx1;
    private int cy1;
    private int startX;
    private int startY;
    private int dragOffX;
    private int dragOffY;
    private String copiedId = "";

    ExplorePlan(SelfTest t) {
        this.t = t;
    }

    /** Teleporter for the dimension checks: a small obsidian platform with air above, no portal */
    private static void platform(net.minecraft.world.World w, net.minecraft.entity.Entity e, float yaw) {
        int x = (int) Math.floor(e.posX);
        int z = (int) Math.floor(e.posZ);
        int y = 70;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                w.setBlockState(new net.minecraft.util.math.BlockPos(x + dx, y - 1, z + dz), net.minecraft.init.Blocks.OBSIDIAN.getDefaultState());
                for (int dy = 0; dy < 3; dy++) w.setBlockToAir(new net.minecraft.util.math.BlockPos(x + dx, y + dy, z + dz));
            }
        }
        e.setLocationAndAngles(x + 0.5, y, z + 0.5, yaw, 0f);
        e.motionX = 0;
        e.motionY = 0;
        e.motionZ = 0;
    }

    private static String read(File f) {
        try {
            return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private static Minecraft mc() {
        return Minecraft.getMinecraft();
    }

    private static int scale() {
        return new ScaledResolution(mc()).getScaleFactor();
    }

    private static int panelInt(String name) {
        try {
            Field f = DeathPanel.class.getDeclaredField(name);
            f.setAccessible(true);
            return f.getInt(DeathPanel.INSTANCE);
        } catch (Exception e) {
            return -1;
        }
    }

    private static float panelScale() {
        try {
            Field f = DeathPanel.class.getDeclaredField("sc");
            f.setAccessible(true);
            return f.getFloat(DeathPanel.INSTANCE);
        } catch (Exception e) {
            return 1f;
        }
    }

    private static int panelW() {
        try {
            Field f = DeathPanel.class.getDeclaredField("W");
            f.setAccessible(true);
            return f.getInt(null);
        } catch (Exception e) {
            return 250;
        }
    }

    /** Moves the real mouse to GUI coordinates */
    private int rx;
    private int ry;

    private void moveGui(double gx, double gy) {
        int s = scale();
        rx = ox + (int) Math.round((gx * s + s / 2.0) / kx);
        ry = oy + (int) Math.round((gy * s + s / 2.0) / ky);
        robot.mouseMove(rx, ry);
    }

    private void click() {
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(40);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    private void key(int vk) {
        robot.keyPress(vk);
        robot.delay(30);
        robot.keyRelease(vk);
    }

    private void calibrate() {
        t.waitFor("game window active (needed for real input)", Display::isActive, 1200);
        t.act(() -> {
            if (!Display.isActive()) t.abort("the game window never became active: real input cannot be tested");
        });
        t.act(() -> {
            try {
                robot = new Robot();
            } catch (Exception e) {
                t.ok("java.awt.Robot available", false, e);
                return;
            }
            // an empty corner of the death screen (the middle is the Respawn button)
            robot.mouseMove(Display.getX() + 8 + mc().displayWidth * 9 / 10, Display.getY() + 31 + mc().displayHeight * 9 / 10);
        });
        t.sleep(2);
        t.act(() -> click());
        t.sleep(4);
        t.act(() -> {
            sx1 = Display.getX() + 8 + mc().displayWidth / 4;
            sy1 = Display.getY() + 31 + mc().displayHeight / 4;
            robot.mouseMove(sx1, sy1);
        });
        t.sleep(4);
        t.act(() -> {
            cx1 = Mouse.getX();
            cy1 = mc().displayHeight - Mouse.getY() - 1;
            robot.mouseMove(sx1 + 300, sy1 + 150);
        });
        t.sleep(4);
        t.act(() -> {
            int cx2 = Mouse.getX();
            int cy2 = mc().displayHeight - Mouse.getY() - 1;
            kx = (cx2 - cx1) / 300.0;
            ky = (cy2 - cy1) / 150.0;
            if (kx <= 0.1 || ky <= 0.1 || Double.isNaN(kx) || Double.isNaN(ky)) {
                t.ok("calibration (mouse moved by Robot is seen by the game)", false, "k=" + kx + "," + ky);
                kx = 1;
                ky = 1;
            }
            ox = (int) Math.round(sx1 - cx1 / kx);
            oy = (int) Math.round(sy1 - cy1 / ky);
            t.info("calibration: k=" + kx + "," + ky + " origin " + ox + "," + oy + " (Display " + Display.getX() + "," + Display.getY() + ")");
        });
    }

    /** Many different damage types inside the recap window so the panel needs scrolling */
    private void manyHits() {
        DamageSource[] list = {DamageSource.FALL, DamageSource.IN_FIRE, DamageSource.ON_FIRE, DamageSource.LAVA, DamageSource.MAGIC, DamageSource.WITHER,
            DamageSource.CACTUS, DamageSource.DROWN, DamageSource.STARVE, DamageSource.IN_WALL, DamageSource.GENERIC, DamageSource.ANVIL,
            DamageSource.FLY_INTO_WALL, DamageSource.HOT_FLOOR};
        for (DamageSource s : list) {
            t.srv(p -> {
                p.setHealth(p.getMaxHealth());
                p.extinguish();
                p.attackEntityFrom(s, 1f);
            });
            t.sleep(13);
        }
    }

    private void kill() {
        t.srv(p -> {
            boolean hit = p.attackEntityFrom(DamageSource.OUT_OF_WORLD, 1000f);
            if (!hit) t.info("kill refused: invulnerable=" + p.isEntityInvulnerable(DamageSource.OUT_OF_WORLD) + " dimChange=" + p.isInvulnerableDimensionChange() + " health=" + p.getHealth());
        });
        t.waitFor("death screen", () -> mc().currentScreen instanceof GuiGameOver, 100);
    }

    private void respawnNow() {
        t.act(() -> {
            mc().player.respawnPlayer();
            mc().displayGuiScreen(null);
        });
        t.waitFor("respawned", () -> mc().player != null && mc().player.getHealth() > 0f && !(mc().currentScreen instanceof GuiGameOver), 200);
    }

    private static int count(String s) {
        int n = 0;
        for (net.minecraft.client.gui.ChatLine l : SelfTest.chatLines()) if (l.getChatComponent().getUnformattedText().contains(s)) n++;
        return n;
    }

    /** Finds a chat component with a click event whose value starts with the prefix; returns {screenX, screenY} or null */
    private int[] findClick(String prefix) {
        for (int y = 2; y < mc().displayHeight / 2; y += 2) {
            for (int x = 2; x < mc().displayWidth / 2; x += 3) {
                ITextComponent c = mc().ingameGUI.getChatGUI().getChatComponent(x, y);
                if (c == null) continue;
                ClickEvent ce = c.getStyle().getClickEvent();
                if (ce != null && ce.getValue().startsWith(prefix)) return new int[] {ox + (int) Math.round((x + 2) / kx), oy + (int) Math.round((mc().displayHeight - y - 1) / ky), 0};
            }
        }
        return null;
    }

    private void clickChat(String name, String prefix) {
        t.then(() -> {
            int[] at = findClick(prefix);
            if (at == null) {
                t.ok("chat button found: " + name, false, SelfTest.chatText());
                return true;
            }
            robot.mouseMove(at[0], at[1]);
            return true;
        });
        t.sleep(3);
        t.act(() -> click());
        t.sleep(10);
    }

    private static GuiTextField input() {
        if (!(mc().currentScreen instanceof GuiChat)) return null;
        for (String n : new String[] {"inputField", "field_146415_a"}) {
            try {
                Field f = GuiChat.class.getDeclaredField(n);
                f.setAccessible(true);
                return (GuiTextField) f.get(mc().currentScreen);
            } catch (Exception ignored) {
                // next
            }
        }
        return null;
    }

    private void typed(String text, String expect) {
        t.clearChat();
        t.act(() -> key(KeyEvent.VK_T));
        t.waitFor("chat opened by the T key", () -> mc().currentScreen instanceof GuiChat, 40);
        t.sleep(3);
        t.act(() -> {
            GuiTextField f = input();
            if (f != null) f.setText(text);
        });
        t.act(() -> key(KeyEvent.VK_ENTER));
        t.waitFor("typed " + text + " => " + expect, () -> SelfTest.chatHas(expect), 60);
    }

    private void resize(int gs) {
        t.act(() -> {
            mc().gameSettings.guiScale = gs;
            ScaledResolution sr = new ScaledResolution(mc());
            if (mc().currentScreen != null) mc().currentScreen.onResize(mc(), sr.getScaledWidth(), sr.getScaledHeight());
        });
        t.sleep(4);
        t.check("panel inside the screen at GUI scale " + gs, () -> {
            GuiScreen g = mc().currentScreen;
            if (!(g instanceof GuiGameOver)) return false;
            int px = panelInt("px");
            int py = panelInt("py");
            int ph = panelInt("ph");
            int pw = Math.round(panelW() * panelScale());
            boolean clearOfButtons = gs == 0 || px + pw <= g.width / 2 - 100;
            return g instanceof GuiGameOver && px >= 0 && py >= 0 && px + pw <= g.width && py + ph <= g.height && clearOfButtons;
        }, (java.util.function.Supplier<?>) () -> "px=" + panelInt("px") + " py=" + panelInt("py") + " ph=" + panelInt("ph") + " pw=" + Math.round(panelW() * panelScale())
            + " screen=" + (mc().currentScreen == null ? "null" : mc().currentScreen.width + "x" + mc().currentScreen.height));
        t.screenshot("whm_explore_guiscale_" + gs);
    }

    void plan(boolean second) {
        t.loadWorld("whm_explore");
        t.waitFor("server info received", () -> ClientState.serverHasMod, 400);
        t.srv(p -> {
            p.world.getGameRules().setOrCreateGameRule("keepInventory", "true");
            p.world.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
            p.world.setWorldTime(6000);
        });
        t.sleep(100);
        if (second) {
            planSecond();
            return;
        }
        t.act(() -> {
            WhmConfig.client.saveLayout(4, 0, false);
            startX = WhmConfig.client.offsetX;
            startY = WhmConfig.client.offsetY;
        });
        manyHits();
        kill();
        t.sleep(30);
        calibrate();
        t.sleep(5);
        // ---- drag the header
        t.then(() -> {
            float sc = panelScale();
            moveGui(panelInt("px") + 40 * sc, panelInt("py") + 6 * sc);
            return true;
        });
        t.sleep(3);
        t.act(() -> robot.mousePress(InputEvent.BUTTON1_DOWN_MASK));
        t.sleep(3);
        for (int i = 1; i <= 4; i++) {
            final int k = i;
            t.then(() -> {
                int s = scale();
                rx += (int) Math.round(15 * s / kx);
                ry += (int) Math.round(8 * s / ky);
                robot.mouseMove(rx, ry);
                return true;
            });
            t.sleep(2);
        }
        t.act(() -> robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK));
        t.sleep(5);
        t.check("drag moved the panel and saved offsetX/offsetY", () -> {
            dragOffX = WhmConfig.client.offsetX;
            dragOffY = WhmConfig.client.offsetY;
            return Math.abs(dragOffX - startX - 60) <= 3 && Math.abs(dragOffY - startY - 32) <= 3;
        }, (java.util.function.Supplier<?>) () -> "offset " + WhmConfig.client.offsetX + "," + WhmConfig.client.offsetY + " (start " + startX + "," + startY + ")");
        t.check("drag saved to whathitme-client.cfg", () -> {
            String txt = read(new File(t.gameDir, "config/whathitme/whathitme-client.cfg"));
            return txt.contains("I:offsetX=" + dragOffX) && txt.contains("I:offsetY=" + dragOffY);
        }, null);
        t.act(() -> {
            try {
                Files.write(new File(t.gameDir, "whm_explore_expect.txt").toPath(), Arrays.asList(dragOffX + "," + dragOffY), StandardCharsets.UTF_8);
            } catch (Exception e) {
                t.ok("write expectation", false, e);
            }
        });
        t.screenshot("whm_explore_dragged");
        // ---- wheel scroll
        t.then(() -> {
            float sc = panelScale();
            moveGui(panelInt("px") + 100 * sc, panelInt("py") + (panelInt("linesTop") + 15) * sc);
            return true;
        });
        t.sleep(3);
        t.act(() -> robot.mouseWheel(1));
        t.sleep(4);
        t.check("mouse wheel scrolls the list", () -> panelInt("scroll") > 0, (java.util.function.Supplier<?>) () -> "scroll=" + panelInt("scroll"));
        t.act(() -> robot.mouseWheel(-3));
        t.sleep(4);
        t.check("wheel up scrolls back to the top", () -> panelInt("scroll") == 0, (java.util.function.Supplier<?>) () -> "scroll=" + panelInt("scroll"));
        // ---- hover (tooltip) screenshot
        t.sleep(5);
        t.screenshot("whm_explore_hover");
        // ---- [Copy]
        t.act(() -> GuiScreen.setClipboardString(""));
        t.then(() -> {
            float sc = panelScale();
            int w = mc().fontRenderer.getStringWidth("[-]");
            int cw = mc().fontRenderer.getStringWidth("[" + net.minecraft.client.resources.I18n.format("whathitme.panel.copy") + "]");
            int minX = panelW() - 4 - w;
            moveGui(panelInt("px") + (minX - 4 - cw / 2.0) * sc, panelInt("py") + 6 * sc);
            return true;
        });
        t.sleep(3);
        t.act(() -> click());
        t.sleep(5);
        t.check("[Copy] puts the recap in the clipboard", () -> GuiScreen.getClipboardString().contains("Killing Blow"), (java.util.function.Supplier<?>) GuiScreen::getClipboardString);
        // ---- [-] minimize
        t.then(() -> {
            float sc = panelScale();
            moveGui(panelInt("px") + (panelW() - 8) * sc, panelInt("py") + 6 * sc);
            return true;
        });
        t.sleep(3);
        t.act(() -> click());
        t.sleep(5);
        t.check("[-] minimizes (saved)", () -> WhmConfig.client.minimized && panelInt("ph") < 20, (java.util.function.Supplier<?>) () -> "minimized=" + WhmConfig.client.minimized + " ph=" + panelInt("ph"));
        t.screenshot("whm_explore_minimized");
        t.then(() -> {
            float sc = panelScale();
            moveGui(panelInt("px") + (panelW() - 8) * sc, panelInt("py") + 6 * sc);
            return true;
        });
        t.sleep(3);
        t.act(() -> click());
        t.sleep(5);
        t.check("[+] restores", () -> !WhmConfig.client.minimized, null);
        // ---- GUI scales and window size while the death screen is open
        t.act(() -> WhmConfig.client.saveLayout(4, 0, false));
        resize(1);
        resize(3);
        resize(0);
        resize(2);
        t.act(() -> {
            try {
                Display.setDisplayMode(new DisplayMode(960, 600));
            } catch (Exception e) {
                t.ok("window resize", false, e);
            }
        });
        t.sleep(15);
        t.check("window resized (960x600) and panel inside", () -> {
            GuiScreen g = mc().currentScreen;
            int pw = Math.round(panelW() * panelScale());
            return mc().displayWidth == 960 && g instanceof GuiGameOver && panelInt("px") + pw <= g.width / 2 - 100 && panelInt("py") + panelInt("ph") <= g.height;
        }, (java.util.function.Supplier<?>) () -> mc().displayWidth + "x" + mc().displayHeight + " px=" + panelInt("px") + " py=" + panelInt("py"));
        t.screenshot("whm_explore_small_window");
        t.act(() -> {
            try {
                Display.setDisplayMode(new DisplayMode(1280, 720));
            } catch (Exception e) {
                t.ok("window resize back", false, e);
            }
        });
        t.sleep(15);
        calibrate();
        // ---- the real Respawn button
        t.clearChat();
        t.then(() -> {
            GuiScreen g = mc().currentScreen;
            if (g != null) moveGui(g.width / 2.0, g.height / 4.0 + 82);
            return true;
        });
        t.sleep(3);
        t.act(() -> click());
        t.waitFor("Respawn button (real click) respawns", () -> mc().player != null && mc().player.getHealth() > 0f && !(mc().currentScreen instanceof GuiGameOver), 100);
        t.waitFor("respawn summary", () -> SelfTest.chatHas("Killing Blow"), 100);
        // ---- chat buttons (real clicks)
        t.act(() -> key(KeyEvent.VK_T));
        t.waitFor("chat open", () -> mc().currentScreen instanceof GuiChat, 40);
        t.sleep(5);
        t.act(() -> GuiScreen.setClipboardString(""));
        clickChat("[Copy ID]", "/whm copy ");
        t.check("[Copy ID] click copies minecraft:outOfWorld", () -> "minecraft:outOfWorld".equals(GuiScreen.getClipboardString()), (java.util.function.Supplier<?>) GuiScreen::getClipboardString);
        clickChat("[Mute]", "/whm ignore ");
        t.check("[Mute] click hides the id", () -> WhmConfig.client.ignoredDamageIds.contains("minecraft:outOfWorld"), (java.util.function.Supplier<?>) () -> WhmConfig.client.ignoredDamageIds);
        clickChat("[Mute] again", "/whm ignore ");
        t.check("[Mute] again shows it again", () -> !WhmConfig.client.ignoredDamageIds.contains("minecraft:outOfWorld"), (java.util.function.Supplier<?>) () -> WhmConfig.client.ignoredDamageIds);
        clickChat("recap link", "/whm last");
        t.waitFor("recap link opens the death recap", () -> SelfTest.chatHas("Death Recap"), 60);
        t.act(() -> mc().displayGuiScreen(null));
        // totem alert [Details] (after the respawn invulnerability)
        t.sleep(70);
        t.clearChat();
        t.srv(p -> {
            p.setHealth(p.getMaxHealth());
            p.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        });
        t.sleep(5);
        t.srv(p -> p.attackEntityFrom(DamageSource.MAGIC, 100f));
        t.waitFor("totem alert", () -> SelfTest.chatHas("Totem of Undying saved you"), 60);
        t.act(() -> key(KeyEvent.VK_T));
        t.waitFor("chat open", () -> mc().currentScreen instanceof GuiChat, 40);
        t.sleep(5);
        clickChat("[Details]", "/whm snapshot");
        t.waitFor("[Details] opens the snapshot", () -> SelfTest.chatHas("Totem of Undying (") || SelfTest.chatHas("Snapshot 1 of"), 60);
        t.act(() -> mc().displayGuiScreen(null));
        t.srv(p -> p.clearActivePotions());
        // ---- typed command and Tab with the real keyboard
        typed("/whm history", "Damage History");
        t.act(() -> mc().displayGuiScreen(null));
        t.act(() -> key(KeyEvent.VK_T));
        t.waitFor("chat open", () -> mc().currentScreen instanceof GuiChat, 40);
        t.sleep(3);
        t.act(() -> {
            GuiTextField f = input();
            if (f != null) f.setText("/whm sea");
        });
        t.act(() -> key(KeyEvent.VK_TAB));
        t.sleep(10);
        t.check("Tab key completes /whm sea -> /whm search", () -> input() != null && input().getText().startsWith("/whm search"), (java.util.function.Supplier<?>) () -> input() == null ? "no input" : input().getText());
        t.act(() -> {
            GuiTextField f = input();
            if (f != null) f.setText("/whm search #fi");
        });
        t.act(() -> key(KeyEvent.VK_TAB));
        t.sleep(10);
        t.check("Tab key completes #fi -> #fire", () -> input() != null && input().getText().equals("/whm search #fire"), (java.util.function.Supplier<?>) () -> input() == null ? "no input" : input().getText());
        t.act(() -> mc().displayGuiScreen(null));
        // ---- Tab completion of every subcommand (no exception, useful values)
        t.check("tab completion for every subcommand", () -> {
            ClientCommandWhm c = new ClientCommandWhm();
            for (String s : new String[] {"history", "last", "effects", "togglechat", "ignore", "rename", "sethint", "config", "search", "inspect", "dump", "reload",
                "snapshot", "copy", "copyrecap", "server", "help"}) {
                c.getTabCompletions(null, mc().player, new String[] {s, ""}, null);
                c.getTabCompletions(null, mc().player, new String[] {s, "x", ""}, null);
            }
            return !c.getTabCompletions(null, mc().player, new String[] {"ignore", ""}, null).isEmpty()
                && c.getTabCompletions(null, mc().player, new String[] {"config", ""}, null).size() == WhmConfig.client.properties().size()
                && !c.getTabCompletions(null, mc().player, new String[] {"search", "@"}, null).isEmpty()
                && c.getTabCompletions(null, mc().player, new String[] {"inspect", ""}, null).contains(mc().player.getName())
                && c.getTabCompletions(null, mc().player, new String[] {"snapshot", ""}, null).contains("1")
                && c.getTabCompletions(null, mc().player, new String[] {"server", ""}, null).contains("history")
                && c.getTabCompletions(null, mc().player, new String[] {"config", "anchorPoint", ""}, null).contains("TOP_RIGHT")
                && c.getTabCompletions(null, mc().player, new String[] {"config", "showTimestamp", ""}, null).contains("false");
        }, null);
        // ---- /whm config get / set for every key
        t.act(() -> {
            int bad = 0;
            StringBuilder why = new StringBuilder();
            for (Map.Entry<String, Property> e : WhmConfig.client.properties().entrySet()) {
                String k = e.getKey();
                Property p = e.getValue();
                String before = WhmConfig.client.get(k);
                SelfTest.mc().ingameGUI.getChatGUI().clearChatMessages(false);
                SelfTest.command("/whm config " + k);
                if (!SelfTest.chatHas(k + " = ")) {
                    bad++;
                    why.append("get ").append(k).append("; ");
                }
                String invalid = p.isList() ? null : p.getType() == Property.Type.BOOLEAN ? "maybe" : p.getType() == Property.Type.INTEGER ? "abc"
                    : p.getValidValues() != null && p.getValidValues().length > 0 ? "NOWHERE" : "notanumber";
                if (invalid != null && !k.equals("dummy")) {
                    SelfTest.mc().ingameGUI.getChatGUI().clearChatMessages(false);
                    SelfTest.command("/whm config " + k + " " + invalid);
                    if (!SelfTest.chatHas("Invalid value")) {
                        bad++;
                        why.append("invalid ").append(k).append("; ");
                    }
                }
                // set back the same value (valid)
                if (!p.isList()) {
                    SelfTest.mc().ingameGUI.getChatGUI().clearChatMessages(false);
                    SelfTest.command("/whm config " + k + " " + before);
                    if (!SelfTest.chatHas("Saved: " + k)) {
                        bad++;
                        why.append("set ").append(k).append("; ");
                    }
                }
                if (!before.equals(WhmConfig.client.get(k))) {
                    bad++;
                    why.append("changed ").append(k).append("; ");
                }
            }
            t.ok("/whm config get + invalid + set for every client key (" + WhmConfig.client.properties().size() + ")", bad == 0, why.length() == 0 ? null : why);
        });
        t.cmd("/whm config scale 0.1", "Invalid value", 40);
        t.cmd("/whm config scale 3.5", "Invalid value", 40);
        t.cmd("/whm config maxVisibleLines 0", "Invalid value", 40);
        t.cmd("/whm config backgroundOpacity 1.5", "Invalid value", 40);
        t.cmd("/whm config anchorPoint top_right", "Saved: anchorPoint = TOP_RIGHT", 40);
        t.cmd("/whm config anchorPoint CENTER_LEFT", "Saved: anchorPoint = CENTER_LEFT", 40);
        t.cmd("/whm config ignoredDamageIds minecraft:cactus, minecraft:fall", "Saved: ignoredDamageIds", 40);
        t.check("list setting parsed", () -> WhmConfig.client.ignoredDamageIds.equals(Arrays.asList("minecraft:cactus", "minecraft:fall")), (java.util.function.Supplier<?>) () -> WhmConfig.client.ignoredDamageIds);
        t.cmd("/whm config ignoredDamageIds", "ignoredDamageIds = ", 40);
        t.act(() -> WhmConfig.client.set("ignoredDamageIds", ""));
        t.cmd("/whm config nosuchkey 1", "Unknown setting", 40);
        // ---- reload after editing the files by hand
        t.act(() -> {
            try {
                File f = new File(t.gameDir, "config/whathitme/whathitme-client.cfg");
                String txt = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                Files.write(f.toPath(), txt.replace("B:showTimestamp=true", "B:showTimestamp=false").getBytes(StandardCharsets.UTF_8));
                File c = new File(t.gameDir, "config/whathitme/whathitme-common.cfg");
                String ct = new String(Files.readAllBytes(c.toPath()), StandardCharsets.UTF_8);
                Files.write(c.toPath(), ct.replaceFirst("S:hintMap <\\r?\\n\\s*>", "S:hintMap <\n        minecraft:outOfWorld = Hand edited tip\n     >").getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                t.ok("edit cfg files", false, e);
            }
        });
        t.cmd("/whm reload", "reloaded", 60);
        t.sleep(10);
        t.check("hand edit (client cfg) applied by /whm reload", () -> (WhmConfig.client.mask & WhmConfig.Flag.TIMESTAMP.bit()) == 0, null);
        t.check("hand edit (common cfg) applied and sent by the server", () -> "Hand edited tip".equals(WhmConfig.common.hintMap.get("minecraft:outOfWorld"))
            && ClientState.serverInfo != null && ClientState.serverInfo.hints.contains("minecraft:outOfWorld = Hand edited tip"), (java.util.function.Supplier<?>) () -> ClientState.serverInfo == null ? "no info" : ClientState.serverInfo.hints);
        t.cmd("/whm last", "Hand edited tip", 60);
        t.act(() -> {
            WhmConfig.client.set("showTimestamp", "true");
            WhmConfig.common.putMapEntry("hintMap", "minecraft:outOfWorld", "");
        });
        // ---- repeated quick deaths
        t.clearChat();
        for (int i = 0; i < 3; i++) {
            kill();
            respawnNow();
        }
        t.waitFor("three quick deaths -> three summaries", () -> count("Killing Blow") >= 3, 100);
        // ---- dying with the chat open
        t.clearChat();
        t.act(() -> mc().displayGuiScreen(new GuiChat()));
        t.sleep(5);
        kill();
        t.sleep(25);
        respawnNow();
        t.waitFor("death with chat open -> summary", () -> SelfTest.chatHas("Killing Blow"), 100);
        // ---- other dimensions
        t.clearChat();
        t.srv(p -> p.changeDimension(-1, ExplorePlan::platform));
        t.waitFor("in the nether", () -> mc().player != null && mc().player.dimension == -1, 400);
        t.sleep(100);
        // changeDimension() from code leaves the vanilla "dimension change" invulnerability on until a teleport confirm
        t.srv(p -> {
            p.clearInvulnerableDimensionChange();
            p.clearActivePotions(); // the earlier totem gave Fire Resistance
        });
        t.srv(p -> t.info("lava hit in the nether accepted: " + p.attackEntityFrom(DamageSource.LAVA, 2f) + " invulnerable=" + p.isEntityInvulnerable(DamageSource.LAVA)));
        t.sleep(14);
        kill();
        t.sleep(25);
        respawnNow();
        t.waitFor("death in the nether -> summary after respawn in the overworld", () -> SelfTest.chatHas("Killing Blow") && mc().player.dimension == 0, 200);
        t.cmd("/whm last", "Lava", 60);
        t.clearChat();
        t.srv(p -> p.changeDimension(1, ExplorePlan::platform));
        t.waitFor("in the end", () -> mc().player != null && mc().player.dimension == 1, 400);
        t.sleep(100);
        t.srv(p -> p.clearInvulnerableDimensionChange());
        kill();
        t.sleep(25);
        respawnNow();
        t.waitFor("death in the end -> summary", () -> SelfTest.chatHas("Killing Blow"), 200);
        t.cmd("/whm", "Damage History", 60);
        // ---- leave the world and come back: buffers cleared
        t.act(() -> {
            mc().world.sendQuittingDisconnectingPacket();
            mc().loadWorld(null);
            mc().displayGuiScreen(new GuiMainMenu());
        });
        t.waitFor("back in the main menu", () -> mc().currentScreen instanceof GuiMainMenu && mc().world == null, 400);
        t.sleep(20);
        t.check("client state reset after leaving", () -> !ClientState.serverHasMod && ClientState.currentDeath == null && ClientState.FALLBACK.records().isEmpty(), null);
        t.check("server buffers dropped after leaving", () -> DamageTracker.SERVER.modded().isEmpty() && DamageTracker.SERVER.catalog.ids().isEmpty(), null);
        t.loadWorld("whm_explore");
        t.waitFor("server info again after rejoin", () -> ClientState.serverHasMod, 400);
        t.cmd("/whm", "Damage History (0)", 60);
        t.cmd("/whm last", "No death recorded", 60);
        t.screenshot("whm_explore_end");
        // the layout was reset for the GUI scale checks: put the dragged position back for the restart check (explore2)
        t.act(() -> WhmConfig.client.saveLayout(dragOffX, dragOffY, false));
    }

    private void planSecond() {
        t.check("restart: dragged offset still in the config", () -> {
            String[] p = read(new File(t.gameDir, "whm_explore_expect.txt")).trim().split(",");
            return WhmConfig.client.offsetX == Integer.parseInt(p[0]) && WhmConfig.client.offsetY == Integer.parseInt(p[1]);
        }, (java.util.function.Supplier<?>) () -> WhmConfig.client.offsetX + "," + WhmConfig.client.offsetY);
        kill();
        t.sleep(30);
        t.check("restart: panel drawn at the saved position", () -> {
            GuiScreen g = mc().currentScreen;
            int pw = Math.round(panelW() * panelScale());
            int ph = panelInt("ph");
            int baseY = (g.height - ph) / 2;
            int ex = Math.max(0, Math.min(g.width - pw, WhmConfig.client.offsetX));
            int ey = Math.max(0, Math.min(g.height - ph, baseY + WhmConfig.client.offsetY));
            return panelInt("px") == ex && panelInt("py") == ey;
        }, (java.util.function.Supplier<?>) () -> "px=" + panelInt("px") + " py=" + panelInt("py"));
        t.screenshot("whm_explore_restart");
        t.act(() -> WhmConfig.client.saveLayout(4, 0, false));
        respawnNow();
    }
}
