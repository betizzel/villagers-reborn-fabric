package com.javic.slimpatch.client;

public final class ClientMultiplayerSkinSettings {
   private static boolean allowMultiplayerCustomSkins = true;
   private static int maxCustomSkinSizeKb = 256;
   private static boolean enableHavingChildren = true;

   private ClientMultiplayerSkinSettings() {
   }

   public static void set(boolean allow, int maxSizeKb, boolean enableChildren) {
      allowMultiplayerCustomSkins = allow;
      maxCustomSkinSizeKb = maxSizeKb;
      enableHavingChildren = enableChildren;
   }

   public static boolean isAllowed() {
      return allowMultiplayerCustomSkins;
   }

   public static int getMaxCustomSkinSizeKb() {
      return maxCustomSkinSizeKb;
   }

   public static boolean isHavingChildrenEnabled() {
      return enableHavingChildren;
   }
}
