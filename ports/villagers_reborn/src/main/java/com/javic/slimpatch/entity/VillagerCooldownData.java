package com.javic.slimpatch.entity;

import com.javic.slimpatch.fabric.PersistentEntityData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;

public final class VillagerCooldownData {
   private static final String DIALOGUE_COOLDOWNS_TAG = "SlimPatchDialogueCooldowns";
   private static final String GIFT_COOLDOWNS_TAG = "SlimPatchGiftCooldowns";
   private static final String ROMANTIC_GIFT_COOLDOWNS_TAG = "SlimPatchRomanticGiftCooldowns";
   private static final String SPOUSE_MEAL_COOLDOWNS_TAG = "SlimPatchSpouseMealCooldowns";
   private static final long LEGACY_TIMESTAMP_THRESHOLD = 1000000000000L;
   private static final long GIFT_COOLDOWN_MS = 1200000L;
   private static final long GIFT_COOLDOWN_TICKS = 24000L;
   private static final long ROMANTIC_GIFT_COOLDOWN_TICKS = 12000L;

   private VillagerCooldownData() {
   }

   public static Map<String, Integer> getDialogueCooldownsForClient(Villager villager, UUID playerUuid, Map<String, Long> optionCooldowns) {
      Map<String, Integer> result = new HashMap<>();
      if (villager != null && playerUuid != null) {
         CompoundTag playerCooldowns = PersistentEntityData.get(villager).getCompound("SlimPatchDialogueCooldowns").getCompound(playerUuid.toString());
         long gameTime = villager.level().getGameTime();

         for (String option : optionCooldowns.keySet()) {
            long remainingTicks = getRemainingTicks(playerCooldowns.getLong(option), gameTime);
            result.put(option, (int)(remainingTicks * 50L / 1000L));
         }

         return result;
      } else {
         return result;
      }
   }

   public static boolean isDialogueOptionOnCooldown(Villager villager, UUID playerUuid, String option) {
      return villager != null && playerUuid != null && option != null ? getDialogueCooldownRemainingMs(villager, playerUuid, option) > 0L : false;
   }

   public static void setDialogueCooldown(Villager villager, UUID playerUuid, String option, long cooldownMs) {
      if (villager != null && playerUuid != null && option != null) {
         CompoundTag allCooldowns = PersistentEntityData.get(villager).getCompound("SlimPatchDialogueCooldowns");
         CompoundTag playerCooldowns = allCooldowns.getCompound(playerUuid.toString());
         long cooldownTicks = Math.max(1L, (cooldownMs + 49L) / 50L);
         playerCooldowns.putLong(option, villager.level().getGameTime() + cooldownTicks);
         allCooldowns.put(playerUuid.toString(), playerCooldowns);
         PersistentEntityData.get(villager).put("SlimPatchDialogueCooldowns", allCooldowns);
      }
   }

   public static long getDialogueCooldownRemainingMs(Villager villager, UUID playerUuid, String option) {
      if (villager != null && playerUuid != null && option != null) {
         CompoundTag playerCooldowns = PersistentEntityData.get(villager).getCompound("SlimPatchDialogueCooldowns").getCompound(playerUuid.toString());
         long remainingTicks = getRemainingTicks(playerCooldowns.getLong(option), villager.level().getGameTime());
         return remainingTicks * 50L;
      } else {
         return 0L;
      }
   }

   public static boolean hasGiftCooldown(Villager villager, UUID playerUuid) {
      return getGiftCooldownRemainingMs(villager, playerUuid) > 0L;
   }

   public static void setGiftCooldown(Villager villager, UUID playerUuid) {
      if (villager != null && playerUuid != null) {
         CompoundTag allCooldowns = PersistentEntityData.get(villager).getCompound("SlimPatchGiftCooldowns");
         allCooldowns.putLong(playerUuid.toString(), villager.level().getGameTime() + 24000L);
         PersistentEntityData.get(villager).put("SlimPatchGiftCooldowns", allCooldowns);
      }
   }

   public static long getGiftCooldownRemainingMs(Villager villager, UUID playerUuid) {
      if (villager != null && playerUuid != null) {
         long remainingTicks = getRemainingTicks(
            PersistentEntityData.get(villager).getCompound("SlimPatchGiftCooldowns").getLong(playerUuid.toString()), villager.level().getGameTime()
         );
         return remainingTicks * 50L;
      } else {
         return 0L;
      }
   }

   public static boolean hasRomanticGiftCooldown(Villager villager, UUID playerUuid) {
      return getRomanticGiftCooldownRemainingTicks(villager, playerUuid) > 0L;
   }

   public static void setRomanticGiftCooldown(Villager villager, UUID playerUuid) {
      if (villager != null && playerUuid != null) {
         CompoundTag allCooldowns = PersistentEntityData.get(villager).getCompound("SlimPatchRomanticGiftCooldowns");
         allCooldowns.putLong(playerUuid.toString(), villager.level().getGameTime() + 12000L);
         PersistentEntityData.get(villager).put("SlimPatchRomanticGiftCooldowns", allCooldowns);
      }
   }

   public static long getRomanticGiftCooldownRemainingTicks(Villager villager, UUID playerUuid) {
      if (villager != null && playerUuid != null) {
         long cooldownUntil = PersistentEntityData.get(villager).getCompound("SlimPatchRomanticGiftCooldowns").getLong(playerUuid.toString());
         return Math.max(0L, cooldownUntil - villager.level().getGameTime());
      } else {
         return 0L;
      }
   }

   public static boolean hasSpouseMealCooldown(Villager villager, UUID playerUuid) {
      return getSpouseMealCooldownRemainingTicks(villager, playerUuid) > 0L;
   }

   public static void setSpouseMealCooldown(Villager villager, UUID playerUuid, long cooldownTicks) {
      if (villager != null && playerUuid != null) {
         CompoundTag allCooldowns = PersistentEntityData.get(villager).getCompound("SlimPatchSpouseMealCooldowns");
         allCooldowns.putLong(playerUuid.toString(), villager.level().getGameTime() + cooldownTicks);
         PersistentEntityData.get(villager).put("SlimPatchSpouseMealCooldowns", allCooldowns);
      }
   }

   public static long getSpouseMealCooldownRemainingTicks(Villager villager, UUID playerUuid) {
      if (villager != null && playerUuid != null) {
         long cooldownUntil = PersistentEntityData.get(villager).getCompound("SlimPatchSpouseMealCooldowns").getLong(playerUuid.toString());
         return Math.max(0L, cooldownUntil - villager.level().getGameTime());
      } else {
         return 0L;
      }
   }

   public static void syncDialogueCooldownsToPlayer(Villager villager, ServerPlayer player, Map<String, Long> optionCooldowns) {
      if (villager != null && player != null) {
         com.javic.slimpatch.network.ModNetworking.sendToClient(
            new com.javic.slimpatch.network.VillagerCooldownsPacket(villager.getUUID(), player.getUUID(), villager.getId(),
               getDialogueCooldownsForClient(villager, player.getUUID(), optionCooldowns), getGiftCooldownRemainingMs(villager, player.getUUID())), player);
      }
   }

   private static long getRemainingTicks(long cooldownUntil, long gameTime) {
      return cooldownUntil >= 1000000000000L ? 0L : Math.max(0L, cooldownUntil - gameTime);
   }
}
