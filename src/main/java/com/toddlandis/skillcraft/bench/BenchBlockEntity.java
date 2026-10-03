package com.toddlandis.skillcraft.bench;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.toddlandis.skillcraft.registry.ModBlockEntities;
import com.toddlandis.skillcraft.registry.ModDataComponents;
import com.toddlandis.skillcraft.skill.SkillManager;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// Shared block entity for the Forging Anvil and Crafting Bench.
//   - Queueing takes the ingredients from the player right away and stores them in the job,
//     so cancelling refunds exactly what was put in.
//   - The bench works through the queue on its own (one item at a time), putting results in
//     its 9 output slots. If those are full it pauses.
//   - XP goes to whoever queued the job; if they're offline it's paid next time they open a bench.
public class BenchBlockEntity extends BaseContainerBlockEntity {
    public static final int OUTPUT_SLOTS = 9;
    public static final int MAX_JOBS = 8;
    public static final int MAX_PER_JOB = 64;
    private static final int EFFECT_INTERVAL = 10;

    // One queued order: `remaining` more of `item` for `owner`, with the ingredients set aside for them
    private static final class Job {
        static final Codec<Job> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(j -> j.item),
                Codec.INT.fieldOf("remaining").forGetter(j -> j.remaining),
                UUIDUtil.CODEC.fieldOf("owner").forGetter(j -> j.owner),
                Codec.STRING.optionalFieldOf("owner_name", "").forGetter(j -> j.ownerName),
                ItemStack.CODEC.listOf().fieldOf("reserved").forGetter(j -> j.reserved)
        ).apply(instance, Job::new));

        final Item item;
        int remaining;
        final UUID owner;
        final String ownerName;
        final List<ItemStack> reserved;

        Job(Item item, int remaining, UUID owner, String ownerName, List<ItemStack> reserved) {
            this.item = item;
            this.remaining = remaining;
            this.owner = owner;
            this.ownerName = ownerName;
            this.reserved = new ArrayList<>(reserved);
        }
    }

    private static final Codec<Map<UUID, Double>> PENDING_XP_CODEC = Codec.unboundedMap(Codec.STRING, Codec.DOUBLE)
            .xmap(map -> {
                Map<UUID, Double> result = new HashMap<>();
                map.forEach((key, value) -> result.put(UUID.fromString(key), value));
                return result;
            }, map -> {
                Map<String, Double> result = new HashMap<>();
                map.forEach((key, value) -> result.put(key.toString(), value));
                return result;
            });

    private final Station station;
    private NonNullList<ItemStack> items = NonNullList.withSize(OUTPUT_SLOTS, ItemStack.EMPTY);
    private final Deque<Job> queue = new ArrayDeque<>();
    private final Map<UUID, Double> pendingXp = new HashMap<>();
    private int progress;

    // Synced to an open menu: progress, total time, current item id, current remaining, total queued
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            Job job = queue.peek();
            BenchRecipe recipe = job == null ? null : BenchRecipe.get(job.item);
            return switch (index) {
                case 0 -> progress;
                case 1 -> recipe == null ? 0 : recipe.time();
                case 2 -> job == null ? -1 : BuiltInRegistries.ITEM.getId(job.item);
                case 3 -> job == null ? 0 : job.remaining;
                case 4 -> queue.stream().mapToInt(j -> j.remaining).sum();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return 5;
        }
    };

    public BenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BENCH.get(), pos, state);
        this.station = ((BenchBlock) state.getBlock()).station();
    }

    public Station station() {
        return station;
    }

    // --- Queueing (called from the network handler) ---

    public void queue(ServerPlayer player, Item item, int requested) {
        BenchRecipe recipe = BenchRecipe.get(item);
        if (recipe == null || recipe.station() != station) return;
        if (SkillManager.level(player, station.skill()) < recipe.level()) {
            message(player, Component.translatable("message.skillcraft.resource_level", station.skill().displayName(), recipe.level()), ChatFormatting.RED);
            return;
        }
        if (queue.size() >= MAX_JOBS) {
            message(player, Component.translatable("message.skillcraft.bench_queue_full"), ChatFormatting.RED);
            return;
        }
        List<ItemStack> inventory = player.getInventory().getNonEquipmentItems();
        int count = Math.min(Math.min(requested, MAX_PER_JOB), recipe.maxCrafts(inventory));
        if (count <= 0) {
            message(player, Component.translatable("message.skillcraft.bench_missing"), ChatFormatting.RED);
            return;
        }
        List<ItemStack> taken = recipe.take(inventory, count);
        player.getInventory().setChanged();
        queue.add(new Job(item, count, player.getUUID(), player.getName().getString(), taken));
        setChanged();
        message(player, Component.translatable("message.skillcraft.bench_queued", count, item.getDefaultInstance().getHoverName()), ChatFormatting.GREEN);
    }

    // Cancels this player's jobs and gives back the ingredients not yet used
    public void cancel(ServerPlayer player) {
        boolean any = false;
        for (var it = queue.iterator(); it.hasNext(); ) {
            Job job = it.next();
            if (!job.owner.equals(player.getUUID())) continue;
            for (ItemStack stack : job.reserved) {
                if (!stack.isEmpty() && !player.getInventory().add(stack)) player.drop(stack, false);
            }
            if (job == queue.peek()) progress = 0;
            it.remove();
            any = true;
        }
        if (any) {
            setChanged();
            message(player, Component.translatable("message.skillcraft.bench_cancelled"), ChatFormatting.GRAY);
        }
    }

    public void payPendingXp(ServerPlayer player) {
        Double xp = pendingXp.remove(player.getUUID());
        if (xp != null && xp > 0) {
            SkillManager.awardXp(player, station.skill(), xp);
            setChanged();
        }
    }

    // --- Processing ---

    public static void serverTick(Level level, BlockPos pos, BlockState state, BenchBlockEntity bench) {
        bench.tick((ServerLevel) level, pos);
    }

    private void tick(ServerLevel level, BlockPos pos) {
        Job job = queue.peek();
        if (job == null) return;
        BenchRecipe recipe = BenchRecipe.get(job.item);
        if (recipe == null) { // recipe removed by a datapack change: refund nothing, skip the job
            queue.poll();
            progress = 0;
            setChanged();
            return;
        }
        ItemStack result = makeResult(job, recipe);
        if (!canFit(result)) return; // output full: wait

        progress++;
        if (progress % EFFECT_INTERVAL == 0) workEffects(level, pos);
        if (progress < recipe.time()) return;

        progress = 0;
        recipe.take(job.reserved, 1);
        job.reserved.removeIf(ItemStack::isEmpty);
        insert(result);
        awardXp(level, job.owner, recipe.xp());
        if (--job.remaining <= 0) queue.poll();
        setChanged();
    }

    // Upgrades (e.g. diamond -> netherite) keep the base item's enchantments, name and damage.
    // Gear (non-stackable results) is stamped with its maker's name, see loot/CraftedGear.
    private static ItemStack makeResult(Job job, BenchRecipe recipe) {
        ItemStack result = baseResult(job, recipe);
        if (result.getMaxStackSize() == 1) result.set(ModDataComponents.CRAFTED_BY.get(), job.ownerName);
        return result;
    }

    private static ItemStack baseResult(Job job, BenchRecipe recipe) {
        int from = recipe.keepComponentsFrom();
        if (from >= 0 && from < recipe.ingredients().size()) {
            BenchRecipe.Part part = recipe.ingredients().get(from);
            for (ItemStack stack : job.reserved) {
                if (!stack.isEmpty() && part.ingredient().test(stack)) return stack.transmuteCopy(job.item, recipe.count());
            }
        }
        return new ItemStack(job.item, recipe.count());
    }

    private void awardXp(ServerLevel level, UUID owner, double xp) {
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
        if (player != null) {
            SkillManager.awardXp(player, station.skill(), xp);
        } else {
            pendingXp.merge(owner, xp, Double::sum);
        }
    }

    private void workEffects(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5, y = pos.getY() + 1.0, z = pos.getZ() + 0.5;
        if (station == Station.ANVIL) {
            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.25f, 1.3f + level.getRandom().nextFloat() * 0.4f);
            level.sendParticles(ParticleTypes.CRIT, x, y, z, 6, 0.15, 0.05, 0.15, 0.25);
        } else {
            level.playSound(null, pos, SoundEvents.VILLAGER_WORK_FLETCHER, SoundSource.BLOCKS, 0.6f, 0.9f + level.getRandom().nextFloat() * 0.2f);
            level.sendParticles(ParticleTypes.CRIT, x, y, z, 3, 0.2, 0.05, 0.2, 0.05);
        }
    }

    private boolean canFit(ItemStack result) {
        int room = 0;
        for (ItemStack stack : items) {
            if (stack.isEmpty()) room += result.getMaxStackSize();
            else if (ItemStack.isSameItemSameComponents(stack, result)) room += stack.getMaxStackSize() - stack.getCount();
            if (room >= result.getCount()) return true;
        }
        return false;
    }

    private void insert(ItemStack result) {
        for (ItemStack stack : items) {
            if (result.isEmpty()) return;
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, result)) {
                int moved = Math.min(result.getCount(), stack.getMaxStackSize() - stack.getCount());
                stack.grow(moved);
                result.shrink(moved);
            }
        }
        for (int i = 0; i < items.size() && !result.isEmpty(); i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, result.copy());
                result.setCount(0);
            }
        }
    }

    // Ingredients still reserved in queued jobs are dropped if the bench is broken
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null) return;
        for (Job job : queue) {
            for (ItemStack stack : job.reserved) {
                if (!stack.isEmpty()) net.minecraft.world.level.block.Block.popResource(level, pos, stack);
            }
        }
        queue.clear();
    }

    private static void message(ServerPlayer player, Component text, ChatFormatting color) {
        player.sendOverlayMessage(text.copy().withStyle(color));
    }

    // --- Container / saving ---

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.skillcraft." + station.getSerializedName());
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
        return OUTPUT_SLOTS;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new BenchMenu(station, containerId, inventory, this, data);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.store("queue", Job.CODEC.listOf(), List.copyOf(queue));
        output.store("pending_xp", PENDING_XP_CODEC, pendingXp);
        output.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(OUTPUT_SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        queue.clear();
        input.read("queue", Job.CODEC.listOf()).ifPresent(queue::addAll);
        pendingXp.clear();
        input.read("pending_xp", PENDING_XP_CODEC).ifPresent(pendingXp::putAll);
        progress = input.getIntOr("progress", 0);
    }
}
