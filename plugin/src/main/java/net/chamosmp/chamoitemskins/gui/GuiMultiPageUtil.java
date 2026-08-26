package net.chamosmp.chamoitemskins.gui;

import java.util.*;
import java.util.function.Predicate;

public class GuiMultiPageUtil<T> {

    private final int inventorySize;
    private final Predicate<Integer> isBorderSlot;
    private final Set<Integer> reservedSlots;
    private final List<Integer> availableSlots;
    private final int itemsPerPage;

    private List<T> allItems = new ArrayList<>();
    private int currentPage = 0;

    public GuiMultiPageUtil(int inventorySize, Predicate<Integer> isBorderSlot, Set<Integer> reservedSlots) {
        this.inventorySize = inventorySize;
        this.isBorderSlot = isBorderSlot;
        this.reservedSlots = new HashSet<>(reservedSlots);
        this.availableSlots = computeAvailableSlots();
        this.itemsPerPage = Math.max(1, availableSlots.size());
    }

    private List<Integer> computeAvailableSlots() {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inventorySize; i++) {
            if (!isBorderSlot.test(i) && !reservedSlots.contains(i)) {
                slots.add(i);
            }
        }
        return slots;
    }

    public void setItems(List<T> items) {
        this.allItems = new ArrayList<>(items);
        this.currentPage = 0;
    }

    public List<T> getCurrentPageItems() {
        if (allItems.isEmpty() || availableSlots.isEmpty()) return Collections.emptyList();
        int total = getTotalPages();
        if (currentPage >= total) currentPage = total - 1;
        int start = currentPage * itemsPerPage;
        int end = Math.min(start + itemsPerPage, allItems.size());
        return allItems.subList(start, end);
    }

    public int getTotalPages() {
        if (allItems.isEmpty() || availableSlots.isEmpty()) return 0;
        return (int) Math.ceil((double) allItems.size() / itemsPerPage);
    }

    public boolean hasNext() {
        return getTotalPages() > 0 && currentPage < getTotalPages() - 1;
    }

    public boolean hasPrev() {
        return currentPage > 0;
    }

    public void nextPage() {
        if (hasNext()) currentPage++;
    }

    public void prevPage() {
        if (hasPrev()) currentPage--;
    }

    public List<Integer> getAvailableSlots() {
        return availableSlots;
    }

    public void clear() {
        allItems.clear();
    }

}