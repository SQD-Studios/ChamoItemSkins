// --- api/src/main/java/net/chamosmp/chamoitemskins/api/model/Rarity.java ---
package net.chamosmp.chamoitemskins.api.model;

/**
 * Represents the rarity of a skin.
 */
public enum Rarity {
    COMMON("<gray>Common"),
    RARE("<blue>Rare"),
    EPIC("<purple>Epic"),
    LEGENDARY("<gold>Legendary");

    private final String displayName;

    Rarity(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
