package com.toddlandis.skillcraft.bank;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

// A player's bank, OSRS-style: one list of entries shared by every Bank Chest.
//   - Each stackable item type (same item + same components) takes ONE slot holding up to
//     Integer.MAX_VALUE (2,147,483,647).
//   - Unstackable items (max stack size 1) take one slot each.
//   - Entries can be put in tabs 1-9 (0 = no tab).
// Stored on the player as a data attachment (ModAttachments.BANK); the open bank screen gets
// a copy via BankSyncPayload.
public class PlayerBank {
    public static final int MAX_TABS = 9;

    public static final class Entry {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemStack.CODEC.fieldOf("item").forGetter(e -> e.item),
                Codec.INT.fieldOf("count").forGetter(e -> e.count),
                Codec.INT.optionalFieldOf("tab", 0).forGetter(e -> e.tab)
        ).apply(instance, Entry::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, e -> e.item,
                ByteBufCodecs.VAR_INT, e -> e.count,
                ByteBufCodecs.VAR_INT, e -> e.tab,
                Entry::new);

        private final ItemStack item; // template, count 1
        private int count;
        private int tab;

        public Entry(ItemStack item, int count, int tab) {
            this.item = item.copyWithCount(1);
            this.count = count;
            this.tab = tab;
        }

        public ItemStack item() { return item; }
        public int count() { return count; }
        public int tab() { return tab; }
    }

    public static final MapCodec<PlayerBank> CODEC = Entry.CODEC.listOf()
            .xmap(PlayerBank::new, bank -> List.copyOf(bank.entries))
            .fieldOf("entries");

    private final List<Entry> entries = new ArrayList<>();

    public PlayerBank() {}

    private PlayerBank(List<Entry> saved) {
        entries.addAll(saved);
    }

    public List<Entry> entries() {
        return entries;
    }

    private static boolean stackable(ItemStack stack) {
        return stack.getMaxStackSize() > 1;
    }

    private int find(ItemStack stack) {
        if (!stackable(stack)) return -1;
        for (int i = 0; i < entries.size(); i++) {
            if (ItemStack.isSameItemSameComponents(entries.get(i).item, stack)) return i;
        }
        return -1;
    }

    // How many of `stack` (up to `amount`) the bank can take right now
    public int room(ItemStack stack, int amount, int capacity) {
        if (stackable(stack)) {
            int index = find(stack);
            if (index >= 0) return (int) Math.min(amount, (long) Integer.MAX_VALUE - entries.get(index).count);
            return entries.size() < capacity ? amount : 0;
        }
        return Math.min(amount, capacity - entries.size());
    }

    // Adds `amount` of `stack`; caller must check room() first. New entries go into `tab`.
    public void add(ItemStack stack, int amount, int tab) {
        if (amount <= 0) return;
        if (stackable(stack)) {
            int index = find(stack);
            if (index >= 0) {
                entries.get(index).count += amount;
            } else {
                entries.add(new Entry(stack, amount, tab));
            }
        } else {
            for (int i = 0; i < amount; i++) entries.add(new Entry(stack, 1, tab));
        }
    }

    // Takes up to `amount` from the entry at `index`; returns how many were taken
    public int take(int index, int amount) {
        if (index < 0 || index >= entries.size()) return 0;
        Entry entry = entries.get(index);
        int taken = Math.min(amount, entry.count);
        entry.count -= taken;
        if (entry.count <= 0) entries.remove(index);
        return taken;
    }

    public void swap(int from, int to) {
        if (valid(from) && valid(to)) {
            Entry a = entries.get(from);
            entries.set(from, entries.get(to));
            entries.set(to, a);
        }
    }

    public void insert(int from, int to) {
        if (valid(from) && valid(to)) entries.add(to, entries.remove(from));
    }

    public void setTab(int index, int tab) {
        if (valid(index)) entries.get(index).tab = Math.clamp(tab, 0, MAX_TABS);
    }

    private boolean valid(int index) {
        return index >= 0 && index < entries.size();
    }
}
