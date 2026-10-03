package com.toddlandis.skillcraft.altar;

import com.toddlandis.skillcraft.node.NodeType;
import com.toddlandis.skillcraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// Remembers which node the altar is being attuned to and how much material has been loaded.
public class AttunementAltarBlockEntity extends BlockEntity {
    private NodeType target;
    private int stored;

    public AttunementAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ATTUNEMENT_ALTAR.get(), pos, state);
    }

    public NodeType target() { return target; }
    public int stored() { return stored; }
    public boolean isEmpty() { return target == null; }
    public boolean isFull() { return target != null && stored >= target.inputCount(); }

    // Adds up to `available` items; returns how many were used
    public int deposit(NodeType type, int available) {
        if (target == null) target = type;
        int used = Math.min(available, target.inputCount() - stored);
        stored += used;
        setChanged();
        return used;
    }

    // Loses a fraction of the stored material (failed attunement)
    public void loseFraction(double fraction) {
        stored -= (int) Math.ceil(stored * fraction);
        if (stored <= 0) clear();
        setChanged();
    }

    public void clear() {
        target = null;
        stored = 0;
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (target != null) {
            output.putString("target", target.getSerializedName());
            output.putInt("stored", stored);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        target = input.read("target", NodeType.CODEC).orElse(null);
        stored = input.getIntOr("stored", 0);
    }
}
