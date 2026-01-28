package fr.lapsito.cobblecoins.items.impl;

/** Cobblewad: 64 cobblenotes = 4096 cobblecoins. */
public final class CobbleWad extends AbstractCurrency {
    public static final CobbleWad INSTANCE = new CobbleWad();

    private CobbleWad() {
        super("cobblewad", "Cobblewad", 64, 64L * 64L);
    }
}
