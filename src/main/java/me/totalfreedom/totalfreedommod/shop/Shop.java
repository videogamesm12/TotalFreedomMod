package me.totalfreedom.totalfreedommod.shop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import me.totalfreedom.totalfreedommod.FreedomService;
import me.totalfreedom.totalfreedommod.config.ConfigEntry;
import me.totalfreedom.totalfreedommod.player.PlayerData;
import me.totalfreedom.totalfreedommod.util.FLog;
import me.totalfreedom.totalfreedommod.util.FUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class Shop extends FreedomService
{
    public final int coinsPerReactionWin = ConfigEntry.SHOP_REACTIONS_COINS_PER_WIN.getInteger();
    public final String prefix = ChatColor.DARK_GRAY + "[" + ChatColor.YELLOW + "Reaction" + ChatColor.DARK_GRAY + "] ";
    private final String LOGIN_MESSAGE_GUI_TITLE = ChatColor.DARK_GREEN + ChatColor.BOLD.toString() + "Login Messages";
    public String reactionString = "";
    public Date reactionStartTime;
    public BukkitTask countdownTask;
    private BukkitTask reactions;
    private BossBar countdownBar = null;

    @Override
    public void onStart()
    {
        if (ConfigEntry.SHOP_REACTIONS_ENABLED.getBoolean())
        {
            startReactionTimer();
        }
    }

    public void startReactionTimer()
    {
        long interval = ConfigEntry.SHOP_REACTIONS_INTERVAL.getInteger() * 20L;

        reactions = new BukkitRunnable()
        {

            @Override
            public void run()
            {
                startReaction();
            }
        }.runTaskLater(plugin, interval);
    }

    public void forceStartReaction()
    {
        reactions.cancel();
        startReaction();
    }

    public void startReaction()
    {
        if (!ConfigEntry.SHOP_ENABLED.getBoolean())
        {
            FLog.debug("The shop is not enabled, therefore a reaction did not start.");
            return;
        }

        reactionString = FUtil.randomAlphanumericString(ConfigEntry.SHOP_REACTIONS_STRING_LENGTH.getInteger());

        FUtil.bcastMsg(prefix + ChatColor.AQUA + "Enter the code above to win " + ChatColor.GOLD + coinsPerReactionWin + ChatColor.AQUA + " coins!", false);

        reactionStartTime = new Date();

        countdownBar = server.createBossBar(reactionString, BarColor.GREEN, BarStyle.SOLID);
        for (Player player : server.getOnlinePlayers())
        {
            countdownBar.addPlayer(player);
        }
        countdownBar.setVisible(true);
        countdownTask = new BukkitRunnable()
        {
            double seconds = 30;
            final double max = seconds;

            @Override
            public void run()
            {
                if ((seconds -= 1) == 0)
                {
                    endReaction(null);
                }
                else
                {
                    countdownBar.setProgress(seconds / max);
                    if (!countdownBar.getColor().equals(BarColor.YELLOW) && seconds / max <= 0.25)
                    {
                        countdownBar.setColor(BarColor.YELLOW);
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    public void endReaction(String winner)
    {
        countdownTask.cancel();
        countdownBar.removeAll();
        countdownBar = null;
        reactionString = "";

        if (winner != null)
        {
            Date currentTime = new Date();
            long seconds = (currentTime.getTime() - reactionStartTime.getTime()) / 1000;
            FUtil.bcastMsg(prefix + ChatColor.GREEN + winner + ChatColor.AQUA + " won in " + seconds + " seconds!", false);
            startReactionTimer();
            return;
        }

        FUtil.bcastMsg(prefix + ChatColor.RED + "No one reacted fast enough", false);
        startReactionTimer();
    }

    @Override
    public void onStop()
    {
        if (ConfigEntry.SHOP_REACTIONS_ENABLED.getBoolean())
        {
            reactions.cancel();
        }
    }

    public String getShopPrefix()
    {
        return FUtil.colorize(ConfigEntry.SHOP_PREFIX.getString());
    }

    public String getShopTitle()
    {
        return FUtil.colorize(ConfigEntry.SHOP_TITLE.getString());
    }

    public Inventory generateShopGUI(PlayerData playerData)
    {
        Inventory gui = server.createInventory(null, 36, getShopTitle());
        for (int slot = 0; slot < 36; slot++)
        {
            ItemStack blank = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
            ItemMeta meta = blank.getItemMeta();
            assert meta != null;
            meta.setDisplayName(" ");
            blank.setItemMeta(meta);
            gui.setItem(slot, blank);
        }
        for (ShopItem shopItem : ShopItem.values())
        {
            ItemStack item = shopGUIItem(shopItem, playerData);
            gui.setItem(shopItem.getSlot(), item);
        }
        // Coins
        ItemStack coins = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta meta = coins.getItemMeta();
        assert meta != null;
        meta.setDisplayName(FUtil.colorize("&c&lYou have &e&l" + playerData.getCoins() + "&c&l coins"));
        coins.setItemMeta(meta);
        gui.setItem(35, coins);
        return gui;
    }

    public Inventory generateLoginMessageGUI(Player player)
    {
        Inventory gui = server.createInventory(null, 36, LOGIN_MESSAGE_GUI_TITLE);
        int slot = 0;
        for (String loginMessage : ConfigEntry.SHOP_LOGIN_MESSAGES.getStringList())
        {
            ItemStack icon = new ItemStack(Material.NAME_TAG);
            ItemMeta meta = icon.getItemMeta();
            assert meta != null;
            meta.setDisplayName(FUtil.colorize(plugin.rm.craftLoginMessage(player, loginMessage)));
            icon.setItemMeta(meta);
            gui.setItem(slot, icon);
            slot++;
        }
        ItemStack clear = new ItemStack(Material.BARRIER);
        ItemMeta meta = clear.getItemMeta();
        assert meta != null;
        meta.setDisplayName(ChatColor.RED + "Clear login message");
        clear.setItemMeta(meta);
        gui.setItem(35, clear);
        return gui;
    }

    public boolean isRealItem(PlayerData data, ShopItem shopItem, PlayerInventory inventory, ItemStack realItem)
    {
        return isRealItem(data, shopItem, inventory.getItemInMainHand(), realItem) || isRealItem(data, shopItem, inventory.getItemInOffHand(), realItem);
    }

    public boolean isRealItem(PlayerData data, ShopItem shopItem, ItemStack givenItem, ItemStack realItem)
    {
        if (!data.hasItem(shopItem) || !givenItem.getType().equals(realItem.getType()))
        {
            return false;
        }

        ItemMeta givenMeta = givenItem.getItemMeta();
        ItemMeta realMeta = realItem.getItemMeta();

        assert givenMeta != null;
        assert realMeta != null;
        return givenMeta.getDisplayName().equals(realMeta.getDisplayName()) && Objects.equals(givenMeta.getLore(), realMeta.getLore());
    }

    public ItemStack getLightningRod()
    {
        ItemStack itemStack = new ItemStack(Material.BLAZE_ROD);
        ItemMeta itemMeta = itemStack.getItemMeta();
        assert itemMeta != null;
        itemMeta.setDisplayName(FUtil.colorize("&bL&3i&bg&3h&bt&3n&bi&3n&bg &3R&bo&3d"));
        itemMeta.setLore(Arrays.asList(ChatColor.AQUA + "Strike others down with the power of lightning.", ChatColor.RED + ChatColor.ITALIC.toString() + "The classic way to exterminate annoyances."));
        itemMeta.addEnchant(Enchantment.CHANNELING, 1, false);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public ItemStack getGrapplingHook()
    {
        ItemStack itemStack = new ItemStack(Material.FISHING_ROD);
        ItemMeta itemMeta = itemStack.getItemMeta();
        assert itemMeta != null;
        itemMeta.setDisplayName(ChatColor.YELLOW + "Grappling Hook");
        itemMeta.setLore(Collections.singletonList(ChatColor.GREEN + "be spider-man but ghetto"));
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public ItemStack getFireBall()
    {
        ItemStack itemStack = new ItemStack(Material.FIRE_CHARGE);
        ItemMeta itemMeta = itemStack.getItemMeta();
        assert itemMeta != null;
        itemMeta.setDisplayName(ChatColor.RED + "Fire Ball");
        itemMeta.setLore(Collections.singletonList(ChatColor.GOLD + "Yeet this at people"));
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public ItemStack getRideablePearl()
    {
        ItemStack itemStack = new ItemStack(Material.ENDER_PEARL);
        ItemMeta itemMeta = itemStack.getItemMeta();
        assert itemMeta != null;
        itemMeta.setDisplayName(ChatColor.DARK_PURPLE + "Rideable Ender Pearl");
        itemMeta.setLore(Arrays.asList(ChatColor.LIGHT_PURPLE + "What the title says.", "", ChatColor.WHITE + ChatColor.ITALIC.toString() + "TotalFreedom is not responsible for any injuries", ChatColor.WHITE + ChatColor.ITALIC.toString() + "sustained while using this item."));
        itemMeta.addEnchant(Enchantment.BINDING_CURSE, 1, false);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public ItemStack getStackingPotato()
    {
        ItemStack itemStack = new ItemStack(Material.POTATO);
        ItemMeta itemMeta = itemStack.getItemMeta();
        assert itemMeta != null;
        itemMeta.setDisplayName(ChatColor.YELLOW + "Stacking Potato");
        itemMeta.setLore(Collections.singletonList(ChatColor.GREEN + "Left click to ride a mob, right click to put a mob on your head."));
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public ItemStack getClownFish()
    {
        ItemStack itemStack = new ItemStack(Material.TROPICAL_FISH);
        ItemMeta itemMeta = itemStack.getItemMeta();
        assert itemMeta != null;
        itemMeta.setDisplayName(ChatColor.GOLD + "Clown Fish");
        itemMeta.setLore(Collections.singletonList(ChatColor.AQUA + ":clown:"));
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public boolean canAfford(int price, int coins)
    {
        return coins >= price;
    }

    public int amountNeeded(int price, int coins)
    {
        return price - coins;
    }

    public ItemStack shopGUIItem(ShopItem item, PlayerData data)
    {
        ItemStack itemStack = new ItemStack(item.getIcon());
        ItemMeta itemMeta = itemStack.getItemMeta();
        assert itemMeta != null;
        itemMeta.setDisplayName(item.getColoredName());
        int price = item.getCost();
        int coins = data.getCoins();
        boolean canAfford = canAfford(price, coins);
        List<String> lore = new ArrayList<>();
        if (!data.hasItem(item))
        {
            lore.add(ChatColor.GOLD + "Price: " + (canAfford ? ChatColor.DARK_GREEN : ChatColor.RED) + price);
            if (!canAfford)
            {
                lore.add(ChatColor.RED + "You can not afford this item!");
                lore.add(ChatColor.RED + "You need " + amountNeeded(price, coins) + " more coins to buy this item.");
            }
        }
        else
        {
            lore.add(ChatColor.RED + "You already purchased this item.");
        }
        itemMeta.setLore(lore);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onShopGUIClick(InventoryClickEvent event)
    {
        if (!(event.getWhoClicked() instanceof Player))
        {
            return;
        }

        Inventory inventory = event.getInventory();
        if (inventory.getSize() != 36 || !event.getView().getTitle().equals(getShopTitle()))
        {
            return;
        }
        event.setCancelled(true);

        ShopItem shopItem = getShopItem(event.getSlot());
        if (shopItem == null)
        {
            return;
        }

        Player player = (Player)event.getWhoClicked();
        PlayerData playerData = plugin.pl.getData(player);
        int price = shopItem.getCost();
        int coins = playerData.getCoins();

        if (playerData.hasItem(shopItem) || !canAfford(price, coins))
        {
            return;
        }

        playerData.giveItem(shopItem);
        playerData.setCoins(coins - price);
        plugin.pl.save(playerData);

        player.closeInventory();

        player.sendMessage(getShopPrefix() + " " + ChatColor.GREEN + "Successfully purchased the \"" + shopItem.getColoredName() + ChatColor.GREEN + "\" for " + ChatColor.GOLD + price + ChatColor.GREEN + "!");

        if (shopItem.getCommand() != null)
        {
            player.sendMessage(ChatColor.GREEN + "Run " + shopItem.getCommand() + " to get one!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLoginMessageGUIClick(InventoryClickEvent event)
    {
        if (!(event.getWhoClicked() instanceof Player))
        {
            return;
        }

        Inventory inventory = event.getInventory();
        if (inventory.getSize() != 36 || !event.getView().getTitle().equals(LOGIN_MESSAGE_GUI_TITLE))
        {
            return;
        }
        event.setCancelled(true);

        int slot = event.getSlot();

        Player player = (Player)event.getWhoClicked();
        PlayerData data = plugin.pl.getData(player);

        if (slot == 35)
        {
            data.setLoginMessage(null);
            plugin.pl.save(data);
            player.sendMessage(ChatColor.GREEN + "Removed your login message");
        }
        else
        {
            String message = ConfigEntry.SHOP_LOGIN_MESSAGES.getStringList().get(slot);
            data.setLoginMessage(message);
            plugin.pl.save(data);
            player.sendMessage(ChatColor.GREEN + "Your login message is now the following:\n" + plugin.rm.craftLoginMessage(player, message));
        }

        player.closeInventory();

    }

    public ShopItem getShopItem(int slot)
    {
        for (ShopItem shopItem : ShopItem.values())
        {
            if (shopItem.getSlot() == slot)
            {
                return shopItem;
            }
        }
        return null;
    }
}