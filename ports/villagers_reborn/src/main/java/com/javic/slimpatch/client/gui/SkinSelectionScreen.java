package com.javic.slimpatch.client.gui;

import com.javic.slimpatch.client.ClientSkinTheme;
import com.javic.slimpatch.data.WorldSkinData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public class SkinSelectionScreen extends Screen {
   public SkinSelectionScreen() {
      super(Component.translatable("slimpatch.screen.skin_selection.title"));
   }

   protected void init() {
      int centerX = this.width / 2;
      int centerY = this.height / 2;
      this.addRenderableWidget(
         Button.builder(Component.translatable("slimpatch.screen.skin_selection.modern"), btn -> this.chooseTheme("modern"))
            .pos(centerX - 100, centerY)
            .size(90, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.translatable("slimpatch.screen.skin_selection.fantasy"), btn -> this.chooseTheme("fantasy"))
            .pos(centerX + 10, centerY)
            .size(90, 20)
            .build()
      );
   }

   private void chooseTheme(String theme) {
      ClientSkinTheme.setTheme(theme);
      ClientSkinTheme.markGuiShownOnce();
      Minecraft mc = Minecraft.getInstance();
      if (mc.getSingleplayerServer() != null) {
         ServerLevel serverLevel = mc.getSingleplayerServer().overworld();
         if (serverLevel != null) {
            WorldSkinData data = WorldSkinData.get(serverLevel);
            data.setTheme(theme);
            data.setGuiShown(true);
            data.setDirty();
         }
      }

      this.onClose();
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      this.renderBackground(graphics);
      super.render(graphics, mouseX, mouseY, partialTicks);
      graphics.pose().pushPose();
      graphics.pose().scale(1.7F, 1.7F, 1.7F);
      int scaledX = (int)(this.width / 2 / 1.7F);
      int scaledY = (int)((this.height / 2 - 100) / 1.7F);
      graphics.drawCenteredString(this.font, Component.translatable("slimpatch.screen.skin_selection.heading"), scaledX, scaledY, 16777045);
      graphics.pose().popPose();
      graphics.drawCenteredString(
         this.font, "Minecraft 1.20.1 support is an initial port and may still have experimental features.", this.width / 2, this.height / 2 - 52, 13619151
      );
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   public void onClose() {
      String theme = ClientSkinTheme.getTheme();
      if (theme != null && !theme.isEmpty()) {
         super.onClose();
      }
   }
}
