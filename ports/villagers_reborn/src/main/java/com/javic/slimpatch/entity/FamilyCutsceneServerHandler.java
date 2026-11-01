package com.javic.slimpatch.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class FamilyCutsceneServerHandler {
   private static final long CUTSCENE_DURATION_TICKS = 200L;
   private static final double CEREMONY_REPOSITION_DISTANCE_SQR = 0.0025;
   private static final Map<UUID, FamilyCutsceneServerHandler.CeremonyState> ACTIVE = new HashMap<>();

   private FamilyCutsceneServerHandler() {
   }

   public static void start(Villager villager, ServerPlayer player) {
      if (villager != null && player != null && villager.level() == player.level()) {
         Vec3 holdPos = findCeremonyPosition(villager, player);
         if (holdPos == null) {
            holdPos = villager.position();
         } else {
            float yaw = getYawTowards(villager.position(), player.position());
            villager.moveTo(holdPos.x, holdPos.y, holdPos.z, yaw, villager.getXRot());
            villager.setYBodyRot(yaw);
            villager.setYHeadRot(yaw);
         }

         holdForCeremony(villager, holdPos);
         ACTIVE.put(villager.getUUID(), new FamilyCutsceneServerHandler.CeremonyState(player.getUUID(), holdPos, villager.level().getGameTime() + 200L));
         spawnParticles(villager, player, 2);
      }
   }

   public static boolean tick(Villager villager) {
      FamilyCutsceneServerHandler.CeremonyState state = ACTIVE.get(villager.getUUID());
      if (state == null) {
         return false;
      }

      if (villager.level().getGameTime() >= state.endsAt()) {
         finishCeremony(villager, state);
         ACTIVE.remove(villager.getUUID());
         return false;
      }

      if (villager.level().getPlayerByUUID(state.playerUuid()) instanceof ServerPlayer player && player.isAlive() && player.level() == villager.level()) {
         holdForCeremony(villager, state.holdPos());
         float yaw = getYawTowards(villager.position(), player.position());
         villager.setYRot(yaw);
         villager.setYBodyRot(yaw);
         villager.setYHeadRot(yaw);
         villager.getLookControl().setLookAt(player, 30.0F, 30.0F);
         if (villager.tickCount % 10 == 0) {
            spawnParticles(villager, player, 1);
         }

         return true;
      } else {
         finishCeremony(villager, state);
         ACTIVE.remove(villager.getUUID());
         return false;
      }
   }

   private static void finishCeremony(Villager villager, FamilyCutsceneServerHandler.CeremonyState state) {
      if (villager instanceof CommandableVillager commandableVillager
         && commandableVillager.getCommandState() == VillagerCommandState.STAY
         && state.holdPos() != null) {
         commandableVillager.setStayPos(BlockPos.containing(state.holdPos()));
         commandableVillager.setStayAnchorPos(state.holdPos());
      }

      villager.getNavigation().stop();
      villager.setDeltaMovement(Vec3.ZERO);
   }

   private static void holdForCeremony(Villager villager, Vec3 holdPos) {
      villager.setTarget(null);
      villager.setAggressive(false);
      villager.getBrain().eraseMemory(MemoryModuleType.NEAREST_HOSTILE);
      villager.getBrain().eraseMemory(MemoryModuleType.AVOID_TARGET);
      villager.getBrain().eraseMemory(MemoryModuleType.IS_PANICKING);
      villager.getBrain().eraseMemory(MemoryModuleType.HURT_BY);
      villager.getBrain().eraseMemory(MemoryModuleType.HURT_BY_ENTITY);
      VillagerCommandHandler.holdForDialogue(villager, holdPos);
      if (holdPos != null && villager.position().distanceToSqr(holdPos) > 0.0025) {
         villager.moveTo(holdPos.x, holdPos.y, holdPos.z, villager.getYRot(), villager.getXRot());
         villager.setDeltaMovement(Vec3.ZERO);
      }
   }

   private static Vec3 findCeremonyPosition(Villager villager, ServerPlayer player) {
      Vec3 offset = villager.position().subtract(player.position());
      Vec3 horizontal = new Vec3(offset.x, 0.0, offset.z);
      if (horizontal.lengthSqr() < 1.0E-4) {
         Vec3 look = player.getLookAngle();
         horizontal = new Vec3(-look.x, 0.0, -look.z);
      }

      if (horizontal.lengthSqr() < 1.0E-4) {
         horizontal = new Vec3(0.0, 0.0, 1.0);
      }

      horizontal = horizontal.normalize().scale(1.1);
      Vec3 candidate = new Vec3(player.getX() + horizontal.x, villager.getY(), player.getZ() + horizontal.z);
      AABB movedBox = villager.getBoundingBox()
         .move(candidate.x - villager.getX(), candidate.y - villager.getY(), candidate.z - villager.getZ());
      BlockPos below = BlockPos.containing(candidate.x, candidate.y - 0.2, candidate.z).below();
      return villager.level().noCollision(villager, movedBox) && !villager.level().getBlockState(below).isAir() ? candidate : null;
   }

   private static float getYawTowards(Vec3 from, Vec3 to) {
      double dx = to.x - from.x;
      double dz = to.z - from.z;
      return Mth.wrapDegrees((float)(Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F);
   }

   private static void spawnParticles(Villager villager, ServerPlayer player, int count) {
      if (villager.level() instanceof ServerLevel serverLevel) {
         Vec3 midpoint = villager.position().add(player.position()).scale(0.5);
         serverLevel.sendParticles(ParticleTypes.HEART, midpoint.x, midpoint.y + 1.35, midpoint.z, count, 0.22, 0.18, 0.22, 0.01);
      }
   }

   private record CeremonyState(UUID playerUuid, Vec3 holdPos, long endsAt) {
   }
}
