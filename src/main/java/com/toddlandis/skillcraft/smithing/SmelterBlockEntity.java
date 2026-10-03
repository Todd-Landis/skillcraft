package com.toddlandis.skillcraft.smithing;

import com.toddlandis.skillcraft.registry.ModBlockEntities;
import com.toddlandis.skillcraft.registry.ModDataMaps;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.SkillManager;
import com.toddlandis.skillcraft.skill.SmeltingRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// Processing station for Smithing: raw ore + coal/charcoal -> ingots, one at a time, over time.
// Runs on its own (like a furnace) while the chunk is loaded. Coal is only used when an ingot
// finishes, so taking items out mid-way loses nothing. Smithing XP builds up and is collected
// with the output.
public class SmelterBlockEntity extends BaseContainerBlockEntity {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_FUEL = 1;
    public static final int SLOT_OUTPUT = 2;

    private NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private int progress;
    private int totalTime;
    private double storedXp;

    // Synced to the open menu: progress, total time, and whether it has enough fuel to work
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> totalTime;
                case 2 -> (int) Math.floor(storedXp);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> totalTime = value;
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public SmelterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SMELTER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SmelterBlockEntity smelter) {
        boolean working = smelter.tick();
        if (state.getValue(SmelterBlock.LIT) != working) {
            level.setBlock(pos, state.setValue(SmelterBlock.LIT, working), Block.UPDATE_ALL);
        }
    }

    // Returns true while actively smelting
    private boolean tick() {
        ItemStack input = items.get(SLOT_INPUT);
        SmeltingRecipe recipe = ModDataMaps.smelting(input);
        if (recipe == null || !hasFuel(recipe) || !outputHasRoom(recipe)) {
            if (progress != 0) {
                progress = 0;
                setChanged();
            }
            totalTime = recipe == null ? 0 : recipe.time();
            return false;
        }

        totalTime = recipe.time();
        if (++progress >= totalTime) {
            progress = 0;
            input.shrink(1);
            items.get(SLOT_FUEL).shrink(recipe.coal());
            ItemStack output = items.get(SLOT_OUTPUT);
            if (output.isEmpty()) {
                items.set(SLOT_OUTPUT, new ItemStack(recipe.result(), recipe.count()));
            } else {
                output.grow(recipe.count());
            }
            storedXp += recipe.xp();
        }
        setChanged();
        return true;
    }

    private boolean hasFuel(SmeltingRecipe recipe) {
        return items.get(SLOT_FUEL).getCount() >= recipe.coal();
    }

    private boolean outputHasRoom(SmeltingRecipe recipe) {
        ItemStack output = items.get(SLOT_OUTPUT);
        if (output.isEmpty()) return true;
        return output.is(recipe.result()) && output.getCount() + recipe.count() <= output.getMaxStackSize();
    }

    // Called when a player takes items from the output slot
    public void awardStoredXp(ServerPlayer player) {
        if (storedXp > 0) {
            SkillManager.awardXp(player, Skill.SMITHING, storedXp);
            storedXp = 0;
            setChanged();
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.skillcraft.smelter");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new SmelterMenu(containerId, inventory, this, data);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("progress", progress);
        output.putDouble("stored_xp", storedXp);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        progress = input.getIntOr("progress", 0);
        storedXp = input.getDoubleOr("stored_xp", 0);
    }
}
