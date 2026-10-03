package com.toddlandis.skillcraft.command;

import java.util.Arrays;
import java.util.Collection;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.toddlandis.skillcraft.SkillCraft;
import com.toddlandis.skillcraft.skill.PlayerSkills;
import com.toddlandis.skillcraft.skill.Skill;
import com.toddlandis.skillcraft.skill.SkillManager;
import com.toddlandis.skillcraft.skill.XpTable;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

// Admin/testing command:
//   /skill get <player>
//   /skill set <player> <skill> <level>
//   /skill addxp <player> <skill> <amount>
//   /skill reset <player>
@EventBusSubscriber(modid = SkillCraft.MODID)
public class SkillCommand {
    private static final DynamicCommandExceptionType UNKNOWN_SKILL =
            new DynamicCommandExceptionType(name -> Component.translatable("commands.skillcraft.unknown_skill", name));

    @SubscribeEvent
    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("skill")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("get")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(c -> get(c.getSource(), EntityArgument.getPlayer(c, "target")))))
                .then(Commands.literal("set")
                        .then(Commands.argument("target", EntityArgument.players())
                                .then(skillArgument()
                                        .then(Commands.argument("level", IntegerArgumentType.integer(1, XpTable.MAX_LEVEL))
                                                .executes(c -> set(c, IntegerArgumentType.getInteger(c, "level")))))))
                .then(Commands.literal("addxp")
                        .then(Commands.argument("target", EntityArgument.players())
                                .then(skillArgument()
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(c -> addXp(c, IntegerArgumentType.getInteger(c, "amount")))))))
                .then(Commands.literal("reset")
                        .then(Commands.argument("target", EntityArgument.players())
                                .executes(SkillCommand::reset))));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> skillArgument() {
        return Commands.argument("skill", StringArgumentType.word())
                .suggests((c, builder) -> SharedSuggestionProvider.suggest(
                        Arrays.stream(Skill.values()).map(Skill::getSerializedName), builder));
    }

    private static Skill getSkill(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        String name = StringArgumentType.getString(c, "skill");
        try {
            return Skill.byName(name);
        } catch (IllegalArgumentException e) {
            throw UNKNOWN_SKILL.create(name);
        }
    }

    private static int get(CommandSourceStack source, ServerPlayer player) {
        PlayerSkills skills = SkillManager.get(player);
        source.sendSuccess(() -> Component.translatable("commands.skillcraft.get.header", player.getDisplayName(), skills.totalLevel()), false);
        for (Skill skill : Skill.values()) {
            source.sendSuccess(() -> Component.translatable("commands.skillcraft.get.line",
                    skill.displayName(), skills.getLevel(skill), skills.getXp(skill)), false);
        }
        return skills.totalLevel();
    }

    private static int set(CommandContext<CommandSourceStack> c, int level) throws CommandSyntaxException {
        Skill skill = getSkill(c);
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(c, "target");
        for (ServerPlayer player : targets) SkillManager.setXp(player, skill, XpTable.xpForLevel(level));
        c.getSource().sendSuccess(() -> Component.translatable("commands.skillcraft.set", skill.displayName(), level, targets.size()), true);
        return targets.size();
    }

    private static int addXp(CommandContext<CommandSourceStack> c, int amount) throws CommandSyntaxException {
        Skill skill = getSkill(c);
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(c, "target");
        for (ServerPlayer player : targets) SkillManager.addXp(player, skill, amount);
        c.getSource().sendSuccess(() -> Component.translatable("commands.skillcraft.addxp", amount, skill.displayName(), targets.size()), true);
        return targets.size();
    }

    private static int reset(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(c, "target");
        for (ServerPlayer player : targets) {
            for (Skill skill : Skill.values()) SkillManager.setXp(player, skill, 0);
        }
        c.getSource().sendSuccess(() -> Component.translatable("commands.skillcraft.reset", targets.size()), true);
        return targets.size();
    }
}
