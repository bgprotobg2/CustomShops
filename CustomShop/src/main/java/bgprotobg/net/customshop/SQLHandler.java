package bgprotobg.net.customshop;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.*;
import java.util.*;

public class SQLHandler {

    private final String url = "jdbc:sqlite:plugins/CustomShop/customshop.db";

    private static final String CREATE_SOLD_ITEMS_TABLE = "CREATE TABLE IF NOT EXISTS sold_items (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "player_uuid TEXT NOT NULL," +
            "item_name TEXT NOT NULL," +
            "amount INTEGER NOT NULL," +
            "price REAL NOT NULL," +
            "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP" +
            ");";

    private static final String CREATE_SHOPS_TABLE = "CREATE TABLE IF NOT EXISTS shops (" +
            "player_uuid TEXT NOT NULL," +
            "world_name TEXT NOT NULL," +
            "x INTEGER NOT NULL," +
            "y INTEGER NOT NULL," +
            "z INTEGER NOT NULL," +
            "PRIMARY KEY (player_uuid, world_name, x, y, z)" +
            ");";

    private static final String CREATE_SHOP_ITEMS_TABLE = "CREATE TABLE IF NOT EXISTS shop_items (" +
            "player_uuid TEXT NOT NULL," +
            "world_name TEXT NOT NULL," +
            "x INTEGER NOT NULL," +
            "y INTEGER NOT NULL," +
            "z INTEGER NOT NULL," +
            "item_name TEXT NOT NULL," +
            "amount INTEGER NOT NULL," +
            "price REAL NOT NULL," +
            "PRIMARY KEY (player_uuid, world_name, x, y, z, item_name)" +
            ");";

    public SQLHandler() {
        File dbDir = new File("plugins/CustomShop");
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }

        try (Connection conn = this.connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_SOLD_ITEMS_TABLE);
            stmt.execute(CREATE_SHOPS_TABLE);
            stmt.execute(CREATE_SHOP_ITEMS_TABLE);
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Error creating tables: " + e.getMessage());
        }
    }

    private Connection connect() {
        try {
            return DriverManager.getConnection(url);
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Error connecting to SQLite database: " + e.getMessage());
            return null;
        }
    }

    public void saveShop(UUID playerUUID, Location location, List<ItemStack> items, Map<ItemStack, Double> prices) {
        try (Connection conn = this.connect()) {
            if (conn == null) {
                Bukkit.getLogger().severe("Database connection is null. Cannot save shop.");
                return;
            }

            String insertShopSQL = "INSERT OR REPLACE INTO shops(player_uuid, world_name, x, y, z) VALUES(?, ?, ?, ?, ?)";
            String insertItemSQL = "INSERT OR REPLACE INTO shop_items(player_uuid, world_name, x, y, z, item_name, amount, price) VALUES(?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement pstmtShop = conn.prepareStatement(insertShopSQL);
                 PreparedStatement pstmtItem = conn.prepareStatement(insertItemSQL)) {

                conn.setAutoCommit(false);

                pstmtShop.setString(1, playerUUID.toString());
                pstmtShop.setString(2, location.getWorld().getName());
                pstmtShop.setInt(3, location.getBlockX());
                pstmtShop.setInt(4, location.getBlockY());
                pstmtShop.setInt(5, location.getBlockZ());
                pstmtShop.executeUpdate();

                for (ItemStack item : items) {
                    pstmtItem.setString(1, playerUUID.toString());
                    pstmtItem.setString(2, location.getWorld().getName());
                    pstmtItem.setInt(3, location.getBlockX());
                    pstmtItem.setInt(4, location.getBlockY());
                    pstmtItem.setInt(5, location.getBlockZ());
                    pstmtItem.setString(6, item.getType().name());
                    pstmtItem.setInt(7, item.getAmount());
                    pstmtItem.setDouble(8, prices.get(item));
                    pstmtItem.addBatch();
                }
                pstmtItem.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                if (conn != null) {
                    conn.rollback();
                }
                Bukkit.getLogger().severe("Error saving shop: " + e.getMessage());
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Error in saveShop: " + e.getMessage());
        }
    }

    public void removeShop(UUID playerUUID, Location location) {
        try (Connection conn = this.connect()) {
            if (conn == null) {
                Bukkit.getLogger().severe("Database connection is null. Cannot remove shop.");
                return;
            }

            String deleteShopSQL = "DELETE FROM shops WHERE player_uuid = ? AND world_name = ? AND x = ? AND y = ? AND z = ?";
            String deleteItemsSQL = "DELETE FROM shop_items WHERE player_uuid = ? AND world_name = ? AND x = ? AND y = ? AND z = ?";

            try (PreparedStatement pstmtShop = conn.prepareStatement(deleteShopSQL);
                 PreparedStatement pstmtItem = conn.prepareStatement(deleteItemsSQL)) {

                conn.setAutoCommit(false);

                pstmtShop.setString(1, playerUUID.toString());
                pstmtShop.setString(2, location.getWorld().getName());
                pstmtShop.setInt(3, location.getBlockX());
                pstmtShop.setInt(4, location.getBlockY());
                pstmtShop.setInt(5, location.getBlockZ());
                pstmtShop.executeUpdate();

                pstmtItem.setString(1, playerUUID.toString());
                pstmtItem.setString(2, location.getWorld().getName());
                pstmtItem.setInt(3, location.getBlockX());
                pstmtItem.setInt(4, location.getBlockY());
                pstmtItem.setInt(5, location.getBlockZ());
                pstmtItem.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                if (conn != null) {
                    conn.rollback();
                }
                Bukkit.getLogger().severe("Error removing shop: " + e.getMessage());
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Error in removeShop: " + e.getMessage());
        }
    }

    public Map<UUID, Map<Location, ShopData>> loadAllShops() {
        Map<UUID, Map<Location, ShopData>> allShopsData = new HashMap<>();
        String selectShopsSQL = "SELECT * FROM shops";
        String selectItemsSQL = "SELECT * FROM shop_items WHERE player_uuid = ? AND world_name = ? AND x = ? AND y = ? AND z = ?";

        try (Connection conn = this.connect();
             Statement stmt = conn.createStatement();
             ResultSet rsShops = stmt.executeQuery(selectShopsSQL)) {

            while (rsShops.next()) {
                UUID playerUUID = UUID.fromString(rsShops.getString("player_uuid"));
                String worldName = rsShops.getString("world_name");
                int x = rsShops.getInt("x");
                int y = rsShops.getInt("y");
                int z = rsShops.getInt("z");
                Location location = new Location(Bukkit.getWorld(worldName), x, y, z);

                if (!allShopsData.containsKey(playerUUID)) {
                    allShopsData.put(playerUUID, new HashMap<>());
                }

                List<ItemStack> items = new ArrayList<>();
                Map<ItemStack, Double> prices = new HashMap<>();

                try (PreparedStatement pstmt = conn.prepareStatement(selectItemsSQL)) {
                    pstmt.setString(1, playerUUID.toString());
                    pstmt.setString(2, worldName);
                    pstmt.setInt(3, x);
                    pstmt.setInt(4, y);
                    pstmt.setInt(5, z);
                    try (ResultSet rsItems = pstmt.executeQuery()) {
                        while (rsItems.next()) {
                            String itemName = rsItems.getString("item_name");
                            int amount = rsItems.getInt("amount");
                            double price = rsItems.getDouble("price");

                            ItemStack item = new ItemStack(Material.valueOf(itemName), amount);
                            items.add(item);
                            prices.put(item, price);
                        }
                    }
                }

                ShopData shopData = new ShopData(items, prices);
                allShopsData.get(playerUUID).put(location, shopData);
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Error loading shops: " + e.getMessage());
        }

        return allShopsData;
    }

    public void logSale(UUID playerUUID, ItemStack item, double price) {
        String sql = "INSERT INTO sold_items(player_uuid, item_name, amount, price) VALUES(?, ?, ?, ?)";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, item.getType().name());
            pstmt.setInt(3, item.getAmount());
            pstmt.setDouble(4, price);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Error logging sale: " + e.getMessage());
        }
    }

    public List<String> getLastSoldItems(UUID playerUUID, int limit) {
        List<String> items = new ArrayList<>();
        String sql = "SELECT item_name, amount, price, timestamp FROM sold_items WHERE player_uuid = ? ORDER BY timestamp DESC LIMIT ?";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setInt(2, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String itemName = rs.getString("item_name");
                    int amount = rs.getInt("amount");
                    double price = rs.getDouble("price");
                    Timestamp timestamp = rs.getTimestamp("timestamp");
                    items.add(String.format("%d x %s sold for $%.2f on %s", amount, itemName, price, timestamp.toString()));
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Error retrieving sold items: " + e.getMessage());
        }
        return items;
    }

    public void updateShop(UUID playerUUID, Location shopLocation, List<ItemStack> items, Map<ItemStack, Double> prices) {
        try (Connection conn = this.connect()) {
            if (conn == null) {
                Bukkit.getLogger().severe("Database connection is null. Cannot update shop.");
                return;
            }

            String deleteOldItemsSQL = "DELETE FROM shop_items WHERE player_uuid = ? AND world_name = ? AND x = ? AND y = ? AND z = ?";
            String insertItemSQL = "INSERT OR REPLACE INTO shop_items(player_uuid, world_name, x, y, z, item_name, amount, price) VALUES(?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement pstmtDelete = conn.prepareStatement(deleteOldItemsSQL);
                 PreparedStatement pstmtInsert = conn.prepareStatement(insertItemSQL)) {

                conn.setAutoCommit(false);

                pstmtDelete.setString(1, playerUUID.toString());
                pstmtDelete.setString(2, shopLocation.getWorld().getName());
                pstmtDelete.setInt(3, shopLocation.getBlockX());
                pstmtDelete.setInt(4, shopLocation.getBlockY());
                pstmtDelete.setInt(5, shopLocation.getBlockZ());
                pstmtDelete.executeUpdate();

                for (ItemStack item : items) {
                    pstmtInsert.setString(1, playerUUID.toString());
                    pstmtInsert.setString(2, shopLocation.getWorld().getName());
                    pstmtInsert.setInt(3, shopLocation.getBlockX());
                    pstmtInsert.setInt(4, shopLocation.getBlockY());
                    pstmtInsert.setInt(5, shopLocation.getBlockZ());
                    pstmtInsert.setString(6, item.getType().name());
                    pstmtInsert.setInt(7, item.getAmount());
                    pstmtInsert.setDouble(8, prices.get(item));
                    pstmtInsert.addBatch();
                }
                pstmtInsert.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                if (conn != null) {
                    conn.rollback();
                }
                Bukkit.getLogger().severe("Error updating shop in database: " + e.getMessage());
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Error in updateShop: " + e.getMessage());
        }
    }

    public static class ShopData {
        private final List<ItemStack> items;
        private final Map<ItemStack, Double> prices;

        public ShopData(List<ItemStack> items, Map<ItemStack, Double> prices) {
            this.items = items;
            this.prices = prices;
        }

        public List<ItemStack> getItems() {
            return items;
        }

        public Map<ItemStack, Double> getPrices() {
            return prices;
        }
    }
}
