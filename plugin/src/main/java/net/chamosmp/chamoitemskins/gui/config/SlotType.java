// --- plugin/src/main/java/net/chamosmp/chamoitemskins/gui/config/SlotType.java ---
package net.chamosmp.chamoitemskins.gui.config;

/**
 * Represents the type of a GUI slot.
 */
public sealed interface SlotType {
    record Decorative() implements SlotType {}
    record SkinSlot(int index) implements SlotType {}
    record FilterSlot() implements SlotType {}
    record BackSlot() implements SlotType {}
    record ActionSlot(String action) implements SlotType {}
}
