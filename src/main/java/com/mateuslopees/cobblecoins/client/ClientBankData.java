package com.mateuslopees.cobblecoins.client;

/**
 * Client-side storage for the player's bank balance.
 * This is synced from the server via network packets.
 */
public class ClientBankData {
    private static long balance = 0;

    public static long getBalance() {
        return balance;
    }

    public static void setBalance(long newBalance) {
        balance = newBalance;
    }

    public static String getFormattedBalance() {
        return formatCurrency(balance);
    }

    public static String formatCurrency(long amount) {
        if (amount >= 1_000_000_000) {
            return String.format("%.1fB", amount / 1_000_000_000.0);
        } else if (amount >= 1_000_000) {
            return String.format("%.1fM", amount / 1_000_000.0);
        } else if (amount >= 1_000) {
            return String.format("%.1fK", amount / 1_000.0);
        }
        return String.valueOf(amount);
    }
}
