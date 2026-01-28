package fr.lapsito.cobblecoins.items.impl;

import fr.lapsito.cobblecoins.items.api.Currency;

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

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public int getMaxStackSize() {
        return maxStackSize;
    }

    @Override
    public long getStackingValue() {
        return stackingValue;
    }

    public String toString() {
        return displayName + " (id=" + id + ", value=" + stackingValue + ", maxStack=" + maxStackSize + ")";
    }
}
