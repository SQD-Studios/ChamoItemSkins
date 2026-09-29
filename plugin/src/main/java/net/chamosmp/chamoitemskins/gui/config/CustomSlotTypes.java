package net.chamosmp.chamoitemskins.gui.config;

import net.chamosmp.sqdlib.paper.chamogui.config.SlotType;

public class CustomSlotTypes {
    public record SkinSlot(int index) implements SlotType {
    }

    public record FilterSlot() implements SlotType {
    }

    public record BackSlot() implements SlotType {
    }

    public record SearchSlot() implements SlotType {
    }
}
