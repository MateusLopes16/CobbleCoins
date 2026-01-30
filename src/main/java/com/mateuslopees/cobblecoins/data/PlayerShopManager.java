package com.mateuslopees.cobblecoins.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.mateuslopees.cobblecoins.CobbleCoins;
import com.mateuslopees.cobblecoins.config.CobbleCoinsConfig;
import com.mateuslopees.cobblecoins.network.NetworkHandler;
import com.mateuslopees.cobblecoins.network.packet.OpenPlayerShopPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerShopManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, PlayerShop> shops = new ConcurrentHashMap<>();
    private static Path savePath;

    public static void init() {
        CobbleCoins.LOGGER.info("PlayerShopManager initialized");
    }

    public static void setServerPath(MinecraftServer server) {
        savePath = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .resolve("cobblecoins")
                .resolve("player_shops.json");
        loadShops();
    }

    public static PlayerShop getShop(UUID playerId) {
        return shops.get(playerId);
    }
    
    public static PlayerShop getOrCreateShop(UUID playerId) {
        return shops.computeIfAbsent(playerId, id -> new PlayerShop(id, "Shop"));
    }
    
    public static boolean createShop(UUID playerId, String shopName) {
        if (!CobbleCoinsConfig.COMMON.enablePlayerShops.get()) {
            return false;
        }
        if (shops.containsKey(playerId)) {
            return false;
        }
        shops.put(playerId, new PlayerShop(playerId, shopName));
        saveShops();
        return true;
    }
    
    public static Map<UUID, PlayerShop> getAllShops() {
        return new HashMap<>(shops);
    }
    
    /**
     * Opens a player shop GUI for the viewer
     */
    public static void openPlayerShop(ServerPlayer viewer, UUID shopOwnerId) {
        PlayerShop shop = getShop(shopOwnerId);
        if (shop == null) {
            viewer.displayClientMessage(
                    Component.literal("This player doesn't have a shop!")
                            .withStyle(ChatFormatting.RED),
                    false);
            return;
        }
        
        // Get owner name
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        String ownerName = "Unknown";
        if (server != null) {
            ServerPlayer owner = server.getPlayerList().getPlayer(shopOwnerId);
            if (owner != null) {
                ownerName = owner.getName().getString();
            } else {
                // Try to get from game profile cache
                var profile = server.getProfileCache();
                if (profile != null) {
                    var optional = profile.get(shopOwnerId);
                    if (optional.isPresent()) {
                        ownerName = optional.get().getName();
                    }
                }
            }
        }
        
        // Build JSON data for the shop
        JsonObject shopData = new JsonObject();
        shopData.addProperty("ownerId", shopOwnerId.toString());
        shopData.addProperty("ownerName", ownerName);
        shopData.addProperty("shopName", shop.getName());
        shopData.addProperty("isOwnShop", viewer.getUUID().equals(shopOwnerId));
        
        JsonArray listingsArray = new JsonArray();
        for (ShopListing listing : shop.getListings()) {
            JsonObject listingJson = new JsonObject();
            listingJson.addProperty("id", listing.getId());
            listingJson.addProperty("itemId", listing.getItemId());
            listingJson.addProperty("amount", listing.getAmount());
            listingJson.addProperty("price", listing.getPrice());
            listingJson.addProperty("sellerId", listing.getSellerId().toString());
            listingJson.addProperty("sellerName", ownerName);
            listingsArray.add(listingJson);
        }
        shopData.add("listings", listingsArray);
        
        NetworkHandler.sendToPlayer(new OpenPlayerShopPacket(GSON.toJson(shopData)), viewer);
    }
    
    /**
     * Opens the viewer's own shop
     */
    public static void openOwnShop(ServerPlayer player) {
        PlayerShop shop = getShop(player.getUUID());
        if (shop == null) {
            player.displayClientMessage(
                    Component.literal("You don't have a shop! Use /playershop create <name>")
                            .withStyle(ChatFormatting.RED),
                    false);
            return;
        }
        openPlayerShop(player, player.getUUID());
    }

    public static void listItem(ServerPlayer player, String itemId, int amount, long price) {
        if (!CobbleCoinsConfig.COMMON.enablePlayerShops.get()) {
            player.displayClientMessage(
                    Component.translatable("message.cobblecoins.player_shops_disabled")
                            .withStyle(ChatFormatting.RED),
                    false);
            return;
        }

        PlayerShop shop = getShop(player.getUUID());
        
        if (shop.getListings().size() >= CobbleCoinsConfig.COMMON.maxPlayerShopSlots.get()) {
            player.displayClientMessage(
                    Component.translatable("message.cobblecoins.shop_full")
                            .withStyle(ChatFormatting.RED),
                    false);
            return;
        }

        // Check if player has the item
        int foundAmount = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(itemId)) {
                foundAmount += stack.getCount();
            }
        }

        if (foundAmount < amount) {
            player.displayClientMessage(
                    Component.translatable("message.cobblecoins.not_enough_items")
                            .withStyle(ChatFormatting.RED),
                    false);
            return;
        }

        // Remove items from inventory
        int remaining = amount;
        for (int i = 0; i < player.getInventory().items.size() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(itemId)) {
                int toRemove = Math.min(stack.getCount(), remaining);
                stack.shrink(toRemove);
                remaining -= toRemove;
            }
        }

        // Add listing
        ShopListing listing = new ShopListing(itemId, amount, price, player.getUUID());
        shop.addListing(listing);
        saveShops();

        player.displayClientMessage(
                Component.translatable("message.cobblecoins.item_listed", amount, itemId, price)
                        .withStyle(ChatFormatting.GREEN),
                false);
    }

    public static void removeItem(ServerPlayer player, String itemId) {
        PlayerShop shop = getShop(player.getUUID());
        ShopListing listing = shop.removeListing(itemId);
        
        if (listing == null) {
            player.displayClientMessage(
                    Component.translatable("message.cobblecoins.listing_not_found")
                            .withStyle(ChatFormatting.RED),
                    false);
            return;
        }

        // Return items to player
        try {
            ResourceLocation itemLoc = ResourceLocation.parse(listing.getItemId());
            Item item = BuiltInRegistries.ITEM.get(itemLoc);
            ItemStack stack = new ItemStack(item, listing.getAmount());
            
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            
            player.displayClientMessage(
                    Component.translatable("message.cobblecoins.listing_removed")
                            .withStyle(ChatFormatting.GREEN),
                    false);
        } catch (Exception e) {
            CobbleCoins.LOGGER.error("Error returning items to player", e);
        }
        
        saveShops();
    }

    public static void buyFromPlayer(ServerPlayer buyer, String listingId, int amount) {
        // Find the listing across all shops
        ShopListing targetListing = null;
        PlayerShop sellerShop = null;
        
        for (PlayerShop shop : shops.values()) {
            for (ShopListing listing : shop.getListings()) {
                if (listing.getId().equals(listingId)) {
                    targetListing = listing;
                    sellerShop = shop;
                    break;
                }
            }
            if (targetListing != null) break;
        }

        if (targetListing == null) {
            buyer.displayClientMessage(
                    Component.translatable("message.cobblecoins.listing_not_found")
                            .withStyle(ChatFormatting.RED),
                    false);
            return;
        }

        if (targetListing.getAmount() < amount) {
            buyer.displayClientMessage(
                    Component.translatable("message.cobblecoins.not_enough_stock")
                            .withStyle(ChatFormatting.RED),
                    false);
            return;
        }

        long totalCost = targetListing.getPrice() * amount;
        
        if (!BankAccountManager.removeBalance(buyer.getUUID(), totalCost)) {
            buyer.displayClientMessage(
                    Component.translatable("message.cobblecoins.insufficient_funds")
                            .withStyle(ChatFormatting.RED),
                    false);
            return;
        }

        // Calculate tax
        double taxRate = CobbleCoinsConfig.COMMON.playerShopTaxRate.get();
        long tax = (long) (totalCost * taxRate);
        long sellerProfit = totalCost - tax;

        // Pay seller
        BankAccountManager.addBalance(targetListing.getSellerId(), sellerProfit);

        // Give items to buyer
        try {
            ResourceLocation itemLoc = ResourceLocation.parse(targetListing.getItemId());
            Item item = BuiltInRegistries.ITEM.get(itemLoc);
            ItemStack stack = new ItemStack(item, amount);
            
            if (!buyer.getInventory().add(stack)) {
                buyer.drop(stack, false);
            }
        } catch (Exception e) {
            // Refund on error
            BankAccountManager.addBalance(buyer.getUUID(), totalCost);
            BankAccountManager.removeBalance(targetListing.getSellerId(), sellerProfit);
            buyer.displayClientMessage(
                    Component.translatable("message.cobblecoins.purchase_error")
                            .withStyle(ChatFormatting.RED),
                    false);
            return;
        }

        // Update listing
        targetListing.reduceAmount(amount);
        if (targetListing.getAmount() <= 0) {
            sellerShop.removeListing(targetListing.getItemId());
        }
        
        saveShops();

        buyer.displayClientMessage(
                Component.translatable("message.cobblecoins.player_purchase_success", amount, targetListing.getItemId())
                        .withStyle(ChatFormatting.GREEN),
                false);

        // Notify seller if online
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ServerPlayer seller = server.getPlayerList().getPlayer(targetListing.getSellerId());
            if (seller != null) {
                seller.displayClientMessage(
                        Component.translatable("message.cobblecoins.item_sold", buyer.getName().getString(), amount, targetListing.getItemId(), sellerProfit)
                                .withStyle(ChatFormatting.GOLD),
                        false);
            }
        }
    }

    public static List<ShopListing> getAllListings() {
        List<ShopListing> allListings = new ArrayList<>();
        for (PlayerShop shop : shops.values()) {
            allListings.addAll(shop.getListings());
        }
        return allListings;
    }

    private static void loadShops() {
        if (savePath == null || !Files.exists(savePath)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(savePath)) {
            Type type = new TypeToken<Map<String, PlayerShop>>() {}.getType();
            Map<String, PlayerShop> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                shops.clear();
                loaded.forEach((key, value) -> {
                    try {
                        UUID uuid = UUID.fromString(key);
                        value.setOwnerId(uuid);
                        shops.put(uuid, value);
                    } catch (IllegalArgumentException e) {
                        CobbleCoins.LOGGER.warn("Invalid UUID in player shops: {}", key);
                    }
                });
                CobbleCoins.LOGGER.info("Loaded {} player shops", shops.size());
            }
        } catch (IOException e) {
            CobbleCoins.LOGGER.error("Failed to load player shops", e);
        }
    }

    public static void saveShops() {
        if (savePath == null) {
            return;
        }

        try {
            Files.createDirectories(savePath.getParent());
            try (Writer writer = Files.newBufferedWriter(savePath)) {
                Map<String, PlayerShop> toSave = new ConcurrentHashMap<>();
                shops.forEach((uuid, shop) -> toSave.put(uuid.toString(), shop));
                GSON.toJson(toSave, writer);
            }
        } catch (IOException e) {
            CobbleCoins.LOGGER.error("Failed to save player shops", e);
        }
    }

    public static class PlayerShop {
        private transient UUID ownerId;
        private String name;
        private final List<ShopListing> listings = new ArrayList<>();

        public PlayerShop() {
            this.name = "Shop";
        }

        public PlayerShop(UUID ownerId) {
            this.ownerId = ownerId;
            this.name = "Shop";
        }
        
        public PlayerShop(UUID ownerId, String name) {
            this.ownerId = ownerId;
            this.name = name;
        }

        public UUID getOwnerId() {
            return ownerId;
        }

        public void setOwnerId(UUID ownerId) {
            this.ownerId = ownerId;
        }
        
        public String getName() {
            return name != null ? name : "Shop";
        }
        
        public void setName(String name) {
            this.name = name;
        }

        public List<ShopListing> getListings() {
            return listings;
        }

        public void addListing(ShopListing listing) {
            listings.add(listing);
        }
        
        public void addListing(String itemId, int amount, long price) {
            listings.add(new ShopListing(itemId, amount, price, ownerId));
        }

        public ShopListing removeListing(String itemId) {
            for (int i = 0; i < listings.size(); i++) {
                if (listings.get(i).getItemId().equals(itemId)) {
                    return listings.remove(i);
                }
            }
            return null;
        }
    }

    public static class ShopListing {
        private String id;
        private String itemId;
        private int amount;
        private long price;
        private UUID sellerId;

        public ShopListing() {
            this.id = UUID.randomUUID().toString();
        }

        public ShopListing(String itemId, int amount, long price, UUID sellerId) {
            this.id = UUID.randomUUID().toString();
            this.itemId = itemId;
            this.amount = amount;
            this.price = price;
            this.sellerId = sellerId;
        }

        public String getId() { return id; }
        public String getItemId() { return itemId; }
        public int getAmount() { return amount; }
        public int getQuantity() { return amount; }  // Alias for getAmount
        public long getPrice() { return price; }
        public UUID getSellerId() { return sellerId; }
        
        public void reduceAmount(int amount) {
            this.amount -= amount;
        }
    }
}
