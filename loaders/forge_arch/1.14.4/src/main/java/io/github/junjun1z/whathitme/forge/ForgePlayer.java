package io.github.junjun1z.whathitme.forge;

import java.util.ArrayList;
import java.util.List;

import io.github.junjun1z.whathitme.core.net.Packets;
import io.github.junjun1z.whathitme.mc.McPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** A Forge 1.14.4 server player: packets through {@link ForgeNet}, settings in the player's persistent NBT (kept on death). */
final class ForgePlayer extends McPlayer {

    private static final String KEY_CHAT_OFF = "whathitme_chatOff";
    private static final String KEY_IGNORED = "whathitme_ignored";

    ForgePlayer(ServerPlayer p) {
        super(p);
    }

    @Override
    public void send(Packets.Packet packet) {
        ForgeNet.sendTo(p, packet);
    }

    private CompoundTag persisted() {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG, 10)) root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }

    @Override
    public boolean chatOff() {
        return persisted().getBoolean(KEY_CHAT_OFF);
    }

    @Override
    public boolean toggleChat() {
        CompoundTag t = persisted();
        boolean off = !t.getBoolean(KEY_CHAT_OFF);
        t.putBoolean(KEY_CHAT_OFF, off);
        return !off;
    }

    @Override
    public List<String> personalIgnored() {
        List<String> out = new ArrayList<>();
        ListTag l = persisted().getList(KEY_IGNORED, 8);
        for (int i = 0; i < l.size(); i++) out.add(l.getString(i));
        return out;
    }

    @Override
    public boolean togglePersonalIgnore(String id) {
        List<String> cur = personalIgnored();
        boolean now = !cur.remove(id);
        if (now) cur.add(id);
        ListTag l = new ListTag();
        for (String s : cur) l.add(new StringTag(s));
        persisted().put(KEY_IGNORED, l);
        return now;
    }
}
