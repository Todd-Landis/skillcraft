package com.toddlandis.skillcraft.leycraft;

import java.util.UUID;

import com.mojang.serialization.Codec;
import com.toddlandis.skillcraft.registry.ModBlockEntities;
import com.toddlandis.skillcraft.registry.ModDataMaps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// The essence loaded on a Ley Altar (one kind, up to CAPACITY) and who loaded it. Like the
// Attunement Altar, a loaded altar belongs to that player until it's woven or broken.
public class LeyAltarBlockEntity extends BlockEntity {
    public static final int CAPACITY = 64;

    private Item essence;
    private int count;
    private UUID owner;
    private String ownerName = "";

    public LeyAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LEY_ALTAR.get(), pos, state);
    }

    public boolean isEmpty() { return essence == null || count <= 0; }
    public Item essence() { return essence; }
    public int count() { return count; }
    public String ownerName() { return ownerName; }

    // The loaded essence counted in ley shards (an amethyst shard is worth half)
    public double shardValue() {
        if (isEmpty()) return 0;
        LeyData.Essence data = LeyWeaving.essence(essence.getDefaultInstance());
        return data == null ? 0 : data.value() * count;
    }

    public boolean isOwner(Player player) {
        return owner == null || owner.equals(player.getUUID());
    }

    // True if this essence can go on the altar now (it's empty, or holds the same kind with room)
    public boolean accepts(Item item) {
        return isEmpty() || (item == essence && count < CAPACITY);
    }

    // Adds up to `available`; returns how many were used
    public int deposit(Player player, Item item, int available) {
        if (!accepts(item)) return 0;
        if (isEmpty()) {
            essence = item;
            count = 0;
            owner = player.getUUID();
            ownerName = player.getName().getString();
        }
        int used = Math.min(available, CAPACITY - count);
        count += used;
        setChanged();
        return used;
    }

    public ItemStack contents() {
        return isEmpty() ? ItemStack.EMPTY : new ItemStack(essence, count);
    }

    public void clear() {
        essence = null;
        count = 0;
        owner = null;
        ownerName = "";
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!isEmpty()) {
            output.store("essence", BuiltInRegistries.ITEM.byNameCodec(), essence);
            output.putInt("count", count);
            output.storeNullable("owner", UUIDUtil.CODEC, owner);
            output.putString("owner_name", ownerName);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        essence = input.read("essence", BuiltInRegistries.ITEM.byNameCodec()).orElse(null);
        count = input.getIntOr("count", 0);
        owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        ownerName = input.read("owner_name", Codec.STRING).orElse("");
    }
}
