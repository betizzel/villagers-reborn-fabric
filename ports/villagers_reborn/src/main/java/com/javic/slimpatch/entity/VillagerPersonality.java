package com.javic.slimpatch.entity;

import net.minecraft.util.RandomSource;

public enum VillagerPersonality {
   FRIENDLY,
   MEAN,
   SHY,
   BRAVE,
   GRUMPY,
   GREEDY,
   ROMANTIC,
   WISE;

   public static VillagerPersonality getRandom(RandomSource random) {
      float roll = random.nextFloat();
      if (roll < 0.3F) {
         return FRIENDLY;
      } else if (roll < 0.4F) {
         return MEAN;
      } else if (roll < 0.5F) {
         return SHY;
      } else if (roll < 0.6F) {
         return BRAVE;
      } else if (roll < 0.69F) {
         return GRUMPY;
      } else if (roll < 0.79F) {
         return GREEDY;
      } else {
         return roll < 0.89F ? ROMANTIC : WISE;
      }
   }
}
