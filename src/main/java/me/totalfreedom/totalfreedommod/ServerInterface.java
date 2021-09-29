package me.totalfreedom.totalfreedommod;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class ServerInterface extends FreedomService
{
    public static final String COMPILE_NMS_VERSION = "v1_17_R1";

    public static void warnVersion()
    {
        final String nms = FUtil.getNMSVersion();

        if (!COMPILE_NMS_VERSION.equals(nms))
        {
            FLog.warning(TotalFreedomMod.pluginName + " is compiled for " + COMPILE_NMS_VERSION + " but the server is running version " + nms + "!");
            FLog.warning("This might result in unexpected behaviour!");
        }
    }

    @Override
    public void onStart()
    {
    }

    @Override
    public void onStop()
    {
    }

    public int purgeWhitelist()
    {
        Set<OfflinePlayer> whitelisted = Bukkit.getWhitelistedPlayers();
        int size = whitelisted.size();
        for (OfflinePlayer player : Bukkit.getWhitelistedPlayers())
        {
            Bukkit.getServer().getWhitelistedPlayers().remove(player);
        }

        try
        {
            Bukkit.reloadWhitelist();
        }
        catch (Exception ex)
        {
            FLog.warning("Could not purge the whitelist!");
            FLog.warning(ex);
        }
        return size;
    }

    public boolean isWhitelisted()
    {
        return Bukkit.getServer().hasWhitelist();
    }

    public List<?> getWhitelisted()
    {
        return Collections.singletonList(Bukkit.getWhitelistedPlayers());
    }

    public String getVersion()
    {
        return Bukkit.getVersion();
    }
}
