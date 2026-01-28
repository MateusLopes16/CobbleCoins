package fr.lapsito.cobblecoins.items.coins.api;

/**
 * Currency-specific interface. Currency items represent values in base "cobblecoins".
 */
public interface Currency extends Item {
    /** Maximum items per inventory stack for this currency item. */
    int getMaxStackSize();

    /**
     * How many base cobblecoins one unit of this currency represents.
     * Example: CobbleNote -> 64, CobbleWad -> 4096.
     */
    long getStackingValue();

    /** Convenience: total base-value for a given count of this item. */
    default long getTotalValue(int count) {
        return getStackingValue() * (long) count;
    }
}
