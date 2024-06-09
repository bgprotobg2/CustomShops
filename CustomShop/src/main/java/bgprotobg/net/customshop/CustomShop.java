package bgprotobg.net.customshop;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import java.io.File;
import java.util.*;

public class CustomShop extends JavaPlugin implements Listener, TabExecutor {

    private Economy econ;
    private FileConfiguration config;
    private File configFile;
    protected SQLHandler sqlHandler;
    private EconomySetup economySetup;

    protected final Map<Location, Inventory> shopInventories = new HashMap<>();
    protected final Map<Location, Map<ItemStack, Double>> itemPrices = new HashMap<>();
    protected final Map<UUID, Location> playerShops = new HashMap<>();
    protected Map<Location, Hologram> shopHolograms = new HashMap<>();
    protected final Map<Location, List<ItemStack>> shopItems = new HashMap<>();
    protected final Set<UUID> placedCustomBlocks = new HashSet<>();

    @Override
    public void onEnable() {
        File pluginDir = new File(getDataFolder(), "CustomShop");
        if (!pluginDir.exists()) {
            pluginDir.mkdirs();
        }
        economySetup = new EconomySetup(this);
        if (!economySetup.setupEconomy()) {
            getLogger().severe("Disabled due to no Vault dependency found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        econ = economySetup.getEconomy();
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);
        new Commands(this);
        Economy econ = getServer().getServicesManager().getRegistration(Economy.class).getProvider();
        getServer().getPluginManager().registerEvents(new BlockPlace(this), this);
        getServer().getPluginManager().registerEvents(new BlockBreak(this), this);
        getServer().getPluginManager().registerEvents(new InventoryClick(this, econ), this);
        getServer().getPluginManager().registerEvents(new PlayerInteract(this), this);
        sqlHandler = new SQLHandler();
        loadShopsFromSQL();
        getLogger().info("CustomShop plugin enabled!");
    }

    @Override
    public void onDisable() {
        saveShopsToSQL();
        getLogger().info("CustomShop plugin disabled!");
    }

    private void loadConfig() {
        configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveDefaultConfig();
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void loadShopsFromSQL() {
        Map<UUID, Map<Location, SQLHandler.ShopData>> allShopsData = sqlHandler.loadAllShops();

        for (Map.Entry<UUID, Map<Location, SQLHandler.ShopData>> playerEntry : allShopsData.entrySet()) {
            UUID playerUUID = playerEntry.getKey();
            Map<Location, SQLHandler.ShopData> playerShopsData = playerEntry.getValue();

            for (Map.Entry<Location, SQLHandler.ShopData> shopEntry : playerShopsData.entrySet()) {
                Location location = shopEntry.getKey();
                SQLHandler.ShopData shopData = shopEntry.getValue();

                playerShops.put(playerUUID, location);
                Inventory shopInventory = Bukkit.createInventory(null, 27, ChatColor.DARK_GREEN + "Custom Shop");

                for (ItemStack item : shopData.getItems()) {
                    Double price = shopData.getPrices().get(item);
                    if (price != null) {
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                            lore.add(ChatColor.GREEN + "Price: $" + ChatColor.YELLOW + price);
                            meta.setLore(lore);
                            item.setItemMeta(meta);
                        }
                    }
                    shopInventory.addItem(item);
                }
                shopInventories.put(location, shopInventory);
                shopItems.put(location, shopData.getItems());
                itemPrices.put(location, shopData.getPrices());

                String hologramName = "shop_" + location.getWorld().getName() + "_" + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();
                List<String> lines = Arrays.asList(ChatColor.AQUA + Bukkit.getOfflinePlayer(playerUUID).getName() + "'s Shop", ChatColor.GRAY + "Right click to open");
                Hologram hologram = DHAPI.createHologram(hologramName, location.clone().add(0.5, 2, 0.5), lines);
                shopHolograms.put(location, hologram);
            }
        }
    }

    private void saveShopsToSQL() {
        for (UUID playerUUID : playerShops.keySet()) {
            Location shopLocation = playerShops.get(playerUUID);
            List<ItemStack> items = shopItems.get(shopLocation);
            Map<ItemStack, Double> prices = itemPrices.get(shopLocation);

            sqlHandler.saveShop(playerUUID, shopLocation, items, prices);
        }
    }
}
