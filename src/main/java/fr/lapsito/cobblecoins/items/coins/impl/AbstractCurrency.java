package fr.lapsito.cobblecoins.items.coins.impl;

import fr.lapsito.cobblecoins.items.coins.api.Currency;

/**
 * Base implementation for currencies providing common fields and behavior.
 */
public abstract class AbstractCurrency implements Currency {
    private final String id;
    private final String displayName;
    private final int maxStackSize;
    private final long stackingValue;

    protected AbstractCurrency(String id, String displayName, int maxStackSize, long stackingValue) {
        this.id = id;
        this.displayName = displayName;
        this.maxStackSize = maxStackSize;
        this.stackingValue = stackingValue;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxStackSize() {
        return maxStackSize;
    }

    public long getStackingValue() {
        return stackingValue;
    }

    public String toString() {
        return displayName + " (id=" + id + ", value=" + stackingValue + ", maxStack=" + maxStackSize + ")";
    }
}
