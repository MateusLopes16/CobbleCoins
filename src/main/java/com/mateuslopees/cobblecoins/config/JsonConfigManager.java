package com.mateuslopees.cobblecoins.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mateuslopees.cobblecoins.CobbleCoins;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Manages JSON configuration files for CobbleCoins.
 * Creates and manages config files in the config/cobblecoins folder.
 */
public class JsonConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path configFolder;
    private static Path shopConfigPath;
    private static Path rewardsConfigPath;
    
    // Rewards configuration values
    private static int rewardOnCatch = 10;
    private static int rewardOnPokedexEntry = 100;
    private static int rewardOn50Capture = 50;
    private static int rewardOn100Capture = 100;
    private static int rewardOn50PokedexEntries = 500;
    private static int rewardOn100PokedexEntries = 1000;
    private static double rewardOnPokemonDeathMultiplier = 1.5;
    private static int baseRewardOnPokemonDeath = 0;
    private static boolean isMoneyBoxVisible = true;
    
    /**
     * Initialize the JSON config manager.
     * Creates the config folder and default config files if they don't exist.
     */
    public static void init() {
        configFolder = FMLPaths.CONFIGDIR.get().resolve("cobblecoins");
        shopConfigPath = configFolder.resolve("shop_config.json");
        rewardsConfigPath = configFolder.resolve("rewards_config.json");
        
        try {
            // Create config folder if it doesn't exist
            if (!Files.exists(configFolder)) {
                Files.createDirectories(configFolder);
                CobbleCoins.LOGGER.info("Created CobbleCoins config folder at: {}", configFolder);
            }
            
            // Create default shop config if it doesn't exist
            if (!Files.exists(shopConfigPath)) {
                createDefaultShopConfig();
                CobbleCoins.LOGGER.info("Created default shop config at: {}", shopConfigPath);
            }
            
            // Create default rewards config if it doesn't exist
            if (!Files.exists(rewardsConfigPath)) {
                createDefaultRewardsConfig();
                CobbleCoins.LOGGER.info("Created default rewards config at: {}", rewardsConfigPath);
            }
            
            // Load rewards config
            loadRewardsConfig();
            
        } catch (IOException e) {
            CobbleCoins.LOGGER.error("Failed to initialize JSON config manager", e);
        }
    }
    
    /**
     * Creates the default shop configuration file.
     */
    private static void createDefaultShopConfig() throws IOException {
        JsonObject config = new JsonObject();
        
        // Server buy shop
        JsonArray buyShop = new JsonArray();
        
        // Pokeballs category
        JsonObject pokeballsCategory = new JsonObject();
        pokeballsCategory.addProperty("category", "pokeballs");
        JsonArray pokeballsContent = new JsonArray();
        
        JsonObject pokeball = new JsonObject();
        pokeball.addProperty("item", "cobblemon:poke_ball");
        pokeball.addProperty("price", 100);
        pokeball.addProperty("currency", "cobblecoins:cobblecoin");
        pokeballsContent.add(pokeball);
        
        JsonObject greatball = new JsonObject();
        greatball.addProperty("item", "cobblemon:great_ball");
        greatball.addProperty("price", 300);
        greatball.addProperty("currency", "cobblecoins:cobblecoin");
        pokeballsContent.add(greatball);
        
        JsonObject ultraball = new JsonObject();
        ultraball.addProperty("item", "cobblemon:ultra_ball");
        ultraball.addProperty("price", 800);
        ultraball.addProperty("currency", "cobblecoins:cobblecoin");
        pokeballsContent.add(ultraball);
        
        pokeballsCategory.add("content", pokeballsContent);
        buyShop.add(pokeballsCategory);
        
        // Healing category
        JsonObject healingCategory = new JsonObject();
        healingCategory.addProperty("category", "healing");
        JsonArray healingContent = new JsonArray();
        
        JsonObject potion = new JsonObject();
        potion.addProperty("item", "cobblemon:potion");
        potion.addProperty("price", 50);
        potion.addProperty("currency", "cobblecoins:cobblecoin");
        healingContent.add(potion);
        
        JsonObject superPotion = new JsonObject();
        superPotion.addProperty("item", "cobblemon:super_potion");
        superPotion.addProperty("price", 150);
        superPotion.addProperty("currency", "cobblecoins:cobblecoin");
        healingContent.add(superPotion);
        
        JsonObject hyperPotion = new JsonObject();
        hyperPotion.addProperty("item", "cobblemon:hyper_potion");
        hyperPotion.addProperty("price", 400);
        hyperPotion.addProperty("currency", "cobblecoins:cobblecoin");
        healingContent.add(hyperPotion);
        
        healingCategory.add("content", healingContent);
        buyShop.add(healingCategory);
        
        config.add("server_buy_shop", buyShop);
        
        // Server sell shop
        JsonArray sellShop = new JsonArray();
        
        // Materials category
        JsonObject materialsCategory = new JsonObject();
        materialsCategory.addProperty("category", "materials");
        JsonArray materialsContent = new JsonArray();
        
        JsonObject diamond = new JsonObject();
        diamond.addProperty("item", "minecraft:diamond");
        diamond.addProperty("price", 500);
        diamond.addProperty("currency", "cobblecoins:cobblecoin");
        materialsContent.add(diamond);
        
        JsonObject gold = new JsonObject();
        gold.addProperty("item", "minecraft:gold_ingot");
        gold.addProperty("price", 100);
        gold.addProperty("currency", "cobblecoins:cobblecoin");
        materialsContent.add(gold);
        
        JsonObject iron = new JsonObject();
        iron.addProperty("item", "minecraft:iron_ingot");
        iron.addProperty("price", 50);
        iron.addProperty("currency", "cobblecoins:cobblecoin");
        materialsContent.add(iron);
        
        materialsCategory.add("content", materialsContent);
        sellShop.add(materialsCategory);
        
        config.add("server_sell_shop", sellShop);
        
        // Write to file
        try (Writer writer = Files.newBufferedWriter(shopConfigPath)) {
            GSON.toJson(config, writer);
        }
    }
    
    /**
     * Creates the default rewards configuration file.
     */
    private static void createDefaultRewardsConfig() throws IOException {
        JsonObject config = new JsonObject();
        
        config.addProperty("rewardoncatch", 10);
        config.addProperty("rewardonpokedexentry", 100);
        config.addProperty("rewardon50capture", 50);
        config.addProperty("rewardon100capture", 100);
        config.addProperty("rewardon50pokedexentries", 500);
        config.addProperty("rewardon100pokedexentries", 1000);
        config.addProperty("rewardonpokemondeathmultiplyer", 1.5);
        config.addProperty("baserewardonpokemondeath", 0);
        config.addProperty("ismoneyboxvisible", true);
        
        // Write to file
        try (Writer writer = Files.newBufferedWriter(rewardsConfigPath)) {
            GSON.toJson(config, writer);
        }
    }
    
    /**
     * Loads the rewards configuration from the JSON file.
     */
    public static void loadRewardsConfig() {
        if (rewardsConfigPath == null || !Files.exists(rewardsConfigPath)) {
            CobbleCoins.LOGGER.warn("Rewards config not found, using defaults");
            return;
        }
        
        try (Reader reader = Files.newBufferedReader(rewardsConfigPath)) {
            JsonObject config = GSON.fromJson(reader, JsonObject.class);
            
            if (config.has("rewardoncatch")) {
                rewardOnCatch = config.get("rewardoncatch").getAsInt();
            }
            if (config.has("rewardonpokedexentry")) {
                rewardOnPokedexEntry = config.get("rewardonpokedexentry").getAsInt();
            }
            if (config.has("rewardon50capture")) {
                rewardOn50Capture = config.get("rewardon50capture").getAsInt();
            }
            if (config.has("rewardon100capture")) {
                rewardOn100Capture = config.get("rewardon100capture").getAsInt();
            }
            if (config.has("rewardon50pokedexentries")) {
                rewardOn50PokedexEntries = config.get("rewardon50pokedexentries").getAsInt();
            }
            if (config.has("rewardon100pokedexentries")) {
                rewardOn100PokedexEntries = config.get("rewardon100pokedexentries").getAsInt();
            }
            if (config.has("rewardonpokemondeathmultiplyer")) {
                rewardOnPokemonDeathMultiplier = config.get("rewardonpokemondeathmultiplyer").getAsDouble();
            }
            if (config.has("baserewardonpokemondeath")) {
                baseRewardOnPokemonDeath = config.get("baserewardonpokemondeath").getAsInt();
            }
            if (config.has("ismoneyboxvisible")) {
                isMoneyBoxVisible = config.get("ismoneyboxvisible").getAsBoolean();
            }
            
            CobbleCoins.LOGGER.info("Loaded rewards config from JSON");
        } catch (IOException e) {
            CobbleCoins.LOGGER.error("Failed to load rewards config", e);
        }
    }
    
    /**
     * Saves the current rewards configuration to the JSON file.
     */
    public static void saveRewardsConfig() {
        if (rewardsConfigPath == null) return;
        
        try {
            JsonObject config = new JsonObject();
            
            config.addProperty("rewardoncatch", rewardOnCatch);
            config.addProperty("rewardonpokedexentry", rewardOnPokedexEntry);
            config.addProperty("rewardon50capture", rewardOn50Capture);
            config.addProperty("rewardon100capture", rewardOn100Capture);
            config.addProperty("rewardon50pokedexentries", rewardOn50PokedexEntries);
            config.addProperty("rewardon100pokedexentries", rewardOn100PokedexEntries);
            config.addProperty("rewardonpokemondeathmultiplyer", rewardOnPokemonDeathMultiplier);
            config.addProperty("baserewardonpokemondeath", baseRewardOnPokemonDeath);
            config.addProperty("ismoneyboxvisible", isMoneyBoxVisible);
            
            try (Writer writer = Files.newBufferedWriter(rewardsConfigPath)) {
                GSON.toJson(config, writer);
            }
            
            CobbleCoins.LOGGER.info("Saved rewards config to JSON");
        } catch (IOException e) {
            CobbleCoins.LOGGER.error("Failed to save rewards config", e);
        }
    }
    
    // Getters for rewards config values
    public static int getRewardOnCatch() {
        return rewardOnCatch;
    }
    
    public static int getRewardOnPokedexEntry() {
        return rewardOnPokedexEntry;
    }
    
    public static int getRewardOn50Capture() {
        return rewardOn50Capture;
    }
    
    public static int getRewardOn100Capture() {
        return rewardOn100Capture;
    }
    
    public static int getRewardOn50PokedexEntries() {
        return rewardOn50PokedexEntries;
    }
    
    public static int getRewardOn100PokedexEntries() {
        return rewardOn100PokedexEntries;
    }
    
    public static double getRewardOnPokemonDeathMultiplier() {
        return rewardOnPokemonDeathMultiplier;
    }
    
    public static int getBaseRewardOnPokemonDeath() {
        return baseRewardOnPokemonDeath;
    }
    
    public static boolean isMoneyBoxVisible() {
        return isMoneyBoxVisible;
    }
    
    /**
     * Gets the path to the shop config file.
     */
    public static Path getShopConfigPath() {
        return shopConfigPath;
    }
    
    /**
     * Gets the path to the config folder.
     */
    public static Path getConfigFolder() {
        return configFolder;
    }
}
