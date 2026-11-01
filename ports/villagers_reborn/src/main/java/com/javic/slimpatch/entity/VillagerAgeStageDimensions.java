package com.javic.slimpatch.entity;

import net.minecraft.world.entity.EntityDimensions;

final class VillagerAgeStageDimensions {
   private static final float TEEN_WIDTH = 0.56F;
   private static final float TEEN_HEIGHT = 1.8F;
   private static final float CHILD_WIDTH = 0.52F;
   private static final float CHILD_HEIGHT = 1.55F;
   private static final float TODDLER_WIDTH = 0.48F;
   private static final float TODDLER_HEIGHT = 1.18F;

   private VillagerAgeStageDimensions() {
   }

   public static EntityDimensions forStage(VillagerAgeStage ageStage, EntityDimensions adultDimensions) {
      if (ageStage != null && ageStage != VillagerAgeStage.ADULT) {
         return switch (ageStage) {
            case TEEN -> scale(adultDimensions, 0.56F, 1.8F);
            case CHILD -> scale(adultDimensions, 0.52F, 1.55F);
            case TODDLER -> scale(adultDimensions, 0.48F, 1.18F);
            case ADULT -> adultDimensions;
         };
      } else {
         return adultDimensions;
      }
   }

   private static EntityDimensions scale(EntityDimensions adultDimensions, float width, float height) {
      return adultDimensions.scale(width / adultDimensions.width, height / adultDimensions.height);
   }
}
