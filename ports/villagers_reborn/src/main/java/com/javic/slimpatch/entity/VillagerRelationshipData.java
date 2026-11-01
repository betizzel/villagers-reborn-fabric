package com.javic.slimpatch.entity;

import com.javic.slimpatch.fabric.PersistentEntityData;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.npc.Villager;

public final class VillagerRelationshipData {
   private static final String PLAYER_RELATIONSHIPS_TAG = "SlimPatchRelationships";
   private static final String CLIENT_RELATIONSHIP_TAG = "SlimPatchClientRelationship";
   private static final String PLAYER_GOLDEN_RELATIONSHIPS_TAG = "SlimPatchGoldenRelationships";
   private static final String CLIENT_GOLDEN_RELATIONSHIP_TAG = "SlimPatchClientGoldenRelationship";

   private VillagerRelationshipData() {
   }

   public static boolean usesPerPlayerRelationships(Villager villager) {
      return villager.getServer() != null && (villager.getServer().isDedicatedServer() || villager.getServer().isPublished());
   }

   public static float getRelationshipForPlayer(Villager villager, UUID playerUuid, float globalFallback) {
      float fallback = clamp(globalFallback);
      if (playerUuid != null && usesPerPlayerRelationships(villager)) {
         CompoundTag relationships = PersistentEntityData.get(villager).getCompound("SlimPatchRelationships");
         String key = playerUuid.toString();
         if (relationships.contains(key)) {
            return clamp(relationships.getFloat(key));
         }

         relationships.putFloat(key, fallback);
         PersistentEntityData.get(villager).put("SlimPatchRelationships", relationships);
         return fallback;
      } else {
         return fallback;
      }
   }

   public static float setRelationshipForPlayer(Villager villager, UUID playerUuid, float value, float globalFallback) {
      float clamped = clamp(value);
      if (playerUuid != null && usesPerPlayerRelationships(villager)) {
         CompoundTag relationships = PersistentEntityData.get(villager).getCompound("SlimPatchRelationships");
         relationships.putFloat(playerUuid.toString(), clamped);
         PersistentEntityData.get(villager).put("SlimPatchRelationships", relationships);
         return clamped;
      } else {
         return clamped;
      }
   }

   public static float getDisplayedRelationship(Villager villager, float fallback) {
      CompoundTag data = PersistentEntityData.get(villager);
      return data.contains("SlimPatchClientRelationship") ? clamp(data.getFloat("SlimPatchClientRelationship")) : clamp(fallback);
   }

   public static float getGoldenRelationshipForPlayer(Villager villager, UUID playerUuid, float globalFallback) {
      float fallback = clampGolden(globalFallback);
      if (playerUuid != null && usesPerPlayerRelationships(villager)) {
         CompoundTag relationships = PersistentEntityData.get(villager).getCompound("SlimPatchGoldenRelationships");
         String key = playerUuid.toString();
         if (relationships.contains(key)) {
            return clampGolden(relationships.getFloat(key));
         }

         relationships.putFloat(key, fallback);
         PersistentEntityData.get(villager).put("SlimPatchGoldenRelationships", relationships);
         return fallback;
      } else {
         return fallback;
      }
   }

   public static float setGoldenRelationshipForPlayer(Villager villager, UUID playerUuid, float value, float globalFallback) {
      float clamped = clampGolden(value);
      if (playerUuid != null && usesPerPlayerRelationships(villager)) {
         CompoundTag relationships = PersistentEntityData.get(villager).getCompound("SlimPatchGoldenRelationships");
         relationships.putFloat(playerUuid.toString(), clamped);
         PersistentEntityData.get(villager).put("SlimPatchGoldenRelationships", relationships);
         return clamped;
      } else {
         return clamped;
      }
   }

   public static float getDisplayedGoldenRelationship(Villager villager, float fallback) {
      CompoundTag data = PersistentEntityData.get(villager);
      return data.contains("SlimPatchClientGoldenRelationship") ? clampGolden(data.getFloat("SlimPatchClientGoldenRelationship")) : clampGolden(fallback);
   }

   public static void setClientRelationship(Villager villager, float relationship) {
      PersistentEntityData.get(villager).putFloat("SlimPatchClientRelationship", clamp(relationship));
   }

   public static void setClientGoldenRelationship(Villager villager, float goldenRelationship) {
      PersistentEntityData.get(villager).putFloat("SlimPatchClientGoldenRelationship", clampGolden(goldenRelationship));
   }

   public static void load(CompoundTag tag, Villager villager) {
      if (tag.contains("SlimPatchRelationships")) {
         PersistentEntityData.get(villager).put("SlimPatchRelationships", tag.getCompound("SlimPatchRelationships").copy());
      }

      if (tag.contains("SlimPatchGoldenRelationships")) {
         PersistentEntityData.get(villager).put("SlimPatchGoldenRelationships", tag.getCompound("SlimPatchGoldenRelationships").copy());
      }
   }

   public static void save(CompoundTag tag, Villager villager) {
      CompoundTag data = PersistentEntityData.get(villager);
      if (data.contains("SlimPatchRelationships")) {
         tag.put("SlimPatchRelationships", data.getCompound("SlimPatchRelationships").copy());
      }

      if (data.contains("SlimPatchGoldenRelationships")) {
         tag.put("SlimPatchGoldenRelationships", data.getCompound("SlimPatchGoldenRelationships").copy());
      }
   }

   private static float clamp(float value) {
      return Math.max(0.0F, Math.min(5.0F, value));
   }

   private static float clampGolden(float value) {
      return VillagerFamilyData.clampGoldenRelationship(value);
   }
}
