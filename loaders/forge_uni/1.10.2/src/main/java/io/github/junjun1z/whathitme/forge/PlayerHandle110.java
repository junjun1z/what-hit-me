package io.github.junjun1z.whathitme.forge;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.github.junjun1z.whathitme.core.data.Entry;
import io.github.junjun1z.whathitme.core.data.Gear;
import io.github.junjun1z.whathitme.core.net.Packets;
import io.github.junjun1z.whathitme.core.server.PlayerHandle;
import io.github.junjun1z.whathitme.core.text.Txt;
import io.github.junjun1z.whathitme.mc.Facts;
import io.github.junjun1z.whathitme.mc.McText;
import io.github.junjun1z.whathitme.mc.Registries110;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraftforge.common.util.Constants;

/** A 1.10 server player for the core. Per-player settings live in the player's persistent NBT. */
public class PlayerHandle110 implements PlayerHandle {

    private static final String KEY_CHAT_OFF = "whathitme_chatOff";
    private static final String KEY_IGNORED = "whathitme_ignored";

    final EntityPlayerMP p;

    public PlayerHandle110(EntityPlayerMP p) {
        this.p = p;
    }

    @Override
    public UUID uuid() {
        return p.getUniqueID();
    }

    @Override
    public String name() {
        return p.getName();
    }

    /** Permission level 2: an operator, or the owner of a single player / LAN world with cheats */
    @Override
    public boolean isAdmin() {
        return p.canUseCommand(2, "whm");
    }

    @Override
    public boolean isAlive() {
        return !p.isDead;
    }

    @Override
    public float health() {
        return p.getHealth();
    }

    @Override
    public float maxHealth() {
        return p.getMaxHealth();
    }

    @Override
    public int totems() {
        return totems(p);
    }

    static int totems(EntityPlayer p) {
        return 0; // no Totem of Undying before 1.11
    }

    @Override
    public void chat(Txt line) {
        p.sendMessage(McText.of(line));
    }

    @Override
    public void send(Packets.Packet packet) {
        WhmNetwork.channel.sendTo(WhmNetwork.wrap(packet), p);
    }

    private NBTTagCompound persisted() {
        NBTTagCompound root = p.getEntityData();
        if (!root.hasKey(EntityPlayer.PERSISTED_NBT_TAG, Constants.NBT.TAG_COMPOUND)) root.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
        return root.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
    }

    @Override
    public boolean chatOff() {
        return persisted().getBoolean(KEY_CHAT_OFF);
    }

    @Override
    public boolean toggleChat() {
        NBTTagCompound t = persisted();
        boolean off = !t.getBoolean(KEY_CHAT_OFF);
        t.setBoolean(KEY_CHAT_OFF, off);
        return !off;
    }

    @Override
    public List<String> personalIgnored() {
        List<String> out = new ArrayList<>();
        NBTTagList l = persisted().getTagList(KEY_IGNORED, Constants.NBT.TAG_STRING);
        for (int i = 0; i < l.tagCount(); i++) out.add(l.getStringTagAt(i));
        return out;
    }

    @Override
    public boolean togglePersonalIgnore(String id) {
        List<String> cur = personalIgnored();
        boolean now = !cur.remove(id);
        if (now) cur.add(id);
        NBTTagList l = new NBTTagList();
        for (String s : cur) l.appendTag(new NBTTagString(s));
        persisted().setTag(KEY_IGNORED, l);
        return now;
    }

    @Override
    public List<Entry> effects() {
        return Registries110.effects(p);
    }

    @Override
    public String environment() {
        return Facts.environment(p);
    }

    @Override
    public Gear.Item[] armor() {
        return Facts.armor(p);
    }

    @Override
    public String dimension() {
        return Facts.dimension(p.world);
    }

    @Override
    public double[] position() {
        return new double[] {p.posX, p.posY, p.posZ};
    }

    @Override
    public Boolean hasTag(String tag) {
        return p.getTags().contains(tag);
    }

    @Override
    public Long score(String objective) {
        Scoreboard sb = p.getWorldScoreboard();
        ScoreObjective o = sb.getObjective(objective);
        if (o == null) return NO_SCORE;
        String name = p.getName();
        Score s = (Score) sb.getObjectivesForEntity(name).get(o);
        return s == null ? NO_SCORE : (long) s.getScorePoints();
    }

    @Override
    public Object nativePlayer() {
        return p;
    }

    @Override
    public boolean disconnect(Txt reason) {
        p.connection.disconnect(McText.of(reason).getFormattedText());
        return true;
    }
}
