package com.javic.slimpatch.entity;

import com.javic.slimpatch.Config;
import com.javic.slimpatch.config.SlimPatchConfig;
import com.javic.slimpatch.familytree.FamilyTreeSavedData;
import com.javic.slimpatch.familytree.FamilyTreeVillagerSnapshot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;

public final class FamilyPregnancyHandler {
   public static final long EXPECTING_DURATION_TICKS = 24000L;

   private FamilyPregnancyHandler() {
   }

   public static Result startFamily(Villager villager, FamilyVillager familyVillager, ServerPlayer player) {
      if (player == null || villager == null || familyVillager == null) {
         return new Result(false, "slimpatch.message.family_not_spouse");
      }
      if (!SlimPatchConfig.effectiveEnableHavingChildren(player.serverLevel().getServer())) {
         return new Result(false, "slimpatch.message.family_children_disabled");
      }
      if (!villager.isAlive() || villager.level() != player.level()
            || familyVillager.getRelationshipStage() != VillagerRelationshipStage.MARRIED
            || !player.getUUID().equals(familyVillager.getSpousePlayerUuid())) {
         return new Result(false, "slimpatch.message.family_not_spouse");
      }
      if (familyVillager.getAgeStage() != VillagerAgeStage.ADULT) {
         return new Result(false, "slimpatch.message.family_requires_adult");
      }
      if (familyVillager.isExpectingChild()) {
         return new Result(false, "slimpatch.message.family_already_expecting");
      }
      long gameTime = villager.level().getGameTime();
      familyVillager.setExpectingChild(true);
      familyVillager.setExpectingParentPlayerUuid(player.getUUID());
      familyVillager.setExpectingParentPlayerName(player.getGameProfile().getName());
      familyVillager.setExpectingStartedAt(gameTime);
      familyVillager.setExpectingEndsAt(gameTime + getExpectingDurationTicks());
      return new Result(true, "slimpatch.message.family_started");
   }

   public static void startVillagerPregnancy(Villager carryingParent, FamilyVillager carryingFamilyVillager, Villager otherParent, long gameTime) {
      if (carryingParent != null && carryingFamilyVillager != null && otherParent != null) {
         carryingFamilyVillager.setExpectingChild(true);
         carryingFamilyVillager.setExpectingParentPlayerUuid(null);
         carryingFamilyVillager.setExpectingParentPlayerName("");
         carryingFamilyVillager.setExpectingOtherParentVillagerUuid(otherParent.getUUID());
         carryingFamilyVillager.setExpectingOtherParentVillagerName(otherParent.getName().getString());
         carryingFamilyVillager.setExpectingStartedAt(gameTime);
         carryingFamilyVillager.setExpectingEndsAt(gameTime + getExpectingDurationTicks());
      }
   }

   public static long getExpectingDurationTicks() {
      return Config.PREGNANCY_DURATION_TICKS.get();
   }

   public static void tickPregnancy(Villager villager, FamilyVillager familyVillager) {
      if (villager.level() instanceof ServerLevel serverLevel && familyVillager.isExpectingChild()
            && familyVillager.getExpectingParentPlayerUuid() == null
            && serverLevel.getGameTime() >= familyVillager.getExpectingEndsAt()) {
         Villager otherParent = resolveOtherParent(serverLevel, familyVillager);
         if (otherParent == null) {
            FamilyTreeVillagerSnapshot snapshot = FamilyTreeSavedData.get(serverLevel.getServer())
                  .getVillager(familyVillager.getExpectingOtherParentVillagerUuid());
            if (snapshot != null && !snapshot.isAlive()) {
               familyVillager.setExpectingChild(false);
            }
         } else {
            FamilyBirthHandler.spawnVillagerFamilyChild(serverLevel, villager, familyVillager, otherParent);
         }
      }
   }

   private static Villager resolveOtherParent(ServerLevel serverLevel, FamilyVillager familyVillager) {
      if (serverLevel != null && familyVillager != null && familyVillager.getExpectingOtherParentVillagerUuid() != null
            && serverLevel.getEntity(familyVillager.getExpectingOtherParentVillagerUuid()) instanceof Villager villager) {
         return villager.isAlive() ? villager : null;
      }
      return null;
   }

   public record Result(boolean success, String messageKey) {
   }
}
