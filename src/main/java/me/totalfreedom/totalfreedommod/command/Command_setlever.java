package me.totalfreedom.totalfreedommod.command;

import java.util.List;
import me.totalfreedom.totalfreedommod.rank.Rank;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Switch;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.NON_OP, source = SourceType.BOTH)
@CommandParameters(description = "Set the on/off state of the lever at position x, y, z in world 'worldname'.", usage = "/<command> <x> <y> <z> <worldname> <on|off>")
public class Command_setlever extends FreedomCommand
{

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length != 5)
        {
            return false;
        }

        double x, y, z;
        try
        {
            x = Double.parseDouble(args[0]);
            y = Double.parseDouble(args[1]);
            z = Double.parseDouble(args[2]);
        }
        catch (NumberFormatException ex)
        {
            msg("Invalid coordinates.");
            return true;
        }

        if (x > 29999998 || x < -29999998 || y > 29999998 || y < -29999998 || z > 29999998 || z < -29999998)
        {
            msg("Coordinates cannot be larger than 29999998 or smaller than -29999998 blocks.");
            return true;
        }

        World world = null;
        final String needleWorldName = args[3].trim();
        final List<World> worlds = server.getWorlds();
        for (final World testWorld : worlds)
        {
            if (testWorld.getName().trim().equalsIgnoreCase(needleWorldName))
            {
                world = testWorld;
                break;
            }
        }

        if (world == null)
        {
            msg("Invalid world name.");
            return true;
        }

        final Location leverLocation = new Location(world, x, y, z);

        final boolean leverOn = (args[4].trim().equalsIgnoreCase("on") || args[4].trim().equalsIgnoreCase("1"));

        final Block targetBlock = leverLocation.getBlock();

        if (targetBlock.getType() == Material.LEVER)
        {
            BlockState state = targetBlock.getState();
            BlockData data = state.getBlockData();
            Switch caster = (Switch)data;

            caster.setPowered(leverOn);
            state.setBlockData(data);
            state.update();

            plugin.cpb.getCoreProtectAPI().logInteraction(sender.getName(), leverLocation);
        }
        else
        {
            msg("Target block " + targetBlock + "  is not a lever.");
            return true;
        }

        return true;
    }
}
