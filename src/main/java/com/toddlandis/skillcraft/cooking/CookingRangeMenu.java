package com.toddlandis.skillcraft.cooking;

import com.toddlandis.skillcraft.registry.ModMenuTypes;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.SkillManager;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

// Cooking Range screen layout (furnace positions): raw food top-left, fuel bottom-left, cooked
// food in the big slot and burnt food beside it. Only food the player has the Cooking level for can
// go in, and whoever puts it in is the cook whose level decides the burn chance.
public class CookingRangeMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = 4;
    public static final int BURNT_X = 146, BURNT_Y = 35;

    private final Container container;
    private final ContainerData data;
    private final Player player;

    // Client side: empty container, filled in by vanilla syncing
    public CookingRangeMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(MACHINE_SLOTS), new SimpleContainerData(5));
    }

    public CookingRangeMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
        super(ModMenuTypes.COOKING_RANGE.get(), containerId);
        this.container = container;
        this.data = data;
        this.player = inventory.player;
        checkContainerSize(container, MACHINE_SLOTS);

        addSlot(new Slot(container, CookingRangeBlockEntity.SLOT_INPUT, 56, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return canCook(stack);
            }

            @Override
            public void setByPlayer(ItemStack stack, ItemStack previous) {
                super.setByPlayer(stack, previous);
                becomeCook();
            }
        });
        addSlot(new Slot(container, CookingRangeBlockEntity.SLOT_FUEL, 56, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isFuel(stack);
            }
        });
        addSlot(new OutputSlot(container, CookingRangeBlockEntity.SLOT_OUTPUT, 116, 35, true));
        addSlot(new OutputSlot(container, CookingRangeBlockEntity.SLOT_BURNT, BURNT_X, BURNT_Y, false));
        addStandardInventorySlots(inventory, 8, 84);
        addDataSlots(data);
    }

    private class OutputSlot extends Slot {
        private final boolean paysXp;

        OutputSlot(Container container, int index, int x, int y, boolean paysXp) {
            super(container, index, x, y);
            this.paysXp = paysXp;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public void onTake(Player taker, ItemStack stack) {
            super.onTake(taker, stack);
            if (paysXp && taker instanceof ServerPlayer serverPlayer && container instanceof CookingRangeBlockEntity range) {
                range.awardStoredXp(serverPlayer);
            }
        }
    }

    private void becomeCook() {
        if (player instanceof ServerPlayer serverPlayer && container instanceof CookingRangeBlockEntity range) {
            range.setCook(serverPlayer);
        }
    }

    private boolean canCook(ItemStack stack) {
        CookingRecipe recipe = CookingRecipe.get(stack);
        return recipe != null && SkillManager.level(player, Skill.COOKING) >= recipe.level();
    }

    private boolean isFuel(ItemStack stack) {
        return stack.getBurnTime(RecipeType.SMOKING, player.level().fuelValues()) > 0;
    }

    public float progress() {
        int total = data.get(1);
        return total <= 0 ? 0 : data.get(0) / (float) total;
    }

    public float fuelLeft() {
        int duration = data.get(3);
        return duration <= 0 ? 0 : data.get(2) / (float) duration;
    }

    public boolean isLit() {
        return data.get(2) > 0;
    }

    public int storedXp() {
        return data.get(4);
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
            if (!moveItemStackTo(stack, inventoryStart, inventoryEnd, true)) return ItemStack.EMPTY;
            if (index == CookingRangeBlockEntity.SLOT_OUTPUT) slot.onTake(player, original);
        } else if (canCook(stack)) {
            if (!moveItemStackTo(stack, CookingRangeBlockEntity.SLOT_INPUT, CookingRangeBlockEntity.SLOT_INPUT + 1, false)) return ItemStack.EMPTY;
            becomeCook();
        } else if (isFuel(stack)) {
            if (!moveItemStackTo(stack, CookingRangeBlockEntity.SLOT_FUEL, CookingRangeBlockEntity.SLOT_FUEL + 1, false)) return ItemStack.EMPTY;
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
