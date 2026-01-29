package com.mateuslopees.cobblecoins.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class CobbleCoinsConfig {
    public static final ModConfigSpec SPEC;
    public static final CommonConfig COMMON;
    
    public static final ModConfigSpec CLIENT_SPEC;
    public static final ClientConfig CLIENT;

    static {
        final Pair<CommonConfig, ModConfigSpec> commonPair = new ModConfigSpec.Builder().configure(CommonConfig::new);
        COMMON = commonPair.getLeft();
        SPEC = commonPair.getRight();
        
        final Pair<ClientConfig, ModConfigSpec> clientPair = new ModConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT = clientPair.getLeft();
        CLIENT_SPEC = clientPair.getRight();
    }

    public static class CommonConfig {
        // Capture rewards
        public final ModConfigSpec.IntValue rewardOnCatch;
        public final ModConfigSpec.IntValue rewardOnPokedexEntry;
        public final ModConfigSpec.IntValue rewardOn50Captures;
        public final ModConfigSpec.IntValue rewardOn100Captures;
        public final ModConfigSpec.IntValue rewardOn50PokedexEntries;
        public final ModConfigSpec.IntValue rewardOn100PokedexEntries;
        
        // Defeat rewards
        public final ModConfigSpec.DoubleValue rewardOnPokemonDefeatMultiplier;
        public final ModConfigSpec.IntValue baseRewardOnPokemonDefeat;
        
        // General settings
        public final ModConfigSpec.LongValue startingBalance;
        public final ModConfigSpec.LongValue maxBalance;
        
        // Streak settings
        public final ModConfigSpec.BooleanValue enableStreaks;
        public final ModConfigSpec.DoubleValue streakBonusPerCapture;
        public final ModConfigSpec.DoubleValue streakBonusPerDefeat;
        public final ModConfigSpec.DoubleValue maxStreakMultiplier;
        public final ModConfigSpec.BooleanValue streakResetOnLogout;
        public final ModConfigSpec.IntValue streakTimeoutMinutes;
        
        // Streak IV Bonus settings
        public final ModConfigSpec.BooleanValue enableStreakIVBonus;
        public final ModConfigSpec.IntValue streakFor1PerfectIV;
        public final ModConfigSpec.IntValue streakFor2PerfectIVs;
        public final ModConfigSpec.IntValue streakFor3PerfectIVs;
        
        // Shop settings
        public final ModConfigSpec.BooleanValue enablePlayerShops;
        public final ModConfigSpec.IntValue maxPlayerShopSlots;
        public final ModConfigSpec.DoubleValue playerShopTaxRate;

        CommonConfig(ModConfigSpec.Builder builder) {
            builder.comment("CobbleCoins Common Configuration")
                   .push("rewards");

            rewardOnCatch = builder
                    .comment("CobbleCoins earned per Pokémon capture")
                    .defineInRange("rewardOnCatch", 10, 0, 10000);

            rewardOnPokedexEntry = builder
                    .comment("CobbleCoins earned for each new Pokédex entry")
                    .defineInRange("rewardOnPokedexEntry", 100, 0, 10000);

            rewardOn50Captures = builder
                    .comment("Bonus CobbleCoins for every 50 captures milestone")
                    .defineInRange("rewardOn50Captures", 50, 0, 100000);

            rewardOn100Captures = builder
                    .comment("Bonus CobbleCoins for every 100 captures milestone")
                    .defineInRange("rewardOn100Captures", 100, 0, 100000);

            rewardOn50PokedexEntries = builder
                    .comment("Bonus CobbleCoins for every 50 Pokédex entries milestone")
                    .defineInRange("rewardOn50PokedexEntries", 500, 0, 100000);

            rewardOn100PokedexEntries = builder
                    .comment("Bonus CobbleCoins for every 100 Pokédex entries milestone")
                    .defineInRange("rewardOn100PokedexEntries", 1000, 0, 100000);

            builder.pop().push("defeat_rewards");

            rewardOnPokemonDefeatMultiplier = builder
                    .comment("Multiplier applied to Pokémon level for defeat rewards")
                    .defineInRange("rewardOnPokemonDefeatMultiplier", 1.5, 0.0, 100.0);

            baseRewardOnPokemonDefeat = builder
                    .comment("Base CobbleCoins added to defeat rewards")
                    .defineInRange("baseRewardOnPokemonDefeat", 0, 0, 10000);

            builder.pop().push("general");

            startingBalance = builder
                    .comment("Starting CobbleCoins balance for new players")
                    .defineInRange("startingBalance", 0L, 0L, Long.MAX_VALUE);

            maxBalance = builder
                    .comment("Maximum CobbleCoins balance a player can have")
                    .defineInRange("maxBalance", Long.MAX_VALUE, 1L, Long.MAX_VALUE);

            builder.pop().push("streaks");
            
            enableStreaks = builder
                    .comment("Enable capture/defeat streak bonuses")
                    .define("enableStreaks", true);
            
            streakBonusPerCapture = builder
                    .comment("Bonus percentage per streak level for captures (0.1 = 10% bonus per streak)")
                    .defineInRange("streakBonusPerCapture", 0.1, 0.0, 5.0);
            
            streakBonusPerDefeat = builder
                    .comment("Bonus percentage per streak level for defeats (0.1 = 10% bonus per streak)")
                    .defineInRange("streakBonusPerDefeat", 0.1, 0.0, 5.0);
            
            maxStreakMultiplier = builder
                    .comment("Maximum streak multiplier cap (2.0 = 200% max bonus)")
                    .defineInRange("maxStreakMultiplier", 2.0, 1.0, 10.0);
            
            streakResetOnLogout = builder
                    .comment("Reset streaks when player logs out")
                    .define("streakResetOnLogout", false);
            
            streakTimeoutMinutes = builder
                    .comment("Minutes of inactivity before streak resets (0 = never timeout)")
                    .defineInRange("streakTimeoutMinutes", 30, 0, 1440);
            
            builder.pop().push("streak_iv_bonus");
            
            enableStreakIVBonus = builder
                    .comment("Enable guaranteed perfect IVs based on capture streak")
                    .define("enableStreakIVBonus", true);
            
            streakFor1PerfectIV = builder
                    .comment("Capture streak required for 1 guaranteed perfect IV (31)")
                    .defineInRange("streakFor1PerfectIV", 10, 1, 100);
            
            streakFor2PerfectIVs = builder
                    .comment("Capture streak required for 2 guaranteed perfect IVs (31)")
                    .defineInRange("streakFor2PerfectIVs", 20, 1, 100);
            
            streakFor3PerfectIVs = builder
                    .comment("Capture streak required for 3 guaranteed perfect IVs (31)")
                    .defineInRange("streakFor3PerfectIVs", 30, 1, 100);

            builder.pop().push("shops");

            enablePlayerShops = builder
                    .comment("Enable player-owned shops")
                    .define("enablePlayerShops", true);

            maxPlayerShopSlots = builder
                    .comment("Maximum number of slots in a player shop")
                    .defineInRange("maxPlayerShopSlots", 27, 1, 54);

            playerShopTaxRate = builder
                    .comment("Tax rate on player shop sales (0.0 = no tax, 0.1 = 10% tax)")
                    .defineInRange("playerShopTaxRate", 0.0, 0.0, 1.0);

            builder.pop();
        }
    }

    public static class ClientConfig {
        public final ModConfigSpec.BooleanValue showMoneyHud;
        public final ModConfigSpec.IntValue hudPositionX;
        public final ModConfigSpec.IntValue hudPositionY;
        public final ModConfigSpec.EnumValue<HudAnchor> hudAnchor;

        ClientConfig(ModConfigSpec.Builder builder) {
            builder.comment("CobbleCoins Client Configuration")
                   .push("hud");

            showMoneyHud = builder
                    .comment("Show the money HUD display")
                    .define("showMoneyHud", true);

            hudPositionX = builder
                    .comment("HUD X offset from anchor position")
                    .defineInRange("hudPositionX", -10, -1000, 1000);

            hudPositionY = builder
                    .comment("HUD Y offset from anchor position")
                    .defineInRange("hudPositionY", -10, -1000, 1000);

            hudAnchor = builder
                    .comment("HUD anchor position on screen")
                    .defineEnum("hudAnchor", HudAnchor.BOTTOM_RIGHT);

            builder.pop();
        }
    }

    public enum HudAnchor {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }
}
