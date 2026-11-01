package com.javic.slimpatch.client.camera;

import com.javic.slimpatch.client.gui.VillagerDialogueScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

public final class DialogueThirdPersonCameraHandler {
   private static final float MAX_PITCH_UP_OFFSET = 18.0F;
   private static final float MAX_PITCH_DOWN_OFFSET = 12.0F;
   private static float yawOffset;
   private static float pitchOffset;
   private static boolean applied;
   private static float previousYRot;
   private static float previousYRotO;
   private static float previousXRot;
   private static float previousXRotO;

   private DialogueThirdPersonCameraHandler() {
   }

   public static void update(float yawDelta, float pitchDelta, float sensitivity, float deltaTicks) {
      if (!(deltaTicks <= 0.0F)) {
         yawOffset = Mth.wrapDegrees(yawOffset + yawDelta * sensitivity * deltaTicks);
         pitchOffset = Mth.clamp(pitchOffset - pitchDelta * sensitivity * deltaTicks, -12.0F, 18.0F);
      }
   }

   public static void reset() {
      restorePlayerRotation();
      yawOffset = 0.0F;
      pitchOffset = 0.0F;
   }

   public static void renderStart() { applyPlayerRotation(); }
   public static void renderEnd() { restorePlayerRotation(); }

   private static void applyPlayerRotation() {
      Minecraft minecraft = Minecraft.getInstance();
      if (!(minecraft.screen instanceof VillagerDialogueScreen)) {
         restorePlayerRotation();
      } else if (minecraft.options != null && !minecraft.options.getCameraType().isFirstPerson()) {
         LocalPlayer player = minecraft.player;
         if (player == null) {
            restorePlayerRotation();
         } else {
            if (!applied) {
               previousYRot = player.getYRot();
               previousYRotO = player.yRotO;
               previousXRot = player.getXRot();
               previousXRotO = player.xRotO;
               applied = true;
            }

            float targetYaw = Mth.wrapDegrees(previousYRot + yawOffset);
            float targetPitch = Mth.clamp(previousXRot + pitchOffset, -90.0F, 90.0F);
            player.setYRot(targetYaw);
            player.yRotO = targetYaw;
            player.setXRot(targetPitch);
            player.xRotO = targetPitch;
         }
      } else {
         restorePlayerRotation();
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

}
