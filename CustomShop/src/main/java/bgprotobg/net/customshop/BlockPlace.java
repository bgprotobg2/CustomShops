package bgprotobg.net.customshop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;

import java.util.*;

public class BlockPlace implements Listener {
    private final CustomShop plugin;

    public BlockPlace(CustomShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType() == Material.END_STONE) {
            if (plugin.placedCustomBlocks.contains(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "You can only place one custom shop block at a time.");
                event.setCancelled(true);
                return;
            }
            ItemMeta meta = itemInHand.getItemMeta();
            if (meta != null && meta.hasDisplayName() && meta.hasLore() &&
                    meta.getDisplayName().equals(ChatColor.AQUA + "" + ChatColor.BOLD + "Custom Shop") &&
                    meta.getLore().contains(ChatColor.DARK_GRAY + "Place it to work")) {

                Block block = event.getBlock();
                Location location = block.getLocation();
                plugin.placedCustomBlocks.add(player.getUniqueId());
                plugin.playerShops.put(player.getUniqueId(), location);
                plugin.shopInventories.put(location, Bukkit.createInventory(null, 27, ChatColor.DARK_GREEN + "Custom Shop"));
                player.sendMessage(ChatColor.GREEN + "You have placed your CustomShop");

                String hologramName = player.getName().replaceAll("[^a-zA-Z0-9_-]", "") + "_shop";
                List<String> lines = Arrays.asList(ChatColor.AQUA + player.getName() + "'s Shop", ChatColor.GRAY + "Right click to open");
                Hologram hologram = DHAPI.createHologram(hologramName, location.clone().add(0.5, 2, 0.5), lines);
                plugin.shopHolograms.put(location, hologram);

                plugin.shopItems.put(location, new ArrayList<>());
                plugin.sqlHandler.saveShop(player.getUniqueId(), location, new ArrayList<>(), new HashMap<>());
            }
        }
    }
}
