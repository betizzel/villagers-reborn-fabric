package com.javic.slimpatch.client.gui;

import com.javic.slimpatch.ModEntities;
import com.javic.slimpatch.client.ClientCustomSkinHelper;
import com.javic.slimpatch.entity.BirthScreenData;
import com.javic.slimpatch.entity.FemaleVillagerEntity;
import com.javic.slimpatch.entity.MaleVillagerEntity;
import com.javic.slimpatch.entity.VillagerAgeStage;
import com.javic.slimpatch.entity.VillagerPersonality;
import com.javic.slimpatch.network.ConfirmBirthPacket;
import com.javic.slimpatch.network.ModNetworking;
import com.javic.slimpatch.util.MultiplayerSkinStorage;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.joml.Quaternionf;

public class BirthChildScreen extends Screen {
   private static final int SKIN_COLUMNS = 4;
   private static final int SKIN_BUTTON_SIZE = 40;
   private static final int SKIN_BUTTON_SPACING = 4;
   private static final int SKIN_VISIBLE_ROWS = 3;
   private static final int CONTENT_TOP = 38;
   private static final int CONTENT_HEIGHT = 386;
   private static final int CONTENT_BOTTOM_MARGIN = 16;
   private static final int GENERAL_SCROLLBAR_WIDTH = 4;
   private final int spouseEntityId;
   private final String villagerName;
   private final String childGender;
   private final String skinType;
   private final List<Integer> skinIds;
   private final List<BirthChildScreen.SkinOptionButton> skinButtons = new ArrayList<>();
   private final String pendingSkinKey;
   private EditBox nameField;
   private EditBox customSkinField;
   private Button confirmButton;
   private Button openSkinsFolderButton;
   private Button previousPersonalityButton;
   private Button nextPersonalityButton;
   private BirthChildScreen.CustomSkinOptionButton customSkinButton;
   private LivingEntity previewEntity;
   private VillagerPersonality selectedPersonality;
   private int selectedSkinId;
   private boolean useCustomSkin;
   private String customSkinInput = "";
   private byte[] customSkinPngData = new byte[0];
   private String customSkinPreviewPath = "";
   private int skinScrollRow;
   private int screenScrollOffset;
   private float previewYaw = 180.0F;
   private boolean draggingPreview;

   public BirthChildScreen(int spouseEntityId, String villagerName, String childGender, String initialPersonality, String skinType, int initialSkinId) {
      super(Component.translatable("slimpatch.screen.birth.title"));
      this.spouseEntityId = spouseEntityId;
      this.villagerName = villagerName;
      this.childGender = BirthScreenData.normalizeGender(childGender);
      this.skinType = BirthScreenData.normalizeSkinType(skinType);
      this.skinIds = BirthScreenData.getValidSkinIds(this.childGender, this.skinType);
      this.pendingSkinKey = ClientCustomSkinHelper.getPendingBirthSkinKey(spouseEntityId);
      this.selectedPersonality = BirthScreenData.sanitizeBirthPersonality(this.parsePersonality(initialPersonality));
      this.selectedSkinId = BirthScreenData.isValidSkin(this.childGender, this.skinType, initialSkinId)
         ? initialSkinId
         : BirthScreenData.getDefaultSkinId(this.childGender, this.skinType);
   }

   protected void init() {
      super.init();
      this.previewEntity = this.createPreviewEntity();
      String previousName = this.nameField != null ? this.nameField.getValue() : "";
      String previousCustomSkinInput = this.customSkinField != null ? this.customSkinField.getValue() : this.customSkinInput;
      int rightColumnX = this.width / 2 + 16;
      int topY = this.getTopY();
      this.nameField = new EditBox(this.font, rightColumnX, topY + 202, 180, 20, Component.translatable("slimpatch.screen.birth.name"));
      this.nameField.setMaxLength(32);
      this.nameField.setValue(previousName);
      this.nameField.setResponder(value -> this.updatePreviewEntity());
      this.addRenderableWidget(this.nameField);
      this.setInitialFocus(this.nameField);
      this.customSkinField = new EditBox(this.font, rightColumnX, topY + 300, 180, 20, Component.translatable("slimpatch.screen.birth.skindex_url"));
      this.customSkinField.setMaxLength(500);
      this.customSkinField.setValue(previousCustomSkinInput == null ? "" : previousCustomSkinInput);
      this.customSkinField.setResponder(value -> this.customSkinInput = value.trim());
      this.addRenderableWidget(this.customSkinField);
      this.skinButtons.clear();

      for (int i = 0; i < this.skinIds.size(); i++) {
         int skinId = this.skinIds.get(i);
         BirthChildScreen.SkinOptionButton button = new BirthChildScreen.SkinOptionButton(0, 0, skinId);
         this.skinButtons.add(button);
         this.addRenderableWidget(button);
      }

      this.customSkinButton = (BirthChildScreen.CustomSkinOptionButton)this.addRenderableWidget(new BirthChildScreen.CustomSkinOptionButton(0, 0));
      this.updateSkinButtonPositions();
      this.previousPersonalityButton = (Button)this.addRenderableWidget(
         Button.builder(Component.literal("<"), buttonx -> this.cyclePersonality(-1)).bounds(rightColumnX, topY + 252, 20, 20).build()
      );
      this.nextPersonalityButton = (Button)this.addRenderableWidget(
         Button.builder(Component.literal(">"), buttonx -> this.cyclePersonality(1)).bounds(rightColumnX + 160, topY + 252, 20, 20).build()
      );
      this.openSkinsFolderButton = (Button)this.addRenderableWidget(
         Button.builder(Component.translatable("slimpatch.screen.birth.open_skins_folder"), buttonx -> this.openSkinsFolder())
            .bounds(rightColumnX, topY + 326, 180, 20)
            .build()
      );
      this.confirmButton = (Button)this.addRenderableWidget(
         Button.builder(Component.translatable("slimpatch.screen.birth.confirm"), buttonx -> this.confirmBirth())
            .bounds(rightColumnX, topY + 352, 180, 20)
            .build()
      );
      this.clampScreenScroll();
      this.updateLayout();
      this.updatePreviewEntity();
      this.updateConfirmButton();
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   public void onClose() {
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
         return true;
      } else if (keyCode == 256) {
         this.openCancelConfirmation();
         return true;
      } else if ((keyCode == 257 || keyCode == 335) && this.customSkinField != null && this.customSkinField.isFocused()) {
         this.applyCustomSkinFromField();
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
      if (this.isMouseOverSkinGrid(mouseX, mouseY)) {
         int nextRow = Math.max(0, Math.min(this.getMaxScrollRow(), this.skinScrollRow - (int)Math.signum(scrollY)));
         if (nextRow != this.skinScrollRow) {
            this.skinScrollRow = nextRow;
            this.updateSkinButtonPositions();
            return true;
         }
      }

      int nextScrollOffset = Math.max(0, Math.min(this.getMaxScreenScroll(), this.screenScrollOffset - (int)Math.signum(scrollY) * 24));
      if (nextScrollOffset != this.screenScrollOffset) {
         this.screenScrollOffset = nextScrollOffset;
         this.updateLayout();
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, scrollY);
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0 && this.isMouseOverPreview(mouseX, mouseY)) {
         this.draggingPreview = true;
         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (this.draggingPreview && button == 0) {
         this.previewYaw = this.wrapDegrees(this.previewYaw + (float)dragX * 2.0F);
         return true;
      } else {
         return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
      }
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (button == 0 && this.draggingPreview) {
         this.draggingPreview = false;
         return true;
      } else {
         return super.mouseReleased(mouseX, mouseY, button);
      }
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics);
      int leftColumnX = this.width / 2 - 150;
      int rightColumnX = this.width / 2 + 16;
      int topY = this.getTopY();
      graphics.drawCenteredString(this.font, this.title, this.width / 2, 16, 16777215);
      graphics.enableScissor(0, this.getViewportTop(), this.width, this.getViewportBottom());
      graphics.fill(leftColumnX - 12, topY - 6, leftColumnX + 152, topY + 190, 1711276032);
      graphics.fill(rightColumnX - 8, topY - 6, rightColumnX + 188, topY + 386, 1711276032);
      graphics.drawString(this.font, Component.translatable("slimpatch.screen.birth.appearance"), rightColumnX, topY, 16777215);
      graphics.drawString(this.font, Component.translatable("slimpatch.screen.birth.name"), rightColumnX, topY + 190, 16777215);
      graphics.drawString(this.font, Component.translatable("slimpatch.screen.birth.personality"), rightColumnX, topY + 228, 16777215);
      graphics.drawString(this.font, Component.translatable("slimpatch.screen.birth.skindex_url"), rightColumnX, topY + 288, 16777215);
      graphics.drawCenteredString(this.font, BirthScreenData.getPersonalityName(this.selectedPersonality), rightColumnX + 90, topY + 262, 16777215);
      graphics.drawCenteredString(
         this.font,
         Component.translatable("female".equals(this.childGender) ? "slimpatch.dialogue.gender.female" : "slimpatch.dialogue.gender.male"),
         leftColumnX + 70,
         topY + 12,
         11184810
      );
      this.renderSkinScrollBar(graphics, rightColumnX, topY);
      if (this.previewEntity != null) {
         this.renderEntityPreview(graphics, leftColumnX, topY + 4, leftColumnX + 140, topY + 170, 60, this.previewEntity, this.previewYaw);
      }

      super.render(graphics, mouseX, mouseY, partialTick);
      graphics.disableScissor();
      this.renderGeneralScrollBar(graphics, rightColumnX);
   }

   public void handleConfirmResult(boolean success, String translationKey, String translationArgument) {
      if (this.minecraft != null && this.minecraft.player != null) {
         this.minecraft
            .player
            .displayClientMessage(
               translationArgument != null && !translationArgument.isEmpty()
                  ? Component.translatable(translationKey, new Object[]{translationArgument})
                  : Component.translatable(translationKey),
               true
            );
      }

      if (success && this.minecraft != null) {
         this.minecraft.setScreen(null);
      }
   }

   private void confirmBirth() {
      if (this.minecraft != null && this.minecraft.player != null && this.isSelectionValid()) {
         ModNetworking.sendToServer(
            new ConfirmBirthPacket(
               this.spouseEntityId,
               this.nameField.getValue(),
               this.selectedSkinId,
               this.selectedPersonality.name(),
               this.childGender,
               this.useCustomSkin,
               this.customSkinInput,
               this.customSkinPngData
            )
         );
      }
   }

   private void cyclePersonality(int direction) {
      this.selectedPersonality = BirthScreenData.cyclePersonality(this.selectedPersonality, direction);
      this.updatePreviewEntity();
      this.updateConfirmButton();
   }

   private void openCancelConfirmation() {
      if (this.minecraft != null) {
         this.minecraft
            .setScreen(
               new ConfirmScreen(
                  confirmed -> {
                     if (this.minecraft != null) {
                        this.minecraft.setScreen(confirmed ? null : this);
                     }
                  },
                  Component.translatable("slimpatch.screen.birth.cancel.title"),
                  Component.translatable("slimpatch.screen.birth.cancel.message"),
                  Component.translatable("slimpatch.dialogue.action.yes"),
                  Component.translatable("slimpatch.dialogue.action.no")
               )
            );
      }
   }

   private void updatePreviewEntity() {
      if (this.previewEntity instanceof MaleVillagerEntity male) {
         this.applyPreviewSkin(male);
         male.setAgeStage(VillagerAgeStage.TODDLER);
         male.setPersonality(this.selectedPersonality);
         male.setCustomName(
            Component.literal(
               BirthScreenData.isValidChildName(this.nameField != null ? this.nameField.getValue() : "")
                  ? BirthScreenData.sanitizeChildName(this.nameField.getValue())
                  : "?"
            )
         );
      } else {
         if (this.previewEntity instanceof FemaleVillagerEntity female) {
            this.applyPreviewSkin(female);
            female.setAgeStage(VillagerAgeStage.TODDLER);
            female.setPersonality(this.selectedPersonality);
            female.setCustomName(
               Component.literal(
                  BirthScreenData.isValidChildName(this.nameField != null ? this.nameField.getValue() : "")
                     ? BirthScreenData.sanitizeChildName(this.nameField.getValue())
                     : "?"
               )
            );
         }
      }
   }

   private void updateConfirmButton() {
      if (this.confirmButton != null) {
         this.confirmButton.active = this.isSelectionValid();
      }
   }

   private void updateLayout() {
      int rightColumnX = this.width / 2 + 16;
      int topY = this.getTopY();
      if (this.nameField != null) {
         this.nameField.setX(rightColumnX);
         this.nameField.setY(topY + 202);
      }

      if (this.customSkinField != null) {
         this.customSkinField.setX(rightColumnX);
         this.customSkinField.setY(topY + 300);
      }

      if (this.previousPersonalityButton != null) {
         this.previousPersonalityButton.setX(rightColumnX);
         this.previousPersonalityButton.setY(topY + 252);
      }

      if (this.nextPersonalityButton != null) {
         this.nextPersonalityButton.setX(rightColumnX + 160);
         this.nextPersonalityButton.setY(topY + 252);
      }

      if (this.openSkinsFolderButton != null) {
         this.openSkinsFolderButton.setX(rightColumnX);
         this.openSkinsFolderButton.setY(topY + 326);
      }

      if (this.confirmButton != null) {
         this.confirmButton.setX(rightColumnX);
         this.confirmButton.setY(topY + 352);
      }

      this.updateGeneralWidgetVisibility(this.nameField);
      this.updateGeneralWidgetVisibility(this.customSkinField);
      this.updateGeneralWidgetVisibility(this.previousPersonalityButton);
      this.updateGeneralWidgetVisibility(this.nextPersonalityButton);
      this.updateGeneralWidgetVisibility(this.openSkinsFolderButton);
      this.updateGeneralWidgetVisibility(this.confirmButton);
      this.updateSkinButtonPositions();
   }

   private void updateGeneralWidgetVisibility(AbstractWidget widget) {
      if (widget != null) {
         int viewportTop = this.getViewportTop();
         int viewportBottom = this.getViewportBottom();
         boolean visible = widget.getY() + widget.getHeight() > viewportTop && widget.getY() < viewportBottom;
         widget.visible = visible;
         widget.active = visible;
         if (!visible && this.getFocused() == widget) {
            this.setFocused(null);
         }
      }
   }

   private void updateSkinButtonPositions() {
      int rightColumnX = this.width / 2 + 16;
      int topY = this.getTopY();

      for (int i = 0; i < this.skinButtons.size(); i++) {
         BirthChildScreen.SkinOptionButton button = this.skinButtons.get(i);
         int row = i / 4;
         int column = i % 4;
         int visibleRow = row - this.skinScrollRow;
         button.setX(rightColumnX + column * 44);
         button.setY(topY + 16 + visibleRow * 44);
         button.visible = visibleRow >= 0 && visibleRow < 3;
         button.active = button.visible;
      }

      this.updateCustomSkinButtonPosition(rightColumnX, topY);
   }

   private void updateCustomSkinButtonPosition(int rightColumnX, int topY) {
      if (this.customSkinButton != null) {
         if (this.useCustomSkin && MultiplayerSkinStorage.validatePng(this.customSkinPngData) == null) {
            int index = this.skinButtons.size();
            int row = index / 4;
            int column = index % 4;
            int visibleRow = row - this.skinScrollRow;
            this.customSkinButton.setX(rightColumnX + column * 44);
            this.customSkinButton.setY(topY + 16 + visibleRow * 44);
            this.customSkinButton.visible = visibleRow >= 0 && visibleRow < 3;
            this.customSkinButton.active = this.customSkinButton.visible;
         } else {
            this.customSkinButton.visible = false;
            this.customSkinButton.active = false;
         }
      }
   }

   private void renderSkinScrollBar(GuiGraphics graphics, int rightColumnX, int topY) {
      int totalRows = (int)Math.ceil(this.getTotalSkinOptions() / 4.0);
      if (totalRows > 3) {
         int barX1 = rightColumnX + 160 + 12 + 4;
         int barY1 = topY + 16;
         int barY2 = barY1 + 120 + 8;
         graphics.fill(barX1, barY1, barX1 + 4, barY2, 1715749956);
         int thumbHeight = Math.max(12, (barY2 - barY1) * 3 / totalRows);
         int maxScrollRow = totalRows - 3;
         int travel = barY2 - barY1 - thumbHeight;
         int thumbY = barY1 + (maxScrollRow == 0 ? 0 : travel * this.skinScrollRow / maxScrollRow);
         graphics.fill(barX1, thumbY, barX1 + 4, thumbY + thumbHeight, -5197648);
      }
   }

   private int getMaxScrollRow() {
      return Math.max(0, (int)Math.ceil(this.getTotalSkinOptions() / 4.0) - 3);
   }

   private int getTotalSkinOptions() {
      return this.skinButtons.size() + (this.useCustomSkin && MultiplayerSkinStorage.validatePng(this.customSkinPngData) == null ? 1 : 0);
   }

   private int getTopY() {
      return 38 - this.screenScrollOffset;
   }

   private int getViewportTop() {
      return 38;
   }

   private int getViewportBottom() {
      return this.height - 16;
   }

   private int getMaxScreenScroll() {
      return Math.max(0, 424 - this.getViewportBottom());
   }

   private void clampScreenScroll() {
      this.screenScrollOffset = Math.max(0, Math.min(this.getMaxScreenScroll(), this.screenScrollOffset));
   }

   private void renderGeneralScrollBar(GuiGraphics graphics, int rightColumnX) {
      int maxScroll = this.getMaxScreenScroll();
      if (maxScroll > 0) {
         int barX1 = rightColumnX + 188 + 8;
         int barY1 = this.getViewportTop();
         int barY2 = this.getViewportBottom();
         graphics.fill(barX1, barY1, barX1 + 4, barY2, 1715749956);
         int thumbHeight = Math.max(18, (barY2 - barY1) * (barY2 - barY1) / 386);
         int travel = barY2 - barY1 - thumbHeight;
         int thumbY = barY1 + (maxScroll == 0 ? 0 : travel * this.screenScrollOffset / maxScroll);
         graphics.fill(barX1, thumbY, barX1 + 4, thumbY + thumbHeight, -5197648);
      }
   }

   private boolean isMouseOverSkinGrid(double mouseX, double mouseY) {
      int rightColumnX = this.width / 2 + 16;
      int topY = this.getTopY();
      int gridX1 = rightColumnX;
      int gridY1 = topY + 16;
      int gridX2 = gridX1 + 160 + 12;
      int gridY2 = gridY1 + 120 + 8;
      return mouseX >= gridX1 && mouseX <= gridX2 + 8 && mouseY >= gridY1 && mouseY <= gridY2;
   }

   private boolean isSelectionValid() {
      return BirthScreenData.isValidChildName(this.nameField != null ? this.nameField.getValue() : "")
         && (
            this.useCustomSkin
               ? MultiplayerSkinStorage.validatePng(this.customSkinPngData) == null
               : BirthScreenData.isValidSkin(this.childGender, this.skinType, this.selectedSkinId)
         )
         && BirthScreenData.isValidPersonality(this.selectedPersonality);
   }

   private void applyPreviewSkin(MaleVillagerEntity male) {
      if (this.useCustomSkin && MultiplayerSkinStorage.validatePng(this.customSkinPngData) == null) {
         male.setSavedSkinInput(this.customSkinInput);
         male.setCustomSkinPath(this.customSkinPreviewPath);
      } else {
         male.setSavedSkinInput("");
         String curatedSkinPath = BirthScreenData.getCuratedSkinResourcePath(this.childGender, this.skinType, this.selectedSkinId);
         male.setCustomSkinPath(curatedSkinPath == null ? "" : curatedSkinPath);
         male.setSkinIndex(this.selectedSkinId);
      }
   }

   private void applyPreviewSkin(FemaleVillagerEntity female) {
      if (this.useCustomSkin && MultiplayerSkinStorage.validatePng(this.customSkinPngData) == null) {
         female.setSavedSkinInput(this.customSkinInput);
         female.setCustomSkinPath(this.customSkinPreviewPath);
      } else {
         female.setSavedSkinInput("");
         String curatedSkinPath = BirthScreenData.getCuratedSkinResourcePath(this.childGender, this.skinType, this.selectedSkinId);
         female.setCustomSkinPath(curatedSkinPath == null ? "" : curatedSkinPath);
         female.setSkinIndex(this.selectedSkinId);
      }
   }

   private void applyCustomSkinFromField() {
      this.applyCustomSkinInput(this.customSkinField != null ? this.customSkinField.getValue() : "");
   }

   private void applyCustomSkinInput(String skinInput) {
      String value = skinInput == null ? "" : skinInput.trim();
      if (value.isEmpty()) {
         this.useCustomSkin = false;
         this.customSkinInput = "";
         this.customSkinPngData = new byte[0];
         this.customSkinPreviewPath = "";
         this.updatePreviewEntity();
         this.updateSkinButtonPositions();
         this.updateConfirmButton();
      } else if (!ClientCustomSkinHelper.allowLocalCustomSkins() && !ClientCustomSkinHelper.canUploadMultiplayerCustomSkin()) {
         if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(Component.translatable("slimpatch.message.custom_skin_disabled"), true);
         }
      } else {
         String previewPath = ClientCustomSkinHelper.resolvePendingBirthSkinPath(this.pendingSkinKey, value);
         byte[] pngData = this.readCustomSkinBytes(value);
         if (pngData != null && MultiplayerSkinStorage.validatePng(pngData) == null) {
            this.useCustomSkin = true;
            this.customSkinInput = value;
            this.customSkinPngData = pngData;
            this.customSkinPreviewPath = previewPath;
            this.updatePreviewEntity();
            this.scrollToCustomSkin();
            this.updateConfirmButton();
         } else {
            if (this.minecraft != null && this.minecraft.player != null) {
               this.minecraft.player.displayClientMessage(Component.translatable("slimpatch.message.birth_invalid_skin"), true);
            }
         }
      }
   }

   private byte[] readCustomSkinBytes(String skinInput) {
      return ClientCustomSkinHelper.readPendingBirthSkinBytes(this.pendingSkinKey, skinInput);
   }

   private void openSkinsFolder() {
      ClientCustomSkinHelper.openPendingBirthSkinFolder(this.pendingSkinKey);
   }

   private LivingEntity createPreviewEntity() {
      Minecraft minecraft = Minecraft.getInstance();
      Level level = minecraft.level;
      if (level == null) {
         return null;
      }

      if ("female".equals(this.childGender)) {
         FemaleVillagerEntity villager = (FemaleVillagerEntity)((EntityType)ModEntities.FEMALE_VILLAGER).create(level);
         if (villager != null) {
            villager.setNoAi(true);
            villager.setSilent(true);
         }

         return villager;
      } else {
         MaleVillagerEntity villager = (MaleVillagerEntity)((EntityType)ModEntities.MALE_VILLAGER).create(level);
         if (villager != null) {
            villager.setNoAi(true);
            villager.setSilent(true);
         }

         return villager;
      }
   }

   private VillagerPersonality parsePersonality(String personalityName) {
      try {
         return VillagerPersonality.valueOf(personalityName);
      } catch (IllegalArgumentException e) {
         return VillagerPersonality.FRIENDLY;
      }
   }

   private void renderEntityPreview(GuiGraphics graphics, int x1, int y1, int x2, int y2, int scale, LivingEntity entity, float yaw) {
      if (entity != null) {
         float bodyRot = entity.yBodyRot;
         float yRot = entity.getYRot();
         float xRot = entity.getXRot();
         float yHeadRotO = entity.yHeadRotO;
         float yHeadRot = entity.yHeadRot;
         entity.yBodyRot = yaw;
         entity.setYRot(yaw);
         entity.setXRot(0.0F);
         entity.yHeadRot = entity.getYRot();
         entity.yHeadRotO = entity.getYRot();
         float entityScale = entity.getScale();
         int centerY = (y1 + y2) / 2 + Math.round((y2 - y1) * 0.12F);
         graphics.enableScissor(x1, y1, x2, y2);
         InventoryScreen.renderEntityInInventory(
            graphics, (x1 + x2) / 2, centerY, Math.round(scale / entityScale), new Quaternionf().rotateZ((float) Math.PI), null, entity
         );
         graphics.disableScissor();
         entity.yBodyRot = bodyRot;
         entity.setYRot(yRot);
         entity.setXRot(xRot);
         entity.yHeadRotO = yHeadRotO;
         entity.yHeadRot = yHeadRot;
      }
   }

   private boolean isMouseOverPreview(double mouseX, double mouseY) {
      int topY = this.getTopY();
      return mouseX >= this.width / 2 - 150 && mouseX <= this.width / 2 - 10 && mouseY >= topY + 4 && mouseY <= topY + 170;
   }

   private float wrapDegrees(float value) {
      float wrapped = value % 360.0F;
      return wrapped < 0.0F ? wrapped + 360.0F : wrapped;
   }

   private void scrollToCustomSkin() {
      this.skinScrollRow = this.getMaxScrollRow();
      this.updateSkinButtonPositions();
   }

   private class CustomSkinOptionButton extends Button {
      private CustomSkinOptionButton(int x, int y) {
         super(x, y, 40, 40, Component.empty(), button -> {
            BirthChildScreen.this.useCustomSkin = true;
            BirthChildScreen.this.updatePreviewEntity();
            BirthChildScreen.this.updateSkinButtonPositions();
            BirthChildScreen.this.updateConfirmButton();
         }, DEFAULT_NARRATION);
      }

      protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
         graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, this.isHovered ? -1438357880 : -2009901705);
         LivingEntity preview = BirthChildScreen.this.createPreviewEntity();
         if (preview instanceof MaleVillagerEntity male) {
            male.setSavedSkinInput(BirthChildScreen.this.customSkinInput);
            male.setCustomSkinPath(BirthChildScreen.this.customSkinPreviewPath);
            male.setAgeStage(VillagerAgeStage.TODDLER);
            male.setPersonality(BirthChildScreen.this.selectedPersonality);
            BirthChildScreen.this.renderEntityPreview(
               graphics, this.getX() + 2, this.getY() + 2, this.getX() + this.width - 2, this.getY() + this.height - 2, 18, male, 180.0F
            );
         } else if (preview instanceof FemaleVillagerEntity female) {
            female.setSavedSkinInput(BirthChildScreen.this.customSkinInput);
            female.setCustomSkinPath(BirthChildScreen.this.customSkinPreviewPath);
            female.setAgeStage(VillagerAgeStage.TODDLER);
            female.setPersonality(BirthChildScreen.this.selectedPersonality);
            BirthChildScreen.this.renderEntityPreview(
               graphics, this.getX() + 2, this.getY() + 2, this.getX() + this.width - 2, this.getY() + this.height - 2, 18, female, 180.0F
            );
         }

         graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 2, -5187329);
         graphics.fill(this.getX(), this.getY() + this.height - 2, this.getX() + this.width, this.getY() + this.height, -5187329);
         graphics.fill(this.getX(), this.getY(), this.getX() + 2, this.getY() + this.height, -5187329);
         graphics.fill(this.getX() + this.width - 2, this.getY(), this.getX() + this.width, this.getY() + this.height, -5187329);
      }
   }

   private class SkinOptionButton extends Button {
      private final int skinId;

      private SkinOptionButton(int x, int y, int skinId) {
         super(
            x,
            y,
            40,
            40,
            Component.empty(),
            button -> {
               BirthChildScreen.this.useCustomSkin = false;
               BirthChildScreen.this.customSkinInput = BirthChildScreen.this.customSkinField != null
                  ? BirthChildScreen.this.customSkinField.getValue().trim()
                  : "";
               BirthChildScreen.this.customSkinPngData = new byte[0];
               BirthChildScreen.this.customSkinPreviewPath = "";
               BirthChildScreen.this.selectedSkinId = skinId;
               BirthChildScreen.this.updatePreviewEntity();
               BirthChildScreen.this.updateSkinButtonPositions();
               BirthChildScreen.this.updateConfirmButton();
            },
            DEFAULT_NARRATION
         );
         this.skinId = skinId;
      }

      protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
         boolean selected = !BirthChildScreen.this.useCustomSkin && BirthChildScreen.this.selectedSkinId == this.skinId;
         graphics.fill(
            this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, selected ? -5205952 : (this.isHovered ? -1438366652 : -2013265920)
         );
         LivingEntity preview = BirthChildScreen.this.createPreviewEntity();
         if (preview instanceof MaleVillagerEntity male) {
            String curatedSkinPath = BirthScreenData.getCuratedSkinResourcePath(BirthChildScreen.this.childGender, BirthChildScreen.this.skinType, this.skinId);
            male.setCustomSkinPath(curatedSkinPath == null ? "" : curatedSkinPath);
            male.setSkinIndex(this.skinId);
            male.setAgeStage(VillagerAgeStage.TODDLER);
            male.setPersonality(BirthChildScreen.this.selectedPersonality);
            BirthChildScreen.this.renderEntityPreview(
               graphics, this.getX() + 2, this.getY() + 2, this.getX() + this.width - 2, this.getY() + this.height - 2, 18, male, 180.0F
            );
         } else if (preview instanceof FemaleVillagerEntity female) {
            String curatedSkinPath = BirthScreenData.getCuratedSkinResourcePath(BirthChildScreen.this.childGender, BirthChildScreen.this.skinType, this.skinId);
            female.setCustomSkinPath(curatedSkinPath == null ? "" : curatedSkinPath);
            female.setSkinIndex(this.skinId);
            female.setAgeStage(VillagerAgeStage.TODDLER);
            female.setPersonality(BirthChildScreen.this.selectedPersonality);
            BirthChildScreen.this.renderEntityPreview(
               graphics, this.getX() + 2, this.getY() + 2, this.getX() + this.width - 2, this.getY() + this.height - 2, 18, female, 180.0F
            );
         }

         if (selected) {
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 2, -1);
            graphics.fill(this.getX(), this.getY() + this.height - 2, this.getX() + this.width, this.getY() + this.height, -1);
            graphics.fill(this.getX(), this.getY(), this.getX() + 2, this.getY() + this.height, -1);
            graphics.fill(this.getX() + this.width - 2, this.getY(), this.getX() + this.width, this.getY() + this.height, -1);
         }
      }
   }
}
