package com.toddlandis.skillcraft.client.ability;

import com.mojang.blaze3d.platform.InputConstants;
import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.ability.AbilityData;
import com.toddlandis.skillcraft.network.UseAbilityPayload;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

// Ability key bindings, under "SkillCraft – Abilities" in Controls (all rebindable).
// Defaults are R, Z, B, N: the spec's R/G/Z/X clashes with 26.2's Quick Actions (G) and
// Load Toolbar (X) keys. J opens the Ability Book; switching bars has no default key.
public final class AbilityKeys {
    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(SkillCraft.MODID, "abilities"));
    private static final int[] DEFAULTS = {InputConstants.KEY_R, InputConstants.KEY_Z, InputConstants.KEY_B, InputConstants.KEY_N};

    public static final KeyMapping[] SLOTS = new KeyMapping[AbilityData.SLOTS];
    public static final KeyMapping BOOK = new KeyMapping("key.skillcraft.ability_book", InputConstants.KEY_J, CATEGORY);
    public static final KeyMapping NEXT_LOADOUT = new KeyMapping("key.skillcraft.next_loadout", InputConstants.UNKNOWN.getValue(), CATEGORY);

    static {
        for (int i = 0; i < SLOTS.length; i++) {
            SLOTS[i] = new KeyMapping("key.skillcraft.ability_" + (i + 1), DEFAULTS[i], CATEGORY);
        }
    }

    private AbilityKeys() {}

    public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        for (KeyMapping key : SLOTS) event.register(key);
        event.register(BOOK);
        event.register(NEXT_LOADOUT);
    }

    // Called every client tick
    public static void handle(Minecraft mc) {
        for (int i = 0; i < SLOTS.length; i++) {
            while (SLOTS[i].consumeClick()) {
                if (mc.player != null && mc.gui.screen() == null) ClientPacketDistributor.sendToServer(new UseAbilityPayload(i));
            }
        }
        while (BOOK.consumeClick()) {
            if (mc.player != null && mc.gui.screen() == null) mc.gui.setScreen(new AbilityBookScreen());
        }
        while (NEXT_LOADOUT.consumeClick()) {
            if (mc.player == null) continue;
            AbilityData data = ClientAbilities.data(mc.player);
            int next = (data.activeLoadout() + 1) % AbilityData.LOADOUTS;
            ClientPacketDistributor.sendToServer(AbilityBookScreen.payload(data, next, true));
        }
    }

    // Short label for a slot's key, e.g. "R" (long names like "Mouse 4" are trimmed)
    public static String label(int slot) {
        String text = SLOTS[slot].getTranslatedKeyMessage().getString();
        return text.length() <= 2 ? text.toUpperCase(java.util.Locale.ROOT) : text.substring(0, 2);
    }
}
