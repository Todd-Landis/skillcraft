package com.toddlandis.skillcraft;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class only loads on the game client, never on a dedicated server,
// so it is the safe place for screens, HUD rendering and key bindings.
@Mod(value = SkillCraft.MODID, dist = Dist.CLIENT)
public class SkillCraftClient {
    public SkillCraftClient(ModContainer container) {
        // Adds a config button for this mod on the Mods screen.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
