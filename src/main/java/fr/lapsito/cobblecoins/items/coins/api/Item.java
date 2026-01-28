package fr.lapsito.cobblecoins.items.coins.api;

/**
 * Global item interface for mod items.
 */
public interface Item {
    /** A unique id for the item (e.g., "cobblecoin"). */
    String getId();

    /** Human-readable display name. */
    String getDisplayName();
}
