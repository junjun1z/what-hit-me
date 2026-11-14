package io.github.junjun1z.whathitme.neoforge;

import java.util.ArrayList;
import java.util.List;

import io.github.junjun1z.whathitme.core.net.Packets;
import io.github.junjun1z.whathitme.mc.McPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** A NeoForge 26.3 server player: packets through {@link NeoNet}, settings in the player's persistent NBT (kept on death). */
final class NeoPlayer extends McPlayer {

    private static final String KEY_CHAT_OFF = "whathitme_chatOff";
    private static final String KEY_IGNORED = "whathitme_ignored";

    NeoPlayer(ServerPlayer p) {
        super(p);
    }

    @Override
    public void send(Packets.Packet packet) {
        NeoNet.sendTo(p, packet);
    }

    private CompoundTag persisted() {
        CompoundTag root = p.getPersistentData();
        java.util.Optional<CompoundTag> t = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (t.isPresent()) return t.get();
        CompoundTag n = new CompoundTag();
        root.put(Player.PERSISTED_NBT_TAG, n);
        return n;
    }

    @Override
    public boolean chatOff() {
        return persisted().getBooleanOr(KEY_CHAT_OFF, false);
    }

    @Override
    public boolean toggleChat() {
        CompoundTag t = persisted();
        boolean off = !t.getBooleanOr(KEY_CHAT_OFF, false);
        t.putBoolean(KEY_CHAT_OFF, off);
        return !off;
    }

    @Override
    public List<String> personalIgnored() {
        List<String> out = new ArrayList<>();
        ListTag l = persisted().getListOrEmpty(KEY_IGNORED);
        for (int i = 0; i < l.size(); i++) l.getString(i).ifPresent(out::add);
        return out;
    }

    @Override
    public boolean togglePersonalIgnore(String id) {
        List<String> cur = personalIgnored();
        boolean now = !cur.remove(id);
        if (now) cur.add(id);
        ListTag l = new ListTag();
        for (String s : cur) l.add(StringTag.valueOf(s));
        persisted().put(KEY_IGNORED, l);
        return now;
    }
}
