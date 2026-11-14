package io.github.junjun1z.whathitme.forge;

import java.util.Collections;
import java.util.List;

import io.github.junjun1z.whathitme.core.server.CommandSource;
import io.github.junjun1z.whathitme.core.server.PlayerHandle;
import io.github.junjun1z.whathitme.core.server.ServerCore;
import io.github.junjun1z.whathitme.core.text.Txt;
import io.github.junjun1z.whathitme.mc.McText;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

/** /whm on the server: everything is done by {@link ServerCore#execute}. */
public final class ServerCommandWhm extends CommandBase {

    @Override
    public String getName() {
        return "whm";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("whathitme");
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/whm [history|last|effects|togglechat|ignore|rename|sethint|config|search|inspect|dump|reload|snapshot|help]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    static CommandSource source(ICommandSender s) {
        final PlayerHandle player = s instanceof EntityPlayerMP ? new PlayerHandle112((EntityPlayerMP) s) : null;
        return new CommandSource() {
            @Override
            public PlayerHandle player() {
                return player;
            }

            @Override
            public boolean isAdmin() {
                return player != null ? player.isAdmin() : s.canUseCommand(2, "whm");
            }

            @Override
            public void send(Txt line) {
                s.sendMessage(McText.of(line));
            }
        };
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        ServerCore.execute(source(sender), args);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
        return ServerCore.complete(source(sender), args);
    }
}
