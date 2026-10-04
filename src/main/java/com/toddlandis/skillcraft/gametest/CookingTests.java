package com.toddlandis.skillcraft.gametest;

import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.cooking.CookingRangeBlockEntity;
import com.toddlandis.skillcraft.cooking.CookingRecipe;
import com.toddlandis.skillcraft.registry.ModBlocks;
import com.toddlandis.skillcraft.registry.ModItems;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.SkillManager;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.phys.Vec3;

// Cooking: burn chance maths, a novice and a master cooking at two ranges, and vanilla
// furnaces/smokers/campfires no longer cooking food.
final class CookingTests {
    private static final int FOOD = 4;

    private CookingTests() {}

    static void range(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        CookingRecipe cod = CookingRecipe.get(new ItemStack(Items.COD));
        helper.assertTrue(cod != null && cod.result() == Items.COOKED_COD, "raw cod has no Cooking Range recipe");
        helper.assertTrue(Math.abs(cod.burnChance(cod.level()) - CookingRecipe.MAX_BURN) < 1e-9, "burn chance at the required level should be " + CookingRecipe.MAX_BURN);
        helper.assertTrue(cod.burnChance(cod.stopBurn()) == 0, "food still burns at its stop-burn level");
        helper.assertTrue(cod.burnChance(cod.level() + 10) < cod.burnChance(cod.level() + 5), "burn chance should fall with level");

        // Vanilla cooking is switched off: the furnace, smoker and campfire no longer cook food
        SingleRecipeInput rawCod = new SingleRecipeInput(new ItemStack(Items.COD));
        helper.assertTrue(level.recipeAccess().getRecipeFor(RecipeType.SMELTING, rawCod, level, (net.minecraft.resources.ResourceKey<net.minecraft.world.item.crafting.Recipe<?>>) null).isEmpty(), "furnaces can still cook cod");
        helper.assertTrue(level.recipeAccess().getRecipeFor(RecipeType.SMOKING, rawCod, level, (net.minecraft.resources.ResourceKey<net.minecraft.world.item.crafting.Recipe<?>>) null).isEmpty(), "smokers can still cook cod");
        helper.assertTrue(level.recipeAccess().getRecipeFor(RecipeType.CAMPFIRE_COOKING, rawCod, level, (net.minecraft.resources.ResourceKey<net.minecraft.world.item.crafting.Recipe<?>>) null).isEmpty(), "campfires can still cook cod");

        // A novice (level 1, 50% burn on cod) and a master (level 99, never burns)
        ServerPlayer novice = AbilityTests.player(helper, "skillcraft_cook_novice", new Vec3(3, 1, 3), 0f);
        ServerPlayer master = AbilityTests.player(helper, "skillcraft_cook_master", new Vec3(12, 1, 3), 0f);
        AbilityTests.level(master, Skill.COOKING, 99);
        CookingRangeBlockEntity noviceRange = load(helper, new BlockPos(4, 1, 8), novice);
        CookingRangeBlockEntity masterRange = load(helper, new BlockPos(11, 1, 8), master);

        int ticks = FOOD * cod.time() + 40;
        helper.runAfterDelay(ticks, () -> {
            int noviceCooked = noviceRange.getItem(CookingRangeBlockEntity.SLOT_OUTPUT).getCount();
            int noviceBurnt = noviceRange.getItem(CookingRangeBlockEntity.SLOT_BURNT).getCount();
            int masterCooked = masterRange.getItem(CookingRangeBlockEntity.SLOT_OUTPUT).getCount();
            int masterBurnt = masterRange.getItem(CookingRangeBlockEntity.SLOT_BURNT).getCount();
            helper.assertTrue(noviceCooked + noviceBurnt == FOOD, "novice range finished " + (noviceCooked + noviceBurnt) + "/" + FOOD);
            helper.assertTrue(masterCooked == FOOD && masterBurnt == 0, "master should cook all " + FOOD + " without burning: " + masterCooked + " cooked, " + masterBurnt + " burnt");
            helper.assertTrue(noviceRange.getItem(CookingRangeBlockEntity.SLOT_INPUT).isEmpty(), "raw food left in the novice range");
            helper.assertTrue(masterRange.getItem(CookingRangeBlockEntity.SLOT_OUTPUT).is(Items.COOKED_COD), "the range made the wrong item");
            if (noviceBurnt > 0) {
                helper.assertTrue(noviceRange.getItem(CookingRangeBlockEntity.SLOT_BURNT).is(ModItems.BURNT_FOOD.get()), "burnt slot holds the wrong item");
            }

            // XP is paid only for food that didn't burn, when it's collected
            double before = SkillManager.get(novice).getXp(Skill.COOKING);
            noviceRange.awardStoredXp(novice);
            double gained = SkillManager.get(novice).getXp(Skill.COOKING) - before;
            helper.assertTrue(Math.abs(gained - noviceCooked * cod.xp()) < 0.01, "novice should get " + noviceCooked * cod.xp() + " XP, got " + gained);
            SkillCraft.LOGGER.info("[cooking] {} cod: level 1 -> {} cooked, {} burnt (+{} XP); level 99 -> {} cooked, {} burnt",
                    FOOD, noviceCooked, noviceBurnt, gained, masterCooked, masterBurnt);
            helper.succeed();
        });
    }

    private static CookingRangeBlockEntity load(GameTestHelper helper, BlockPos relative, ServerPlayer cook) {
        helper.setBlock(relative, ModBlocks.COOKING_RANGE.get().defaultBlockState());
        CookingRangeBlockEntity range = (CookingRangeBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(relative));
        range.setItem(CookingRangeBlockEntity.SLOT_INPUT, new ItemStack(Items.COD, FOOD));
        range.setItem(CookingRangeBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL, 1));
        range.setCook(cook);
        return range;
    }
}
