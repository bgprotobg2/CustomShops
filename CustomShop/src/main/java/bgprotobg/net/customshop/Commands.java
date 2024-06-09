package bgprotobg.net.customshop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Commands implements CommandExecutor {

    private final CustomShop plugin;
    private final FileConfiguration config;

    public Commands(CustomShop plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        plugin.getCommand("customshop").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("customshop")) {
            if (args.length < 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /customshop <give|add|list>");
                return true;
            }

            if (args[0].equalsIgnoreCase("give")) {
                if (!sender.hasPermission("customshop.give")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    return true;
                }

                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /customshop give <player> <amount>");
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }

                int amount;
                try {
                    amount = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Amount must be a number.");
                    return true;
                }

                ItemStack endStone = new ItemStack(Material.END_STONE, amount);
                ItemMeta meta = endStone.getItemMeta();

                meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Custom Shop");
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.DARK_GRAY + "Place it to work");
                meta.setLore(lore);
                endStone.setItemMeta(meta);

                target.getInventory().addItem(endStone);
                sender.sendMessage(ChatColor.GREEN + "Gave " + ChatColor.YELLOW + amount + ChatColor.GREEN + " Custom Shop blocks to " + ChatColor.YELLOW + target.getName());
                return true;

            } else if (args[0].equalsIgnoreCase("add")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "This command can only be executed by a player.");
                    return true;
                }

                Player player = (Player) sender;
                ItemStack itemInHand = player.getInventory().getItemInMainHand();
                if (itemInHand.getType() == Material.AIR) {
                    player.sendMessage(ChatColor.RED + "You must be holding an item to add it to your shop.");
                    return true;
                }

                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /customshop add <price>");
                    return true;
                }

                double price;
                try {
                    price = Double.parseDouble(args[1]);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Price must be a number.");
                    return true;
                }
                double maxPrice = config.getDouble("max_price", 100.0);
                if (price > maxPrice) {
                    player.sendMessage(ChatColor.RED + "Price cannot exceed $" + maxPrice);
                    return true;
                }

                UUID playerUUID = player.getUniqueId();
                Location shopLocation = plugin.playerShops.get(playerUUID);
                if (shopLocation == null) {
                    player.sendMessage(ChatColor.RED + "You have not placed a custom shop.");
                    return true;
                }

                Inventory shopInventory = plugin.shopInventories.get(shopLocation);
                if (shopInventory == null) {
                    player.sendMessage(ChatColor.RED + "This block is not a custom shop.");
                    return true;
                }

                ItemMeta meta = itemInHand.getItemMeta();
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                lore.add(ChatColor.GREEN + "Price: $" + ChatColor.YELLOW + price);
                meta.setLore(lore);
                itemInHand.setItemMeta(meta);

                shopInventory.addItem(itemInHand);
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));

                List<ItemStack> items = plugin.shopItems.get(shopLocation);
                if (items == null) {
                    items = new ArrayList<>();
                    plugin.shopItems.put(shopLocation, items);
                }
                items.add(itemInHand);

                plugin.itemPrices.computeIfAbsent(shopLocation, k -> new HashMap<>()).put(itemInHand, price);

                plugin.sqlHandler.updateShop(playerUUID, shopLocation, items, plugin.itemPrices.get(shopLocation));

                player.sendMessage(ChatColor.GREEN + "Added item to your shop for $" + ChatColor.YELLOW + price);
                return true;

            } else if (args[0].equalsIgnoreCase("list")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                    return true;
                }

                Player player = (Player) sender;
                List<String> soldItems = plugin.sqlHandler.getLastSoldItems(player.getUniqueId(), 5);
                if (soldItems.isEmpty()) {
                    player.sendMessage(ChatColor.GREEN + "You have not sold any items yet.");
                } else {
                    player.sendMessage(ChatColor.GREEN + "Last 5 items sold:");
                    for (String item : soldItems) {
                        player.sendMessage(ChatColor.YELLOW + item);
                    }
                }
                return true;
            }
        }
        return false;
    }
}