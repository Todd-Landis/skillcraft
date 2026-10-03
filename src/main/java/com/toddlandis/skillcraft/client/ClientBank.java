package com.toddlandis.skillcraft.client;

import java.util.ArrayList;
import java.util.List;

import com.toddlandis.skillcraft.bank.PlayerBank;
import com.toddlandis.skillcraft.network.BankSyncPayload;

// Client-side copy of the player's bank (from BankSyncPayload) plus remembered screen settings,
// which persist between openings like OSRS (quantity mode, last X, rearrange mode).
public final class ClientBank {
    public static List<PlayerBank.Entry> entries = new ArrayList<>();
    public static int capacity = 800;

    public static int quantityMode = 0; // index into BankScreen.QUANTITIES
    public static int lastX = 1;
    public static boolean insertMode = false;
    public static int currentTab = 0;

    private ClientBank() {}

    public static void apply(BankSyncPayload payload) {
        entries = new ArrayList<>(payload.entries());
        capacity = payload.capacity();
    }
}
