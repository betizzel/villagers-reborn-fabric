package com.javic.slimpatch.events;

import com.javic.slimpatch.entity.FemaleVillagerEntity;
import com.javic.slimpatch.entity.HumanZombieVillagerEntity;
import com.javic.slimpatch.entity.MaleVillagerEntity;
import com.javic.slimpatch.familytree.FamilyTreeTracker;
import com.javic.slimpatch.graves.GravePlacementHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class FamilyTreeEventHandler {
    private FamilyTreeEventHandler() {}

    public static void onEntityJoinLevel(Entity entity, ServerLevel level) {
        if (entity instanceof MaleVillagerEntity male) {
            FamilyTreeTracker.upsertVillager(level.getServer(), male);
        } else if (entity instanceof FemaleVillagerEntity female) {
            FamilyTreeTracker.upsertVillager(level.getServer(), female);
        }
    }

    public static void onLivingDeath(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        if (entity instanceof MaleVillagerEntity male) {
            FamilyTreeTracker.markVillagerDead(level.getServer(), male.getUUID());
            GravePlacementHandler.handleVillagerDeath(level, male);
        } else if (entity instanceof FemaleVillagerEntity female) {
            FamilyTreeTracker.markVillagerDead(level.getServer(), female.getUUID());
            GravePlacementHandler.handleVillagerDeath(level, female);
        } else if (entity instanceof HumanZombieVillagerEntity zombie) {
            GravePlacementHandler.handleZombieVillagerDeath(level, zombie);
        }
    }

    public static void onLivingConversionPost(LivingEntity original, LivingEntity outcome) {
        if (original.level() instanceof ServerLevel level
                && (original instanceof MaleVillagerEntity || original instanceof FemaleVillagerEntity)
                && outcome instanceof HumanZombieVillagerEntity) {
            GravePlacementHandler.clearGraveForHistoricalEntityUuid(level, original.getUUID());
        }
    }
}
