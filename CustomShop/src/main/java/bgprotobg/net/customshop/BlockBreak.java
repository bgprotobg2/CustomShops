package bgprotobg.net.customshop;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import eu.decentsoftware.holograms.api.holograms.Hologram;

import java.util.*;

public class BlockBreak implements Listener {
    private final CustomShop plugin;

    public BlockBreak(CustomShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (block.getType() == Material.END_STONE) {
            Location location = block.getLocation();
            if (plugin.shopInventories.containsKey(location)) {
                UUID ownerUUID = getShopOwner(location);
                if (ownerUUID == null) {
                    player.sendMessage(ChatColor.RED + "This custom shop does not have an owner.");
                    event.setCancelled(true);
                    return;
                }

                if (!ownerUUID.equals(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "You can only break your own custom shop.");
                    event.setCancelled(true);
                    return;
                }

                Inventory shopInventory = plugin.shopInventories.get(location);
                for (ItemStack item : shopInventory.getContents()) {
                    if (item != null && item.getType() != Material.AIR) {
                        removePriceLore(item);
                        player.getInventory().addItem(item);
                    }
                }
                event.setDropItems(false);

                ItemStack customEndStone = new ItemStack(Material.END_STONE);
                ItemMeta meta = customEndStone.getItemMeta();
                meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Custom Shop");
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.DARK_GRAY + "Place it to work");
                meta.setLore(lore);
                customEndStone.setItemMeta(meta);
                player.getInventory().addItem(customEndStone);

                player.sendMessage(ChatColor.GREEN + "You removed your custom shop.");

                plugin.shopInventories.remove(location);
                plugin.itemPrices.remove(location);
                plugin.playerShops.values().removeIf(loc -> loc.equals(location));

                Hologram hologram = plugin.shopHolograms.remove(location);
                if (hologram != null) {
                    hologram.delete();
                }

                plugin.shopItems.remove(location);

                plugin.sqlHandler.removeShop(ownerUUID, location);
                plugin.placedCustomBlocks.remove(player.getUniqueId());
            }
        }
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
