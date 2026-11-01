package com.javic.slimpatch.entity;

import com.javic.slimpatch.Config;
import com.javic.slimpatch.ModEntities;
import com.javic.slimpatch.config.SlimPatchConfig;
import com.javic.slimpatch.config.VillagerNameConfig;
import com.javic.slimpatch.data.WorldSkinData;
import com.javic.slimpatch.familytree.FamilyTreeSavedData;
import com.javic.slimpatch.familytree.FamilyTreeTracker;
import com.javic.slimpatch.familytree.FamilyTreeVillagerSnapshot;
import com.javic.slimpatch.graves.GraveIdentityResolver;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class FamilyCharmHandler {
   private static final double MAX_PLAYER_USE_DISTANCE_SQR = 64.0;
   private static final double MAX_PARENT_DISTANCE_SQR = 144.0;
   private static final Map<UUID, FamilyCharmHandler.SelectedParent> SELECTED_PARENTS = new HashMap<>();

   private FamilyCharmHandler() {
   }

   public static InteractionResult handleUseOnVillager(ServerPlayer player, ItemStack stack, Villager target) {
      if (!SlimPatchConfig.effectiveEnableHavingChildren(player.serverLevel().getServer())) {
         clearSelection(player, false);
         player.displayClientMessage(Component.translatable("slimpatch.message.family_children_disabled"), true);
         return InteractionResult.SUCCESS;
      }

      FamilyCharmHandler.ValidationResult targetValidation = validateCandidate(player, target);
      if (!targetValidation.valid()) {
         player.displayClientMessage(Component.translatable(targetValidation.messageKey()), true);
         return InteractionResult.SUCCESS;
      }

      FamilyCharmHandler.SelectedParent selectedParent = SELECTED_PARENTS.get(player.getUUID());
      if (selectedParent == null) {
         selectParent(player, target);
         return InteractionResult.SUCCESS;
      }

      Villager firstParent = resolveSelectedParent(player, selectedParent);
      if (firstParent == null) {
         SELECTED_PARENTS.remove(player.getUUID());
         selectParent(player, target);
         return InteractionResult.SUCCESS;
      }

      FamilyCharmHandler.ValidationResult firstValidation = validateCandidate(player, firstParent);
      if (!firstValidation.valid()) {
         SELECTED_PARENTS.remove(player.getUUID());
         player.displayClientMessage(Component.translatable(firstValidation.messageKey()), true);
         return InteractionResult.SUCCESS;
      }

      FamilyCharmHandler.ValidationResult pairValidation = validatePair(player, firstParent, target);
      if (!pairValidation.valid()) {
         if (pairValidation.clearSelection()) {
            SELECTED_PARENTS.remove(player.getUUID());
         }

         player.displayClientMessage(Component.translatable(pairValidation.messageKey()), true);
         return InteractionResult.SUCCESS;
      } else if (firstParent instanceof FamilyVillager firstFamilyVillager && target instanceof FamilyVillager secondFamilyVillager) {
         clearDeceasedSpouseIfNeeded(player.serverLevel(), firstParent);
         clearDeceasedSpouseIfNeeded(player.serverLevel(), target);
         Villager carryingParent = chooseFamilyCharmCarryingParent(firstParent, target);
         Villager otherParent = carryingParent == firstParent ? target : firstParent;
         FamilyVillager carryingFamilyVillager = carryingParent == firstParent ? firstFamilyVillager : secondFamilyVillager;
         String childLimitMessage = validateChildLimit(
            player.serverLevel(), GraveIdentityResolver.resolveHistoricalUuid(firstParent), GraveIdentityResolver.resolveHistoricalUuid(target)
         );
         if (childLimitMessage != null) {
            player.displayClientMessage(Component.translatable(childLimitMessage), true);
            return InteractionResult.SUCCESS;
         }

         linkSpouses(firstParent, firstFamilyVillager, target, secondFamilyVillager);
         FamilyPregnancyHandler.startVillagerPregnancy(carryingParent, carryingFamilyVillager, otherParent, player.serverLevel().getGameTime());
         FamilyTreeTracker.upsertVillager(player.serverLevel().getServer(), firstParent);
         FamilyTreeTracker.upsertVillager(player.serverLevel().getServer(), target);
         playFamilyCharmSuccessEffects(player.serverLevel(), firstParent, target);
         if (!player.getAbilities().instabuild) {
            stack.shrink(1);
         }

         SELECTED_PARENTS.remove(player.getUUID());
         player.displayClientMessage(
            Component.translatable("slimpatch.message.family_charm_family_started", new Object[]{firstParent.getName().getString(), target.getName().getString()}),
            true
         );
         return InteractionResult.SUCCESS;
      } else {
         player.displayClientMessage(Component.translatable("slimpatch.message.family_charm_invalid_target"), true);
         return InteractionResult.SUCCESS;
      }
   }

   public static boolean clearSelection(ServerPlayer player, boolean notify) {
      if (player == null) {
         return false;
      }

      FamilyCharmHandler.SelectedParent removed = SELECTED_PARENTS.remove(player.getUUID());
      if (removed != null && notify) {
         player.displayClientMessage(Component.translatable("slimpatch.message.family_charm_selection_cleared"), true);
      }

      return removed != null;
   }

   public static void notifyNearbyBirth(ServerLevel level, Villager mother, Villager otherParent, Villager child) {
      if (level != null && mother != null && otherParent != null && child != null) {
         String message = "slimpatch.message.family_charm_child_born";

         for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(mother) <= 144.0 || player.distanceToSqr(otherParent) <= 144.0) {
               player.displayClientMessage(Component.translatable(message, new Object[]{child.getName().getString()}), true);
            }
         }
      }
   }

   public static Villager spawnVillagerFamilyChild(ServerLevel level, Villager carryingParent, FamilyVillager carryingFamilyVillager, Villager otherParent) {
      String childGender = BabyGenderCharmHandler.resolveChildGender(carryingFamilyVillager, level.random.nextBoolean() ? "male" : "female");
      Villager child = createVillagerForGender(level, childGender);
      if (child instanceof FamilyVillager childFamilyVillager) {
         Vec3 spawnPos = findSpawnPosition(level, child, carryingParent, otherParent);
         child.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, carryingParent.getYRot(), 0.0F);
         applyChildData(level, child, childFamilyVillager, carryingParent, carryingFamilyVillager, otherParent, childGender);
         if (!level.addFreshEntity(child)) {
            return null;
         }

         FamilyTreeTracker.upsertVillager(level.getServer(), carryingParent);
         FamilyTreeTracker.upsertVillager(level.getServer(), otherParent);
         FamilyTreeTracker.upsertVillager(level.getServer(), child);
         FamilyTreeTracker.linkChildToParents(level.getServer(), child);
         notifyNearbyBirth(level, carryingParent, otherParent, child);
         return child;
      } else {
         return null;
      }
   }

   public static void onPlayerLoggedOut(ServerPlayer player) {
      SELECTED_PARENTS.remove(player.getUUID());
   }

   public static void onPlayerChangedDimension(ServerPlayer player) {
      SELECTED_PARENTS.remove(player.getUUID());
   }

   public static void onServerStopping(MinecraftServer server) {
      SELECTED_PARENTS.clear();
   }

   private static void selectParent(ServerPlayer player, Villager target) {
      SELECTED_PARENTS.put(
         player.getUUID(), new FamilyCharmHandler.SelectedParent(target.getUUID(), player.level().dimension(), player.level().getGameTime())
      );
      player.displayClientMessage(Component.translatable("slimpatch.message.family_charm_first_parent_selected"), true);
   }

   private static Villager resolveSelectedParent(ServerPlayer player, FamilyCharmHandler.SelectedParent selectedParent) {
      if (player != null && selectedParent != null) {
         if (!player.level().dimension().equals(selectedParent.dimension())) {
            return null;
         } else if (!(player.serverLevel().getEntity(selectedParent.parentVillagerUuid()) instanceof Villager villager && villager.isAlive())) {
            return null;
         } else {
            return player.distanceToSqr(villager) > 144.0 ? null : villager;
         }
      } else {
         return null;
      }
   }

   private static FamilyCharmHandler.ValidationResult validateCandidate(ServerPlayer player, Villager villager) {
      if (villager instanceof FamilyVillager familyVillager) {
         if (!(villager instanceof MaleVillagerEntity) && !(villager instanceof FemaleVillagerEntity)) {
            return FamilyCharmHandler.ValidationResult.failure("slimpatch.message.family_charm_invalid_target");
         } else if (!villager.isAlive()) {
            return FamilyCharmHandler.ValidationResult.failure("slimpatch.message.family_charm_invalid_target");
         } else if (player.distanceToSqr(villager) > 64.0) {
            return FamilyCharmHandler.ValidationResult.failure("slimpatch.message.family_charm_too_far");
         } else if (familyVillager.getAgeStage() != VillagerAgeStage.ADULT) {
            return FamilyCharmHandler.ValidationResult.failure("slimpatch.message.family_charm_must_be_adult");
         } else if (familyVillager.isExpectingChild()) {
            return FamilyCharmHandler.ValidationResult.failure("slimpatch.message.family_charm_already_expecting");
         } else if (familyVillager.getSpousePlayerUuid() != null) {
            return FamilyCharmHandler.ValidationResult.failure("slimpatch.message.family_charm_married_to_player");
         } else if (familyVillager.getRelationshipStage() == VillagerRelationshipStage.DATING) {
            return FamilyCharmHandler.ValidationResult.failure("slimpatch.message.family_charm_dating_player");
         } else {
            return VillagerFamilyData.isFamilyChildOf(villager, player) && familyVillager.getAgeStage() != VillagerAgeStage.ADULT
               ? FamilyCharmHandler.ValidationResult.failure("slimpatch.message.family_charm_child_of_player")
               : FamilyCharmHandler.ValidationResult.ok();
         }
      } else {
         return FamilyCharmHandler.ValidationResult.failure("slimpatch.message.family_charm_invalid_target");
      }
   }

   private static FamilyCharmHandler.ValidationResult validatePair(ServerPlayer player, Villager firstParent, Villager secondParent) {
      FamilyCharmHandler.ValidationResult secondValidation = validateCandidate(player, secondParent);
      if (!secondValidation.valid()) {
         return secondValidation;
      }

      if (firstParent.getUUID().equals(secondParent.getUUID())) {
         return FamilyCharmHandler.ValidationResult.failure("slimpatch.message.family_charm_same_villager");
      }

      if (!firstParent.level().dimension().equals(secondParent.level().dimension())) {
         return FamilyCharmHandler.ValidationResult.failureAndClear("slimpatch.message.family_charm_too_far");
      }

      if (firstParent.distanceToSqr(secondParent) > 144.0) {
         return FamilyCharmHandler.ValidationResult.failure("slimpatch.message.family_charm_too_far");
      }

      if (firstParent instanceof FamilyVillager firstFamilyVillager && secondParent instanceof FamilyVillager secondFamilyVillager) {
         UUID firstParentUuid = GraveIdentityResolver.resolveHistoricalUuid(firstParent);
         UUID secondParentUuid = GraveIdentityResolver.resolveHistoricalUuid(secondParent);
         UUID firstSpouseUuid = GraveIdentityResolver.resolveHistoricalUuid(player.serverLevel().getServer(), firstFamilyVillager.getSpouseVillagerUuid());
         UUID secondSpouseUuid = GraveIdentityResolver.resolveHistoricalUuid(player.serverLevel().getServer(), secondFamilyVillager.getSpouseVillagerUuid());
         if (firstSpouseUuid != null && !firstSpouseUuid.equals(secondParentUuid)) {
            FamilyCharmHandler.ValidationResult firstSpouseValidation = validateVillagerSpouse(player.serverLevel(), firstParent, secondParent);
            if (!firstSpouseValidation.valid()) {
               return firstSpouseValidation;
            }
         }

         if (secondSpouseUuid != null && !secondSpouseUuid.equals(firstParentUuid)) {
            FamilyCharmHandler.ValidationResult secondSpouseValidation = validateVillagerSpouse(player.serverLevel(), secondParent, firstParent);
            if (!secondSpouseValidation.valid()) {
               return secondSpouseValidation;
            }
         }

         return FamilyCharmHandler.ValidationResult.ok();
      } else {
         return FamilyCharmHandler.ValidationResult.failure("slimpatch.message.family_charm_invalid_target");
      }
   }

   private static FamilyCharmHandler.ValidationResult validateVillagerSpouse(ServerLevel level, Villager villager, Villager targetSpouse) {
      if (villager instanceof FamilyVillager familyVillager) {
         if (familyVillager.getSpousePlayerUuid() != null) {
            return FamilyCharmHandler.ValidationResult.failure("slimpatch.message.family_charm_married_to_player");
         } else {
            UUID spouseVillagerUuid = GraveIdentityResolver.resolveHistoricalUuid(level.getServer(), familyVillager.getSpouseVillagerUuid());
            UUID targetSpouseUuid = GraveIdentityResolver.resolveHistoricalUuid(targetSpouse);
            if (spouseVillagerUuid == null || spouseVillagerUuid.equals(targetSpouseUuid)) {
               return FamilyCharmHandler.ValidationResult.ok();
            } else {
               return isVillagerSpouseAlive(level, spouseVillagerUuid)
                  ? FamilyCharmHandler.ValidationResult.failure("slimpatch.message.family_charm_only_spouse")
                  : FamilyCharmHandler.ValidationResult.ok();
            }
         }
      } else {
         return FamilyCharmHandler.ValidationResult.failure("slimpatch.message.family_charm_invalid_target");
      }
   }

   private static boolean isVillagerSpouseAlive(ServerLevel level, UUID spouseVillagerUuid) {
      if (level != null && spouseVillagerUuid != null) {
         spouseVillagerUuid = GraveIdentityResolver.resolveHistoricalUuid(level.getServer(), spouseVillagerUuid);
         if (level.getEntity(spouseVillagerUuid) instanceof Villager spouseVillager) {
            return spouseVillager.isAlive();
         } else {
            FamilyTreeVillagerSnapshot spouseSnapshot = FamilyTreeSavedData.get(level.getServer()).getVillager(spouseVillagerUuid);
            return spouseSnapshot == null || spouseSnapshot.isAlive();
         }
      } else {
         return false;
      }
   }

   private static void clearDeceasedSpouseIfNeeded(ServerLevel level, Villager villager) {
      if (level != null && villager instanceof FamilyVillager familyVillager) {
         UUID villagerUuid = GraveIdentityResolver.resolveHistoricalUuid(villager);
         UUID spouseVillagerUuid = GraveIdentityResolver.resolveHistoricalUuid(level.getServer(), familyVillager.getSpouseVillagerUuid());
         if (spouseVillagerUuid != null && !isVillagerSpouseAlive(level, spouseVillagerUuid)) {
            familyVillager.setSpouseVillagerUuid(null);
            familyVillager.setSpouseVillagerName("");
            FamilyTreeSavedData data = FamilyTreeSavedData.get(level.getServer());
            FamilyTreeVillagerSnapshot villagerSnapshot = data.getOrCreateVillager(villagerUuid);
            if (villagerSnapshot != null) {
               villagerSnapshot.setSpouseVillagerUuid(null);
               villagerSnapshot.setSpouseVillagerName("");
               data.putVillager(villagerSnapshot);
            }

            FamilyTreeVillagerSnapshot spouseSnapshot = data.getVillager(spouseVillagerUuid);
            if (spouseSnapshot != null && villagerUuid.equals(spouseSnapshot.getSpouseVillagerUuid())) {
               spouseSnapshot.setSpouseVillagerUuid(null);
               spouseSnapshot.setSpouseVillagerName("");
               data.putVillager(spouseSnapshot);
            }

            Entity spouseEntity = level.getEntity(spouseVillagerUuid);
            if (spouseEntity instanceof FamilyVillager spouseFamilyVillager
               && villagerUuid.equals(GraveIdentityResolver.resolveHistoricalUuid(level.getServer(), spouseFamilyVillager.getSpouseVillagerUuid()))) {
               spouseFamilyVillager.setSpouseVillagerUuid(null);
               spouseFamilyVillager.setSpouseVillagerName("");
               if (spouseEntity instanceof Villager spouseVillager) {
                  FamilyTreeTracker.upsertVillager(level.getServer(), spouseVillager);
               }
            }

            FamilyTreeTracker.upsertVillager(level.getServer(), villager);
         }
      }
   }

   private static String validateChildLimit(ServerLevel level, UUID parentA, UUID parentB) {
      if (level != null && parentA != null && parentB != null) {
         FamilyTreeSavedData savedData = FamilyTreeSavedData.get(level.getServer());
         FamilyTreeVillagerSnapshot snapshot = savedData.getVillager(parentA);
         if (snapshot == null) {
            return null;
         }

         int children = 0;

         for (UUID childUuid : snapshot.getChildVillagerUuids()) {
            if (childUuid != null) {
               FamilyTreeVillagerSnapshot childSnapshot = savedData.getVillager(childUuid);
               if (childSnapshot != null) {
                  boolean matchesForward = parentA.equals(childSnapshot.getParentVillagerUuid()) && parentB.equals(childSnapshot.getParentVillager2Uuid());
                  boolean matchesReverse = parentB.equals(childSnapshot.getParentVillagerUuid()) && parentA.equals(childSnapshot.getParentVillager2Uuid());
                  if (matchesForward || matchesReverse) {
                     if (++children >= (Integer)Config.FAMILY_CHARM_MAX_CHILDREN_PER_PAIR.get()) {
                        return "slimpatch.message.family_charm_child_limit";
                     }
                  }
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static void linkSpouses(Villager firstParent, FamilyVillager firstFamilyVillager, Villager secondParent, FamilyVillager secondFamilyVillager) {
      firstFamilyVillager.setSpouseVillagerUuid(GraveIdentityResolver.resolveHistoricalUuid(secondParent));
      firstFamilyVillager.setSpouseVillagerName(secondParent.getName().getString());
      secondFamilyVillager.setSpouseVillagerUuid(GraveIdentityResolver.resolveHistoricalUuid(firstParent));
      secondFamilyVillager.setSpouseVillagerName(firstParent.getName().getString());
   }

   private static Villager chooseFamilyCharmCarryingParent(Villager firstParent, Villager secondParent) {
      boolean firstFemale = firstParent instanceof FemaleVillagerEntity;
      boolean secondFemale = secondParent instanceof FemaleVillagerEntity;
      if (firstFemale != secondFemale) {
         return firstFemale ? firstParent : secondParent;
      } else {
         return compareUuids(firstParent.getUUID(), secondParent.getUUID()) <= 0 ? firstParent : secondParent;
      }
   }

   private static int compareUuids(UUID first, UUID second) {
      int mostSignificantComparison = Long.compare(first.getMostSignificantBits(), second.getMostSignificantBits());
      return mostSignificantComparison != 0 ? mostSignificantComparison : Long.compare(first.getLeastSignificantBits(), second.getLeastSignificantBits());
   }

   private static Villager createVillagerForGender(ServerLevel level, String gender) {
      EntityType<? extends Villager> entityType = "female".equals(BirthScreenData.normalizeGender(gender))
         ? (EntityType)ModEntities.FEMALE_VILLAGER
         : (EntityType)ModEntities.MALE_VILLAGER;
      return (Villager)entityType.create(level);
   }

   private static Vec3 findSpawnPosition(ServerLevel level, Entity entity, Villager mother, Villager otherParent) {
      Vec3[] anchors = new Vec3[]{mother.position(), otherParent.position()};

      for (Vec3 anchor : anchors) {
         for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
               BlockPos pos = BlockPos.containing(anchor.x + dx, anchor.y, anchor.z + dz);
               Vec3 spawnPos = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
               entity.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, mother.getYRot(), 0.0F);
               if (level.noCollision(entity)) {
                  return spawnPos;
               }

               Vec3 raisedSpawnPos = spawnPos.add(0.0, 1.0, 0.0);
               entity.moveTo(raisedSpawnPos.x, raisedSpawnPos.y, raisedSpawnPos.z, mother.getYRot(), 0.0F);
               if (level.noCollision(entity)) {
                  return raisedSpawnPos;
               }
            }
         }
      }

      return mother.position().add(0.0, 1.0, 0.0);
   }

   private static void applyChildData(
      ServerLevel level,
      Villager child,
      FamilyVillager childFamilyVillager,
      Villager mother,
      FamilyVillager motherFamilyVillager,
      Villager otherParent,
      String childGender
   ) {
      String normalizedGender = BirthScreenData.normalizeGender(childGender);
      String name = "female".equals(normalizedGender)
         ? VillagerNameConfig.getRandomFemaleName(level.random)
         : VillagerNameConfig.getRandomMaleName(level.random);
      child.setCustomName(Component.literal(name));
      child.setCustomNameVisible(true);
      childFamilyVillager.setBornFromFamilySystem(false);
      childFamilyVillager.setParentPlayerUuid(null);
      childFamilyVillager.setParentPlayerName("");
      childFamilyVillager.setParentVillagerUuid(mother.getUUID());
      childFamilyVillager.setParentVillagerName(mother.getName().getString());
      childFamilyVillager.setParentVillager2Uuid(otherParent.getUUID());
      childFamilyVillager.setParentVillager2Name(otherParent.getName().getString());
      childFamilyVillager.setFamilyOwnerPlayerUuid(null);
      childFamilyVillager.setFamilyOwnerPlayerName("");
      childFamilyVillager.setNaturalFamilyMember(true);
      childFamilyVillager.setSpouseVillagerUuid(null);
      childFamilyVillager.setSpouseVillagerName("");
      childFamilyVillager.setRelationshipStage(VillagerRelationshipStage.FRIENDSHIP);
      UUID groupId = motherFamilyVillager.getNaturalFamilyGroupId();
      if (groupId == null && otherParent instanceof FamilyVillager otherParentFamilyVillager) {
         groupId = otherParentFamilyVillager.getNaturalFamilyGroupId();
      }

      if (groupId == null) {
         groupId = UUID.randomUUID();
      }

      motherFamilyVillager.setNaturalFamilyGroupId(groupId);
      motherFamilyVillager.setNaturalFamilyMember(true);
      if (otherParent instanceof FamilyVillager otherParentFamilyVillager) {
         otherParentFamilyVillager.setNaturalFamilyGroupId(groupId);
         otherParentFamilyVillager.setNaturalFamilyMember(true);
      }

      childFamilyVillager.setNaturalFamilyGroupId(groupId);
      FamilyAgingHandler.initializeForBirth(childFamilyVillager, level.getGameTime());
      VillagerPersonality personality = BirthScreenData.getRandomPersonality(level.random);
      String skinType = BirthScreenData.normalizeSkinType(WorldSkinData.get(level).getTheme());
      int skinId = BirthScreenData.getRandomSkinId(normalizedGender, skinType, level.random);
      String curatedSkinPath = BirthScreenData.getCuratedSkinResourcePath(normalizedGender, skinType, skinId);
      if (child instanceof MaleVillagerEntity maleChild) {
         maleChild.setPersonality(personality);
         maleChild.setSkinIndex(skinId);
         maleChild.setSavedSkinInput("");
         maleChild.setCustomSkinPath(curatedSkinPath == null ? "" : curatedSkinPath);
      } else if (child instanceof FemaleVillagerEntity femaleChild) {
         femaleChild.setPersonality(personality);
         femaleChild.setSkinIndex(skinId);
         femaleChild.setSavedSkinInput("");
         femaleChild.setCustomSkinPath(curatedSkinPath == null ? "" : curatedSkinPath);
      }
   }

   private static void playFamilyCharmSuccessEffects(ServerLevel level, Villager firstParent, Villager secondParent) {
      if (level != null && firstParent != null && secondParent != null) {
         Vec3 midpoint = firstParent.position().add(secondParent.position()).scale(0.5);
         level.playSound(null, BlockPos.containing(midpoint), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.55F, 1.2F);
         level.sendParticles(ParticleTypes.HEART, midpoint.x, midpoint.y + 1.2, midpoint.z, 6, 0.18, 0.18, 0.18, 0.01);
         level.sendParticles(ParticleTypes.HAPPY_VILLAGER, midpoint.x, midpoint.y + 0.9, midpoint.z, 6, 0.22, 0.2, 0.22, 0.01);
      }
   }

   private record SelectedParent(UUID parentVillagerUuid, ResourceKey<Level> dimension, long selectedAt) {
   }

   private record ValidationResult(boolean valid, String messageKey, boolean clearSelection) {
      private static FamilyCharmHandler.ValidationResult ok() {
         return new FamilyCharmHandler.ValidationResult(true, "", false);
      }

      private static FamilyCharmHandler.ValidationResult failure(String messageKey) {
         return new FamilyCharmHandler.ValidationResult(false, messageKey, false);
      }

      private static FamilyCharmHandler.ValidationResult failureAndClear(String messageKey) {
         return new FamilyCharmHandler.ValidationResult(false, messageKey, true);
      }
   }
}
