package com.toddlandis.skillcraft.altar;

import java.util.UUID;

import com.mojang.serialization.Codec;
import com.toddlandis.skillcraft.node.ElementalHeartItem;
import com.toddlandis.skillcraft.node.NodeType;
import com.toddlandis.skillcraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// Remembers which node the altar is being attuned to (from the Elemental Heart set on it) and who
// set it there. Only the owner can withdraw, attune or break it while it is loaded.
public class AttunementAltarBlockEntity extends BlockEntity {
    // Hearts needed for one attunement
    public static final int CAPACITY = 1;

    private NodeType target;
    private int stored;
    private UUID owner;
    private String ownerName = "";

    public AttunementAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ATTUNEMENT_ALTAR.get(), pos, state);
    }

    public NodeType target() { return target; }
    public int stored() { return stored; }
    public boolean isEmpty() { return target == null; }
    public boolean isFull() { return target != null && stored >= CAPACITY; }

    // What's on the altar, as items to hand back
    public ItemStack contents() {
        if (target == null || stored <= 0) return ItemStack.EMPTY;
        ItemStack hearts = ElementalHeartItem.of(target);
        hearts.setCount(stored);
        return hearts;
    }
    public String ownerName() { return ownerName; }

    // Anyone may use an empty altar; a loaded one belongs to whoever loaded it
    public boolean isOwner(Player player) {
        return owner == null || owner.equals(player.getUUID());
    }

    // Adds up to `available` items; returns how many were used
    public int deposit(Player player, NodeType type, int available) {
        if (target == null) {
            target = type;
            owner = player.getUUID();
            ownerName = player.getName().getString();
        }
        int used = Math.min(available, CAPACITY - stored);
        stored += used;
        setChanged();
        return used;
    }

    // A failed attunement: the heart cracks (and is lost) with this chance; true if it did
    public boolean crack(double chance, net.minecraft.util.RandomSource random) {
        if (random.nextDouble() >= chance) return false;
        clear();
        return true;
    }

    public void clear() {
        target = null;
        stored = 0;
        owner = null;
        ownerName = "";
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (target != null) {
            output.putString("target", target.getSerializedName());
            output.putInt("stored", stored);
            output.storeNullable("owner", UUIDUtil.CODEC, owner);
            output.putString("owner_name", ownerName);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        target = input.read("target", NodeType.CODEC).orElse(null);
        stored = input.getIntOr("stored", 0);
        owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        ownerName = input.read("owner_name", Codec.STRING).orElse("");
    }
}
