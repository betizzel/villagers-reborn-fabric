package com.javic.slimpatch.client.gui;

import com.javic.slimpatch.client.key.ModKeyBindings;
import com.javic.slimpatch.entity.CommandableVillager;
import com.javic.slimpatch.entity.VillagerCombatMode;
import com.javic.slimpatch.entity.VillagerFollowMode;
import com.javic.slimpatch.menu.VillagerEquipmentMenu;
import com.javic.slimpatch.network.ModNetworking;
import com.javic.slimpatch.network.VillagerArmorVisibilityPacket;
import com.javic.slimpatch.network.VillagerCombatModePacket;
import com.javic.slimpatch.network.VillagerFollowModePacket;
import com.javic.slimpatch.network.VillagerMutePacket;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;

public class VillagerEquipmentScreen extends AbstractContainerScreen<VillagerEquipmentMenu> {
   private static final ResourceLocation CONTAINER_TEXTURE = new ResourceLocation("textures/gui/container/generic_54.png");
   private static final ResourceLocation INVENTORY_TEXTURE = new ResourceLocation("textures/gui/container/inventory.png");
   private static final int LEFT_PANEL_WIDTH = 100;
   private static final int SLOT_TEXTURE_U = 7;
   private static final int SLOT_TEXTURE_V = 83;
   private static final int SLOT_SIZE = 18;
   private static final int OFFHAND_SLOT_VISUAL_COVER_WIDTH = 1;
   private static final int OFFHAND_SLOT_VISUAL_COVER_HEIGHT = 1;
   private static final int OFFHAND_SLOT_VISUAL_CORNER_COLOR = -3750202;
   private static final int COMBAT_BUTTON_X_OFFSET = 278;
   private static final int COMBAT_BUTTON_Y = 4;
   private static final int COMBAT_BUTTON_WIDTH = 16;
   private static final int COMBAT_BUTTON_HEIGHT = 16;
   private static final int COMBAT_BUTTON_SPACING = 22;
   private static final int FOLLOW_BUTTON_GROUP_GAP = 6;
   private static final int ARMOR_VISIBILITY_BUTTON_X_OFFSET = -17;
   private static final int ARMOR_VISIBILITY_BUTTON_Y = 4;
   private static final int MUTE_BUTTON_X_OFFSET = -17;
   private static final int MUTE_BUTTON_Y = 26;
   private static final int ICON_BUTTON_SIZE = 16;
   private static final int ICON_TEXTURE_SIZE = 16;
   private static final int ICON_BACKPLATE_SIZE = 20;
   private static final int ICON_BACKPLATE_PADDING = 2;
   private static final int ICON_BACKPLATE_COLOR = -7631989;
   private static final int ICON_BACKPLATE_LIGHT = -3158065;
   private static final int ICON_BACKPLATE_DARK = -11579569;
   private static final ResourceLocation COMBAT_AGGRESSIVE_ON_TEXTURE = new ResourceLocation("slimpatch", "textures/gui/inventory/combat_aggressive_on.png");
   private static final ResourceLocation COMBAT_AGGRESSIVE_OFF_TEXTURE = new ResourceLocation("slimpatch", "textures/gui/inventory/combat_aggressive_off.png");
   private static final ResourceLocation COMBAT_DEFENSIVE_ON_TEXTURE = new ResourceLocation("slimpatch", "textures/gui/inventory/combat_defensive_on.png");
   private static final ResourceLocation COMBAT_DEFENSIVE_OFF_TEXTURE = new ResourceLocation("slimpatch", "textures/gui/inventory/combat_defensive_off.png");
   private static final ResourceLocation COMBAT_PASSIVE_ON_TEXTURE = new ResourceLocation("slimpatch", "textures/gui/inventory/combat_passive_on.png");
   private static final ResourceLocation COMBAT_PASSIVE_OFF_TEXTURE = new ResourceLocation("slimpatch", "textures/gui/inventory/combat_passive_off.png");
   private static final ResourceLocation FOLLOW_CLOSE_ON_TEXTURE = new ResourceLocation("slimpatch", "textures/gui/inventory/follow_close_on.png");
   private static final ResourceLocation FOLLOW_CLOSE_OFF_TEXTURE = new ResourceLocation("slimpatch", "textures/gui/inventory/follow_close_off.png");
   private static final ResourceLocation FOLLOW_RELAXED_ON_TEXTURE = new ResourceLocation("slimpatch", "textures/gui/inventory/follow_relaxed_on.png");
   private static final ResourceLocation FOLLOW_RELAXED_OFF_TEXTURE = new ResourceLocation("slimpatch", "textures/gui/inventory/follow_relaxed_off.png");
   private static final ResourceLocation ARMOR_HIDDEN_ON_TEXTURE = new ResourceLocation("slimpatch", "textures/gui/inventory/armor_hidden_on.png");
   private static final ResourceLocation ARMOR_HIDDEN_OFF_TEXTURE = new ResourceLocation("slimpatch", "textures/gui/inventory/armor_hidden_off.png");
   private static final ResourceLocation SOUND_ON_TEXTURE = new ResourceLocation("slimpatch", "textures/gui/inventory/sound_on.png");
   private static final ResourceLocation SOUND_OFF_TEXTURE = new ResourceLocation("slimpatch", "textures/gui/inventory/sound_off.png");
   private static LivingEntity previewVillager;
   private final LivingEntity villagerPreview;

   public VillagerEquipmentScreen(VillagerEquipmentMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
      this.villagerPreview = previewVillager;
      previewVillager = null;
      this.imageWidth = 276;
      this.imageHeight = 168;
      this.inventoryLabelX = 108;
      this.inventoryLabelY = 74;
      this.titleLabelX = 108;
      this.titleLabelY = 6;
   }

   public static void setPreviewVillager(LivingEntity villager) {
      previewVillager = villager;
   }

   protected void init() {
      super.init();
      int buttonX = this.leftPos + 278;
      this.addRenderableWidget(
         new VillagerEquipmentScreen.CombatModeButton(
            buttonX,
            this.topPos + 4,
            VillagerCombatMode.AGGRESSIVE,
            COMBAT_AGGRESSIVE_ON_TEXTURE,
            COMBAT_AGGRESSIVE_OFF_TEXTURE,
            Component.translatable("slimpatch.screen.villager_equipment.combat.aggressive.tooltip")
         )
      );
      this.addRenderableWidget(
         new VillagerEquipmentScreen.CombatModeButton(
            buttonX,
            this.topPos + 4 + 22,
            VillagerCombatMode.DEFENSIVE,
            COMBAT_DEFENSIVE_ON_TEXTURE,
            COMBAT_DEFENSIVE_OFF_TEXTURE,
            Component.translatable("slimpatch.screen.villager_equipment.combat.defensive.tooltip")
         )
      );
      this.addRenderableWidget(
         new VillagerEquipmentScreen.CombatModeButton(
            buttonX,
            this.topPos + 4 + 44,
            VillagerCombatMode.PASSIVE,
            COMBAT_PASSIVE_ON_TEXTURE,
            COMBAT_PASSIVE_OFF_TEXTURE,
            Component.translatable("slimpatch.screen.villager_equipment.combat.passive.tooltip")
         )
      );
      int followButtonY = this.topPos + 4 + 66 + 6;
      this.addRenderableWidget(
         new VillagerEquipmentScreen.FollowModeButton(
            buttonX,
            followButtonY,
            VillagerFollowMode.CLOSE,
            FOLLOW_CLOSE_ON_TEXTURE,
            FOLLOW_CLOSE_OFF_TEXTURE,
            Component.translatable("slimpatch.screen.villager_equipment.follow.close.tooltip")
         )
      );
      this.addRenderableWidget(
         new VillagerEquipmentScreen.FollowModeButton(
            buttonX,
            followButtonY + 22,
            VillagerFollowMode.RELAXED,
            FOLLOW_RELAXED_ON_TEXTURE,
            FOLLOW_RELAXED_OFF_TEXTURE,
            Component.translatable("slimpatch.screen.villager_equipment.follow.relaxed.tooltip")
         )
      );
      int armorVisibilityButtonX = this.leftPos + -17;
      this.addRenderableWidget(new VillagerEquipmentScreen.ArmorVisibilityButton(armorVisibilityButtonX, this.topPos + 4));
      this.addRenderableWidget(new VillagerEquipmentScreen.MuteButton(this.leftPos + -17, this.topPos + 26));
   }

   protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
      int x = this.leftPos;
      int y = this.topPos;
      graphics.blit(INVENTORY_TEXTURE, x, y, 0.0F, 0.0F, 97, 83, 256, 256);
      graphics.blit(CONTAINER_TEXTURE, x + 100, y, 0.0F, 0.0F, 176, 71, 256, 256);
      graphics.blit(CONTAINER_TEXTURE, x + 100, y + 71, 0.0F, 126.0F, 176, 96, 256, 256);
      this.renderIconBackplate(graphics, x + -17 - 2, y + 4 - 2);
      this.renderIconBackplate(graphics, x + -17 - 2, y + 26 - 2);
      int rightButtonX = x + 278;
      this.renderIconBackplate(graphics, rightButtonX - 2, y + 4 - 2);
      this.renderIconBackplate(graphics, rightButtonX - 2, y + 4 + 22 - 2);
      this.renderIconBackplate(graphics, rightButtonX - 2, y + 4 + 44 - 2);
      int followButtonY = y + 4 + 66 + 6;
      this.renderIconBackplate(graphics, rightButtonX - 2, followButtonY - 2);
      this.renderIconBackplate(graphics, rightButtonX - 2, followButtonY + 22 - 2);
      this.renderEquipmentSlotBackground(graphics, x + 77, y + 40);
      graphics.blit(INVENTORY_TEXTURE, x + 77 - 1, y + 62, 75.0F, 62.0F, 1, 18, 256, 256);
      graphics.blit(INVENTORY_TEXTURE, x + 77, y + 62 - 1, 77.0F, 60.0F, 18, 1, 256, 256);
      graphics.fill(x + 77 - 1, y + 62 - 1, x + 77, y + 62, -3750202);
      this.renderEquipmentSlotBackground(graphics, x + 77, y + 62);
      if (this.villagerPreview != null) {
         int renderX1 = x + 26;
         int renderY1 = y + 8;
         int renderX2 = x + 75;
         int renderY2 = y + 78;
         float centerX = (renderX1 + renderX2) / 2.0F;
         float centerY = (renderY1 + renderY2) / 2.0F;
         InventoryScreen.renderEntityInInventoryFollowsMouse(
            graphics, (renderX1 + renderX2) / 2, renderY2, 34, centerX - mouseX, centerY - mouseY, this.villagerPreview
         );
      }
   }

   public void renderTransparentBackground(GuiGraphics graphics) {
      graphics.fill(0, 0, this.width, this.height, 2013265920);
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      this.renderBackground(graphics);
      super.render(graphics, mouseX, mouseY, partialTicks);
      this.renderTooltip(graphics, mouseX, mouseY);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (ModKeyBindings.OPEN_DIALOGUE != null && ModKeyBindings.OPEN_DIALOGUE.matches(keyCode, scanCode)) {
         this.onClose();
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
      graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
      String hpText = this.getHealthText();
      graphics.drawString(this.font, hpText, 276 - this.font.width(hpText) - 20, 6, 4210752, false);
   }

   private void renderEquipmentSlotBackground(GuiGraphics graphics, int x, int y) {
      graphics.blit(INVENTORY_TEXTURE, x, y, 7.0F, 83.0F, 18, 18, 256, 256);
   }

   private void renderIconBackplate(GuiGraphics graphics, int x, int y) {
      graphics.fill(x + 2, y, x + 20 - 2, y + 20, -7631989);
      graphics.fill(x, y + 2, x + 20, y + 20 - 2, -7631989);
      graphics.fill(x + 2, y, x + 20 - 2, y + 1, -3158065);
      graphics.fill(x, y + 2, x + 1, y + 20 - 2, -3158065);
      graphics.fill(x + 1, y + 1, x + 2, y + 2, -3158065);
      graphics.fill(x + 2, y + 20 - 1, x + 20 - 2, y + 20, -11579569);
      graphics.fill(x + 20 - 1, y + 2, x + 20, y + 20 - 2, -11579569);
      graphics.fill(x + 20 - 2, y + 20 - 2, x + 20 - 1, y + 20 - 1, -11579569);
   }

   private VillagerCombatMode getCombatMode() {
      return this.villagerPreview instanceof CommandableVillager commandableVillager ? commandableVillager.getCombatMode() : VillagerCombatMode.DEFENSIVE;
   }

   private VillagerFollowMode getFollowMode() {
      return this.villagerPreview instanceof CommandableVillager commandableVillager ? commandableVillager.getFollowMode() : VillagerFollowMode.CLOSE;
   }

   private boolean isArmorHidden() {
      return this.villagerPreview instanceof CommandableVillager commandableVillager ? commandableVillager.isArmorHidden() : false;
   }

   private boolean isMuted() {
      return this.villagerPreview instanceof CommandableVillager commandableVillager ? commandableVillager.isMuted() : false;
   }

   private String getHealthText() {
      return this.villagerPreview == null
         ? Component.translatable("slimpatch.screen.villager_equipment.hp", new Object[]{0, 0}).getString()
         : Component.translatable(
               "slimpatch.screen.villager_equipment.hp",
               new Object[]{this.formatHealthValue(this.villagerPreview.getHealth()), this.formatHealthValue(this.villagerPreview.getMaxHealth())}
            )
            .getString();
   }

   private String formatHealthValue(float value) {
      return Math.abs(value - Math.round(value)) < 0.01F ? Integer.toString(Math.round(value)) : String.format(Locale.ROOT, "%.1f", value);
   }

   private void sendCombatMode(VillagerCombatMode combatMode) {
      if (this.villagerPreview != null && this.minecraft != null && this.minecraft.player != null) {
         ModNetworking.sendToServer(new VillagerCombatModePacket(this.villagerPreview.getId(), combatMode));
      }
   }

   private void sendFollowMode(VillagerFollowMode followMode) {
      if (this.villagerPreview != null && this.minecraft != null && this.minecraft.player != null) {
         ModNetworking.sendToServer(new VillagerFollowModePacket(this.villagerPreview.getId(), followMode));
      }
   }

   private void sendArmorVisibility(boolean hidden) {
      if (this.villagerPreview != null && this.minecraft != null && this.minecraft.player != null) {
         ModNetworking.sendToServer(new VillagerArmorVisibilityPacket(this.villagerPreview.getId(), hidden));
      }
   }

   private void sendMute(boolean muted) {
      if (this.villagerPreview != null && this.minecraft != null && this.minecraft.player != null) {
         ModNetworking.sendToServer(new VillagerMutePacket(this.villagerPreview.getId(), muted));
      }
   }

   private class ArmorVisibilityButton extends Button {
      private ArmorVisibilityButton(int x, int y) {
         super(
            x,
            y,
            16,
            16,
            Component.empty(),
            button -> VillagerEquipmentScreen.this.sendArmorVisibility(!VillagerEquipmentScreen.this.isArmorHidden()),
            DEFAULT_NARRATION
         );
      }

      protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
         boolean armorHidden = VillagerEquipmentScreen.this.isArmorHidden();
         ResourceLocation texture = armorHidden ? VillagerEquipmentScreen.ARMOR_HIDDEN_ON_TEXTURE : VillagerEquipmentScreen.ARMOR_HIDDEN_OFF_TEXTURE;
         Component tooltip = Component.translatable(
            armorHidden
               ? "slimpatch.screen.villager_equipment.armor.toggle.tooltip.hidden"
               : "slimpatch.screen.villager_equipment.armor.toggle.tooltip.visible"
         );
         graphics.blit(texture, this.getX(), this.getY(), 0.0F, 0.0F, this.width, this.height, 16, 16);
         if (this.isHovered) {
            graphics.renderTooltip(VillagerEquipmentScreen.this.font, tooltip, mouseX, mouseY);
         }
      }
   }

   private class CombatModeButton extends Button {
      private final VillagerCombatMode combatMode;
      private final ResourceLocation onTexture;
      private final ResourceLocation offTexture;
      private final Component tooltip;

      private CombatModeButton(int x, int y, VillagerCombatMode combatMode, ResourceLocation onTexture, ResourceLocation offTexture, Component tooltip) {
         super(x, y, 16, 16, Component.empty(), button -> VillagerEquipmentScreen.this.sendCombatMode(combatMode), DEFAULT_NARRATION);
         this.combatMode = combatMode;
         this.onTexture = onTexture;
         this.offTexture = offTexture;
         this.tooltip = tooltip;
      }

      protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
         boolean activeMode = VillagerEquipmentScreen.this.getCombatMode() == this.combatMode;
         ResourceLocation texture = activeMode ? this.onTexture : this.offTexture;
         graphics.blit(texture, this.getX(), this.getY(), 0.0F, 0.0F, this.width, this.height, 16, 16);
         if (this.isHovered) {
            graphics.renderTooltip(VillagerEquipmentScreen.this.font, this.tooltip, mouseX, mouseY);
         }
      }
   }

   private class FollowModeButton extends Button {
      private final VillagerFollowMode followMode;
      private final ResourceLocation onTexture;
      private final ResourceLocation offTexture;
      private final Component tooltip;

      private FollowModeButton(int x, int y, VillagerFollowMode followMode, ResourceLocation onTexture, ResourceLocation offTexture, Component tooltip) {
         super(x, y, 16, 16, Component.empty(), button -> VillagerEquipmentScreen.this.sendFollowMode(followMode), DEFAULT_NARRATION);
         this.followMode = followMode;
         this.onTexture = onTexture;
         this.offTexture = offTexture;
         this.tooltip = tooltip;
      }

      protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
         boolean activeMode = VillagerEquipmentScreen.this.getFollowMode() == this.followMode;
         ResourceLocation texture = activeMode ? this.onTexture : this.offTexture;
         graphics.blit(texture, this.getX(), this.getY(), 0.0F, 0.0F, this.width, this.height, 16, 16);
         if (this.isHovered) {
            graphics.renderTooltip(VillagerEquipmentScreen.this.font, this.tooltip, mouseX, mouseY);
         }
      }
   }

   private class MuteButton extends Button {
      private MuteButton(int x, int y) {
         super(x, y, 16, 16, Component.empty(), button -> VillagerEquipmentScreen.this.sendMute(!VillagerEquipmentScreen.this.isMuted()), DEFAULT_NARRATION);
      }

      protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
         boolean muted = VillagerEquipmentScreen.this.isMuted();
         ResourceLocation texture = muted ? VillagerEquipmentScreen.SOUND_OFF_TEXTURE : VillagerEquipmentScreen.SOUND_ON_TEXTURE;
         Component tooltip = Component.translatable(
            muted ? "slimpatch.screen.villager_equipment.mute.tooltip.muted" : "slimpatch.screen.villager_equipment.mute.tooltip.enabled"
         );
         graphics.blit(texture, this.getX(), this.getY(), 0.0F, 0.0F, this.width, this.height, 16, 16);
         if (this.isHovered) {
            graphics.renderTooltip(VillagerEquipmentScreen.this.font, tooltip, mouseX, mouseY);
         }
      }
   }
}
