package com.javic.slimpatch.entity;

public enum VillagerCombatMode {
   AGGRESSIVE,
   DEFENSIVE,
   PASSIVE;

   public static VillagerCombatMode fromName(String name) {
      if (name != null && !name.isEmpty()) {
         try {
            return valueOf(name);
         } catch (IllegalArgumentException e) {
            return DEFENSIVE;
         }
      } else {
         return DEFENSIVE;
      }
   }
}
