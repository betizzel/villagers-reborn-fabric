package com.javic.slimpatch.network;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

public final class VillagerCooldownsStorage {
   private static final Map<UUID, Map<UUID, Map<String, Long>>> DATA = new HashMap<>();
   private static final Map<Integer, UUID> ID_LINKS = new HashMap<>();
   private static final Map<UUID, Map<UUID, Long>> GIFT_COOLDOWNS = new HashMap<>();

   private VillagerCooldownsStorage() {
   }

   public static void setCooldowns(UUID villagerUuid, UUID playerUuid, Map<String, Integer> cooldowns) {
      if (villagerUuid != null && playerUuid != null) {
         Map<String, Long> expiryMap = new HashMap<>();
         long now = System.currentTimeMillis();

         for (Entry<String, Integer> entry : cooldowns.entrySet()) {
            expiryMap.put(entry.getKey(), now + Math.max(0, entry.getValue()) * 1000L);
         }

         DATA.computeIfAbsent(villagerUuid, ignored -> new HashMap<>()).put(playerUuid, expiryMap);
      }
   }

   public static Map<String, Integer> getCooldowns(UUID villagerUuid, UUID playerUuid) {
      if (villagerUuid != null && playerUuid != null) {
         Map<String, Long> expiryMap = DATA.getOrDefault(villagerUuid, Collections.emptyMap()).getOrDefault(playerUuid, Collections.emptyMap());
         Map<String, Integer> result = new HashMap<>();
         long now = System.currentTimeMillis();

         for (Entry<String, Long> entry : expiryMap.entrySet()) {
            result.put(entry.getKey(), (int)Math.max(0L, (entry.getValue() - now) / 1000L));
         }

         return result;
      } else {
         return Collections.emptyMap();
      }
   }

   public static void ensureLinked(int entityId, UUID uuid) {
      if (!ID_LINKS.containsKey(entityId)) {
         ID_LINKS.put(entityId, uuid);
      }
   }

   public static boolean hasGiftCooldown(UUID villagerUuid, UUID playerUuid) {
      if (villagerUuid != null && playerUuid != null) {
         Long lastGift = GIFT_COOLDOWNS.getOrDefault(villagerUuid, Collections.emptyMap()).get(playerUuid);
         return lastGift == null ? false : System.currentTimeMillis() < lastGift;
      } else {
         return false;
      }
   }

   public static void setGiftCooldownRemaining(UUID villagerUuid, UUID playerUuid, long remainingMs) {
      if (villagerUuid != null && playerUuid != null) {
         if (remainingMs <= 0L) {
            Map<UUID, Long> byPlayer = GIFT_COOLDOWNS.get(villagerUuid);
            if (byPlayer != null) {
               byPlayer.remove(playerUuid);
            }
         } else {
            GIFT_COOLDOWNS.computeIfAbsent(villagerUuid, ignored -> new HashMap<>()).put(playerUuid, System.currentTimeMillis() + remainingMs);
         }
      }
   }

   public static long getGiftCooldownRemaining(UUID villagerUuid, UUID playerUuid) {
      if (villagerUuid != null && playerUuid != null) {
         Long lastGift = GIFT_COOLDOWNS.getOrDefault(villagerUuid, Collections.emptyMap()).get(playerUuid);
         return lastGift == null ? 0L : Math.max(0L, lastGift - System.currentTimeMillis());
      } else {
         return 0L;
      }
   }
}
