package com.javic.slimpatch.client.camera;

import com.javic.slimpatch.client.cutscene.WeddingCutsceneController;
import com.javic.slimpatch.client.gui.WeddingCutsceneOverlayScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

public final class WeddingCutsceneCameraHandler {
   private static boolean applied;
   private static float previousYRot;
   private static float previousYRotO;
   private static float previousXRot;
   private static float previousXRotO;

   private WeddingCutsceneCameraHandler() {
   }

   public static void renderStart() { applyPlayerRotation(); }
   public static void renderEnd() { restorePlayerRotation(); }
   public static float roll() {
      Minecraft minecraft = Minecraft.getInstance();
      return WeddingCutsceneController.isActive() && minecraft.player != null
            && minecraft.gameRenderer.getMainCamera().getEntity() == minecraft.player
            ? computeRollOffset(WeddingCutsceneController.getProgress()) : 0.0F;
   }

   private static void applyPlayerRotation() {
      Minecraft minecraft = Minecraft.getInstance();
      if (!WeddingCutsceneController.isActive()) {
         restorePlayerRotation();
      } else if (minecraft.options == null || minecraft.options.getCameraType().isFirstPerson()) {
         restorePlayerRotation();
      } else if (minecraft.screen != null && !(minecraft.screen instanceof WeddingCutsceneOverlayScreen)) {
         restorePlayerRotation();
      } else {
         LocalPlayer player = minecraft.player;
         if (player != null && minecraft.level != null) {
            if (applied) {
               restorePlayerRotation();
            }

            previousYRot = player.getYRot();
            previousYRotO = player.yRotO;
            previousXRot = player.getXRot();
            previousXRotO = player.xRotO;
            applied = true;
            float progress = WeddingCutsceneController.getProgress();
            float targetYaw = Mth.wrapDegrees(previousYRot + computeYawOffset(progress));
            float targetPitch = Mth.clamp(previousXRot + computePitchOffset(progress), -90.0F, 90.0F);
            player.setYRot(targetYaw);
            player.yRotO = targetYaw;
            player.setXRot(targetPitch);
            player.xRotO = targetPitch;
         } else {
            restorePlayerRotation();
         }
      }
   }

   private static void restorePlayerRotation() {
      if (applied) {
         Minecraft minecraft = Minecraft.getInstance();
         LocalPlayer player = minecraft.player;
         if (player != null) {
            player.setYRot(previousYRot);
            player.yRotO = previousYRotO;
            player.setXRot(previousXRot);
            player.xRotO = previousXRotO;
         }

         applied = false;
      }
   }

   private static float computeYawOffset(float progress) {
      float smoothEntry = computeSmoothEntry(progress);
      float drift = (float)Math.sin(progress * Math.PI * 2.0) * 1.8F;
      return Mth.lerp(smoothEntry, 18.0F, 44.0F) + drift;
   }

   private static float computePitchOffset(float progress) {
      float smoothEntry = computeSmoothEntry(progress);
      return Mth.lerp(smoothEntry, -5.0F, -8.0F + (float)Math.sin(progress * Math.PI) * -2.0F);
   }

   private static float computeRollOffset(float progress) {
      return computeSmoothEntry(progress) * (float)Math.sin(progress * Math.PI * 2.0) * 0.35F;
   }

   private static float computeSmoothEntry(float progress) {
      float entry = Mth.clamp(progress / 0.65F, 0.0F, 1.0F);
      return entry * entry * entry * (entry * (entry * 6.0F - 15.0F) + 10.0F);
   }
}
