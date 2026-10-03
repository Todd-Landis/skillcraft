package com.toddlandis.skillcraft.bank;

import java.util.List;

import com.toddlandis.skillcraft.Config;
import com.toddlandis.skillcraft.network.BankActionPayload;
import com.toddlandis.skillcraft.network.BankSyncPayload;
import com.toddlandis.skillcraft.registry.ModAttachments;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// Server side of the bank: every withdraw/deposit/rearrange request is validated here.
public final class BankActions {
    // Special amounts sent by the client
    public static final int ALL = -1;
    public static final int ALL_BUT_ONE = -2;

    public enum Action { WITHDRAW, DEPOSIT, DEPOSIT_INVENTORY, DEPOSIT_WORN, SWAP, INSERT, SET_TAB, DEPOSIT_CARRIED }

    private BankActions() {}

    public static PlayerBank bank(ServerPlayer player) {
        return player.getData(ModAttachments.BANK);
    }

    public static void sync(ServerPlayer player) {
        PlayerBank bank = bank(player);
        PacketDistributor.sendToPlayer(player, new BankSyncPayload(List.copyOf(bank.entries()), Config.BANK_SLOTS.get()));
    }

    public static void handle(BankActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!(player.containerMenu instanceof BankMenu menu) || menu.containerId != payload.containerId()) return;
        Action[] actions = Action.values();
        if (payload.action() < 0 || payload.action() >= actions.length) return;

        PlayerBank bank = bank(player);
        switch (actions[payload.action()]) {
            case WITHDRAW -> withdraw(player, bank, payload.index(), payload.amount());
            case DEPOSIT -> {
                if (payload.index() >= 0 && payload.index() < menu.slots.size()) {
                    ItemStack type = menu.slots.get(payload.index()).getItem();
                    if (!type.isEmpty()) deposit(player, bank, type.copy(), payload.amount(), payload.target());
                }
            }
            case DEPOSIT_INVENTORY -> depositAll(player, bank, payload.target());
            case DEPOSIT_WORN -> depositWorn(player, bank, payload.target());
            case SWAP -> bank.swap(payload.index(), payload.target());
            case INSERT -> bank.insert(payload.index(), payload.target());
            case SET_TAB -> bank.setTab(payload.index(), payload.target());
            case DEPOSIT_CARRIED -> depositCarried(player, menu, bank, payload.amount(), payload.target());
        }
        menu.broadcastChanges();
        player.getInventory().setChanged();
        sync(player);
    }

    private static int resolve(int amount, int available) {
        if (amount == ALL) return available;
        if (amount == ALL_BUT_ONE) return Math.max(0, available - 1);
        return Math.max(0, Math.min(amount, available));
    }

    // Withdraws as many as requested that fit in the player's inventory
    private static void withdraw(ServerPlayer player, PlayerBank bank, int index, int amount) {
        if (index < 0 || index >= bank.entries().size()) return;
        PlayerBank.Entry entry = bank.entries().get(index);
        ItemStack template = entry.item();
        int wanted = resolve(amount, entry.count());
        int fits = Math.min(wanted, inventoryRoom(player.getInventory(), template));
        if (fits < wanted) warn(player, "message.skillcraft.inventory_full");
        int taken = bank.take(index, fits);
        int max = template.getMaxStackSize();
        while (taken > 0) {
            int part = Math.min(max, taken);
            player.getInventory().add(template.copyWithCount(part));
            taken -= part;
        }
    }

    // Deposits `amount` of this item type from anywhere in the inventory (OSRS-style)
    private static void deposit(ServerPlayer player, PlayerBank bank, ItemStack type, int amount, int tab) {
        Inventory inventory = player.getInventory();
        int have = 0;
        for (ItemStack stack : inventory.getNonEquipmentItems()) {
            if (ItemStack.isSameItemSameComponents(stack, type)) have += stack.getCount();
        }
        int wanted = resolve(amount, have);
        int room = bank.room(type, wanted, Config.BANK_SLOTS.get());
        if (room < wanted) warn(player, "message.skillcraft.bank_full");
        int remaining = room;
        for (ItemStack stack : inventory.getNonEquipmentItems()) {
            if (remaining <= 0) break;
            if (!ItemStack.isSameItemSameComponents(stack, type)) continue;
            int moved = Math.min(remaining, stack.getCount());
            stack.shrink(moved);
            remaining -= moved;
        }
        bank.add(type, room - remaining, tab);
    }

    // The item stack held on the mouse cursor, dropped onto the bank grid
    private static void depositCarried(ServerPlayer player, BankMenu menu, PlayerBank bank, int amount, int tab) {
        ItemStack carried = menu.getCarried();
        if (carried.isEmpty()) return;
        int wanted = resolve(amount, carried.getCount());
        int room = bank.room(carried, wanted, Config.BANK_SLOTS.get());
        if (room < wanted) warn(player, "message.skillcraft.bank_full");
        bank.add(carried, room, tab);
        carried.shrink(room);
    }

    private static void depositAll(ServerPlayer player, PlayerBank bank, int tab) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.isEmpty()) continue;
            int room = bank.room(stack, stack.getCount(), Config.BANK_SLOTS.get());
            if (room <= 0) {
                warn(player, "message.skillcraft.bank_full");
                continue;
            }
            bank.add(stack, room, tab);
            stack.shrink(room);
        }
    }

    private static void depositWorn(ServerPlayer player, PlayerBank bank, int tab) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            int room = bank.room(stack, stack.getCount(), Config.BANK_SLOTS.get());
            if (room < stack.getCount()) {
                warn(player, "message.skillcraft.bank_full");
                continue;
            }
            bank.add(stack, room, tab);
            player.setItemSlot(slot, ItemStack.EMPTY);
        }
    }

    // How many of this item the main inventory (not armor/offhand) can still hold
    private static int inventoryRoom(Inventory inventory, ItemStack template) {
        long room = 0;
        int max = template.getMaxStackSize();
        for (ItemStack stack : inventory.getNonEquipmentItems()) {
            if (stack.isEmpty()) room += max;
            else if (ItemStack.isSameItemSameComponents(stack, template)) room += max - stack.getCount();
        }
        return (int) Math.min(room, Integer.MAX_VALUE);
    }

    private static void warn(ServerPlayer player, String key) {
        player.sendOverlayMessage(Component.translatable(key).withStyle(ChatFormatting.RED));
    }
}
