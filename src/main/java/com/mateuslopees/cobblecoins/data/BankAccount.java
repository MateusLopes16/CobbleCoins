package com.mateuslopees.cobblecoins.data;

import java.util.UUID;

public class BankAccount {
    private transient UUID playerId;
    private long balance;
    private int totalCaptures;
    private int totalPokedexEntries;
    private long totalEarned;
    private long totalSpent;

    public BankAccount() {
        this.balance = 0;
        this.totalCaptures = 0;
        this.totalPokedexEntries = 0;
        this.totalEarned = 0;
        this.totalSpent = 0;
    }

    public BankAccount(UUID playerId) {
        this();
        this.playerId = playerId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public void setPlayerId(UUID playerId) {
        this.playerId = playerId;
    }

    public long getBalance() {
        return balance;
    }

    public void setBalance(long balance) {
        long diff = balance - this.balance;
        if (diff > 0) {
            this.totalEarned += diff;
        } else {
            this.totalSpent += Math.abs(diff);
        }
        this.balance = balance;
    }

    public int getTotalCaptures() {
        return totalCaptures;
    }

    public void incrementCaptures() {
        this.totalCaptures++;
    }

    public int getTotalPokedexEntries() {
        return totalPokedexEntries;
    }

    public void incrementPokedexEntries() {
        this.totalPokedexEntries++;
    }

    public long getTotalEarned() {
        return totalEarned;
    }

    public long getTotalSpent() {
        return totalSpent;
    }
}
