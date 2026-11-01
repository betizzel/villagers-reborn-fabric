package com.javic.slimpatch.entity;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.Vec3;

public class ReturnHomeAtNightGoal extends Goal {
   private static final double RETURN_SPEED = 0.6;
   private static final double HOME_ARRIVAL_DISTANCE_SQR = 2.25;
   private static final int REPATH_TICKS = 20;
   private static final int RETRY_COOLDOWN_TICKS = 60;
   private final Villager villager;
   private final CommandableVillager commandableVillager;
   private int nextRepathTick;
   private int retryCooldownTick;

   public ReturnHomeAtNightGoal(Villager villager, CommandableVillager commandableVillager) {
      this.villager = villager;
      this.commandableVillager = commandableVillager;
      this.setFlags(EnumSet.of(Flag.MOVE));
   }

   public boolean canUse() {
      return this.shouldReturnHome();
   }

   public boolean canContinueToUse() {
      return this.shouldReturnHome();
   }

   public void start() {
      this.nextRepathTick = 0;
      this.retryCooldownTick = 0;
      this.tryMoveHome();
   }

   public void tick() {
      if (this.nextRepathTick > 0) {
         this.nextRepathTick--;
      }

      if (this.retryCooldownTick > 0) {
         this.retryCooldownTick--;
      } else {
         if (this.villager.getNavigation().isDone() && this.nextRepathTick <= 0) {
            this.tryMoveHome();
         }
      }
   }

   public void stop() {
      this.nextRepathTick = 0;
      this.retryCooldownTick = 0;
      this.villager.getNavigation().stop();
   }

   private boolean shouldReturnHome() {
      if (this.villager.level().isClientSide) {
         return false;
      } else if (this.commandableVillager.getCommandState() != VillagerCommandState.NONE) {
         return false;
      } else if (!this.commandableVillager.hasHome()) {
         return false;
      } else {
         BlockPos homePos = this.commandableVillager.getHomePos();
         if (homePos == null) {
            return false;
         } else if (this.commandableVillager.getHomeDimension() == null || this.villager.level().dimension() != this.commandableVillager.getHomeDimension()) {
            return false;
         } else {
            return !this.isNightOrDusk() ? false : this.villager.position().distanceToSqr(Vec3.atBottomCenterOf(homePos)) > 2.25;
         }
      }
   }

   private boolean isNightOrDusk() {
      long timeOfDay = this.villager.level().getDayTime() % 24000L;
      return timeOfDay >= 12000L && timeOfDay < 23000L;
   }

   private void tryMoveHome() {
      BlockPos homePos = this.commandableVillager.getHomePos();
      if (homePos == null) {
         this.villager.getNavigation().stop();
      } else {
         boolean started = this.villager.getNavigation().moveTo(homePos.getX() + 0.5, homePos.getY(), homePos.getZ() + 0.5, 0.6);
         if (started) {
            this.nextRepathTick = 20;
            this.retryCooldownTick = 0;
         } else {
            this.nextRepathTick = 0;
            this.retryCooldownTick = 60;
         }
      }
   }
}
