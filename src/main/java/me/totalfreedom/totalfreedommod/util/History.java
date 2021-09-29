package me.totalfreedom.totalfreedommod.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import me.totalfreedom.totalfreedommod.TotalFreedomMod;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

public class History
{

    public static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void reportHistory(final CommandSender sender, final String username)
    {
        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                UUID uuid = UUIDFetcher.fetch(username);
                if (uuid != null)
                {
                    Gson gson = new GsonBuilder().create();
                    String compactUuid = uuid.toString().replace("-", "");
                    try
                    {
                        //UUIDs or playernames actually work with this one
                        //TODO: fix the stupid api on how it's not working name histories
                        //URL url = new URL("https://api.ashcon.app/mojang/v2/user/" + compactUuid);
                        URL url = new URL("https://api.mojang.com/user/profiles/" + compactUuid + "/names");
                        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                        //conn.setRequestProperty("User-Agent", "");
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        FName[] oldNames = gson.fromJson(reader, FName[].class);
                        if (oldNames == null)
                        {
                            FSync.playerMsg(sender, ChatColor.RED + "Player not found!");
                            return;
                        }
                        reader.close();
                        conn.disconnect();
                        Arrays.sort(oldNames);
                        printHistory(sender, oldNames);
                    }
                    catch (Exception ex)
                    {
                        FSync.playerMsg(sender, ChatColor.RED + "Error, see logs for more details.");
                        FLog.severe(ex);
                    }
                }
                else
                {
                    FSync.playerMsg(sender, ChatColor.RED + "Player not found!");
                }
            }
        }.runTaskAsynchronously(TotalFreedomMod.getPlugin());
    }

    private static void printHistory(CommandSender sender, FName[] oldNames)
    {
        if (oldNames.length == 1)
        {
            FSync.playerMsg(sender, ChatColor.GREEN + oldNames[0].getName() + ChatColor.GOLD + " has never changed their name.");
            return;
        }
        FSync.playerMsg(sender, ChatColor.GOLD + "Original name: " + ChatColor.GREEN + oldNames[0].getName());
        for (int i = 1; i < oldNames.length; i++)
        {
            Date date = new Date(oldNames[i].getChangedToAt());
            String formattedDate = dateFormat.format(date);
            FSync.playerMsg(sender, ChatColor.BLUE + formattedDate + ChatColor.GOLD + " changed to " + ChatColor.GREEN + oldNames[i].getName());
        }
    }

    private static class FName implements Comparable<FName>
    {
        private final String name;
        private final long changedToAt;

        //Added constructor because otherwise there's no way name or changedToAt would have been anything other than null.
        public FName(String name, long changedToAt)
        {
            this.name = name;
            this.changedToAt = changedToAt;
        }

        @Override
        public int compareTo(FName other)
        {
            return Long.compare(this.changedToAt, other.changedToAt);
        }

        public String getName()
        {
            return name;
        }

        public long getChangedToAt()
        {
            return changedToAt;
        }
    }
}