package com.mateuslopees.cobblecoins.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mateuslopees.cobblecoins.CobbleCoins;
import com.mateuslopees.cobblecoins.network.NetworkHandler;
import com.mateuslopees.cobblecoins.network.packet.SyncBalancePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BankAccountManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, BankAccount> accounts = new ConcurrentHashMap<>();
    private static Path savePath;
    private static boolean initialized = false;

    public static void init() {
        initialized = true;
        CobbleCoins.LOGGER.info("BankAccountManager initialized");
    }

    public static void setServerPath(MinecraftServer server) {
        savePath = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .resolve("cobblecoins")
                .resolve("bank_accounts.json");
        loadAccounts();
    }

    public static BankAccount getAccount(UUID playerId) {
        return accounts.computeIfAbsent(playerId, id -> new BankAccount(id));
    }

    public static long getBalance(UUID playerId) {
        return getAccount(playerId).getBalance();
    }

    public static void setBalance(UUID playerId, long amount) {
        getAccount(playerId).setBalance(Math.max(0, amount));
        saveAccounts();
        syncToPlayer(playerId);
    }

    public static void addBalance(UUID playerId, long amount) {
        BankAccount account = getAccount(playerId);
        account.setBalance(account.getBalance() + amount);
        saveAccounts();
        syncToPlayer(playerId);
    }

    public static boolean removeBalance(UUID playerId, long amount) {
        BankAccount account = getAccount(playerId);
        if (account.getBalance() >= amount) {
            account.setBalance(account.getBalance() - amount);
            saveAccounts();
            syncToPlayer(playerId);
            return true;
        }
        return false;
    }

    public static boolean transfer(UUID from, UUID to, long amount) {
        if (removeBalance(from, amount)) {
            addBalance(to, amount);
            return true;
        }
        return false;
    }

    // Tracking stats
    public static void incrementCaptures(UUID playerId) {
        BankAccount account = getAccount(playerId);
        account.incrementCaptures();
        saveAccounts();
    }

    public static void incrementPokedexEntries(UUID playerId) {
        BankAccount account = getAccount(playerId);
        account.incrementPokedexEntries();
        saveAccounts();
    }

    public static int getCaptures(UUID playerId) {
        return getAccount(playerId).getTotalCaptures();
    }

    public static int getPokedexEntries(UUID playerId) {
        return getAccount(playerId).getTotalPokedexEntries();
    }

    private static void syncToPlayer(UUID playerId) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                NetworkHandler.sendToPlayer(new SyncBalancePacket(getBalance(playerId)), player);
            }
        }
    }

    public static void syncToPlayer(ServerPlayer player) {
        NetworkHandler.sendToPlayer(new SyncBalancePacket(getBalance(player.getUUID())), player);
    }

    private static void loadAccounts() {
        if (savePath == null || !Files.exists(savePath)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(savePath)) {
            Type type = new TypeToken<Map<String, BankAccount>>() {}.getType();
            Map<String, BankAccount> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                accounts.clear();
                loaded.forEach((key, value) -> {
                    try {
                        UUID uuid = UUID.fromString(key);
                        value.setPlayerId(uuid);
                        accounts.put(uuid, value);
                    } catch (IllegalArgumentException e) {
                        CobbleCoins.LOGGER.warn("Invalid UUID in bank accounts: {}", key);
                    }
                });
                CobbleCoins.LOGGER.info("Loaded {} bank accounts", accounts.size());
            }
        } catch (IOException e) {
            CobbleCoins.LOGGER.error("Failed to load bank accounts", e);
        }
    }

    public static void saveAccounts() {
        if (savePath == null) {
            return;
        }

        try {
            Files.createDirectories(savePath.getParent());
            try (Writer writer = Files.newBufferedWriter(savePath)) {
                Map<String, BankAccount> toSave = new ConcurrentHashMap<>();
                accounts.forEach((uuid, account) -> toSave.put(uuid.toString(), account));
                GSON.toJson(toSave, writer);
            }
        } catch (IOException e) {
            CobbleCoins.LOGGER.error("Failed to save bank accounts", e);
        }
    }

    public static void onServerStarting(MinecraftServer server) {
        setServerPath(server);
    }

    public static void onServerStopping() {
        saveAccounts();
    }
}
