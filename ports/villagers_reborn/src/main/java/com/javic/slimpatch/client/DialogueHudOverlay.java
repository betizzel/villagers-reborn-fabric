package com.javic.slimpatch.client;

import com.javic.slimpatch.Config;
import com.javic.slimpatch.client.key.ModKeyBindings;
import com.javic.slimpatch.entity.FemaleVillagerEntity;
import com.javic.slimpatch.entity.MaleVillagerEntity;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult.Type;

public final class DialogueHudOverlay {
   private static boolean suppressingCrosshairAttackIndicator;

   private DialogueHudOverlay() {
   }

   public static void beforeCrosshair() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.options.attackIndicator().get() == AttackIndicatorStatus.CROSSHAIR && shouldShowPressToTalkPrompt(mc)) {
         mc.options.attackIndicator().set(AttackIndicatorStatus.OFF);
         suppressingCrosshairAttackIndicator = true;
      }
   }

   public static void afterCrosshair(GuiGraphics graphics) {
      Minecraft mc = Minecraft.getInstance();
      if (shouldShowPressToTalkPrompt(mc)) renderPressToTalk(graphics, mc);
      if (suppressingCrosshairAttackIndicator) {
         mc.options.attackIndicator().set(AttackIndicatorStatus.CROSSHAIR);
         suppressingCrosshairAttackIndicator = false;
      }
   }

   private static boolean shouldShowPressToTalkPrompt(Minecraft mc) {
      if (!(Boolean)Config.PRESS_R_TO_TALK_INDICATOR.get()) {
         return false;
      } else {
         LocalPlayer player = mc.player;
         if (player != null && mc.screen == null && mc.hitResult != null && mc.hitResult.getType() == Type.ENTITY) {
            Entity entity = ((EntityHitResult)mc.hitResult).getEntity();
            return !(entity instanceof MaleVillagerEntity) && !(entity instanceof FemaleVillagerEntity)
               ? false
               : entity.isAlive() && player.distanceToSqr(entity) <= 16.0;
         } else {
            return false;
         }
      }
   }

   private static void renderPressToTalk(GuiGraphics graphics, Minecraft mc) {
      int centerX = mc.getWindow().getGuiScaledWidth() / 2;
      int y = mc.getWindow().getGuiScaledHeight() / 2 + 10;
      float pulse = 0.88F + 0.12F * (float)((Math.sin(System.currentTimeMillis() * (Math.PI * 2) / 2000.0) + 1.0) * 0.5);
      int textColor = Math.min(255, Math.max(0, (int)(235.0F * pulse))) << 16
         | Math.min(255, Math.max(0, (int)(235.0F * pulse))) << 8
         | Math.min(255, Math.max(0, (int)(235.0F * pulse)));
      int keyColor = Math.min(255, Math.max(0, (int)(255.0F * pulse))) << 16
         | Math.min(255, Math.max(0, (int)(255.0F * pulse))) << 8
         | Math.min(255, Math.max(0, (int)(255.0F * pulse)));
      String keyName = ModKeyBindings.getOpenDialogueKeyMessage().getString();
      String text = Component.translatable("slimpatch.overlay.press_to_talk", new Object[]{keyName}).getString();
      float keyScale = 1.15F;
      int textWidth = mc.font.width(text);
      int keyIndex = text.indexOf(keyName);
      if (keyIndex < 0) {
         drawOutlinedString(graphics, mc, text, centerX - textWidth / 2, y, textColor);
      } else {
         String left = text.substring(0, keyIndex);
         String right = text.substring(keyIndex + keyName.length());
         int leftWidth = mc.font.width(left);
         int scaledKeyWidth = Math.round(mc.font.width(keyName) * keyScale);
         int rightWidth = mc.font.width(right);
         int startX = centerX - (leftWidth + scaledKeyWidth + rightWidth) / 2;
         drawOutlinedString(graphics, mc, left, startX, y, textColor);
         drawScaledOutlinedString(graphics, mc, keyName, startX + leftWidth, y - 1, keyScale, keyColor);
         drawOutlinedString(graphics, mc, right, startX + leftWidth + scaledKeyWidth, y, textColor);
      }
   }

   private static void drawOutlinedString(GuiGraphics graphics, Minecraft mc, String text, int x, int y, int color) {
      graphics.drawString(mc.font, text, x + 1, y, 0);
      graphics.drawString(mc.font, text, x - 1, y, 0);
      graphics.drawString(mc.font, text, x, y + 1, 0);
      graphics.drawString(mc.font, text, x, y - 1, 0);
      graphics.drawString(mc.font, text, x, y, color);
   }

   private static void drawScaledOutlinedString(GuiGraphics graphics, Minecraft mc, String text, int x, int y, float scale, int color) {
      graphics.pose().pushPose();
      graphics.pose().translate(x, y, 0.0F);
      graphics.pose().scale(scale, scale, 1.0F);
      drawOutlinedString(graphics, mc, text, 0, 0, color);
      graphics.pose().popPose();
   }
}
