package com.toddlandis.skillcraft.smithing;

import com.toddlandis.skillcraft.registry.ModDataMaps;
import com.toddlandis.skillcraft.registry.ModMenuTypes;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.SkillManager;
import com.toddlandis.skillcraft.skill.SmeltingRecipe;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

// Smelter screen layout (same positions as the vanilla furnace): ore top-left, coal bottom-left,
// ingots on the right. Only ores the player has the Smithing level for can go in.
public class SmelterMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = 3;

    private final Container container;
    private final ContainerData data;
    private final Player player;

    // Client side: empty container, filled in by vanilla syncing
    public SmelterMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(MACHINE_SLOTS), new SimpleContainerData(3));
    }

    public SmelterMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(ModMenuTypes.SMELTER.get(), containerId);
        this.container = container;
        this.data = data;
        this.player = inventory.player;
        checkContainerSize(container, MACHINE_SLOTS);

        addSlot(new Slot(container, SmelterBlockEntity.SLOT_INPUT, 56, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return canSmelt(stack);
            }
        });
        addSlot(new Slot(container, SmelterBlockEntity.SLOT_FUEL, 56, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ItemTags.COALS);
            }
        });
        addSlot(new Slot(container, SmelterBlockEntity.SLOT_OUTPUT, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player taker, ItemStack stack) {
                super.onTake(taker, stack);
                if (taker instanceof ServerPlayer serverPlayer && container instanceof SmelterBlockEntity smelter) {
                    smelter.awardStoredXp(serverPlayer);
                }
            }
        });
        addStandardInventorySlots(inventory, 8, 84);
        addDataSlots(data);
    }

    private boolean canSmelt(ItemStack stack) {
        SmeltingRecipe recipe = ModDataMaps.smelting(stack);
        return recipe != null && SkillManager.level(player, Skill.SMITHING) >= recipe.level();
    }

    public float progress() {
        int total = data.get(1);
        return total <= 0 ? 0 : data.get(0) / (float) total;
    }

    public boolean isWorking() {
        return data.get(0) > 0;
    }

    public int storedXp() {
        return data.get(2);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int inventoryStart = MACHINE_SLOTS;
        int inventoryEnd = slots.size();

        if (index < MACHINE_SLOTS) {
            // Out of the smelter into the player's inventory
            if (!moveItemStackTo(stack, inventoryStart, inventoryEnd, true)) return ItemStack.EMPTY;
            if (index == SmelterBlockEntity.SLOT_OUTPUT) slot.onTake(player, original);
        } else if (stack.is(ItemTags.COALS)) {
            if (!moveItemStackTo(stack, SmelterBlockEntity.SLOT_FUEL, SmelterBlockEntity.SLOT_FUEL + 1, false)) return ItemStack.EMPTY;
        } else if (canSmelt(stack)) {
            if (!moveItemStackTo(stack, SmelterBlockEntity.SLOT_INPUT, SmelterBlockEntity.SLOT_INPUT + 1, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }
}
