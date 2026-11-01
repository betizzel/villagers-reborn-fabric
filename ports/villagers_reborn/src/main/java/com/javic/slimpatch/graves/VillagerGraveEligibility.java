package com.javic.slimpatch.graves;

import com.javic.slimpatch.entity.CommandableVillager;
import com.javic.slimpatch.entity.FamilyVillager;
import com.javic.slimpatch.entity.FemaleVillagerEntity;
import com.javic.slimpatch.entity.MaleVillagerEntity;
import com.javic.slimpatch.entity.VillagerCommandState;
import com.javic.slimpatch.entity.VillagerRelationshipData;
import com.javic.slimpatch.entity.VillagerRelationshipStage;
import com.javic.slimpatch.fabric.PersistentEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.npc.Villager;

public final class VillagerGraveEligibility {
   private VillagerGraveEligibility() {}

   public static boolean shouldCreateDeathRecord(Villager villager) {
      if (!(villager instanceof MaleVillagerEntity) && !(villager instanceof FemaleVillagerEntity)) {
         return false;
      }
      return villager instanceof FamilyVillager familyVillager
         ? hasEligibleRelationship(villager) || hasDirectPlayerFamilyLink(familyVillager)
            || isDatingWithPlayer(villager, familyVillager) || isMarriedToPlayer(familyVillager)
            || hasPlayerCommandAttachment(villager)
         : hasEligibleRelationship(villager) || hasPlayerCommandAttachment(villager);
   }

   private static boolean hasEligibleRelationship(Villager villager) {
      if (VillagerRelationshipData.usesPerPlayerRelationships(villager)) {
         CompoundTag relationships = PersistentEntityData.get(villager).getCompound("SlimPatchRelationships");
         for (String key : relationships.getAllKeys()) {
            if (relationships.getFloat(key) >= 3.0F) return true;
         }
         return false;
      }
      if (villager instanceof MaleVillagerEntity maleVillager) return maleVillager.getRelationship() >= 3.0F;
      return villager instanceof FemaleVillagerEntity femaleVillager && femaleVillager.getRelationship() >= 3.0F;
   }

   private static boolean hasDirectPlayerFamilyLink(FamilyVillager familyVillager) {
      return familyVillager.getParentPlayerUuid() != null || familyVillager.getFamilyOwnerPlayerUuid() != null || familyVillager.getSpousePlayerUuid() != null;
   }

   private static boolean isDatingWithPlayer(Villager villager, FamilyVillager familyVillager) {
      if (VillagerRelationshipData.usesPerPlayerRelationships(villager)) {
         CompoundTag relationships = PersistentEntityData.get(villager).getCompound("SlimPatchGoldenRelationships");
         for (String key : relationships.getAllKeys()) {
            if (relationships.getFloat(key) > 0.0F) return true;
         }
         return false;
      }
      return familyVillager.getRelationshipStage() == VillagerRelationshipStage.DATING;
   }

   private static boolean isMarriedToPlayer(FamilyVillager familyVillager) {
      return familyVillager.getSpousePlayerUuid() != null
         && (familyVillager.getRelationshipStage() == VillagerRelationshipStage.MARRIED || familyVillager.hasSpouse());
   }

   private static boolean hasPlayerCommandAttachment(Villager villager) {
      if (villager instanceof CommandableVillager commandableVillager) {
         VillagerCommandState commandState = commandableVillager.getCommandState();
         return (commandState == VillagerCommandState.FOLLOW || commandState == VillagerCommandState.STAY)
            && commandableVillager.getCommandOwnerUuid() != null;
      }
      return false;
   }
}
