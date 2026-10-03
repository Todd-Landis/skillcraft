package com.toddlandis.skillcraft.bench;

import com.toddlandis.skillcraft.registry.ModMenuTypes;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// Bench screen: a row of 9 output slots (take-only) above the player's inventory.
// The recipe list and queue are drawn by BenchScreen; queue/cancel go over the network.
public class BenchMenu extends AbstractContainerMenu {
    public static final int OUTPUT_Y = 124;
    public static final int INVENTORY_Y = 154;

    private final Station station;
    private final Container container;
    private final ContainerData data;
    private final BenchBlockEntity bench; // server side only

    // Client side
    public BenchMenu(Station station, int containerId, Inventory inventory) {
        this(station, containerId, inventory, new SimpleContainer(BenchBlockEntity.OUTPUT_SLOTS), new SimpleContainerData(5));
    }

    public BenchMenu(Station station, int containerId, Inventory inventory, Container container, ContainerData data) {
        super(station == Station.ANVIL ? ModMenuTypes.FORGING_ANVIL.get() : ModMenuTypes.CRAFTING_BENCH.get(), containerId);
        this.station = station;
        this.container = container;
        this.data = data;
        this.bench = container instanceof BenchBlockEntity be ? be : null;
        checkContainerSize(container, BenchBlockEntity.OUTPUT_SLOTS);

        for (int i = 0; i < BenchBlockEntity.OUTPUT_SLOTS; i++) {
            addSlot(new Slot(container, i, 35 + i * 18, OUTPUT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        addStandardInventorySlots(inventory, 35, INVENTORY_Y);
        addDataSlots(data);
    }

    public Station station() { return station; }
    public BenchBlockEntity bench() { return bench; }

    public float progress() {
        int total = data.get(1);
        return total <= 0 ? 0 : data.get(0) / (float) total;
    }

    public Item currentItem() {
        int id = data.get(2);
        return id < 0 ? null : BuiltInRegistries.ITEM.byId(id);
    }

    public int currentRemaining() { return data.get(3); }
    public int totalQueued() { return data.get(4); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || index >= BenchBlockEntity.OUTPUT_SLOTS) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (!moveItemStackTo(stack, BenchBlockEntity.OUTPUT_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }
}
