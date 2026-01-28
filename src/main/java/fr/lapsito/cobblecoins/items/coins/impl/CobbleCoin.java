package fr.lapsito.cobblecoins.items.coins.impl;

/** Single cobblecoin, base unit (value = 1). */
public final class CobbleCoin extends AbstractCurrency {
    public static final CobbleCoin INSTANCE = new CobbleCoin();

    private CobbleCoin() {
        super("cobblecoin", "Cobblecoin", 64, 1L);
    }
}
