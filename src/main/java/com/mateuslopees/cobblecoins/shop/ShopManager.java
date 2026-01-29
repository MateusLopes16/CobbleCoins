package com.mateuslopees.cobblecoins.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mateuslopees.cobblecoins.CobbleCoins;
import com.mateuslopees.cobblecoins.config.JsonConfigManager;
import com.mateuslopees.cobblecoins.data.BankAccountManager;
import com.mateuslopees.cobblecoins.network.NetworkHandler;
import com.mateuslopees.cobblecoins.network.packet.OpenShopPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path configPath;
    
    private static final List<ShopItem> buyShop = new ArrayList<>();
    private static final List<ShopItem> sellShop = new ArrayList<>();
    private static final Map<String, Long> sellPrices = new HashMap<>();

    public static void init(MinecraftServer server) {
        // Use the config folder from JsonConfigManager (config/cobblecoins/)
        configPath = JsonConfigManager.getShopConfigPath();
        loadConfig();
    }

    private static void loadConfig() {
        if (configPath == null) {
            createDefaultConfig();
            return;
        }

        if (!Files.exists(configPath)) {
            createDefaultConfig();
            saveConfig();
            return;
        }

        try (Reader reader = Files.newBufferedReader(configPath)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            
            buyShop.clear();
            sellShop.clear();
            sellPrices.clear();
            
            if (json.has("server_buy_shop")) {
                JsonArray categories = json.getAsJsonArray("server_buy_shop");
                for (int i = 0; i < categories.size(); i++) {
                    JsonObject category = categories.get(i).getAsJsonObject();
                    String categoryName = category.has("category") ? category.get("category").getAsString() : "misc";
                    
                    if (category.has("content")) {
                        JsonArray items = category.getAsJsonArray("content");
                        for (int j = 0; j < items.size(); j++) {
                            JsonObject itemObj = items.get(j).getAsJsonObject();
                            ShopItem shopItem = new ShopItem();
                            shopItem.itemId = itemObj.has("item") ? itemObj.get("item").getAsString() : "";
                            shopItem.price = itemObj.has("price") ? itemObj.get("price").getAsLong() : 0;
                            shopItem.category = categoryName;
                            buyShop.add(shopItem);
                        }
                    }
                }
            }
            
            if (json.has("server_sell_shop")) {
                JsonArray categories = json.getAsJsonArray("server_sell_shop");
                for (int i = 0; i < categories.size(); i++) {
                    JsonObject category = categories.get(i).getAsJsonObject();
                    String categoryName = category.has("category") ? category.get("category").getAsString() : "misc";
                    
                    if (category.has("content")) {
                        JsonArray items = category.getAsJsonArray("content");
                        for (int j = 0; j < items.size(); j++) {
                            JsonObject itemObj = items.get(j).getAsJsonObject();
                            ShopItem shopItem = new ShopItem();
                            shopItem.itemId = itemObj.has("item") ? itemObj.get("item").getAsString() : "";
                            shopItem.price = itemObj.has("price") ? itemObj.get("price").getAsLong() : 0;
                            shopItem.category = categoryName;
                            sellShop.add(shopItem);
                            sellPrices.put(shopItem.itemId, shopItem.price);
                        }
                    }
                }
            }
            
            CobbleCoins.LOGGER.info("Loaded shop config: {} buy items, {} sell items", buyShop.size(), sellShop.size());
        } catch (IOException e) {
            CobbleCoins.LOGGER.error("Failed to load shop config", e);
            createDefaultConfig();
        }
    }

    private static void createDefaultConfig() {
        buyShop.clear();
        sellShop.clear();
        sellPrices.clear();
        
        // Default buy items
        buyShop.add(new ShopItem("minecraft:poke_ball", 100, "pokeballs"));
        buyShop.add(new ShopItem("minecraft:great_ball", 300, "pokeballs"));
        buyShop.add(new ShopItem("minecraft:ultra_ball", 800, "pokeballs"));
        buyShop.add(new ShopItem("minecraft:potion", 50, "healing"));
        buyShop.add(new ShopItem("minecraft:super_potion", 150, "healing"));
        buyShop.add(new ShopItem("minecraft:hyper_potion", 400, "healing"));
        
        // Default sell items
        sellShop.add(new ShopItem("minecraft:diamond", 500, "materials"));
        sellShop.add(new ShopItem("minecraft:gold_ingot", 100, "materials"));
        sellShop.add(new ShopItem("minecraft:iron_ingot", 50, "materials"));
        
        for (ShopItem item : sellShop) {
            sellPrices.put(item.itemId, item.price);
        }
    }

    private static void saveConfig() {
        if (configPath == null) return;
        
        try {
            Files.createDirectories(configPath.getParent());
            
            JsonObject json = new JsonObject();
            
            // Group buy items by category
            Map<String, List<ShopItem>> buyByCategory = new HashMap<>();
            for (ShopItem item : buyShop) {
                buyByCategory.computeIfAbsent(item.category, k -> new ArrayList<>()).add(item);
            }
            
            JsonArray buyArray = new JsonArray();
            for (Map.Entry<String, List<ShopItem>> entry : buyByCategory.entrySet()) {
                JsonObject categoryObj = new JsonObject();
                categoryObj.addProperty("category", entry.getKey());
                
                JsonArray content = new JsonArray();
                for (ShopItem item : entry.getValue()) {
                    JsonObject itemObj = new JsonObject();
                    itemObj.addProperty("item", item.itemId);
                    itemObj.addProperty("price", item.price);
                    content.add(itemObj);
                }
                categoryObj.add("content", content);
                buyArray.add(categoryObj);
            }
            json.add("server_buy_shop", buyArray);
            
            // Group sell items by category
            Map<String, List<ShopItem>> sellByCategory = new HashMap<>();
            for (ShopItem item : sellShop) {
                sellByCategory.computeIfAbsent(item.category, k -> new ArrayList<>()).add(item);
            }
            
            JsonArray sellArray = new JsonArray();
            for (Map.Entry<String, List<ShopItem>> entry : sellByCategory.entrySet()) {
                JsonObject categoryObj = new JsonObject();
                categoryObj.addProperty("category", entry.getKey());
                
                JsonArray content = new JsonArray();
                for (ShopItem item : entry.getValue()) {
                    JsonObject itemObj = new JsonObject();
                    itemObj.addProperty("item", item.itemId);
                    itemObj.addProperty("price", item.price);
                    content.add(itemObj);
                }
                categoryObj.add("content", content);
                sellArray.add(categoryObj);
            }
            json.add("server_sell_shop", sellArray);
            
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(json, writer);
            }
        } catch (IOException e) {
            CobbleCoins.LOGGER.error("Failed to save shop config", e);
        }
    }

    public static void openShop(ServerPlayer player) {
        JsonObject shopData = new JsonObject();
        
        JsonArray buyArray = new JsonArray();
        for (ShopItem item : buyShop) {
            JsonObject itemObj = new JsonObject();
            itemObj.addProperty("item", item.itemId);
            itemObj.addProperty("price", item.price);
            itemObj.addProperty("category", item.category);
            buyArray.add(itemObj);
        }
        shopData.add("buy", buyArray);
        
        JsonArray sellArray = new JsonArray();
        for (ShopItem item : sellShop) {
            JsonObject itemObj = new JsonObject();
            itemObj.addProperty("item", item.itemId);
            itemObj.addProperty("price", item.price);
            itemObj.addProperty("category", item.category);
            sellArray.add(itemObj);
        }
        shopData.add("sell", sellArray);
        
        NetworkHandler.sendToPlayer(new OpenShopPacket(GSON.toJson(shopData)), player);
    }

    public static void handlePurchase(ServerPlayer player, String itemId, int amount) {
        // Find the item in buy shop
        ShopItem shopItem = null;
        for (ShopItem item : buyShop) {
            if (item.itemId.equals(itemId)) {
                shopItem = item;
                break;
            }
        }
        
        if (shopItem == null) {
            player.displayClientMessage(
                    Component.translatable("message.cobblecoins.item_not_found")
                            .withStyle(ChatFormatting.RED),
                    false);
            return;
        }
        
        long totalCost = shopItem.price * amount;
        
        if (!BankAccountManager.removeBalance(player.getUUID(), totalCost)) {
            player.displayClientMessage(
                    Component.translatable("message.cobblecoins.insufficient_funds")
                            .withStyle(ChatFormatting.RED),
                    false);
            return;
        }
        
        // Give item to player
        try {
            ResourceLocation itemLoc = ResourceLocation.parse(itemId);
            Item item = BuiltInRegistries.ITEM.get(itemLoc);
            ItemStack stack = new ItemStack(item, amount);
            
            if (!player.getInventory().add(stack)) {
                // Drop item if inventory is full
                player.drop(stack, false);
            }
            
            player.displayClientMessage(
                    Component.translatable("message.cobblecoins.purchase_success", amount, item.getDescription())
                            .withStyle(ChatFormatting.GREEN),
                    false);
        } catch (Exception e) {
            // Refund on error
            BankAccountManager.addBalance(player.getUUID(), totalCost);
            player.displayClientMessage(
                    Component.translatable("message.cobblecoins.purchase_error")
                            .withStyle(ChatFormatting.RED),
                    false);
        }
    }

    public static void handleSell(ServerPlayer player, int slot, int amount) {
        ItemStack stack;
        
        if (slot == -1) {
            // Sell from main hand
            stack = player.getMainHandItem();
        } else {
            stack = player.getInventory().getItem(slot);
        }
        
        if (stack.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.cobblecoins.no_item_to_sell")
                            .withStyle(ChatFormatting.RED),
                    false);
            return;
        }
        
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        Long price = sellPrices.get(itemId);
        
        if (price == null) {
            player.displayClientMessage(
                    Component.translatable("message.cobblecoins.item_not_sellable")
                            .withStyle(ChatFormatting.RED),
                    false);
            return;
        }
        
        int sellAmount = Math.min(amount, stack.getCount());
        long totalPrice = price * sellAmount;
        
        stack.shrink(sellAmount);
        BankAccountManager.addBalance(player.getUUID(), totalPrice);
        
        player.displayClientMessage(
                Component.translatable("message.cobblecoins.sell_success", sellAmount, stack.getItem().getDescription(), totalPrice)
                        .withStyle(ChatFormatting.GREEN),
                false);
    }

    public static class ShopItem {
        public String itemId;
        public long price;
        public String category;

        public ShopItem() {}

        public ShopItem(String itemId, long price, String category) {
            this.itemId = itemId;
            this.price = price;
            this.category = category;
        }
    }
}
