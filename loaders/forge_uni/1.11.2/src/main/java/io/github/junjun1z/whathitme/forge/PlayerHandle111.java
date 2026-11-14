package io.github.junjun1z.whathitme.forge;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.github.junjun1z.whathitme.core.data.Entry;
import io.github.junjun1z.whathitme.core.net.Packets;
import io.github.junjun1z.whathitme.core.server.PlayerHandle;
import io.github.junjun1z.whathitme.core.text.Txt;
import io.github.junjun1z.whathitme.mc.McText;
import io.github.junjun1z.whathitme.mc.Registries111;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.util.Constants;

/** A 1.11 server player for the core. Per-player settings live in the player's persistent NBT. */
public class PlayerHandle111 implements PlayerHandle {

    private static final String KEY_CHAT_OFF = "whathitme_chatOff";
    private static final String KEY_IGNORED = "whathitme_ignored";

    final EntityPlayerMP p;

    public PlayerHandle111(EntityPlayerMP p) {
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

    @Override
    public boolean isAdmin() {
        if (p.canUseCommand(2, "whm")) return true;
        MinecraftServer s = p.getServer();
        return s != null && !s.isDedicatedServer() && p.getName().equals(s.getServerOwner());
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
        int n = 0;
        for (ItemStack s : new ItemStack[] {p.getHeldItemMainhand(), p.getHeldItemOffhand()}) {
            if (!s.isEmpty() && s.getItem() == Items.TOTEM) n += s.getCount();
        }
        return n;
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
        return Registries111.effects(p);
    }
}
