package bgprotobg.net.customshop;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InventoryClick implements Listener {
    private final CustomShop plugin;
    private final Economy econ;

    public InventoryClick(CustomShop plugin, Economy econ) {
        this.plugin = plugin;
        this.econ = econ;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        Inventory topInventory = event.getView().getTopInventory();

        if (event.getView().getTitle().equals(ChatColor.DARK_GREEN + "Custom Shop")) {
            event.setCancelled(true);

            if (clickedInventory != null && clickedInventory.equals(player.getInventory())) {
                return;
            }

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            Location shopLocation = getShopLocation(topInventory);
            if (shopLocation == null) {
                player.sendMessage(ChatColor.RED + "Could not find the shop location.");
                return;
            }

            UUID ownerUUID = getShopOwner(shopLocation);
            if (ownerUUID != null && ownerUUID.equals(player.getUniqueId())) {
                if (event.getClick().isLeftClick()) {
                    topInventory.removeItem(clickedItem);
                    removePriceLore(clickedItem);
                    player.getInventory().addItem(clickedItem);
                    player.sendMessage(ChatColor.YELLOW + "Removed item from your shop.");

                    List<ItemStack> items = plugin.shopItems.get(shopLocation);
                    Map<ItemStack, Double> prices = plugin.itemPrices.get(shopLocation);

                    if (items != null) {
                        items.remove(clickedItem);
                    }

                    if (prices != null) {
                        prices.remove(clickedItem);
                    }

                    plugin.sqlHandler.updateShop(ownerUUID, shopLocation, items, prices);
                } else {
                    player.sendMessage(ChatColor.RED + "You cannot buy from your own shop.");
                }
                return;
            }

            Map<ItemStack, Double> prices = plugin.itemPrices.get(shopLocation);
            if (prices == null || !prices.containsKey(clickedItem)) {
                player.sendMessage(ChatColor.RED + "Item not found in the shop.");
                return;
            }

            double price = prices.get(clickedItem);
            if (!econ.has(player, price)) {
                player.sendMessage(ChatColor.RED + "You do not have enough money to buy this item.");
                return;
            }

            econ.withdrawPlayer(player, price);
            econ.depositPlayer(Bukkit.getOfflinePlayer(ownerUUID), price);

            topInventory.removeItem(clickedItem);
            removePriceLore(clickedItem);
            player.getInventory().addItem(clickedItem);

            player.sendMessage(ChatColor.GREEN + "You bought " + ChatColor.YELLOW + clickedItem.getType() + ChatColor.GREEN + " for $" + ChatColor.YELLOW + price);
            Player owner = Bukkit.getPlayer(ownerUUID);
            if (owner != null) {
                owner.sendMessage(ChatColor.YELLOW + player.getName() + ChatColor.GREEN + " bought " + ChatColor.YELLOW + clickedItem.getType() + ChatColor.GREEN + " from your shop for $" + ChatColor.YELLOW + price);
            }

            List<ItemStack> items = plugin.shopItems.get(shopLocation);
            if (items != null) {
                items.remove(clickedItem);
            }
            prices.remove(clickedItem);
            plugin.sqlHandler.logSale(ownerUUID, clickedItem, price);

            plugin.sqlHandler.updateShop(ownerUUID, shopLocation, items, prices);
        }
    }

    private Location getShopLocation(Inventory inventory) {
        for (Map.Entry<Location, Inventory> entry : plugin.shopInventories.entrySet()) {
            if (entry.getValue().equals(inventory)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private UUID getShopOwner(Location location) {
        for (Map.Entry<UUID, Location> entry : plugin.playerShops.entrySet()) {
            if (entry.getValue().equals(location)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void removePriceLore(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasLore()) {
            List<String> lore = meta.getLore();
            if (lore != null) {
                lore.removeIf(line -> line.startsWith(ChatColor.GREEN.toString()));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
        }
    }
}
