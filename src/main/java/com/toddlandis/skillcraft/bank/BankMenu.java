package com.toddlandis.skillcraft.bank;

import com.toddlandis.skillcraft.registry.ModBlocks;
import com.toddlandis.skillcraft.registry.ModMenuTypes;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

// The bank screen's menu. Only the player's inventory uses real slots; bank contents are not
// vanilla slots (they hold far more than 99 items) and are drawn from the synced copy in
// ClientBank. All bank actions go through BankActionPayload -> BankActions.
public class BankMenu extends AbstractContainerMenu {
    public static final int INVENTORY_X = 11;
    public static final int INVENTORY_Y = 190;

    private final ContainerLevelAccess access;

    // Client side
    public BankMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public BankMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenuTypes.BANK.get(), containerId);
        this.access = access;
        addStandardInventorySlots(inventory, INVENTORY_X, INVENTORY_Y);
    }

    // Clicking inventory items deposits them (handled by the screen), so no shift-moving
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.BANK_CHEST.get());
    }
}
