package com.javic.slimpatch.client.gui;

import com.javic.slimpatch.client.ClientCustomSkinHelper;
import com.javic.slimpatch.client.ClientMultiplayerSkinSettings;
import com.javic.slimpatch.config.SlimPatchConfig;
import com.javic.slimpatch.entity.AbstractRomanceVillagerEntity;
import com.javic.slimpatch.network.ModNetworking;
import com.javic.slimpatch.network.VillagerCustomSkinUploadPacket;
import com.javic.slimpatch.network.VillagerEditDataPacket;
import com.javic.slimpatch.network.VillagerNamePacket;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.Villager;

public class VillagerEditScreen extends Screen {
   private static final int MAX_NAME_LENGTH = 64;
   private static final double MIN_SCALE = 0.5;
   private static final double MAX_SCALE = 1.5;
   private static final int CONTENT_TOP_MARGIN = 24;
   private static final int CONTENT_BOTTOM_MARGIN = 32;
   private static final int LABEL_WIDTH = 55;
   private static final int LABEL_HEIGHT = 20;
   private static final int FORM_WIDTH = 200;
   private static final int CONTENT_HEIGHT = 250;
   private static final int SCROLL_STEP = 24;
   private static final int SCROLLBAR_WIDTH = 4;
   private final Villager villager;
   private final Screen parentScreen;
   private EditBox nameField;
   private EditBox skinField;
   private VillagerEditScreen.ScaleSlider heightSlider;
   private VillagerEditScreen.ScaleSlider widthSlider;
   private Button saveButton;
   private Button openSkinsFolderButton;
   private Component heightLabel;
   private Component widthLabel;
   private int scrollOffset;

   public VillagerEditScreen(Villager villager, Screen parentScreen) {
      super(Component.translatable("slimpatch.screen.villager_edit.title"));
      this.villager = villager;
      this.parentScreen = parentScreen;
   }

   protected void init() {
      super.init();
      this.scrollOffset = 0;
      this.heightLabel = Component.translatable("slimpatch.screen.villager_edit.height");
      this.widthLabel = Component.translatable("slimpatch.screen.villager_edit.width");
      this.nameField = new EditBox(this.font, 0, 0, 200, 20, Component.translatable("slimpatch.screen.villager_edit.name"));
      this.nameField.setMaxLength(64);
      this.nameField.setValue(this.villager.getName().getString());
      this.skinField = new EditBox(this.font, 0, 0, 200, 20, Component.translatable("slimpatch.screen.villager_edit.skin_placeholder"));
      this.skinField.setMaxLength(500);
      this.skinField.setValue(this.getSavedSkinInput());
      this.heightSlider = new VillagerEditScreen.ScaleSlider(0, 0, 200, 20, this.heightLabel, this.getSavedScale(true));
      this.widthSlider = new VillagerEditScreen.ScaleSlider(0, 0, 200, 20, this.widthLabel, this.getSavedScale(false));
      this.saveButton = (Button)this.addRenderableWidget(
         Button.builder(Component.translatable("slimpatch.screen.villager_edit.save"), button -> this.saveChanges()).bounds(0, 0, 200, 20).build()
      );
      this.openSkinsFolderButton = (Button)this.addRenderableWidget(
         Button.builder(Component.translatable("slimpatch.screen.villager_edit.open_skins_folder"), button -> this.openSkinsFolder())
            .bounds(0, 0, 200, 20)
            .build()
      );
      this.addRenderableWidget(this.nameField);
      this.addRenderableWidget(this.skinField);
      this.addRenderableWidget(this.heightSlider);
      this.addRenderableWidget(this.widthSlider);
      this.setInitialFocus(this.nameField);
      this.clampScroll();
      this.updateLayout();
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
      int nextScrollOffset = Math.max(0, Math.min(this.getMaxScroll(), this.scrollOffset - (int)Math.signum(scrollY) * 24));
      if (nextScrollOffset != this.scrollOffset) {
         this.scrollOffset = nextScrollOffset;
         this.updateLayout();
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, scrollY);
      }
   }

   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(guiGraphics);
      int viewportTop = this.getViewportTop();
      int viewportBottom = this.getViewportBottom();
      guiGraphics.enableScissor(0, viewportTop, this.width, viewportBottom);
      int color = 16777215;
      super.render(guiGraphics, mouseX, mouseY, partialTick);
      int labelX = this.getFieldX() - 55 - 6;
      this.renderLabel(guiGraphics, Component.translatable("slimpatch.screen.villager_edit.name"), labelX, this.nameField.getY(), 55, 20, color);
      this.renderLabel(guiGraphics, Component.translatable("slimpatch.screen.villager_edit.skin"), labelX, this.skinField.getY(), 55, 20, color);
      guiGraphics.disableScissor();
      this.renderScrollBar(guiGraphics);
   }

   private void renderLabel(GuiGraphics guiGraphics, Component text, int x, int y, int width, int height, int color) {
      guiGraphics.fill(x, y, x + width, y + height, -1442840576);
      guiGraphics.fill(x, y, x + width, y + 1, -1);
      guiGraphics.fill(x, y, x + 1, y + height, -1);
      guiGraphics.fill(x + width - 1, y, x + width, y + height, -11184811);
      guiGraphics.fill(x, y + height - 1, x + width, y + height, -11184811);
      guiGraphics.drawCenteredString(this.font, text, x + width / 2, y + 6, color);
   }

   private void updateLayout() {
      int fieldX = this.getFieldX();
      int contentY = this.getContentStartY() - this.scrollOffset;
      this.nameField.setX(fieldX);
      this.nameField.setY(contentY);
      this.skinField.setX(fieldX);
      this.skinField.setY(contentY + 32);
      this.heightSlider.setX(fieldX);
      this.heightSlider.setY(contentY + 72);
      this.widthSlider.setX(fieldX);
      this.widthSlider.setY(contentY + 112);
      if (this.saveButton != null) {
         this.saveButton.setX(fieldX);
         this.saveButton.setY(contentY + 152);
      }

      if (this.openSkinsFolderButton != null) {
         this.openSkinsFolderButton.setX(fieldX);
         this.openSkinsFolderButton.setY(contentY + 182);
      }

      this.updateWidgetVisibility(this.nameField);
      this.updateWidgetVisibility(this.skinField);
      this.updateWidgetVisibility(this.heightSlider);
      this.updateWidgetVisibility(this.widthSlider);
      if (this.saveButton != null) {
         this.updateWidgetVisibility(this.saveButton);
      }

      if (this.openSkinsFolderButton != null) {
         this.updateWidgetVisibility(this.openSkinsFolderButton);
      }
   }

   private void updateWidgetVisibility(AbstractWidget widget) {
      int viewportTop = this.getViewportTop();
      int viewportBottom = this.getViewportBottom();
      boolean visible = widget.getY() + widget.getHeight() > viewportTop && widget.getY() < viewportBottom;
      widget.visible = visible;
      widget.active = visible;
      if (!visible && this.getFocused() == widget) {
         this.setFocused(null);
      }
   }

   private int getFieldX() {
      return this.width / 2 - 100;
   }

   private int getContentStartY() {
      return Math.max(24, this.height / 2 - 52);
   }

   private int getViewportTop() {
      return 24;
   }

   private int getViewportBottom() {
      return this.height - 32;
   }

   private int getMaxScroll() {
      return Math.max(0, this.getContentStartY() + 250 - this.getViewportBottom());
   }

   private void clampScroll() {
      this.scrollOffset = Math.max(0, Math.min(this.getMaxScroll(), this.scrollOffset));
   }

   private void renderScrollBar(GuiGraphics guiGraphics) {
      int maxScroll = this.getMaxScroll();
      if (maxScroll > 0) {
         int barX1 = this.getFieldX() + 200 + 8;
         int barY1 = this.getViewportTop();
         int barY2 = this.getViewportBottom();
         guiGraphics.fill(barX1, barY1, barX1 + 4, barY2, 1715749956);
         int thumbHeight = Math.max(18, (barY2 - barY1) * (barY2 - barY1) / 250);
         int travel = barY2 - barY1 - thumbHeight;
         int thumbY = barY1 + (maxScroll == 0 ? 0 : travel * this.scrollOffset / maxScroll);
         guiGraphics.fill(barX1, thumbY, barX1 + 4, thumbY + thumbHeight, -5197648);
      }
   }

   private void saveChanges() {
      boolean applyLocally = this.allowLocalCustomSkins();
      if (applyLocally) {
         this.villager.setCustomName(Component.literal(this.nameField.getValue()));
      }

      ModNetworking.sendToServer(new VillagerNamePacket(this.villager.getUUID(), this.nameField.getValue()));
      if (applyLocally) {
         com.javic.slimpatch.fabric.PersistentEntityData.get(this.villager).putString("SavedName", this.nameField.getValue());
      }

      String skinName = this.skinField.getValue().trim();
      if (!skinName.isEmpty() && this.allowLocalCustomSkins()) {
         if (this.villager instanceof AbstractRomanceVillagerEntity romanceVillager) {
            romanceVillager.setSavedSkinInput(skinName);
         } else {
            com.javic.slimpatch.fabric.PersistentEntityData.get(this.villager).putString("SavedSkinInput", skinName);
         }

         this.applySkin(skinName);
      } else if (this.canUploadMultiplayerCustomSkin()) {
         if (skinName.isEmpty()) {
            ModNetworking.sendToServer(new VillagerCustomSkinUploadPacket(this.villager.getId(), "", new byte[0], true));
         } else {
            byte[] pngData = this.readSkinBytesForUpload(skinName);
            if (pngData == null) {
               return;
            }

            ModNetworking.sendToServer(new VillagerCustomSkinUploadPacket(this.villager.getId(), skinName, pngData, false));
         }
      } else if (!this.allowLocalCustomSkins()) {
         if (this.villager instanceof AbstractRomanceVillagerEntity romanceVillager) {
            romanceVillager.setSavedSkinInput("");
            romanceVillager.setCustomSkinPath("");
         } else {
            com.javic.slimpatch.fabric.PersistentEntityData.get(this.villager).remove("SavedSkinInput");
            com.javic.slimpatch.fabric.PersistentEntityData.get(this.villager).remove("CustomSkinPath");
         }
      }

      int savedHeight = (int)Math.round(this.heightSlider.getScaleValue() * 100.0);
      int savedWidth = (int)Math.round(this.widthSlider.getScaleValue() * 100.0);
      if (applyLocally) {
         if (this.villager instanceof AbstractRomanceVillagerEntity romanceVillager) {
            romanceVillager.setVisualHeight(savedHeight);
            romanceVillager.setVisualWidth(savedWidth);
         } else {
            com.javic.slimpatch.fabric.PersistentEntityData.get(this.villager).putInt("Height", savedHeight);
            com.javic.slimpatch.fabric.PersistentEntityData.get(this.villager).putInt("Width", savedWidth);
         }
      }

      String savedSkinInput = com.javic.slimpatch.fabric.PersistentEntityData.get(this.villager).getString("SavedSkinInput");
      String customSkinPath = com.javic.slimpatch.fabric.PersistentEntityData.get(this.villager).getString("CustomSkinPath");
      ModNetworking.sendToServer(new VillagerEditDataPacket(this.villager.getUUID(), savedSkinInput, customSkinPath, savedHeight, savedWidth));
      this.closeToParent();
   }

   private double getSavedScale(boolean height) {
      if (this.villager instanceof AbstractRomanceVillagerEntity romanceVillager) {
         int value = height ? romanceVillager.getVisualHeight() : romanceVillager.getVisualWidth();
         return Math.max(0.5, Math.min(1.5, value / 100.0));
      } else {
         String key = height ? "Height" : "Width";
         return !com.javic.slimpatch.fabric.PersistentEntityData.get(this.villager).contains(key) ? 1.0 : Math.max(0.5, Math.min(1.5, com.javic.slimpatch.fabric.PersistentEntityData.get(this.villager).getInt(key) / 100.0));
      }
   }

   private String getSavedSkinInput() {
      return this.villager instanceof AbstractRomanceVillagerEntity romanceVillager
         ? romanceVillager.getSavedSkinInput()
         : com.javic.slimpatch.fabric.PersistentEntityData.get(this.villager).getString("SavedSkinInput");
   }

   private String getCustomSkinPath() {
      return this.villager instanceof AbstractRomanceVillagerEntity romanceVillager
         ? romanceVillager.getCustomSkinPath()
         : com.javic.slimpatch.fabric.PersistentEntityData.get(this.villager).getString("CustomSkinPath");
   }

   private void applySkin(String skinName) {
      if (this.allowLocalCustomSkins()) {
         String safePath = ClientCustomSkinHelper.storeLocalSkinInput(this.villager.getUUID(), skinName);
         if (!safePath.isEmpty()) {
            if (this.villager instanceof AbstractRomanceVillagerEntity romanceVillager) {
               romanceVillager.setCustomSkinPath(safePath);
            } else {
               com.javic.slimpatch.fabric.PersistentEntityData.get(this.villager).putString("CustomSkinPath", safePath);
            }

            com.javic.slimpatch.fabric.PersistentEntityData.get(this.villager).putString("SavedName", this.villager.getName().getString());
         }
      }
   }

   private void openSkinsFolder() {
      if (this.allowLocalCustomSkins() || this.canUploadMultiplayerCustomSkin()) {
         ClientCustomSkinHelper.openVillagerSkinFolder(this.villager.getUUID());
      }
   }

   private boolean allowLocalCustomSkins() {
      return ClientCustomSkinHelper.allowLocalCustomSkins();
   }

   private boolean canUploadMultiplayerCustomSkin() {
      return this.minecraft != null && this.minecraft.getSingleplayerServer() != null && this.minecraft.getSingleplayerServer().isPublished()
         ? (Boolean)SlimPatchConfig.SERVER.allowMultiplayerCustomSkins.get()
         : ClientMultiplayerSkinSettings.isAllowed();
   }

   private byte[] readSkinBytesForUpload(String skinName) {
      try {
         String trimmedSkinName = skinName.trim();
         if (trimmedSkinName.isEmpty()) {
            return null;
         }

         int maxBytes = ClientMultiplayerSkinSettings.getMaxCustomSkinSizeKb() * 1024;
         byte[] data = ClientCustomSkinHelper.readSkinBytes(trimmedSkinName, this.villager.getUUID());
         return data != null && data.length <= maxBytes ? data : null;
      } catch (Exception e) {
         return null;
      }
   }

   private void closeToParent() {
      if (this.minecraft != null) {
         this.minecraft.setScreen(this.parentScreen);
      }
   }

   public void onClose() {
      this.closeToParent();
   }

   public boolean isPauseScreen() {
      return false;
   }

   private static class ScaleSlider extends AbstractSliderButton {
      private final String translationKey;

      private ScaleSlider(int x, int y, int width, int height, Component message, double scaleValue) {
         super(x, y, width, height, message, toSliderValue(scaleValue));
         this.translationKey = message.getString().equals(Component.translatable("slimpatch.screen.villager_edit.width").getString())
            ? "slimpatch.screen.villager_edit.width"
            : "slimpatch.screen.villager_edit.height";
         this.updateMessage();
      }

      protected void updateMessage() {
         this.setMessage(Component.translatable(this.translationKey).append(" " + String.format(Locale.ROOT, "%.2f", this.getScaleValue())));
      }

      protected void applyValue() {
      }

      private double getScaleValue() {
         return 0.5 + this.value * 1.0;
      }

      private static double toSliderValue(double scaleValue) {
         return (scaleValue - 0.5) / 1.0;
      }
   }
}
