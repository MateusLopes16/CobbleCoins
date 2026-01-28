package fr.lapsito.cobblecoins.items.util;

import fr.lapsito.cobblecoins.items.api.Currency;

/** Utility methods to work with currency values. */
public final class CurrencyOperations {
    private CurrencyOperations() {}

    /** Return total base cobblecoins for the given currency and count. */
    public static long toBaseCoins(Currency currency, int count) {
        return currency.getTotalValue(count);
    }

    /** Convert a base-coin amount into units of the target currency.
     * Returns the number of whole target units and the leftover base coins.
     */
    public static ConversionResult convertTo(Currency target, long baseCoins) {
        if (target.getStackingValue() <= 0) {
            throw new IllegalArgumentException("Target stacking value must be positive");
        }
        long units = baseCoins / target.getStackingValue();
        long remainder = baseCoins % target.getStackingValue();
        return new ConversionResult(units, remainder);
    }

    public static final class ConversionResult {
        private final long units;
        private final long remainderBaseCoins;

        public ConversionResult(long units, long remainderBaseCoins) {
            this.units = units;
            this.remainderBaseCoins = remainderBaseCoins;
        }

        public long getUnits() {
            return units;
        }

        public long getRemainderBaseCoins() {
            return remainderBaseCoins;
        }
    }
}
