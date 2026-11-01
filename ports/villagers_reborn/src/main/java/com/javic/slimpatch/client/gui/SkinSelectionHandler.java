package com.javic.slimpatch.client.gui;

import com.javic.slimpatch.Config;
import com.javic.slimpatch.client.ClientSkinTheme;
import com.javic.slimpatch.config.NewWorldSkinTypeMode;
import com.javic.slimpatch.data.WorldSkinData;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;

public class SkinSelectionHandler {
   private static boolean checked;
   private static boolean published;

   public static void onClientTick() {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.level != null) {
            if (mc.getSingleplayerServer() == null) {
               checked = true;
            } else {
               boolean isPublished = mc.getSingleplayerServer().isPublished();
               if ((!checked || published != isPublished) && applySingleplayerTheme(mc, isPublished)) {
                  checked = true;
                  published = isPublished;
               }
            }
         }
   }

   private static boolean applySingleplayerTheme(Minecraft mc, boolean isPublished) {
      if (mc.getSingleplayerServer() == null) {
         return false;
      }

      ServerLevel serverLevel = mc.getSingleplayerServer().overworld();
      if (serverLevel == null) {
         return false;
      }

      WorldSkinData data = WorldSkinData.get(serverLevel);
      String theme = data.getTheme();
      if (!data.isInitialized()) {
         NewWorldSkinTypeMode mode = (NewWorldSkinTypeMode)Config.NEW_WORLD_SKIN_TYPE.get();
         if (!isPublished && mode.usesSelectionScreen()) {
            if (mc.screen == null) {
               mc.setScreen(new SkinSelectionScreen());
               ClientSkinTheme.markGuiShownOnce();
               return true;
            }

            if (mc.screen instanceof SkinSelectionScreen) {
               return true;
            }

            return false;
         }

         data.setTheme(mode.resolveAutomaticTheme());
         data.setGuiShown(true);
         theme = data.getTheme();
      }

      if (theme != null && !theme.isEmpty()) {
         ClientSkinTheme.setTheme(theme);
      } else {
         ClientSkinTheme.clear();
      }

      return true;
   }

   public static void onLogout() {
      checked = false;
      published = false;
      ClientSkinTheme.clear();
   }
}
