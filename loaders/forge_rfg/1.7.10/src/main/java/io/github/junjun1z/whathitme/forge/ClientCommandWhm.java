package io.github.junjun1z.whathitme.forge;

import java.util.Collections;
import java.util.List;

import io.github.junjun1z.whathitme.core.client.ClientCore;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** /whm on a client with the mod: everything is done by {@link ClientCore#execute}. */
@SideOnly(Side.CLIENT)
public final class ClientCommandWhm extends CommandBase {

    @Override
    public String getCommandName() {
        return "whm";
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List getCommandAliases() {
        return Collections.singletonList("whathitme");
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "whathitme.command.usage";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        ClientCore.execute(args);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        return ClientCore.complete(args);
    }
}
