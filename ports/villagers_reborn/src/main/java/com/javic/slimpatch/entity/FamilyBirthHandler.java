package com.javic.slimpatch.entity;

import com.javic.slimpatch.ModEntities;
import com.javic.slimpatch.fabric.PersistentEntityData;
import com.javic.slimpatch.data.WorldSkinData;
import com.javic.slimpatch.events.MultiplayerSkinSyncHandler;
import com.javic.slimpatch.familytree.FamilyTreeTracker;
import com.javic.slimpatch.network.ModNetworking;
import com.javic.slimpatch.network.RelationshipSyncPacket;
import com.javic.slimpatch.util.MultiplayerSkinStorage;
import java.io.IOException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.Vec3;

public final class FamilyBirthHandler {
   private FamilyBirthHandler() {
   }

   public static void spawnVillagerFamilyChild(ServerLevel level, Villager carryingParent, FamilyVillager carryingFamilyVillager, Villager otherParent) {
      if (level != null && carryingParent != null && carryingFamilyVillager != null && otherParent != null) {
         Villager child = FamilyCharmHandler.spawnVillagerFamilyChild(level, carryingParent, carryingFamilyVillager, otherParent);
         if (child != null) {
            carryingFamilyVillager.setExpectingChild(false);
            carryingFamilyVillager.setExpectingParentPlayerUuid(null);
            carryingFamilyVillager.setExpectingParentPlayerName("");
            carryingFamilyVillager.setExpectingOtherParentVillagerUuid(null);
            carryingFamilyVillager.setExpectingOtherParentVillagerName("");
            carryingFamilyVillager.setForcedBabyGender("");
            carryingFamilyVillager.setExpectingStartedAt(0L);
            carryingFamilyVillager.setExpectingEndsAt(0L);
            playBirthEffects(level, carryingParent, child);
         }
      }
   }

   public static String spawnChild(
      ServerPlayer player,
      Villager spouse,
      FamilyVillager familyVillager,
      String childName,
      String childGender,
      VillagerPersonality personality,
      int selectedSkinId,
      boolean customSkin,
      String customSkinInput,
      byte[] customSkinPngData
   ) {
      ServerLevel level = player.serverLevel();
      Villager child = createChild(level, childGender);
      if (child == null) {
         return "slimpatch.message.birth_invalid_skin";
      }

      Vec3 spawnPos = findSpawnPosition(level, child, spouse, player);
      child.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, spouse.getYRot(), 0.0F);
      String skinType = BirthScreenData.normalizeSkinType(WorldSkinData.get(level).getTheme());
      if (child instanceof MaleVillagerEntity male) {
         applyChildData(male, player, spouse, childName, personality, selectedSkinId, childGender, skinType, customSkin, customSkinInput, customSkinPngData);
      } else if (child instanceof FemaleVillagerEntity female) {
         applyChildData(female, player, spouse, childName, personality, selectedSkinId, childGender, skinType, customSkin, customSkinInput, customSkinPngData);
      }

      if (!level.addFreshEntity(child)) {
         return "slimpatch.message.birth_invalid_skin";
      }

      if (customSkin) {
         syncCustomSkin(child, customSkinInput, customSkinPngData);
      }

      FamilyTreeTracker.upsertVillager(level.getServer(), child);
      FamilyTreeTracker.upsertPlayer(level.getServer(), player.getUUID(), player.getGameProfile().getName());
      FamilyTreeTracker.linkChildToParents(level.getServer(), child);
      applyParentInitialRelationship(child, player);
      clearExpecting(spouse, familyVillager, player);
      playBirthEffects(level, spouse, child);
      return null;
   }

   private static Villager createChild(ServerLevel level, String childGender) {
      EntityType<? extends Villager> type = "female".equals(BirthScreenData.normalizeGender(childGender))
         ? (EntityType)ModEntities.FEMALE_VILLAGER
         : (EntityType)ModEntities.MALE_VILLAGER;
      return (Villager)type.create(level);
   }

   private static Vec3 findSpawnPosition(ServerLevel level, Entity child, Villager spouse, ServerPlayer player) {
      Vec3[] anchors = new Vec3[]{spouse.position(), player.position()};

      for (Vec3 anchor : anchors) {
         for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
               BlockPos pos = BlockPos.containing(anchor.x + dx, anchor.y, anchor.z + dz);
               Vec3 spawnPos = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
               child.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, spouse.getYRot(), 0.0F);
               if (level.noCollision(child)) {
                  return spawnPos;
               }

               Vec3 raisedSpawnPos = spawnPos.add(0.0, 1.0, 0.0);
               child.moveTo(raisedSpawnPos.x, raisedSpawnPos.y, raisedSpawnPos.z, spouse.getYRot(), 0.0F);
               if (level.noCollision(child)) {
                  return raisedSpawnPos;
               }
            }
         }
      }

      return spouse.position().add(0.0, 1.0, 0.0);
   }

   private static void applyChildData(
      MaleVillagerEntity child,
      ServerPlayer player,
      Villager spouse,
      String childName,
      VillagerPersonality personality,
      int selectedSkinId,
      String childGender,
      String skinType,
      boolean customSkin,
      String customSkinInput,
      byte[] customSkinPngData
   ) {
      child.setCustomName(Component.literal(childName));
      child.setCustomNameVisible(true);
      child.setPersonality(personality);
      child.setRelationshipStage(VillagerRelationshipStage.FRIENDSHIP);
      child.setRelationship(0.5F);
      child.setGoldenRelationship(0.0F);
      applyFamilyData(child, player, spouse);
      FamilyAgingHandler.initializeForBirth(child, player.serverLevel().getGameTime());
      applyCommandOwnership(child, player);
      applySkin(child, selectedSkinId, childGender, skinType, customSkin, customSkinInput, customSkinPngData);
   }

   private static void applyChildData(
      FemaleVillagerEntity child,
      ServerPlayer player,
      Villager spouse,
      String childName,
      VillagerPersonality personality,
      int selectedSkinId,
      String childGender,
      String skinType,
      boolean customSkin,
      String customSkinInput,
      byte[] customSkinPngData
   ) {
      child.setCustomName(Component.literal(childName));
      child.setCustomNameVisible(true);
      child.setPersonality(personality);
      child.setRelationshipStage(VillagerRelationshipStage.FRIENDSHIP);
      child.setRelationship(0.5F);
      child.setGoldenRelationship(0.0F);
      applyFamilyData(child, player, spouse);
      FamilyAgingHandler.initializeForBirth(child, player.serverLevel().getGameTime());
      applyCommandOwnership(child, player);
      applySkin(child, selectedSkinId, childGender, skinType, customSkin, customSkinInput, customSkinPngData);
   }

   private static void applyFamilyData(FamilyVillager child, ServerPlayer player, Villager spouse) {
      child.setBornFromFamilySystem(true);
      child.setParentPlayerUuid(player.getUUID());
      child.setParentPlayerName(player.getGameProfile().getName());
      child.setParentVillagerUuid(spouse.getUUID());
      child.setParentVillagerName(spouse.getName().getString());
      child.setFamilyOwnerPlayerUuid(player.getUUID());
      child.setFamilyOwnerPlayerName(player.getGameProfile().getName());
   }

   private static void applyCommandOwnership(CommandableVillager child, ServerPlayer player) {
      child.setCommandOwnerUuid(player.getUUID());
      child.setCommandOwnerName(player.getGameProfile().getName());
      child.setCommandState(VillagerCommandState.NONE);
      child.setCommandTargetUuid(null);
   }

   private static void applySkin(
      MaleVillagerEntity child, int selectedSkinId, String childGender, String skinType, boolean customSkin, String customSkinInput, byte[] customSkinPngData
   ) {
      if (customSkin) {
         String savedInput = MultiplayerSkinStorage.sanitizeSavedInput(customSkinInput);
         child.setSavedSkinInput(savedInput);
         child.setCustomSkinPath(MultiplayerSkinStorage.getStoredSkinPath(child.getUUID()));
      } else {
         String curatedSkinPath = BirthScreenData.getCuratedSkinResourcePath(childGender, skinType, selectedSkinId);
         child.setSkinIndex(selectedSkinId);
         child.setSavedSkinInput("");
         if (curatedSkinPath != null) {
            child.setCustomSkinPath(curatedSkinPath);
         } else {
            child.setCustomSkinPath("");
         }
      }
   }

   private static void applySkin(
      FemaleVillagerEntity child, int selectedSkinId, String childGender, String skinType, boolean customSkin, String customSkinInput, byte[] customSkinPngData
   ) {
      if (customSkin) {
         String savedInput = MultiplayerSkinStorage.sanitizeSavedInput(customSkinInput);
         child.setSavedSkinInput(savedInput);
         child.setCustomSkinPath(MultiplayerSkinStorage.getStoredSkinPath(child.getUUID()));
      } else {
         String curatedSkinPath = BirthScreenData.getCuratedSkinResourcePath(childGender, skinType, selectedSkinId);
         child.setSkinIndex(selectedSkinId);
         child.setSavedSkinInput("");
         if (curatedSkinPath != null) {
            child.setCustomSkinPath(curatedSkinPath);
         } else {
            child.setCustomSkinPath("");
         }
      }
   }

   private static void syncCustomSkin(Villager child, String customSkinInput, byte[] customSkinPngData) {
      try {
         MultiplayerSkinStorage.saveSkin(child.getUUID(), customSkinPngData);
         MultiplayerSkinSyncHandler.broadcastSkin(child, customSkinPngData, MultiplayerSkinStorage.sanitizeSavedInput(customSkinInput), false);
      } catch (IOException var4) {
      }
   }

   private static void applyParentInitialRelationship(Villager child, ServerPlayer player) {
      if (child instanceof MaleVillagerEntity male) {
         float updated = VillagerRelationshipData.setRelationshipForPlayer(male, player.getUUID(), 4.0F, male.getRelationship());
         if (!VillagerRelationshipData.usesPerPlayerRelationships(male)) {
            male.setRelationship(updated);
         }

         ModNetworking.sendToClient(new RelationshipSyncPacket(male.getId(), updated, male.getGoldenRelationship()), player);
      } else if (child instanceof FemaleVillagerEntity female) {
         float updated = VillagerRelationshipData.setRelationshipForPlayer(female, player.getUUID(), 4.0F, female.getRelationship());
         if (!VillagerRelationshipData.usesPerPlayerRelationships(female)) {
            female.setRelationship(updated);
         }

         ModNetworking.sendToClient(new RelationshipSyncPacket(female.getId(), updated, female.getGoldenRelationship()), player);
      }
   }

   private static void clearExpecting(Villager spouse, FamilyVillager familyVillager, ServerPlayer player) {
      familyVillager.setExpectingChild(false);
      familyVillager.setExpectingParentPlayerUuid(null);
      familyVillager.setExpectingParentPlayerName("");
      familyVillager.setForcedBabyGender("");
      familyVillager.setExpectingStartedAt(0L);
      familyVillager.setExpectingEndsAt(0L);
      CompoundTag pendingGenders = PersistentEntityData.get(spouse).getCompound("SlimPatchBirthScreenPendingGenders");
      pendingGenders.remove(player.getUUID().toString());
      if (pendingGenders.isEmpty()) {
         PersistentEntityData.get(spouse).remove("SlimPatchBirthScreenPendingGenders");
      } else {
         PersistentEntityData.get(spouse).put("SlimPatchBirthScreenPendingGenders", pendingGenders);
      }
   }

   private static void playBirthEffects(ServerLevel level, Villager spouse, Villager child) {
      level.playSound(null, child.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.7F, 1.08F);
      level.sendParticles(ParticleTypes.HEART, child.getX(), child.getY() + 1.0, child.getZ(), 8, 0.25, 0.2, 0.25, 0.01);
      level.sendParticles(ParticleTypes.HAPPY_VILLAGER, spouse.getX(), spouse.getY() + 1.0, spouse.getZ(), 8, 0.25, 0.2, 0.25, 0.01);
   }
}
