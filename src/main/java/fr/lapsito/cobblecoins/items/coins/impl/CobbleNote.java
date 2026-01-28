package fr.lapsito.cobblecoins.items.impl;

/** Cobblenote: 64 cobblecoins. */
public final class CobbleNote extends AbstractCurrency {
    public static final CobbleNote INSTANCE = new CobbleNote();

    private CobbleNote() {
        super("cobblenote", "Cobblenote", 64, 64L);
    }
}
