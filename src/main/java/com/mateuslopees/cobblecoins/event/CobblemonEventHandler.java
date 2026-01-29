package com.mateuslopees.cobblecoins.event;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.mateuslopees.cobblecoins.CobbleCoins;
import com.mateuslopees.cobblecoins.config.CobbleCoinsConfig;
import com.mateuslopees.cobblecoins.data.BankAccountManager;
import kotlin.Unit;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CobblemonEventHandler {
    // Track which Pokemon species each player has caught for Pokedex rewards
    private static final ConcurrentHashMap<UUID, Set<String>> playerPokedex = new ConcurrentHashMap<>();
    
    // Streak tracking
    private static final ConcurrentHashMap<UUID, Integer> captureStreaks = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Integer> defeatStreaks = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> lastCaptureTime = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> lastDefeatTime = new ConcurrentHashMap<>();

    public CobblemonEventHandler() {
        // Register Cobblemon events using their Kotlin event system
        registerCobblemonEvents();
    }

    private void registerCobblemonEvents() {
        // Pokemon captured event
        CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.NORMAL, event -> {
            try {
                handlePokemonCaptured(event);
            } catch (Exception e) {
                CobbleCoins.LOGGER.error("Error handling Pokemon capture event", e);
            }
            return Unit.INSTANCE;
        });

        // Battle victory event - reward for defeating Pokemon
        CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.NORMAL, event -> {
            try {
                handleBattleVictory(event);
            } catch (Exception e) {
                CobbleCoins.LOGGER.error("Error handling battle victory event", e);
            }
            return Unit.INSTANCE;
        });
    }
    
    /**
     * Get the current capture streak for a player, checking for timeout
     */
    private int getCaptureStreak(UUID playerId) {
        if (!CobbleCoinsConfig.COMMON.enableStreaks.get()) return 0;
        
        int timeout = CobbleCoinsConfig.COMMON.streakTimeoutMinutes.get();
        if (timeout > 0) {
            Long lastTime = lastCaptureTime.get(playerId);
            if (lastTime != null) {
                long minutesPassed = (System.currentTimeMillis() - lastTime) / 60000;
                if (minutesPassed >= timeout) {
                    captureStreaks.put(playerId, 0);
                }
            }
        }
        return captureStreaks.getOrDefault(playerId, 0);
    }
    
    /**
     * Get the current defeat streak for a player, checking for timeout
     */
    private int getDefeatStreak(UUID playerId) {
        if (!CobbleCoinsConfig.COMMON.enableStreaks.get()) return 0;
        
        int timeout = CobbleCoinsConfig.COMMON.streakTimeoutMinutes.get();
        if (timeout > 0) {
            Long lastTime = lastDefeatTime.get(playerId);
            if (lastTime != null) {
                long minutesPassed = (System.currentTimeMillis() - lastTime) / 60000;
                if (minutesPassed >= timeout) {
                    defeatStreaks.put(playerId, 0);
                }
            }
        }
        return defeatStreaks.getOrDefault(playerId, 0);
    }
    
    /**
     * Calculate the streak multiplier
     */
    private double getStreakMultiplier(int streak, double bonusPerStreak, double maxMultiplier) {
        double multiplier = 1.0 + (streak * bonusPerStreak);
        return Math.min(multiplier, maxMultiplier);
    }
    
    /**
     * Apply IV bonuses based on capture streak
     * 10+ streak: 1 perfect IV
     * 20+ streak: 2 perfect IVs
     * 30+ streak: 3 perfect IVs
     */
    private int applyStreakIVBonus(Pokemon pokemon, int streak, ServerPlayer player) {
        if (!CobbleCoinsConfig.COMMON.enableStreakIVBonus.get()) return 0;
        
        int perfectIVs = 0;
        if (streak >= CobbleCoinsConfig.COMMON.streakFor3PerfectIVs.get()) {
            perfectIVs = 3;
        } else if (streak >= CobbleCoinsConfig.COMMON.streakFor2PerfectIVs.get()) {
            perfectIVs = 2;
        } else if (streak >= CobbleCoinsConfig.COMMON.streakFor1PerfectIV.get()) {
            perfectIVs = 1;
        }
        
        if (perfectIVs > 0) {
            try {
                // Get the IVs object from the Pokemon
                com.cobblemon.mod.common.pokemon.IVs ivs = pokemon.getIvs();
                
                // List of IV stat names
                String[] statNames = {"hp", "attack", "defence", "special_attack", "special_defence", "speed"};
                java.util.List<String> availableStats = new java.util.ArrayList<>(java.util.Arrays.asList(statNames));
                java.util.Collections.shuffle(availableStats);
                
                // Set random IVs to 31
                for (int i = 0; i < perfectIVs && i < availableStats.size(); i++) {
                    String stat = availableStats.get(i);
                    // Use reflection to set IV since the API might differ
                    try {
                        // Try using the Cobblemon API method
                        com.cobblemon.mod.common.api.pokemon.stats.Stat statEnum = null;
                        switch (stat) {
                            case "hp" -> statEnum = com.cobblemon.mod.common.api.pokemon.stats.Stats.HP;
                            case "attack" -> statEnum = com.cobblemon.mod.common.api.pokemon.stats.Stats.ATTACK;
                            case "defence" -> statEnum = com.cobblemon.mod.common.api.pokemon.stats.Stats.DEFENCE;
                            case "special_attack" -> statEnum = com.cobblemon.mod.common.api.pokemon.stats.Stats.SPECIAL_ATTACK;
                            case "special_defence" -> statEnum = com.cobblemon.mod.common.api.pokemon.stats.Stats.SPECIAL_DEFENCE;
                            case "speed" -> statEnum = com.cobblemon.mod.common.api.pokemon.stats.Stats.SPEED;
                        }
                        if (statEnum != null) {
                            ivs.set(statEnum, 31);
                        }
                    } catch (Exception e) {
                        CobbleCoins.LOGGER.debug("Could not set IV for stat {}: {}", stat, e.getMessage());
                    }
                }
                
                CobbleCoins.LOGGER.debug("Applied {} perfect IVs to captured Pokemon for streak {}", perfectIVs, streak);
            } catch (Exception e) {
                CobbleCoins.LOGGER.error("Error applying streak IV bonus", e);
            }
        }
        
        return perfectIVs;
    }

    private void handlePokemonCaptured(com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent event) {
        // Get UUID from the player object - Cobblemon returns a cross-mapped type
        // Use reflection since the type mappings differ between Cobblemon (Yarn) and NeoForge (Mojang)
        UUID playerId = null;
        try {
            java.lang.reflect.Method getPlayer = event.getClass().getMethod("getPlayer");
            Object playerObj = getPlayer.invoke(event);
            if (playerObj != null) {
                playerId = getPlayerUUID(playerObj);
            }
        } catch (Exception e) {
            CobbleCoins.LOGGER.debug("Could not get player from capture event", e);
            return;
        }
        
        if (playerId == null) return;
        
        ServerPlayer player = getServerPlayer(playerId);
        if (player == null) return;
        
        Pokemon pokemon = event.getPokemon();
        if (pokemon == null) return;

        String speciesId = pokemon.getSpecies().getName();
        String pokemonName = pokemon.getSpecies().getName();

        // Get current streak and calculate multiplier
        int currentStreak = getCaptureStreak(playerId);
        double streakMultiplier = getStreakMultiplier(
                currentStreak,
                CobbleCoinsConfig.COMMON.streakBonusPerCapture.get(),
                CobbleCoinsConfig.COMMON.maxStreakMultiplier.get()
        );
        
        // Apply IV bonus based on streak BEFORE incrementing
        int perfectIVsApplied = applyStreakIVBonus(pokemon, currentStreak, player);
        
        // Capture reward with streak bonus
        int baseReward = CobbleCoinsConfig.COMMON.rewardOnCatch.get();
        int captureReward = (int) Math.round(baseReward * streakMultiplier);
        BankAccountManager.addBalance(playerId, captureReward);
        BankAccountManager.incrementCaptures(playerId);
        
        // Update streak
        int newStreak = currentStreak + 1;
        captureStreaks.put(playerId, newStreak);
        lastCaptureTime.put(playerId, System.currentTimeMillis());

        // Send chat message for capture reward
        StringBuilder message = new StringBuilder();
        message.append("§a+").append(captureReward).append(" ¢ §7for capturing §e").append(pokemonName);
        
        if (CobbleCoinsConfig.COMMON.enableStreaks.get() && currentStreak > 0) {
            int bonusPercent = (int) ((streakMultiplier - 1.0) * 100);
            message.append(" §7(§6Streak x").append(newStreak).append(" §7+§e").append(bonusPercent).append("%§7)");
        }
        
        player.displayClientMessage(Component.literal(message.toString()), false);
        
        // Send IV bonus message if applicable
        if (perfectIVsApplied > 0) {
            player.displayClientMessage(
                    Component.literal("§b✦ STREAK BONUS! §f" + perfectIVsApplied + " Perfect IV" + 
                            (perfectIVsApplied > 1 ? "s" : "") + " §7guaranteed!"),
                    false);
        }

        // Check if this is a new Pokedex entry
        Set<String> pokedex = playerPokedex.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet());
        if (pokedex.add(speciesId)) {
            // New Pokedex entry!
            int pokedexReward = CobbleCoinsConfig.COMMON.rewardOnPokedexEntry.get();
            BankAccountManager.addBalance(playerId, pokedexReward);
            BankAccountManager.incrementPokedexEntries(playerId);

            player.displayClientMessage(
                    Component.literal("§6✦ NEW POKÉDEX ENTRY! §e" + pokemonName + " §7- §a+" + pokedexReward + " ¢"),
                    false);

            // Check for Pokedex milestones
            int totalEntries = BankAccountManager.getPokedexEntries(playerId);
            checkPokedexMilestones(player, totalEntries);
        }

        // Check for capture milestones
        int totalCaptures = BankAccountManager.getCaptures(playerId);
        checkCaptureMilestones(player, totalCaptures);
    }

    private void handleBattleVictory(com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent event) {
        // Get all winning players
        event.getWinners().forEach(actor -> {
            if (actor instanceof com.cobblemon.mod.common.battles.actor.PlayerBattleActor playerActor) {
                // Get the actual player entity using reflection to avoid mapping issues
                UUID playerId = null;
                try {
                    java.lang.reflect.Method getEntity = playerActor.getClass().getMethod("getEntity");
                    Object playerObj = getEntity.invoke(playerActor);
                    if (playerObj != null) {
                        playerId = getPlayerUUID(playerObj);
                    }
                } catch (Exception e) {
                    CobbleCoins.LOGGER.debug("Could not get player from battle actor", e);
                    return;
                }
                
                if (playerId == null) return;
                
                ServerPlayer player = getServerPlayer(playerId);
                if (player == null) return;

                // Get current streak and calculate multiplier
                int currentStreak = getDefeatStreak(playerId);
                double streakMultiplier = getStreakMultiplier(
                        currentStreak,
                        CobbleCoinsConfig.COMMON.streakBonusPerDefeat.get(),
                        CobbleCoinsConfig.COMMON.maxStreakMultiplier.get()
                );

                // Calculate reward based on defeated Pokemon levels
                final UUID finalPlayerId = playerId;
                final ServerPlayer finalPlayer = player;
                final double finalStreakMultiplier = streakMultiplier;
                final int finalCurrentStreak = currentStreak;
                
                // Track total reward for this battle
                final int[] totalBattleReward = {0};
                final int[] defeatedCount = {0};
                
                event.getBattle().getActors().forEach(battleActor -> {
                    if (battleActor != actor) {
                        battleActor.getPokemonList().forEach(battlePokemon -> {
                            Pokemon pokemon = battlePokemon.getOriginalPokemon();
                            if (pokemon != null) {
                                int level = pokemon.getLevel();
                                double multiplier = CobbleCoinsConfig.COMMON.rewardOnPokemonDefeatMultiplier.get();
                                int baseReward = CobbleCoinsConfig.COMMON.baseRewardOnPokemonDefeat.get();
                                
                                int baseAmount = (int) (level * multiplier + baseReward);
                                int reward = (int) Math.round(baseAmount * finalStreakMultiplier);
                                
                                totalBattleReward[0] += reward;
                                defeatedCount[0]++;
                            }
                        });
                    }
                });
                
                if (totalBattleReward[0] > 0) {
                    BankAccountManager.addBalance(finalPlayerId, totalBattleReward[0]);
                    
                    // Update streak
                    int newStreak = currentStreak + defeatedCount[0];
                    defeatStreaks.put(finalPlayerId, newStreak);
                    lastDefeatTime.put(finalPlayerId, System.currentTimeMillis());
                    
                    // Send chat message for defeat reward
                    if (CobbleCoinsConfig.COMMON.enableStreaks.get() && finalCurrentStreak > 0) {
                        int bonusPercent = (int) ((finalStreakMultiplier - 1.0) * 100);
                        finalPlayer.displayClientMessage(
                                Component.literal("§a+" + totalBattleReward[0] + " ¢ §7for winning battle! " +
                                        "§7(§6Streak x" + newStreak + " §7+§e" + bonusPercent + "%§7)"),
                                false);
                    } else {
                        finalPlayer.displayClientMessage(
                                Component.literal("§a+" + totalBattleReward[0] + " ¢ §7for winning battle!"),
                                false);
                    }
                }
            }
        });
    }
    
    /**
     * Safely extract UUID from a player object using reflection.
     * This handles the type mapping differences between Cobblemon's Yarn mappings
     * and NeoForge's Mojang mappings.
     */
    private UUID getPlayerUUID(Object playerObj) {
        try {
            // Try getUUID method (Mojang mapping)
            java.lang.reflect.Method getUUID = playerObj.getClass().getMethod("getUUID");
            return (UUID) getUUID.invoke(playerObj);
        } catch (Exception e1) {
            try {
                // Try m_142081_ (obfuscated) or uuid field
                java.lang.reflect.Method getUuid = playerObj.getClass().getMethod("getUuid");
                return (UUID) getUuid.invoke(playerObj);
            } catch (Exception e2) {
                try {
                    // Try direct field access
                    java.lang.reflect.Field uuidField = findField(playerObj.getClass(), "uuid");
                    if (uuidField != null) {
                        uuidField.setAccessible(true);
                        return (UUID) uuidField.get(playerObj);
                    }
                } catch (Exception e3) {
                    CobbleCoins.LOGGER.debug("Could not extract UUID from player object: {}", playerObj.getClass().getName());
                }
            }
        }
        return null;
    }
    
    private java.lang.reflect.Field findField(Class<?> clazz, String name) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
    
    private ServerPlayer getServerPlayer(UUID uuid) {
        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            return server.getPlayerList().getPlayer(uuid);
        }
        return null;
    }

    private void checkCaptureMilestones(ServerPlayer player, int totalCaptures) {
        // Check 50 capture milestone
        if (totalCaptures > 0 && totalCaptures % 50 == 0) {
            int reward = CobbleCoinsConfig.COMMON.rewardOn50Captures.get();
            BankAccountManager.addBalance(player.getUUID(), reward);
            player.displayClientMessage(
                    Component.translatable("message.cobblecoins.milestone_captures", 50, reward)
                            .withStyle(ChatFormatting.GOLD),
                    false);
        }

        // Check 100 capture milestone
        if (totalCaptures > 0 && totalCaptures % 100 == 0) {
            int reward = CobbleCoinsConfig.COMMON.rewardOn100Captures.get();
            BankAccountManager.addBalance(player.getUUID(), reward);
            player.displayClientMessage(
                    Component.translatable("message.cobblecoins.milestone_captures", 100, reward)
                            .withStyle(ChatFormatting.GOLD),
                    false);
        }
    }

    private void checkPokedexMilestones(ServerPlayer player, int totalEntries) {
        // Check 50 Pokedex entry milestone
        if (totalEntries > 0 && totalEntries % 50 == 0) {
            int reward = CobbleCoinsConfig.COMMON.rewardOn50PokedexEntries.get();
            BankAccountManager.addBalance(player.getUUID(), reward);
            player.displayClientMessage(
                    Component.translatable("message.cobblecoins.milestone_pokedex", 50, reward)
                            .withStyle(ChatFormatting.GOLD),
                    false);
        }

        // Check 100 Pokedex entry milestone
        if (totalEntries > 0 && totalEntries % 100 == 0) {
            int reward = CobbleCoinsConfig.COMMON.rewardOn100PokedexEntries.get();
            BankAccountManager.addBalance(player.getUUID(), reward);
            player.displayClientMessage(
                    Component.translatable("message.cobblecoins.milestone_pokedex", 100, reward)
                            .withStyle(ChatFormatting.GOLD),
                    false);
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // Sync balance to player
            BankAccountManager.syncToPlayer(serverPlayer);
            
            // Load their Pokedex data from Cobblemon if needed
            // This is done lazily when they catch Pokemon
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        // Save data on logout
        BankAccountManager.saveAccounts();
        
        // Reset streaks on logout if configured
        if (CobbleCoinsConfig.COMMON.streakResetOnLogout.get()) {
            UUID playerId = event.getEntity().getUUID();
            captureStreaks.remove(playerId);
            defeatStreaks.remove(playerId);
            lastCaptureTime.remove(playerId);
            lastDefeatTime.remove(playerId);
        }
    }
    
    /**
     * Get the current capture streak for a player (for commands)
     */
    public static int getPlayerCaptureStreak(UUID playerId) {
        return captureStreaks.getOrDefault(playerId, 0);
    }
    
    /**
     * Get the current defeat streak for a player (for commands)
     */
    public static int getPlayerDefeatStreak(UUID playerId) {
        return defeatStreaks.getOrDefault(playerId, 0);
    }
}
