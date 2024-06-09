package bgprotobg.net.customshop;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;

public class PlayerInteract implements Listener {
    private final CustomShop plugin;

    public PlayerInteract(CustomShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block != null && block.getType() == Material.END_STONE) {
                Player player = event.getPlayer();
                Location location = block.getLocation();

                if (plugin.shopInventories.containsKey(location)) {
                    event.setCancelled(true);
                    Inventory shopInventory = plugin.shopInventories.get(location);
                    player.openInventory(shopInventory);
                }
            }
        }
    }
}
