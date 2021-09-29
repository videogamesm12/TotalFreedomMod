package me.totalfreedom.totalfreedommod.command;

import me.totalfreedom.totalfreedommod.rank.Rank;
import me.totalfreedom.totalfreedommod.util.FUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.SENIOR_ADMIN, source = SourceType.BOTH)
@CommandParameters(description = "Restart the server", usage = "/<command>")
public class Command_fionn extends FreedomCommand
{
    @Override
    public boolean run(final CommandSender sender, final Player playerSender, final Command cmd, final String commandLabel, final String[] args, final boolean senderIsConsole)
    {
        if (!plugin.ptero.isEnabled())
        {
            msg("Pterodactyl integration is currently disabled.", ChatColor.RED);
            return true;
        }
        if (!FUtil.isExecutive(sender.getName()))
        {
            noPerms();
            return true;
        }
        FUtil.bcastMsg(ChatColor.LIGHT_PURPLE + "Fionn is about to corrupt the worlds again!");
        plugin.ptero.fionnTheServer();
        return true;
    }
}
