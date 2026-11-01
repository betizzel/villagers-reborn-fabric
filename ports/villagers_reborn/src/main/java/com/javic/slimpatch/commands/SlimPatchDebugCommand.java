package com.javic.slimpatch.commands;

import com.javic.slimpatch.entity.FamilyAgingHandler;
import com.javic.slimpatch.entity.FamilyVillager;
import com.javic.slimpatch.entity.FemaleVillagerEntity;
import com.javic.slimpatch.entity.MaleVillagerEntity;
import com.javic.slimpatch.entity.VillagerAgeStage;
import com.javic.slimpatch.entity.VillagerRelationshipData;
import com.javic.slimpatch.network.ModNetworking;
import com.javic.slimpatch.network.RelationshipSyncPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class SlimPatchDebugCommand {
    private static final double RANGE = 8.0;
    private SlimPatchDebugCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("villagersreborn").then(Commands.literal("debug")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("relationship").then(Commands.literal("set")
                        .then(Commands.argument("value", FloatArgumentType.floatArg())
                                .executes(ctx -> setRelationship(ctx, FloatArgumentType.getFloat(ctx, "value"))))))
                .then(Commands.literal("golden").then(Commands.literal("set")
                        .then(Commands.argument("value", FloatArgumentType.floatArg())
                                .executes(ctx -> setGolden(ctx, FloatArgumentType.getFloat(ctx, "value"))))))
                .then(Commands.literal("family")
                        .then(Commands.literal("expecting")
                                .then(Commands.literal("ready").executes(ctx -> setExpecting(ctx, 0)))
                                .then(Commands.literal("set").then(Commands.argument("ticksRemaining", IntegerArgumentType.integer(0))
                                        .executes(ctx -> setExpecting(ctx, IntegerArgumentType.getInteger(ctx, "ticksRemaining")))))
                                .then(Commands.literal("clear").executes(SlimPatchDebugCommand::clearExpecting)))
                        .then(Commands.literal("age")
                                .then(Commands.literal("ready").executes(SlimPatchDebugCommand::setAgeReady))
                                .then(Commands.literal("set")
                                        .then(Commands.literal("toddler").executes(ctx -> setAgeStage(ctx, VillagerAgeStage.TODDLER)))
                                        .then(Commands.literal("child").executes(ctx -> setAgeStage(ctx, VillagerAgeStage.CHILD)))
                                        .then(Commands.literal("teen").executes(ctx -> setAgeStage(ctx, VillagerAgeStage.TEEN)))
                                        .then(Commands.literal("adult").executes(ctx -> setAgeStage(ctx, VillagerAgeStage.ADULT))))))));
    }

    private static int setRelationship(CommandContext<CommandSourceStack> ctx, float value) {
        ServerPlayer player = getPlayer(ctx.getSource());
        if (player == null) return 0;
        Entity target = getLookedVillager(player);
        float applied;
        if (target instanceof MaleVillagerEntity male) {
            UUID id = player.getUUID();
            if (VillagerRelationshipData.usesPerPlayerRelationships(male)) {
                applied = VillagerRelationshipData.setRelationshipForPlayer(male, id, value, male.getRelationship());
            } else {
                male.setRelationship(value);
                applied = male.getRelationship();
            }
        } else if (target instanceof FemaleVillagerEntity female) {
            UUID id = player.getUUID();
            if (VillagerRelationshipData.usesPerPlayerRelationships(female)) {
                applied = VillagerRelationshipData.setRelationshipForPlayer(female, id, value, female.getRelationship());
            } else {
                female.setRelationship(value);
                applied = female.getRelationship();
            }
        } else {
            ctx.getSource().sendFailure(Component.literal("Look at a Villagers Reborn villager first."));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Set relationship to " + applied + "."), false);
        return 1;
    }

    private static int setGolden(CommandContext<CommandSourceStack> ctx, float value) {
        ServerPlayer player = getPlayer(ctx.getSource());
        if (player == null) return 0;
        Entity target = getLookedVillager(player);
        if (!(target instanceof FamilyVillager familyVillager)) {
            ctx.getSource().sendFailure(Component.literal("Look at a Villagers Reborn villager first."));
            return 0;
        }
        float applied;
        float relationship;
        if (target instanceof MaleVillagerEntity male) {
            if (VillagerRelationshipData.usesPerPlayerRelationships(male)) {
                applied = VillagerRelationshipData.setGoldenRelationshipForPlayer(male, player.getUUID(), value, male.getGoldenRelationship());
                relationship = VillagerRelationshipData.getRelationshipForPlayer(male, player.getUUID(), male.getRelationship());
            } else {
                male.setGoldenRelationship(value);
                applied = male.getGoldenRelationship();
                relationship = male.getRelationship();
            }
        } else if (target instanceof FemaleVillagerEntity female) {
            if (VillagerRelationshipData.usesPerPlayerRelationships(female)) {
                applied = VillagerRelationshipData.setGoldenRelationshipForPlayer(female, player.getUUID(), value, female.getGoldenRelationship());
                relationship = VillagerRelationshipData.getRelationshipForPlayer(female, player.getUUID(), female.getRelationship());
            } else {
                female.setGoldenRelationship(value);
                applied = female.getGoldenRelationship();
                relationship = female.getRelationship();
            }
        } else {
            familyVillager.setGoldenRelationship(value);
            applied = familyVillager.getGoldenRelationship();
            relationship = 0.5F;
        }
        ModNetworking.sendToClient(new RelationshipSyncPacket(target.getId(), relationship, applied), player);
        ctx.getSource().sendSuccess(() -> Component.literal("Set golden relationship to " + applied + "."), false);
        return 1;
    }

    private static int setExpecting(CommandContext<CommandSourceStack> ctx, int ticksRemaining) {
        ServerPlayer player = getPlayer(ctx.getSource());
        if (player == null) return 0;
        if (getLookedVillager(player) instanceof FamilyVillager villager) {
            long time = player.serverLevel().getGameTime();
            villager.setExpectingChild(true);
            villager.setExpectingParentPlayerUuid(player.getUUID());
            villager.setExpectingParentPlayerName(player.getGameProfile().getName());
            villager.setExpectingStartedAt(time);
            villager.setExpectingEndsAt(time + ticksRemaining);
            ctx.getSource().sendSuccess(() -> Component.literal("Set expecting child with " + ticksRemaining + " ticks remaining."), false);
            return 1;
        }
        ctx.getSource().sendFailure(Component.literal("Look at a Villagers Reborn family villager first."));
        return 0;
    }

    private static int clearExpecting(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = getPlayer(ctx.getSource());
        if (player == null) return 0;
        if (getLookedVillager(player) instanceof FamilyVillager villager) {
            villager.setExpectingChild(false);
            villager.setExpectingParentPlayerUuid(null);
            villager.setExpectingParentPlayerName("");
            villager.setExpectingStartedAt(0L);
            villager.setExpectingEndsAt(0L);
            ctx.getSource().sendSuccess(() -> Component.literal("Cleared expecting child state."), false);
            return 1;
        }
        ctx.getSource().sendFailure(Component.literal("Look at a Villagers Reborn family villager first."));
        return 0;
    }

    private static int setAgeStage(CommandContext<CommandSourceStack> ctx, VillagerAgeStage stage) {
        ServerPlayer player = getPlayer(ctx.getSource());
        if (player == null) return 0;
        if (getLookedVillager(player) instanceof FamilyVillager villager) {
            FamilyAgingHandler.setAgeStageWithTimer(villager, stage, player.serverLevel().getGameTime());
            ctx.getSource().sendSuccess(() -> Component.literal("Set family age stage to " + stage.name().toLowerCase() + "."), false);
            return 1;
        }
        ctx.getSource().sendFailure(Component.literal("Look at a Villagers Reborn family villager first."));
        return 0;
    }

    private static int setAgeReady(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = getPlayer(ctx.getSource());
        if (player == null) return 0;
        if (getLookedVillager(player) instanceof FamilyVillager villager) {
            FamilyAgingHandler.setReadyForNextStage(villager, player.serverLevel().getGameTime());
            ctx.getSource().sendSuccess(() -> Component.literal("Set family age transition ready for the next server tick."), false);
            return 1;
        }
        ctx.getSource().sendFailure(Component.literal("Look at a Villagers Reborn family villager first."));
        return 0;
    }

    private static ServerPlayer getPlayer(CommandSourceStack source) {
        try {
            return source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("This command requires a player looking at a villager."));
            return null;
        }
    }

    private static Entity getLookedVillager(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 view = player.getViewVector(1.0F);
        Vec3 end = eye.add(view.scale(RANGE));
        AABB box = player.getBoundingBox().expandTowards(view.scale(RANGE)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, box,
                entity -> entity instanceof MaleVillagerEntity || entity instanceof FemaleVillagerEntity, RANGE * RANGE);
        return hit == null ? null : hit.getEntity();
    }
}
