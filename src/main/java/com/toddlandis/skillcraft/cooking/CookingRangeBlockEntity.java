package com.toddlandis.skillcraft.cooking;

import java.util.UUID;

import com.toddlandis.skillcraft.registry.ModBlockEntities;
import com.toddlandis.skillcraft.registry.ModItems;
import com.toddlandis.skillcraft.registry.ModSounds;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.SkillManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// Processing station for Cooking: raw food + any furnace fuel -> cooked food, one at a time.
// Runs on its own while the chunk is loaded. Each finished item can burn: the chance depends on
// the Cooking level of the player who loaded the food (CookingRecipe.burnChance), and falls to
// zero at the food's stop-burn level. Burnt food goes to its own slot and gives no XP.
// Cooking XP builds up and is collected with the cooked food, like the Smelter.
public class CookingRangeBlockEntity extends BaseContainerBlockEntity {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_FUEL = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_BURNT = 3;

    private NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
    private int progress;
    private int totalTime;
    private int litTime;
    private int litDuration;
    private double storedXp;
    // Whoever loaded the food, and their Cooking level when they did (used for burn rolls)
    private UUID cook;
    private int cookLevel = 1;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> totalTime;
                case 2 -> litTime;
                case 3 -> litDuration;
                case 4 -> (int) Math.floor(storedXp);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> totalTime = value;
                case 2 -> litTime = value;
                case 3 -> litDuration = value;
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public CookingRangeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COOKING_RANGE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CookingRangeBlockEntity range) {
        boolean lit = range.tick(level);
        if (state.getValue(CookingRangeBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(CookingRangeBlock.LIT, lit), Block.UPDATE_ALL);
        }
    }

    // Returns true while the fire is burning
    private boolean tick(Level level) {
        boolean changed = false;
        if (litTime > 0) {
            litTime--;
            changed = true;
        }

        ItemStack input = items.get(SLOT_INPUT);
        CookingRecipe recipe = CookingRecipe.get(input);
        boolean canCook = recipe != null && hasRoom(SLOT_OUTPUT, recipe.result()) && hasRoom(SLOT_BURNT, ModItems.BURNT_FOOD.get());
        totalTime = recipe == null ? 0 : recipe.time();

        // Only light new fuel when there is something to cook
        if (litTime <= 0 && canCook) {
            ItemStack fuel = items.get(SLOT_FUEL);
            int duration = fuel.getBurnTime(RecipeType.SMOKING, level.fuelValues());
            if (duration > 0) {
                litTime = litDuration = duration;
                // Read the remainder (e.g. the empty bucket from lava) before shrinking, like the furnace
                var remainder = fuel.getCraftingRemainder();
                fuel.shrink(1);
                if (fuel.isEmpty() && remainder != null) items.set(SLOT_FUEL, remainder.create());
                changed = true;
            }
        }

        if (litTime > 0 && canCook) {
            if (++progress >= totalTime) {
                progress = 0;
                finishOne(level, input, recipe);
            }
            changed = true;
        } else if (progress != 0) {
            progress = Math.max(0, progress - 2); // cools off when the fire is out
            changed = true;
        }
        if (changed) setChanged();
        return litTime > 0;
    }

    private void finishOne(Level level, ItemStack input, CookingRecipe recipe) {
        input.shrink(1);
        boolean burnt = level.getRandom().nextDouble() < recipe.burnChance(cookLevel);
        int slot = burnt ? SLOT_BURNT : SLOT_OUTPUT;
        Item result = burnt ? ModItems.BURNT_FOOD.get() : recipe.result();
        ItemStack out = items.get(slot);
        if (out.isEmpty()) items.set(slot, new ItemStack(result));
        else out.grow(1);
        if (!burnt) storedXp += recipe.xp();
        if (level instanceof ServerLevel serverLevel) {
            BlockPos pos = getBlockPos();
            serverLevel.playSound(null, pos, burnt ? ModSounds.RANGE_BURN.get() : ModSounds.RANGE_SIZZLE.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
        }
    }

    private boolean hasRoom(int slot, Item item) {
        ItemStack stack = items.get(slot);
        return stack.isEmpty() || (stack.is(item) && stack.getCount() < stack.getMaxStackSize());
    }

    // Called by the menu when a player puts raw food in: they become the cook
    public void setCook(ServerPlayer player) {
        cook = player.getUUID();
        cookLevel = SkillManager.level(player, Skill.COOKING);
        setChanged();
    }

    public int cookLevel() {
        return cookLevel;
    }

    // Called when a player takes cooked food out
    public void awardStoredXp(ServerPlayer player) {
        if (storedXp > 0) {
            SkillManager.awardXp(player, Skill.COOKING, storedXp);
            storedXp = 0;
            setChanged();
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.skillcraft.cooking_range");
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
        return new CookingRangeMenu(containerId, inventory, this, data);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("progress", progress);
        output.putInt("lit_time", litTime);
        output.putInt("lit_duration", litDuration);
        output.putDouble("stored_xp", storedXp);
        output.putInt("cook_level", cookLevel);
        if (cook != null) output.store("cook", UUIDUtil.CODEC, cook);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        progress = input.getIntOr("progress", 0);
        litTime = input.getIntOr("lit_time", 0);
        litDuration = input.getIntOr("lit_duration", 0);
        storedXp = input.getDoubleOr("stored_xp", 0);
        cookLevel = input.getIntOr("cook_level", 1);
        cook = input.read("cook", UUIDUtil.CODEC).orElse(null);
    }
}
