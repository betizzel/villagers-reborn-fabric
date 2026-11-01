package com.javic.slimpatch.client;

import net.minecraft.client.Minecraft;

public class ClientSkinTheme {
   private static String theme = "";
   private static boolean hasSentToServer = false;
   private static boolean guiShownOnce = false;

   public static void setTheme(String newTheme) {
      String normalized = normalizeTheme(newTheme);
      if (normalized != null) {
         applyTheme(normalized);
      }
   }

   public static void sendThemeToServerIfNeeded() {
   }

   public static String getTheme() {
      return theme;
   }

   public static boolean wasGuiShownOnce() {
      return guiShownOnce;
   }

   public static void markGuiShownOnce() {
      guiShownOnce = true;
   }

   public static void clear() {
      applyTheme("");
      guiShownOnce = false;
   }

   private static void applyTheme(String newTheme) {
      if (!theme.equals(newTheme)) {
         theme = newTheme;
         hasSentToServer = false;
         Minecraft mc = Minecraft.getInstance();
         if (mc.level != null) {
            mc.levelRenderer.allChanged();
         }
      }
   }

   private static String normalizeTheme(String newTheme) {
      if (newTheme != null && !newTheme.isEmpty()) {
         return "fantasy".equalsIgnoreCase(newTheme) ? "fantasy" : "modern";
      } else {
         return null;
      }
   }
}
