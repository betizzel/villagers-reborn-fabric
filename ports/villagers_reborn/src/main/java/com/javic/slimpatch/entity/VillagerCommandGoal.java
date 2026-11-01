package com.javic.slimpatch.entity;

import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class VillagerCommandGoal extends Goal {
   private static final double FOLLOW_SPEED = 0.9;
   private static final double RELAXED_WANDER_SPEED = 0.45;
   private static final double FOLLOW_START_DISTANCE_SQR = 16.0;
   private static final int FOLLOW_REPATH_TICKS = 10;
   private static final int FOLLOW_CLOSE_DOOR_TICKS = 8;
   private static final double RELAXED_WANDER_MIN_RADIUS = 3.0;
   private static final double RELAXED_WANDER_MAX_RADIUS = 6.0;
   private static final double RELAXED_WANDER_MAX_DISTANCE_SQR = 64.0;
   private static final double RELAXED_TARGET_REACHED_DISTANCE_SQR = 2.25;
   private static final double RELAXED_MOVE_AWAY_SPEED_SQR = 0.01;
   private static final double RELAXED_MOVE_AWAY_DOT = 0.08;
   private static final int RELAXED_WANDER_MIN_TICKS = 80;
   private static final int RELAXED_WANDER_MAX_TICKS = 140;
   private static final int RELAXED_RECENT_DAMAGE_TICKS = 60;
   private final Villager villager;
   private final CommandableVillager commandableVillager;
   private int nextRepathTick;
   private Vec3 lastTargetPos;
   private BlockPos openedDoorPos;
   private int closeDoorTick;
   private int relaxedWanderCooldown;
   private Vec3 relaxedTargetPos;

   public VillagerCommandGoal(Villager villager, CommandableVillager commandableVillager) {
      this.villager = villager;
      this.commandableVillager = commandableVillager;
      this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
   }

   public boolean canUse() {
      return this.commandableVillager.getCommandState() != VillagerCommandState.NONE;
   }

   public boolean canContinueToUse() {
      return this.commandableVillager.getCommandState() != VillagerCommandState.NONE;
   }

   public void start() {
      this.nextRepathTick = 0;
      this.lastTargetPos = null;
      this.openedDoorPos = null;
      this.closeDoorTick = 0;
      this.relaxedWanderCooldown = 0;
      this.relaxedTargetPos = null;
      this.villager.getNavigation().stop();
   }

   public void tick() {
      VillagerCommandState commandState = this.commandableVillager.getCommandState();
      if (commandState == VillagerCommandState.STAY) {
         this.tickOpenedDoor();
         this.villager.getNavigation().stop();
      } else if (commandState == VillagerCommandState.FOLLOW) {
         this.tickOpenedDoor();
         UUID targetUuid = this.commandableVillager.getCommandTargetUuid();
         if (targetUuid == null) {
            this.villager.getNavigation().stop();
         } else {
            Player player = this.villager.level().getPlayerByUUID(targetUuid);
            if (player == null || !player.isAlive() || player.level() != this.villager.level()) {
               this.villager.getNavigation().stop();
            } else if (this.commandableVillager.getFollowMode() == VillagerFollowMode.RELAXED) {
               this.tickRelaxedFollow(player);
            } else {
               this.tickCloseFollow(player);
            }
         }
      }
   }

   public void stop() {
      this.nextRepathTick = 0;
      this.lastTargetPos = null;
      this.openedDoorPos = null;
      this.closeDoorTick = 0;
      this.relaxedWanderCooldown = 0;
      this.relaxedTargetPos = null;
      this.villager.getNavigation().stop();
   }

   private void tickCloseFollow(Player player) {
      this.villager.getLookControl().setLookAt(player, 30.0F, 30.0F);
      if (this.villager.distanceToSqr(player) > 16.0) {
         this.tryOpenNearbyWoodenDoor();
         Vec3 targetPos = player.position();
         boolean shouldRepath = this.nextRepathTick <= 0
            || this.villager.getNavigation().isDone()
            || this.lastTargetPos == null
            || this.lastTargetPos.distanceToSqr(targetPos) > 1.0;
         if (shouldRepath) {
            this.villager.getNavigation().moveTo(player, 0.9);
            this.nextRepathTick = 10;
            this.lastTargetPos = targetPos;
         } else {
            this.nextRepathTick--;
         }
      } else {
         this.nextRepathTick = 0;
         this.lastTargetPos = null;
         this.villager.getNavigation().stop();
      }
   }

   private void tickRelaxedFollow(Player player) {
      double distanceToPlayerSqr = this.villager.distanceToSqr(player);
      if (!(distanceToPlayerSqr > 64.0) && (!(distanceToPlayerSqr > 16.0) || !this.isPlayerMovingAwayClearly(player))) {
         if (this.shouldSuspendRelaxedWander()) {
            this.clearRelaxedWander();
            if (distanceToPlayerSqr > 16.0) {
               this.tickCloseFollow(player);
            } else {
               this.villager.getNavigation().stop();
            }
         } else {
            this.nextRepathTick = 0;
            this.lastTargetPos = null;
            if (this.relaxedTargetPos != null
               && (this.villager.position().distanceToSqr(this.relaxedTargetPos) <= 2.25 || this.villager.getNavigation().isDone())) {
               this.clearRelaxedWander();
               this.relaxedWanderCooldown = this.getNextRelaxedWanderCooldown();
            }

            if (this.relaxedWanderCooldown > 0) {
               this.relaxedWanderCooldown--;
            }

            if (this.relaxedTargetPos == null) {
               this.villager.getNavigation().stop();
               if (this.relaxedWanderCooldown <= 0) {
                  this.tryStartRelaxedWander(player);
               }
            }
         }
      } else {
         this.clearRelaxedWander();
         this.relaxedWanderCooldown = 0;
         this.tickCloseFollow(player);
      }
   }

   private boolean shouldSuspendRelaxedWander() {
      return this.villager.getTarget() != null
         || this.commandableVillager.getCombatLockTicks() > 0
         || this.villager.tickCount - this.commandableVillager.getLastDamageTick() < 60;
   }

   private void tryStartRelaxedWander(Player player) {
      Vec3 origin = player.position();

      for (int attempt = 0; attempt < 8; attempt++) {
         double radius = Mth.lerp(this.villager.getRandom().nextDouble(), 3.0, 6.0);
         double angle = this.villager.getRandom().nextDouble() * Math.PI * 2.0;
         Vec3 candidate = origin.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
         if (!(this.villager.distanceToSqr(candidate) > 64.0) && this.villager.getNavigation().moveTo(candidate.x, candidate.y, candidate.z, 0.45)) {
            this.relaxedTargetPos = candidate;
            this.tryOpenNearbyWoodenDoor();
            return;
         }
      }

      this.relaxedWanderCooldown = this.getNextRelaxedWanderCooldown();
   }

   private int getNextRelaxedWanderCooldown() {
      return Mth.nextInt(this.villager.getRandom(), 80, 140);
   }

   private void clearRelaxedWander() {
      this.relaxedTargetPos = null;
   }

   private boolean isPlayerMovingAwayClearly(Player player) {
      Vec3 motion = player.getDeltaMovement();
      double horizontalSpeedSqr = motion.x * motion.x + motion.z * motion.z;
      if (horizontalSpeedSqr < 0.01) {
         return false;
      }

      Vec3 toPlayer = player.position().subtract(this.villager.position());
      double horizontalLength = Math.sqrt(toPlayer.x * toPlayer.x + toPlayer.z * toPlayer.z);
      if (horizontalLength < 1.0E-4) {
         return false;
      }

      double dot = (motion.x * toPlayer.x + motion.z * toPlayer.z) / horizontalLength;
      return dot > 0.08;
   }

   private void tryOpenNearbyWoodenDoor() {
      if (this.closeDoorTick <= 0 && this.villager.getNavigation().getPath() != null) {
         BlockPos currentPos = this.villager.blockPosition();

         for (BlockPos checkPos : BlockPos.betweenClosed(currentPos.offset(-1, 0, -1), currentPos.offset(1, 1, 1))) {
            BlockState state = this.villager.level().getBlockState(checkPos);
            if (state.getBlock() instanceof DoorBlock doorBlock && state.is(BlockTags.WOODEN_DOORS) && !doorBlock.isOpen(state)) {
               this.villager.level().setBlock(checkPos, (BlockState)state.setValue(DoorBlock.OPEN, true), 10);
               this.openedDoorPos = checkPos.immutable();
               this.closeDoorTick = 8;
               break;
            }
         }
      }
   }

   private void tickOpenedDoor() {
      if (this.closeDoorTick > 0) {
         this.closeDoorTick--;
         if (this.closeDoorTick == 0 && this.openedDoorPos != null) {
            BlockState state = this.villager.level().getBlockState(this.openedDoorPos);
            if (state.getBlock() instanceof DoorBlock doorBlock && (Boolean)state.getValue(DoorBlock.OPEN)) {
               this.villager.level().setBlock(this.openedDoorPos, (BlockState)state.setValue(DoorBlock.OPEN, false), 10);
            }

            this.openedDoorPos = null;
         }
      }
   }
}
