package com.javic.slimpatch.client.gui;

import com.javic.slimpatch.Config;
import com.javic.slimpatch.client.ClientMultiplayerSkinSettings;
import com.javic.slimpatch.client.camera.DialogueThirdPersonCameraHandler;
import com.javic.slimpatch.client.key.ModKeyBindings;
import com.javic.slimpatch.entity.CommandableVillager;
import com.javic.slimpatch.entity.FamilyVillager;
import com.javic.slimpatch.entity.FemaleVillagerEntity;
import com.javic.slimpatch.entity.MaleVillagerEntity;
import com.javic.slimpatch.entity.VillagerAgeStage;
import com.javic.slimpatch.entity.VillagerCommandState;
import com.javic.slimpatch.entity.VillagerFamilyData;
import com.javic.slimpatch.entity.VillagerPersonality;
import com.javic.slimpatch.entity.VillagerRelationshipData;
import com.javic.slimpatch.entity.VillagerRelationshipStage;
import com.javic.slimpatch.network.DialogueStatePacket;
import com.javic.slimpatch.network.FamilyStatusPacket;
import com.javic.slimpatch.network.GiftPacket;
import com.javic.slimpatch.network.ModNetworking;
import com.javic.slimpatch.network.RelationshipPacket;
import com.javic.slimpatch.network.RequestBirthScreenPacket;
import com.javic.slimpatch.network.RequestFamilyTreePacket;
import com.javic.slimpatch.network.StartFamilyPacket;
import com.javic.slimpatch.network.VillagerCommandPacket;
import com.javic.slimpatch.network.VillagerCooldownsStorage;
import com.javic.slimpatch.network.VillagerEquipmentPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;

public class VillagerDialogueScreen extends Screen {
   private static final int HEART_SIZE = 16;
   private static final int OPTION_WIDTH = 200;
   private static final int OPTION_HEIGHT = 16;
   private static final int OPTION_SPACING = 4;
   private static final int OPTION_HORIZONTAL_PADDING = 20;
   private static final double EDGE_PAN_ZONE = 72.0;
   private static final float MAX_YAW_STEP = 1.25F;
   private static final float MAX_PITCH_STEP = 0.95F;
   private static final float MAX_PITCH = 90.0F;
   private static final int SIDE_EDGE_PAN_HEIGHT_DIVISOR = 4;
   private static final int TOP_EDGE_PAN_WIDTH_DIVISOR = 4;
   private static final int BOTTOM_EDGE_PAN_WIDTH_DIVISOR = 10;
   private static final int BOTTOM_EDGE_PAN_HEIGHT = 10;
   private static final float EDGE_PAN_TICKS_PER_SECOND = 20.0F;
   private static final float MAX_EDGE_PAN_DELTA_TICKS = 2.0F;
   private static final int CHAR_INTERVAL_MS = 30;
   private static final long EXIT_HINT_DURATION_MS = 3200L;
   private static final long EXIT_HINT_FADE_OUT_MS = 700L;
   private static final ResourceLocation HEART_EMPTY = new ResourceLocation("slimpatch", "textures/gui/heart_empty.png");
   private static final ResourceLocation HEART_HALF = new ResourceLocation("slimpatch", "textures/gui/heart_half.png");
   private static final ResourceLocation HEART_FULL = new ResourceLocation("slimpatch", "textures/gui/heart_full.png");
   private static final ResourceLocation HEART_GOLD_HALF = new ResourceLocation("slimpatch", "textures/gui/heart_gold_half.png");
   private static final ResourceLocation HEART_GOLD_FULL = new ResourceLocation("slimpatch", "textures/gui/heart_gold_full.png");
   private static final ResourceLocation GIFT_ICON_COLOR = new ResourceLocation("slimpatch", "textures/gui/gift_icon_color.png");
   private static final ResourceLocation GIFT_ICON_GRAY = new ResourceLocation("slimpatch", "textures/gui/gift_icon_gray.png");
   private static final ResourceLocation EDIT_BUTTON_TEXTURE = new ResourceLocation("slimpatch", "textures/gui/edit.png");
   private static final ResourceLocation EQUIPMENT_BUTTON_TEXTURE = new ResourceLocation("slimpatch", "textures/gui/equipment.png");
   private final Villager villager;
   private String fullLine = "...";
   private int visibleChars;
   private long lastCharTime;
   private long exitHintStartTime;
   private VillagerDialogueScreen.MenuMode menuMode = VillagerDialogueScreen.MenuMode.SOCIAL;
   private int actionsButtonX;
   private int actionsButtonY;
   private int actionsButtonWidth;
   private int actionsButtonHeight;
   private int familyButtonY;
   private boolean birthReadyConfirmationPending;
   private boolean familyStartConfirmationPending;
   private long lastEdgePanTimeNanos;
   private CameraType previousCameraType;
   private boolean restoreCameraOnClose;
   private boolean closing;

   public VillagerDialogueScreen(Villager villager) {
      super(Component.literal("Dialogue"));
      this.villager = villager;
   }

   protected void init() {
      super.init();
      this.visibleChars = 0;
      this.lastCharTime = System.currentTimeMillis();
      this.exitHintStartTime = System.currentTimeMillis();
      this.lastEdgePanTimeNanos = System.nanoTime();
      this.applyDialogueCameraPreference();
      this.rebuildMenuWidgets();
   }

   private void rebuildMenuWidgets() {
      this.clearWidgets();
      List<String> options = this.getCurrentMenuOptions();
      int currentOptionWidth = this.getCurrentOptionWidth(options);
      int optionCount = Math.max(1, options.size());
      int totalHeight = 20 * optionCount;
      int startY = this.height - totalHeight - 40;
      int centerX = this.width / 2 - currentOptionWidth / 2;
      if (this.menuMode == VillagerDialogueScreen.MenuMode.SOCIAL) {
         for (int i = 0; i < options.size(); i++) {
            this.addRenderableWidget(new VillagerDialogueScreen.DialogueOptionButton(centerX, startY + i * 20, currentOptionWidth, options.get(i)));
         }
      } else {
         for (int i = 0; i < options.size(); i++) {
            this.addRenderableWidget(new VillagerDialogueScreen.ActionOptionButton(centerX, startY + i * 20, currentOptionWidth, options.get(i)));
         }
      }

      int iconButtonX = this.width - 40;
      int iconButtonY = 10;
      int iconButtonSize = 20;
      int iconButtonSpacing = 4;
      this.addRenderableWidget(new VillagerDialogueScreen.EditIconButton(iconButtonX, iconButtonY, iconButtonSize, iconButtonSize));
      this.addRenderableWidget(
         new VillagerDialogueScreen.EquipmentIconButton(iconButtonX, iconButtonY + iconButtonSize + iconButtonSpacing, iconButtonSize, iconButtonSize)
      );
      int buttonWidth = 76;
      int buttonHeight = 20;
      int buttonX = this.width - buttonWidth - 16;
      int buttonY = this.height - buttonHeight - 16;
      int familyButtonY = buttonY - buttonHeight - 4;
      this.actionsButtonX = buttonX;
      this.actionsButtonY = buttonY;
      this.actionsButtonWidth = buttonWidth;
      this.actionsButtonHeight = buttonHeight;
      this.familyButtonY = familyButtonY;
      this.addRenderableWidget(new VillagerDialogueScreen.FamilyButton(buttonX, familyButtonY, buttonWidth, buttonHeight));
      this.addRenderableWidget(new VillagerDialogueScreen.ActionsButton(buttonX, buttonY, buttonWidth, buttonHeight));
   }

   private int getCurrentOptionWidth(List<String> options) {
      if (this.menuMode != VillagerDialogueScreen.MenuMode.SOCIAL) {
         return 200;
      }

      int widestLabel = 0;

      for (String option : options) {
         widestLabel = Math.max(widestLabel, this.font.width(this.getDialogueOptionLabel(option)));
      }

      int maxWidth = Math.max(200, this.width - 32);
      return Math.min(maxWidth, Math.max(200, widestLabel + 20));
   }

   private Component getDialogueOptionLabel(String option) {
      return switch (option) {
         case "Friendly" -> Component.translatable("slimpatch.dialogue.option.friendly");
         case "Mean" -> Component.translatable("slimpatch.dialogue.option.mean");
         case "Joke" -> Component.translatable("slimpatch.dialogue.option.joke");
         case "Flirt" -> Component.translatable("slimpatch.dialogue.option.flirt");
         default -> Component.literal(option);
      };
   }

   private void setMenuMode(VillagerDialogueScreen.MenuMode menuMode) {
      this.menuMode = menuMode;
      this.rebuildMenuWidgets();
   }

   private void sendOption(String option) {
      if (this.minecraft != null && this.minecraft.player != null && this.villager.isAlive()) {
         ModNetworking.sendToServer(new RelationshipPacket(this.villager.getId(), option));
      }
   }

   public void showDialogueResult(int entityId, String option, boolean success, String line) {
      if (entityId == this.villager.getId()) {
         this.fullLine = line != null && !line.isEmpty() ? line : "...";
         this.visibleChars = 0;
         this.lastCharTime = System.currentTimeMillis();
      }
   }

   public void showSpouseCookingLine(int entityId, String lineKey) {
      if (entityId == this.villager.getId()) {
         this.fullLine = Component.translatable(lineKey).getString();
         this.visibleChars = 0;
         this.lastCharTime = System.currentTimeMillis();
      }
   }

   public void showFamilyLine(int entityId, String lineKey) {
      if (entityId == this.villager.getId()) {
         this.setDialogueLine(Component.translatable(lineKey).getString());
         if (this.menuMode == VillagerDialogueScreen.MenuMode.FAMILY_CONFIRM) {
            this.familyStartConfirmationPending = false;
            this.setMenuMode(VillagerDialogueScreen.MenuMode.FAMILY);
         }
      }
   }

   public void showBirthReadyPrompt(int entityId, String lineKey) {
      if (entityId == this.villager.getId()) {
         this.birthReadyConfirmationPending = true;
         this.familyStartConfirmationPending = false;
         this.setMenuMode(VillagerDialogueScreen.MenuMode.FAMILY_CONFIRM);
         this.setDialogueLine(Component.translatable(lineKey).getString());
      }
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.applyEdgePan();
      this.renderBackground(graphics);
      graphics.fill(0, this.height - 150, this.width, this.height, -2013265920);
      super.render(graphics, mouseX, mouseY, partialTick);
      this.renderExitHint(graphics);
      this.renderRelationshipHearts(graphics, mouseX, mouseY);
      this.renderVillagerInfo(graphics, mouseX, mouseY);
      this.renderDialogueLine(graphics);
   }

   private void applyEdgePan() {
      if (this.minecraft != null && this.minecraft.player != null && this.minecraft.getWindow() != null) {
         long now = System.nanoTime();
         if (this.lastEdgePanTimeNanos == 0L) {
            this.lastEdgePanTimeNanos = now;
         } else {
            float deltaTicks = (float)((now - this.lastEdgePanTimeNanos) / 1.0E9) * 20.0F;
            this.lastEdgePanTimeNanos = now;
            if (!(deltaTicks <= 0.0F)) {
               deltaTicks = Math.min(deltaTicks, 2.0F);
               float sensitivity = ((Double)Config.DIALOGUE_CAMERA_SENSITIVITY.get()).floatValue();
               boolean firstPerson = this.minecraft.options == null || this.minecraft.options.getCameraType().isFirstPerson();
               double mouseX = this.minecraft.mouseHandler.xpos() * this.width / this.minecraft.getWindow().getScreenWidth();
               double mouseY = this.minecraft.mouseHandler.ypos() * this.height / this.minecraft.getWindow().getScreenHeight();
               if (!this.isMouseInAllowedEdgePanZone(mouseX, mouseY)) {
                  if (!firstPerson) {
                     DialogueThirdPersonCameraHandler.update(0.0F, 0.0F, sensitivity, deltaTicks);
                  }
               } else {
                  float yawDelta = 0.0F;
                  if (this.isMouseInAllowedLeftEdgePanZone(mouseX, mouseY) || this.isMouseInAllowedRightEdgePanZone(mouseX, mouseY)) {
                     yawDelta = this.getEdgePanDelta(mouseX, this.width, 1.25F);
                  }

                  float pitchDelta = 0.0F;
                  if (this.isMouseInAllowedTopEdgePanZone(mouseX, mouseY) || this.isMouseInAllowedBottomEdgePanZone(mouseX, mouseY)) {
                     pitchDelta = this.getEdgePanDelta(mouseY, this.height, 0.95F);
                  }

                  if (!firstPerson) {
                     if ((Boolean)Config.INVERT_DIALOGUE_CAMERA_X.get()) {
                        yawDelta = -yawDelta;
                     }

                     if ((Boolean)Config.INVERT_DIALOGUE_CAMERA_Y.get()) {
                        pitchDelta = -pitchDelta;
                     }

                     DialogueThirdPersonCameraHandler.update(yawDelta, pitchDelta, sensitivity, deltaTicks);
                  } else if (yawDelta != 0.0F || pitchDelta != 0.0F) {
                     float newYaw = this.minecraft.player.getYRot() + yawDelta * sensitivity * deltaTicks;
                     float newPitch = Mth.clamp(this.minecraft.player.getXRot() + pitchDelta * sensitivity * deltaTicks, -90.0F, 90.0F);
                     this.minecraft.player.setYRot(newYaw);
                     this.minecraft.player.setYHeadRot(newYaw);
                     this.minecraft.player.setXRot(newPitch);
                  }
               }
            }
         }
      } else {
         this.lastEdgePanTimeNanos = System.nanoTime();
      }
   }

   private float getEdgePanDelta(double mousePosition, int size, float maxStep) {
      if (mousePosition <= 72.0) {
         double strength = (72.0 - mousePosition) / 72.0;
         return (float)(-maxStep * strength * strength);
      } else {
         double farEdge = size - 72.0;
         if (mousePosition >= farEdge) {
            double strength = (mousePosition - farEdge) / 72.0;
            return (float)(maxStep * strength * strength);
         } else {
            return 0.0F;
         }
      }
   }

   private boolean isMouseInAllowedEdgePanZone(double mouseX, double mouseY) {
      return this.isMouseInAllowedLeftEdgePanZone(mouseX, mouseY)
         || this.isMouseInAllowedRightEdgePanZone(mouseX, mouseY)
         || this.isMouseInAllowedTopEdgePanZone(mouseX, mouseY)
         || this.isMouseInAllowedBottomEdgePanZone(mouseX, mouseY);
   }

   private boolean isMouseInAllowedLeftEdgePanZone(double mouseX, double mouseY) {
      return this.isPointInRect(mouseX, mouseY, 0, this.getCenteredSideEdgeTop(), 72, this.getCenteredSideEdgeBottom());
   }

   private boolean isMouseInAllowedRightEdgePanZone(double mouseX, double mouseY) {
      return this.isPointInRect(mouseX, mouseY, this.width - 72, this.getCenteredSideEdgeTop(), this.width, this.getCenteredSideEdgeBottom());
   }

   private boolean isMouseInAllowedTopEdgePanZone(double mouseX, double mouseY) {
      return this.isPointInRect(mouseX, mouseY, this.getCenteredTopEdgeLeft(), 0, this.getCenteredTopEdgeRight(), 72);
   }

   private boolean isMouseInAllowedBottomEdgePanZone(double mouseX, double mouseY) {
      return this.isPointInRect(mouseX, mouseY, this.getCenteredBottomEdgeLeft(), this.height - 10, this.getCenteredBottomEdgeRight(), this.height);
   }

   private int getCenteredSideEdgeTop() {
      int allowedHeight = this.height / 4;
      return (this.height - allowedHeight) / 2;
   }

   private int getCenteredSideEdgeBottom() {
      int allowedHeight = this.height / 4;
      return this.getCenteredSideEdgeTop() + allowedHeight;
   }

   private int getCenteredTopEdgeLeft() {
      int allowedWidth = this.width / 4;
      return (this.width - allowedWidth) / 2;
   }

   private int getCenteredTopEdgeRight() {
      int allowedWidth = this.width / 4;
      return this.getCenteredTopEdgeLeft() + allowedWidth;
   }

   private int getCenteredBottomEdgeLeft() {
      int allowedWidth = this.width / 10;
      return (this.width - allowedWidth) / 2;
   }

   private int getCenteredBottomEdgeRight() {
      int allowedWidth = this.width / 10;
      return this.getCenteredBottomEdgeLeft() + allowedWidth;
   }

   private boolean isPointInRect(double mouseX, double mouseY, int minX, int minY, int maxX, int maxY) {
      return mouseX >= minX && mouseX <= maxX && mouseY >= minY && mouseY <= maxY;
   }

   private float getDisplayedRelationship() {
      if (this.villager instanceof MaleVillagerEntity male) {
         return VillagerRelationshipData.getDisplayedRelationship(male, male.getRelationship());
      } else {
         return this.villager instanceof FemaleVillagerEntity female
            ? VillagerRelationshipData.getDisplayedRelationship(female, female.getRelationship())
            : 0.5F;
      }
   }

   private float getDisplayedGoldenRelationship() {
      if (this.villager instanceof MaleVillagerEntity male) {
         return VillagerRelationshipData.getDisplayedGoldenRelationship(male, male.getGoldenRelationship());
      } else {
         return this.villager instanceof FemaleVillagerEntity female
            ? VillagerRelationshipData.getDisplayedGoldenRelationship(female, female.getGoldenRelationship())
            : 0.0F;
      }
   }

   private VillagerPersonality getPersonality() {
      if (this.villager instanceof MaleVillagerEntity male) {
         return male.getPersonality();
      } else {
         return this.villager instanceof FemaleVillagerEntity female ? female.getPersonality() : null;
      }
   }

   private static String formatRelationship(float relationship) {
      return String.format(Locale.ROOT, "%.1f/5.0", Mth.clamp(relationship, 0.0F, 5.0F));
   }

   private void renderRelationshipHearts(GuiGraphics graphics, int mouseX, int mouseY) {
      int heartX = 8;
      int topY = 10;
      float relationship = this.getDisplayedRelationship();

      for (int i = 0; i < 5; i++) {
         int x = heartX + i * 18;
         if (i + 1 <= (int)relationship) {
            graphics.blit(HEART_FULL, x, topY, 0.0F, 0.0F, 16, 16, 16, 16);
         } else if (i + 0.5F <= relationship) {
            graphics.blit(HEART_HALF, x, topY, 0.0F, 0.0F, 16, 16, 16, 16);
         } else {
            graphics.blit(HEART_EMPTY, x, topY, 0.0F, 0.0F, 16, 16, 16, 16);
         }
      }

      float goldenRelationship = Mth.clamp(this.getDisplayedGoldenRelationship(), 0.0F, 5.0F);
      if (goldenRelationship > 0.0F) {
         for (int i = 0; i < 5; i++) {
            int x = heartX + i * 18;
            if (i + 1 <= goldenRelationship) {
               graphics.blit(HEART_GOLD_FULL, x, topY, 0.0F, 0.0F, 16, 16, 16, 16);
            } else if (i == 0 && goldenRelationship > 0.0F) {
               graphics.blit(HEART_GOLD_HALF, x, topY, 0.0F, 0.0F, 16, 16, 16, 16);
            } else if (i + 0.5F <= goldenRelationship) {
               graphics.blit(HEART_GOLD_HALF, x, topY, 0.0F, 0.0F, 16, 16, 16, 16);
            }
         }
      }

      int totalHeartWidth = 90;
      if (mouseX >= heartX && mouseX <= heartX + totalHeartWidth && mouseY >= topY && mouseY <= topY + 16) {
         graphics.renderComponentTooltip(this.font, this.getRelationshipTooltip(), mouseX, mouseY);
      }
   }

   private void renderVillagerInfo(GuiGraphics graphics, int mouseX, int mouseY) {
      int leftX = 10;
      int textY = 34;
      String villagerName = this.villager.hasCustomName() && this.villager.getCustomName() != null
         ? this.villager.getCustomName().getString()
         : this.villager.getName().getString();
      String displayedVillagerName = this.getDisplayedVillagerName(villagerName);
      this.drawTextWithIcon(graphics, displayedVillagerName, leftX, textY, 16777215, null, mouseX, mouseY, null);
      int nameWidth = this.font.width(displayedVillagerName);
      if (mouseX >= leftX && mouseX <= leftX + nameWidth && mouseY >= textY && mouseY <= textY + 9) {
         graphics.renderTooltip(this.font, this.getNameRelationshipLabel(), mouseX, mouseY);
      }

      textY += 20;
      VillagerPersonality personality = this.getPersonality();
      if (personality != null) {
         this.drawTextWithIcon(
            graphics,
            VillagerPersonalityIcons.getName(personality),
            leftX,
            textY,
            16777215,
            Component.translatable("slimpatch.dialogue.hover.personality"),
            mouseX,
            mouseY,
            VillagerPersonalityIcons.getIcon(personality)
         );
         textY += 20;
      }

      VillagerProfession profession = this.villager.getVillagerData().getProfession();
      this.drawTextWithIcon(
         graphics,
         VillagerProfessionIcons.getName(profession),
         leftX,
         textY,
         16777215,
         Component.translatable("slimpatch.dialogue.hover.job"),
         mouseX,
         mouseY,
         VillagerProfessionIcons.getIcon(profession)
      );
      textY = this.getGiftIconY();
      UUID playerUuid = this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getUUID() : null;
      boolean hasCooldown = VillagerCooldownsStorage.hasGiftCooldown(this.villager.getUUID(), playerUuid);
      float relationship = this.getDisplayedRelationship();
      boolean canReceiveGift = relationship >= 5.0F && !hasCooldown;
      ResourceLocation icon = canReceiveGift ? GIFT_ICON_COLOR : GIFT_ICON_GRAY;
      int iconSize = 18;
      int iconX = leftX - 2;
      int iconY = textY;
      boolean hoveringGift = mouseX >= iconX && mouseX <= iconX + iconSize && mouseY >= iconY && mouseY <= iconY + iconSize;
      if (hoveringGift) {
         int glowSize = iconSize + 2;
         graphics.setColor(1.0F, 1.0F, 1.0F, 0.1F);
         graphics.blit(icon, iconX - 1, iconY - 1, 0.0F, 0.0F, glowSize, glowSize, glowSize, glowSize);
         graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      }

      graphics.blit(icon, iconX, iconY, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
      if (hoveringGift) {
         Component tooltip = hasCooldown
            ? Component.translatable("slimpatch.dialogue.gift.cooldown")
            : (canReceiveGift ? Component.translatable("slimpatch.dialogue.gift.available") : Component.translatable("slimpatch.dialogue.gift.locked"));
         graphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
      }
   }

   private void renderDialogueLine(GuiGraphics graphics) {
      long now = System.currentTimeMillis();
      if (this.visibleChars < this.fullLine.length() && now - this.lastCharTime > 30L) {
         this.visibleChars++;
         this.lastCharTime = now;
      }

      String villagerName = this.villager.hasCustomName() && this.villager.getCustomName() != null
         ? this.villager.getCustomName().getString()
         : this.villager.getName().getString();
      String visibleText = this.fullLine.substring(0, Math.min(this.visibleChars, this.fullLine.length()));
      this.drawWrappedBorderedCentered(graphics, villagerName + ": " + visibleText, this.width / 2, this.height - 165, this.width - 80, 16775395);
      String commandStatus = this.getCommandStatusText();
      if (commandStatus != null) {
         graphics.drawCenteredString(this.font, commandStatus, this.actionsButtonX + this.actionsButtonWidth / 2, this.familyButtonY - 9 - 8, 16777215);
      }
   }

   private void renderExitHint(GuiGraphics graphics) {
      long elapsed = System.currentTimeMillis() - this.exitHintStartTime;
      if (elapsed < 3200L) {
         float alpha = 1.0F;
         long fadeStart = 2500L;
         if (elapsed > fadeStart) {
            alpha = 1.0F - (float)(elapsed - fadeStart) / 700.0F;
         }

         alpha = Mth.clamp(alpha, 0.0F, 1.0F);
         float pulse = 0.94F + 0.06F * Mth.sin((float)elapsed / 180.0F);
         int alphaValue = Mth.clamp((int)(255.0F * alpha * pulse), 0, 255);
         if (alphaValue > 3) {
            int textColor = alphaValue << 24 | 16311232;
            int shadowColor = alphaValue << 24;
            int x = this.width / 2;
            int y = 12;
            String exitHintText = Component.translatable("slimpatch.dialogue.exit_hint", new Object[]{ModKeyBindings.getOpenDialogueKeyMessage()}).getString();
            graphics.drawCenteredString(this.font, exitHintText, x + 1, y, shadowColor);
            graphics.drawCenteredString(this.font, exitHintText, x - 1, y, shadowColor);
            graphics.drawCenteredString(this.font, exitHintText, x, y + 1, shadowColor);
            graphics.drawCenteredString(this.font, exitHintText, x, y - 1, shadowColor);
            graphics.drawCenteredString(this.font, exitHintText, x, y, textColor);
         }
      }
   }

   private void drawTextWithIcon(GuiGraphics graphics, String text, int x, int y, int color, Component hover, int mouseX, int mouseY, ResourceLocation icon) {
      graphics.drawString(this.font, text, x + 1, y, 0);
      graphics.drawString(this.font, text, x - 1, y, 0);
      graphics.drawString(this.font, text, x, y + 1, 0);
      graphics.drawString(this.font, text, x, y - 1, 0);
      graphics.drawString(this.font, text, x, y, color);
      int textWidth = this.font.width(text);
      int hoverX1 = x;
      int hoverX2 = x + textWidth;
      int hoverY1 = y;
      int hoverY2 = y + 9;
      if (icon != null) {
         int iconX = x + textWidth + 3;
         int iconY = y + (9 - 15) / 2;
         graphics.blit(icon, iconX, iconY, 0.0F, 0.0F, 15, 15, 15, 15);
         hoverX2 = iconX + 15;
         hoverY1 = Math.min(hoverY1, iconY);
         hoverY2 = Math.max(hoverY2, iconY + 15);
      }

      if (hover != null && mouseX >= hoverX1 && mouseX <= hoverX2 && mouseY >= hoverY1 && mouseY <= hoverY2) {
         graphics.renderTooltip(this.font, hover, mouseX, mouseY);
      }
   }

   private void drawWrappedBorderedCentered(GuiGraphics graphics, String text, int centerX, int topY, int maxWidth, int color) {
      int y = topY;

      for (FormattedCharSequence line : this.font.split(Component.literal(text), maxWidth)) {
         graphics.drawCenteredString(this.font, line, centerX + 1, y, 0);
         graphics.drawCenteredString(this.font, line, centerX - 1, y, 0);
         graphics.drawCenteredString(this.font, line, centerX, y + 1, 0);
         graphics.drawCenteredString(this.font, line, centerX, y - 1, 0);
         graphics.drawCenteredString(this.font, line, centerX, y, color);
         y += 9 + 2;
      }
   }

   private String getDisplayedVillagerName(String villagerName) {
      if (this.villager instanceof FamilyVillager familyVillager) {
         VillagerAgeStage ageStage = familyVillager.getAgeStage();
         return ageStage == VillagerAgeStage.ADULT ? villagerName : villagerName + " (" + this.getLifeStageLabel(ageStage) + ")";
      } else {
         return villagerName;
      }
   }

   private String getLifeStageLabel(VillagerAgeStage ageStage) {
      return Component.translatable("slimpatch.life_stage." + ageStage.name().toLowerCase(Locale.ROOT)).getString();
   }

   private List<Component> getRelationshipTooltip() {
      boolean canUseRomanticInteraction = this.canUseRomanticInteraction();
      if (this.villager instanceof FamilyVillager familyVillager) {
         if (familyVillager.isMarried()) {
            String spousePlayerName = familyVillager.getSpousePlayerName();
            if (!spousePlayerName.isEmpty()) {
               return List.of(Component.translatable("slimpatch.dialogue.hover.relationship_married_to", new Object[]{spousePlayerName}));
            }

            return List.of(Component.translatable("slimpatch.dialogue.hover.relationship_married"));
         }

         float displayedGoldenRelationship = this.getDisplayedGoldenRelationship();
         if (canUseRomanticInteraction && displayedGoldenRelationship >= 5.0F) {
            return List.of(Component.translatable("slimpatch.dialogue.hover.relationship_ring_hint"));
         }

         if (canUseRomanticInteraction && displayedGoldenRelationship > 0.0F) {
            return List.of(Component.translatable("slimpatch.dialogue.hover.relationship_dating"));
         }

         if (canUseRomanticInteraction
            && this.minecraft != null
            && this.minecraft.player != null
            && VillagerFamilyData.canStartDating(familyVillager, this.villager, this.minecraft.player, this.getDisplayedRelationship())) {
            return List.of(Component.translatable("slimpatch.dialogue.hover.relationship_flower_hint"));
         }
      }

      return List.of(Component.translatable("slimpatch.dialogue.hover.relationship"));
   }

   private boolean canUseRomanticInteraction() {
      return this.minecraft != null && this.minecraft.player != null && VillagerFamilyData.canUseRomanticInteraction(this.villager, this.minecraft.player);
   }

   private Component getNameRelationshipLabel() {
      boolean genderedLabels = (Boolean)Config.GENDERED_RELATIONSHIP_LABELS.get();
      boolean male = this.villager instanceof MaleVillagerEntity;
      boolean female = this.villager instanceof FemaleVillagerEntity;
      if (this.minecraft == null || this.minecraft.player == null || !VillagerFamilyData.isFamilyChildOf(this.villager, this.minecraft.player)) {
         if (this.villager instanceof FamilyVillager familyVillager) {
            UUID playerUuid = this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getUUID() : null;
            String playerName = this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getGameProfile().getName() : "";
            if (familyVillager.getRelationshipStage() == VillagerRelationshipStage.MARRIED) {
               boolean isSpouse = playerUuid != null && playerUuid.equals(familyVillager.getSpousePlayerUuid());
               if (!isSpouse && !playerName.isEmpty()) {
                  isSpouse = playerName.equals(familyVillager.getSpousePlayerName());
               }

               if (isSpouse) {
                  if (!genderedLabels) {
                     return Component.translatable("slimpatch.relationship_label.spouse");
                  }

                  if (male) {
                     return Component.translatable("slimpatch.relationship_label.husband");
                  }

                  if (female) {
                     return Component.translatable("slimpatch.relationship_label.wife");
                  }

                  return Component.translatable("slimpatch.relationship_label.spouse");
               }

               return this.getBaseRelationshipLabel();
            }

            boolean isFormerSpouse = playerUuid != null && playerUuid.equals(familyVillager.getFormerSpousePlayerUuid());
            if (!isFormerSpouse && !playerName.isEmpty()) {
               isFormerSpouse = playerName.equals(familyVillager.getFormerSpousePlayerName());
            }

            if (isFormerSpouse) {
               if (!genderedLabels) {
                  return Component.translatable("slimpatch.relationship_label.ex_spouse");
               }

               if (male) {
                  return Component.translatable("slimpatch.relationship_label.ex_husband");
               }

               if (female) {
                  return Component.translatable("slimpatch.relationship_label.ex_wife");
               }

               return Component.translatable("slimpatch.relationship_label.ex_spouse");
            }

            if (this.canUseRomanticInteraction() && this.getDisplayedGoldenRelationship() > 0.0F) {
               if (!genderedLabels) {
                  return Component.translatable("slimpatch.relationship_label.romantic_partner");
               }

               if (male) {
                  return Component.translatable("slimpatch.relationship_label.boyfriend");
               }

               if (female) {
                  return Component.translatable("slimpatch.relationship_label.girlfriend");
               }

               return Component.translatable("slimpatch.relationship_label.romantic_partner");
            }
         }

         return this.getBaseRelationshipLabel();
      } else if (!genderedLabels) {
         return Component.translatable("slimpatch.relationship_label.your_child");
      } else if (male) {
         return Component.translatable("slimpatch.relationship_label.your_son");
      } else {
         return female
            ? Component.translatable("slimpatch.relationship_label.your_daughter")
            : Component.translatable("slimpatch.relationship_label.your_child");
      }
   }

   private Component getBaseRelationshipLabel() {
      float relationship = this.getDisplayedRelationship();
      if (relationship >= 4.0F) {
         return Component.translatable("slimpatch.relationship_label.close_friend");
      } else if (relationship >= 3.0F) {
         return Component.translatable("slimpatch.relationship_label.friend");
      } else {
         return relationship >= 1.0F
            ? Component.translatable("slimpatch.relationship_label.acquaintance")
            : Component.translatable("slimpatch.relationship_label.stranger");
      }
   }

   private void onActionClicked(String action) {
      if (this.menuMode == VillagerDialogueScreen.MenuMode.FAMILY || this.menuMode == VillagerDialogueScreen.MenuMode.FAMILY_CONFIRM) {
         this.onFamilyActionClicked(action);
      } else if (action.equals("Back")) {
         this.setMenuMode(VillagerDialogueScreen.MenuMode.SOCIAL);
      } else if (!this.canUseProtectedCommandAction(action)) {
         this.showOwnerDeniedMessage();
         this.fullLine = "I can't follow your order right now.";
         this.visibleChars = 0;
         this.lastCharTime = System.currentTimeMillis();
      } else {
         String homeActionLabel = this.getHomeActionLabel();
         if (action.equals(homeActionLabel)) {
            if (this.minecraft != null && this.minecraft.player != null && this.villager instanceof CommandableVillager commandableVillager) {
               if (commandableVillager.hasHome()) {
                  commandableVillager.clearHome();
                  ModNetworking.sendToServer(new VillagerCommandPacket(this.villager.getId(), VillagerCommandPacket.Action.CLEAR_HOME));
                  this.fullLine = "I will no longer live here.";
               } else {
                  commandableVillager.setHome(this.villager.blockPosition(), this.villager.level().dimension());
                  ModNetworking.sendToServer(new VillagerCommandPacket(this.villager.getId(), VillagerCommandPacket.Action.SET_HOME));
                  this.fullLine = "I will live here now.";
               }

               this.rebuildMenuWidgets();
               this.visibleChars = 0;
               this.lastCharTime = System.currentTimeMillis();
            }
         } else {
            String followActionLabel = this.getFollowActionLabel();
            if (action.equals(followActionLabel)) {
               if (this.minecraft != null && this.minecraft.player != null && this.villager instanceof CommandableVillager commandableVillager) {
                  if (commandableVillager.getCommandState() == VillagerCommandState.FOLLOW) {
                     commandableVillager.setCommandState(VillagerCommandState.NONE);
                     commandableVillager.setCommandTargetUuid(null);
                     ModNetworking.sendToServer(new VillagerCommandPacket(this.villager.getId(), VillagerCommandPacket.Action.STOP_FOLLOWING));
                     this.fullLine = "I will stop following you.";
                  } else {
                     commandableVillager.setCommandState(VillagerCommandState.FOLLOW);
                     ModNetworking.sendToServer(new VillagerCommandPacket(this.villager.getId(), VillagerCommandState.FOLLOW));
                     this.fullLine = "I will follow you.";
                  }

                  this.rebuildMenuWidgets();
                  this.visibleChars = 0;
                  this.lastCharTime = System.currentTimeMillis();
               }
            } else {
               String stayActionLabel = this.getStayActionLabel();
               if (action.equals(stayActionLabel)
                  && this.minecraft != null
                  && this.minecraft.player != null
                  && this.villager instanceof CommandableVillager commandableVillager) {
                  if (commandableVillager.getCommandState() == VillagerCommandState.STAY) {
                     commandableVillager.setCommandState(VillagerCommandState.NONE);
                     commandableVillager.setStayPos(null);
                     ModNetworking.sendToServer(new VillagerCommandPacket(this.villager.getId(), VillagerCommandPacket.Action.MOVE_FREELY));
                     this.fullLine = "I will move freely again.";
                  } else {
                     commandableVillager.setCommandState(VillagerCommandState.STAY);
                     ModNetworking.sendToServer(new VillagerCommandPacket(this.villager.getId(), VillagerCommandState.STAY));
                     this.fullLine = "I will stay here.";
                  }

                  this.rebuildMenuWidgets();
                  this.visibleChars = 0;
                  this.lastCharTime = System.currentTimeMillis();
               }
            }
         }
      }
   }

   private List<String> getCurrentMenuOptions() {
      List<String> options = new ArrayList<>();
      if (this.menuMode == VillagerDialogueScreen.MenuMode.ACTIONS) {
         options.add(this.getHomeActionLabel());
         options.add(this.getFollowActionLabel());
         options.add(this.getStayActionLabel());
         options.add("Back");
         return options;
      }

      if (this.menuMode != VillagerDialogueScreen.MenuMode.FAMILY) {
         if (this.menuMode == VillagerDialogueScreen.MenuMode.FAMILY_CONFIRM) {
            options.add(Component.translatable("slimpatch.dialogue.action.yes").getString());
            options.add(Component.translatable("slimpatch.dialogue.action.no").getString());
            return options;
         }

         options.add("Friendly");
         options.add("Mean");
         options.add("Joke");
         if (this.canUseRomanticInteraction()) {
            options.add("Flirt");
         }

         return options;
      } else {
         if (this.canShowHaveChildOption()) {
            options.add(Component.translatable("slimpatch.dialogue.action.have_child").getString());
         }

         if (this.canShowAskAboutBabyOption()) {
            options.add(Component.translatable("slimpatch.dialogue.action.ask_about_baby").getString());
         }

         if (this.villager instanceof MaleVillagerEntity || this.villager instanceof FemaleVillagerEntity) {
            options.add(Component.translatable("slimpatch.dialogue.action.family_tree").getString());
         }

         options.add("Back");
         return options;
      }
   }

   private void onFamilyActionClicked(String action) {
      String haveChildLabel = Component.translatable("slimpatch.dialogue.action.have_child").getString();
      String askAboutBabyLabel = Component.translatable("slimpatch.dialogue.action.ask_about_baby").getString();
      String familyTreeLabel = Component.translatable("slimpatch.dialogue.action.family_tree").getString();
      String yesLabel = Component.translatable("slimpatch.dialogue.action.yes").getString();
      String noLabel = Component.translatable("slimpatch.dialogue.action.no").getString();
      if (this.menuMode == VillagerDialogueScreen.MenuMode.FAMILY) {
         if (action.equals("Back")) {
            this.birthReadyConfirmationPending = false;
            this.familyStartConfirmationPending = false;
            this.setMenuMode(VillagerDialogueScreen.MenuMode.SOCIAL);
         } else if (action.equals(haveChildLabel) && this.canShowHaveChildOption()) {
            this.birthReadyConfirmationPending = false;
            this.familyStartConfirmationPending = true;
            this.setMenuMode(VillagerDialogueScreen.MenuMode.FAMILY_CONFIRM);
            this.setDialogueLine(Component.translatable("slimpatch.dialogue.family.confirm").getString());
         } else if (action.equals(askAboutBabyLabel) && this.canShowAskAboutBabyOption()) {
            ModNetworking.sendToServer(new FamilyStatusPacket(this.villager.getId()));
         } else {
            if (action.equals(familyTreeLabel) && (this.villager instanceof MaleVillagerEntity || this.villager instanceof FemaleVillagerEntity)) {
               ModNetworking.sendToServer(new RequestFamilyTreePacket(this.villager.getId()));
               this.onClose();
            }
         }
      } else if (this.menuMode == VillagerDialogueScreen.MenuMode.FAMILY_CONFIRM) {
         if (action.equals(noLabel)) {
            if (this.birthReadyConfirmationPending) {
               this.birthReadyConfirmationPending = false;
               this.setMenuMode(VillagerDialogueScreen.MenuMode.FAMILY);
               this.setDialogueLine(Component.translatable("slimpatch.dialogue.family.birth_decline").getString());
            } else {
               this.familyStartConfirmationPending = false;
               this.setMenuMode(VillagerDialogueScreen.MenuMode.FAMILY);
               this.setDialogueLine(Component.translatable("slimpatch.dialogue.family.decline").getString());
            }
         } else {
            if (action.equals(yesLabel)) {
               if (this.birthReadyConfirmationPending) {
                  this.birthReadyConfirmationPending = false;
                  this.setMenuMode(VillagerDialogueScreen.MenuMode.FAMILY);
                  ModNetworking.sendToServer(new RequestBirthScreenPacket(this.villager.getId()));
                  return;
               }

               if (this.familyStartConfirmationPending) {
                  this.familyStartConfirmationPending = false;
                  this.setMenuMode(VillagerDialogueScreen.MenuMode.FAMILY);
                  ModNetworking.sendToServer(new StartFamilyPacket(this.villager.getId()));
               }
            }
         }
      }
   }

   private void setDialogueLine(String line) {
      this.fullLine = line != null && !line.isEmpty() ? line : "...";
      this.visibleChars = 0;
      this.lastCharTime = System.currentTimeMillis();
   }

   private boolean canShowHaveChildOption() {
      if (!this.isHavingChildrenEnabledForClient()) {
         return false;
      } else {
         return !(this.villager instanceof FamilyVillager familyVillager)
            ? false
            : familyVillager.getRelationshipStage() == VillagerRelationshipStage.MARRIED
               && familyVillager.getAgeStage() == VillagerAgeStage.ADULT
               && !familyVillager.isExpectingChild()
               && this.isLocalPlayerSpouse(familyVillager);
      }
   }

   private boolean canShowAskAboutBabyOption() {
      return !(this.villager instanceof FamilyVillager familyVillager)
         ? false
         : familyVillager.getRelationshipStage() == VillagerRelationshipStage.MARRIED
            && familyVillager.getAgeStage() == VillagerAgeStage.ADULT
            && familyVillager.isExpectingChild()
            && this.isLocalPlayerSpouse(familyVillager);
   }

   private boolean isLocalPlayerSpouse(FamilyVillager familyVillager) {
      UUID playerUuid = this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getUUID() : null;
      String playerName = this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getGameProfile().getName() : "";
      boolean isSpouse = playerUuid != null && playerUuid.equals(familyVillager.getSpousePlayerUuid());
      if (!isSpouse && !playerName.isEmpty()) {
         isSpouse = playerName.equals(familyVillager.getSpousePlayerName());
      }

      return isSpouse;
   }

   private boolean isHavingChildrenEnabledForClient() {
      return this.minecraft != null && this.minecraft.getSingleplayerServer() != null && !this.minecraft.getSingleplayerServer().isPublished()
         ? (Boolean)Config.ENABLE_HAVING_CHILDREN.get()
         : ClientMultiplayerSkinSettings.isHavingChildrenEnabled();
   }

   private boolean canUseProtectedCommandAction(String action) {
      if (action.equals("Back")) {
         return true;
      }

      String homeActionLabel = this.getHomeActionLabel();
      String followActionLabel = this.getFollowActionLabel();
      String stayActionLabel = this.getStayActionLabel();
      return !action.equals(homeActionLabel) && !action.equals(followActionLabel) && !action.equals(stayActionLabel) ? true : this.canUseSensitiveAction();
   }

   private boolean canUseSensitiveAction() {
      if (this.minecraft == null || this.minecraft.player == null) {
         return false;
      } else if (this.minecraft.getSingleplayerServer() != null && !this.minecraft.getSingleplayerServer().isPublished()) {
         return true;
      } else if (!(this.villager instanceof CommandableVillager commandableVillager)) {
         return true;
      } else {
         UUID ownerUuid = commandableVillager.getCommandOwnerUuid();
         return ownerUuid == null || ownerUuid.equals(this.minecraft.player.getUUID());
      }
   }

   private void showOwnerDeniedMessage() {
      if (this.minecraft != null && this.minecraft.player != null) {
         String ownerName = null;
         if (this.villager instanceof CommandableVillager commandableVillager) {
            ownerName = commandableVillager.getCommandOwnerName();
         }

         if (ownerName == null || ownerName.isEmpty()) {
            ownerName = Component.translatable("slimpatch.message.other_player").getString();
         }

         this.minecraft.player.displayClientMessage(Component.translatable("slimpatch.message.villager_controlled", new Object[]{ownerName}), true);
      }
   }

   private String getHomeActionLabel() {
      return this.villager instanceof CommandableVillager commandableVillager && commandableVillager.hasHome() ? "Clear Home" : "Set Home";
   }

   private String getFollowActionLabel() {
      return this.getCommandState() == VillagerCommandState.FOLLOW ? "Stop Following" : "Follow";
   }

   private String getStayActionLabel() {
      return this.getCommandState() == VillagerCommandState.STAY ? "Move Freely" : "Stay";
   }

   private String getActionsButtonLabel() {
      return this.menuMode != VillagerDialogueScreen.MenuMode.ACTIONS ? "Actions" : "Dialogue";
   }

   private String getFamilyButtonLabel() {
      return this.menuMode != VillagerDialogueScreen.MenuMode.FAMILY && this.menuMode != VillagerDialogueScreen.MenuMode.FAMILY_CONFIRM
         ? Component.translatable("slimpatch.dialogue.action.family").getString()
         : "Dialogue";
   }

   private VillagerCommandState getCommandState() {
      return this.villager instanceof CommandableVillager commandableVillager ? commandableVillager.getCommandState() : VillagerCommandState.NONE;
   }

   private void openEditVillagerScreen() {
      if (!this.canUseSensitiveAction()) {
         this.showOwnerDeniedMessage();
      } else {
         if (this.minecraft != null) {
            this.minecraft.setScreen(new VillagerEditScreen(this.villager, this));
         }
      }
   }

   private void openVillagerEquipmentScreen() {
      if (!this.canUseSensitiveAction()) {
         this.showOwnerDeniedMessage();
      } else {
         if (this.minecraft != null && this.minecraft.player != null) {
            VillagerEquipmentScreen.setPreviewVillager(this.villager);
            ModNetworking.sendToServer(new VillagerEquipmentPacket(this.villager.getId()));
            this.onClose();
         }
      }
   }

   private String getCommandStatusText() {
      VillagerCommandState state = null;
      String ownerName = null;
      if (this.villager instanceof MaleVillagerEntity male) {
         state = male.getCommandState();
         ownerName = male.getCommandOwnerName();
      } else if (this.villager instanceof FemaleVillagerEntity female) {
         state = female.getCommandState();
         ownerName = female.getCommandOwnerName();
      }

      if (state == VillagerCommandState.FOLLOW) {
         return this.shouldShowOwnerName(ownerName)
            ? Component.translatable("slimpatch.dialogue.status.following_owner", new Object[]{ownerName}).getString()
            : Component.translatable("slimpatch.dialogue.status.following").getString();
      } else if (state == VillagerCommandState.STAY) {
         return this.shouldShowOwnerName(ownerName)
            ? Component.translatable("slimpatch.dialogue.status.stay_owner", new Object[]{ownerName}).getString()
            : Component.translatable("slimpatch.dialogue.status.stay").getString();
      } else {
         return null;
      }
   }

   private boolean shouldShowOwnerName(String ownerName) {
      return ownerName != null && !ownerName.isEmpty()
         ? this.minecraft == null || this.minecraft.getSingleplayerServer() == null || this.minecraft.getSingleplayerServer().isPublished()
         : false;
   }

   public void onClose() {
      DialogueThirdPersonCameraHandler.reset();
      this.restoreDialogueCameraPreference();
      if (!this.closing) {
         this.closing = true;
         ModNetworking.sendToServer(new DialogueStatePacket(this.villager.getId(), false));
      }

      super.onClose();
   }

   public void removed() {
      DialogueThirdPersonCameraHandler.reset();
      this.restoreDialogueCameraPreference();
      super.removed();
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (ModKeyBindings.OPEN_DIALOGUE != null && ModKeyBindings.OPEN_DIALOGUE.matches(keyCode, scanCode)) {
         this.onClose();
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   private void applyDialogueCameraPreference() {
      if ((Boolean)Config.FORCE_FIRST_PERSON_IN_DIALOGUE.get() && this.minecraft != null && this.minecraft.options != null && !this.restoreCameraOnClose) {
         this.previousCameraType = this.minecraft.options.getCameraType();
         if (this.previousCameraType != CameraType.FIRST_PERSON) {
            this.minecraft.options.setCameraType(CameraType.FIRST_PERSON);
            this.restoreCameraOnClose = true;
         }
      }
   }

   private void restoreDialogueCameraPreference() {
      if (this.restoreCameraOnClose && this.minecraft != null && this.minecraft.options != null && this.previousCameraType != null) {
         this.minecraft.options.setCameraType(this.previousCameraType);
         this.restoreCameraOnClose = false;
         this.previousCameraType = null;
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      int leftX = 10;
      int iconX = leftX - 2;
      int iconY = this.getGiftIconY();
      int iconSize = 18;
      UUID playerUuid = this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getUUID() : null;
      boolean hasCooldown = VillagerCooldownsStorage.hasGiftCooldown(this.villager.getUUID(), playerUuid);
      float relationship = this.getDisplayedRelationship();
      boolean canReceiveGift = relationship >= 5.0F && !hasCooldown;
      if (mouseX >= iconX && mouseX <= iconX + iconSize && mouseY >= iconY && mouseY <= iconY + iconSize) {
         if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            if (canReceiveGift) {
               ModNetworking.sendToServer(new GiftPacket(this.villager.getId()));
            }
         }

         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   private int getGiftIconY() {
      int textY = 34;
      textY += 20;
      if (this.getPersonality() != null) {
         textY += 20;
      }

      return textY + 20;
   }

   public void renderBackground(GuiGraphics graphics) {
      if ((Boolean)Config.DIALOGUE_SCREEN_BLUR.get()) {
         super.renderBackground(graphics);
      }
   }

   public boolean isPauseScreen() {
      return false;
   }

   private class ActionOptionButton extends Button {
      private final String action;

      private ActionOptionButton(int x, int y, int width, String action) {
         super(x, y, width, 16, Component.literal(action), button -> {}, DEFAULT_NARRATION);
         this.action = action;
      }

      public void onPress() {
         VillagerDialogueScreen.this.onActionClicked(this.action);
      }

      protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
         int background = this.isHovered ? -1438366652 : -2013265920;
         graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), background);
         graphics.drawCenteredString(
            Minecraft.getInstance().font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, 16777215
         );
      }
   }

   private class ActionsButton extends Button {
      private ActionsButton(int x, int y, int width, int height) {
         super(
            x,
            y,
            width,
            height,
            Component.literal(VillagerDialogueScreen.this.getActionsButtonLabel()),
            button -> VillagerDialogueScreen.this.setMenuMode(
               VillagerDialogueScreen.this.menuMode == VillagerDialogueScreen.MenuMode.ACTIONS
                  ? VillagerDialogueScreen.MenuMode.SOCIAL
                  : VillagerDialogueScreen.MenuMode.ACTIONS
            ),
            DEFAULT_NARRATION
         );
      }

      protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
         int color = this.isHovered ? 14737632 : 16777215;
         graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), this.isHovered ? -1438366652 : -2013265920);
         graphics.drawCenteredString(
            Minecraft.getInstance().font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, color
         );
      }
   }

   private class DialogueOptionButton extends Button {
      private final String option;

      private DialogueOptionButton(int x, int y, int width, String option) {
         super(x, y, width, 16, VillagerDialogueScreen.this.getDialogueOptionLabel(option), button -> {}, DEFAULT_NARRATION);
         this.option = option;
      }

      public void onPress() {
         if (!this.option.equals("Flirt") || VillagerDialogueScreen.this.canUseRomanticInteraction()) {
            if (VillagerDialogueScreen.this.minecraft != null && VillagerDialogueScreen.this.minecraft.player != null) {
               UUID uuid = VillagerDialogueScreen.this.villager.getUUID();
               Map<String, Integer> cooldowns = VillagerCooldownsStorage.getCooldowns(uuid, VillagerDialogueScreen.this.minecraft.player.getUUID());
               int remaining = cooldowns.getOrDefault(this.option, 0);
               if (remaining <= 0) {
                  VillagerDialogueScreen.this.sendOption(this.option);
               }
            }
         }
      }

      protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
         UUID uuid = VillagerDialogueScreen.this.villager.getUUID();
         UUID playerUuid = VillagerDialogueScreen.this.minecraft != null && VillagerDialogueScreen.this.minecraft.player != null
            ? VillagerDialogueScreen.this.minecraft.player.getUUID()
            : null;
         Map<String, Integer> cooldowns = VillagerCooldownsStorage.getCooldowns(uuid, playerUuid);
         int remaining = cooldowns.getOrDefault(this.option, 0);
         boolean onCooldown = remaining > 0;
         boolean romanticBlocked = this.option.equals("Flirt") && !VillagerDialogueScreen.this.canUseRomanticInteraction();
         int background = !onCooldown && !romanticBlocked ? (this.isHovered ? -1438366652 : -2013265920) : 1429418803;
         graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), background);
         int color = romanticBlocked ? 11184810 : 16777215;
         graphics.drawCenteredString(
            Minecraft.getInstance().font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, color
         );
         if (onCooldown
            && mouseX >= this.getX()
            && mouseX <= this.getX() + this.getWidth()
            && mouseY >= this.getY()
            && mouseY <= this.getY() + this.getHeight()) {
            graphics.renderTooltip(VillagerDialogueScreen.this.font, Component.literal(remaining + "s"), mouseX, mouseY);
         }
      }
   }

   private class EditIconButton extends Button {
      private EditIconButton(int x, int y, int width, int height) {
         super(x, y, width, height, Component.empty(), button -> VillagerDialogueScreen.this.openEditVillagerScreen(), DEFAULT_NARRATION);
      }

      protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
         if (this.isHovered) {
            int glowSize = this.width + 2;
            graphics.setColor(1.0F, 1.0F, 1.0F, 0.1F);
            graphics.blit(VillagerDialogueScreen.EDIT_BUTTON_TEXTURE, this.getX() - 1, this.getY() - 1, 0.0F, 0.0F, glowSize, glowSize, glowSize, glowSize);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
         }

         graphics.blit(VillagerDialogueScreen.EDIT_BUTTON_TEXTURE, this.getX(), this.getY(), 0.0F, 0.0F, this.width, this.height, this.width, this.height);
         if (this.isHovered) {
            graphics.renderTooltip(VillagerDialogueScreen.this.font, Component.translatable("slimpatch.dialogue.edit.tooltip"), mouseX, mouseY);
         }
      }
   }

   private class EquipmentIconButton extends Button {
      private EquipmentIconButton(int x, int y, int width, int height) {
         super(x, y, width, height, Component.empty(), button -> VillagerDialogueScreen.this.openVillagerEquipmentScreen(), DEFAULT_NARRATION);
      }

      protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
         if (this.isHovered) {
            int glowSize = this.width + 2;
            graphics.setColor(1.0F, 1.0F, 1.0F, 0.1F);
            graphics.blit(VillagerDialogueScreen.EQUIPMENT_BUTTON_TEXTURE, this.getX() - 1, this.getY() - 1, 0.0F, 0.0F, glowSize, glowSize, glowSize, glowSize);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
         }

         graphics.blit(VillagerDialogueScreen.EQUIPMENT_BUTTON_TEXTURE, this.getX(), this.getY(), 0.0F, 0.0F, this.width, this.height, this.width, this.height);
         if (this.isHovered) {
            graphics.renderTooltip(VillagerDialogueScreen.this.font, Component.translatable("slimpatch.dialogue.equipment.tooltip"), mouseX, mouseY);
         }
      }
   }

   private class FamilyButton extends Button {
      private FamilyButton(int x, int y, int width, int height) {
         super(
            x,
            y,
            width,
            height,
            Component.literal(VillagerDialogueScreen.this.getFamilyButtonLabel()),
            button -> VillagerDialogueScreen.this.setMenuMode(
               VillagerDialogueScreen.this.menuMode != VillagerDialogueScreen.MenuMode.FAMILY
                     && VillagerDialogueScreen.this.menuMode != VillagerDialogueScreen.MenuMode.FAMILY_CONFIRM
                  ? VillagerDialogueScreen.MenuMode.FAMILY
                  : VillagerDialogueScreen.MenuMode.SOCIAL
            ),
            DEFAULT_NARRATION
         );
      }

      protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
         int color = this.isHovered ? 14737632 : 16777215;
         graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), this.isHovered ? -1438366652 : -2013265920);
         graphics.drawCenteredString(
            Minecraft.getInstance().font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, color
         );
      }
   }

   private enum MenuMode {
      SOCIAL,
      ACTIONS,
      FAMILY,
      FAMILY_CONFIRM;
   }
}
