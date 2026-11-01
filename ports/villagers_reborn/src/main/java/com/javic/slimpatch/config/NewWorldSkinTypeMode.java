package com.javic.slimpatch.config;

public enum NewWorldSkinTypeMode {
   SELECTION_SCREEN,
   MODERN,
   FANTASY;

   public boolean usesSelectionScreen() {
      return this == SELECTION_SCREEN;
   }

   public String resolveAutomaticTheme() {
      return this == FANTASY ? "fantasy" : "modern";
   }

   public String getTranslationKey() {
      return switch (this) {
         case SELECTION_SCREEN -> "slimpatch.config.newWorldSkinType.mode.selection_screen";
         case MODERN -> "slimpatch.config.newWorldSkinType.mode.modern";
         case FANTASY -> "slimpatch.config.newWorldSkinType.mode.fantasy";
      };
   }
}
