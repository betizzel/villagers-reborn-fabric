package com.javic.slimpatch.client.gui.ledger;

import com.javic.slimpatch.client.gui.familytree.FamilyTreePortraitResolver;
import com.javic.slimpatch.ledger.VillagerLedgerEntryPayload;
import com.javic.slimpatch.network.ModNetworking;
import com.javic.slimpatch.network.VillagerLedgerActionPacket;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class VillagerLedgerScreen extends Screen {
   private static final ResourceLocation TELEPORT_ICON = new ResourceLocation("slimpatch", "textures/gui/ledger/teleport_to_player.png");
   private static final ResourceLocation SEND_HOME_ICON = new ResourceLocation("slimpatch", "textures/gui/ledger/send_home.png");
   private static final ResourceLocation HIDE_ON_ICON = new ResourceLocation("slimpatch", "textures/gui/ledger/hide_on.png");
   private static final ResourceLocation HIDE_OFF_ICON = new ResourceLocation("slimpatch", "textures/gui/ledger/hide_off.png");
   private static final ResourceLocation LOCATE_ICON = new ResourceLocation("slimpatch", "textures/gui/ledger/locate.png");
   private static final ResourceLocation DELETE_ICON = new ResourceLocation("slimpatch", "textures/gui/ledger/delete.png");
   private static final int WINDOW_WIDTH = 300;
   private static final int WINDOW_HEIGHT = 196;
   private static final int TAB_Y = 24;
   private static final int TAB_WIDTH = 76;
   private static final int TAB_HEIGHT = 20;
   private static final int LIST_X = 10;
   private static final int LIST_Y = 48;
   private static final int LIST_WIDTH = 280;
   private static final int LIST_HEIGHT = 120;
   private static final int ROW_HEIGHT = 33;
   private static final int PORTRAIT_SIZE = 20;
   private static final int ACTION_BUTTON_SIZE = 18;
   private static final int NAME_X = 28;
   private static final int TAG_Y_OFFSET = 18;
   private static final int HP_COLUMN_X = 74;
   private static final int ALIVE_COLUMN_X = 110;
   private List<VillagerLedgerEntryPayload> companions;
   private List<VillagerLedgerEntryPayload> family;
   private List<VillagerLedgerEntryPayload> hidden;
   private final Map<String, ResourceLocation> portraitCache = new HashMap<>();
   private VillagerLedgerScreen.Tab activeTab = VillagerLedgerScreen.Tab.COMPANIONS;
   private int companionScroll;
   private int familyScroll;
   private int hiddenScroll;
   private Component hpTooltip;
   private UUID selectedVillagerUuid;
   private UUID dragVillagerUuid;
   private double dragStartX;
   private double dragStartY;
   private boolean draggingRow;
   private UUID dragTargetVillagerUuid;
   private boolean dragPlaceAfter;

   public VillagerLedgerScreen(List<VillagerLedgerEntryPayload> companions, List<VillagerLedgerEntryPayload> family, List<VillagerLedgerEntryPayload> hidden) {
      super(Component.translatable("slimpatch.screen.ledger.title"));
      this.companions = companions;
      this.family = family;
      this.hidden = hidden;
   }

   public void handleDataRefresh(List<VillagerLedgerEntryPayload> companions, List<VillagerLedgerEntryPayload> family, List<VillagerLedgerEntryPayload> hidden) {
      this.companions = companions;
      this.family = family;
      this.hidden = hidden;
      this.companionScroll = Math.max(0, Math.min(this.companionScroll, this.getMaxScroll(this.companions)));
      this.familyScroll = Math.max(0, Math.min(this.familyScroll, this.getMaxScroll(this.family)));
      this.hiddenScroll = Math.max(0, Math.min(this.hiddenScroll, this.getMaxScroll(this.hidden)));
      if (this.selectedVillagerUuid != null
         && !this.containsEntry(this.companions, this.selectedVillagerUuid)
         && !this.containsEntry(this.family, this.selectedVillagerUuid)
         && !this.containsEntry(this.hidden, this.selectedVillagerUuid)) {
         this.selectedVillagerUuid = null;
      }

      this.resetDragState();
   }

   protected void init() {
      super.init();
      int left = this.getLeft();
      int top = this.getTop();
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).bounds(left + 110, top + 196 - 24, 80, 20).build());
   }

   public void renderBackground(GuiGraphics graphics) {
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         if (this.isOverTab(mouseX, mouseY, VillagerLedgerScreen.Tab.COMPANIONS)) {
            this.activeTab = VillagerLedgerScreen.Tab.COMPANIONS;
            this.clearSelection();
            return true;
         }

         if (this.isOverTab(mouseX, mouseY, VillagerLedgerScreen.Tab.FAMILY)) {
            this.activeTab = VillagerLedgerScreen.Tab.FAMILY;
            this.clearSelection();
            return true;
         }

         if (this.isOverTab(mouseX, mouseY, VillagerLedgerScreen.Tab.HIDDEN)) {
            this.activeTab = VillagerLedgerScreen.Tab.HIDDEN;
            this.clearSelection();
            return true;
         }

         if (this.isOverDeleteButton(mouseX, mouseY)) {
            if (this.minecraft != null && this.minecraft.player != null) {
               if (this.selectedVillagerUuid == null) {
                  this.minecraft.player.displayClientMessage(Component.translatable("slimpatch.message.ledger.no_selection"), true);
                  return true;
               }

               VillagerLedgerEntryPayload selectedEntry = this.getSelectedEntry();
               if (selectedEntry == null) {
                  this.minecraft.player.displayClientMessage(Component.translatable("slimpatch.message.ledger.no_selection"), true);
                  return true;
               }

               if (this.activeTab != VillagerLedgerScreen.Tab.FAMILY && !this.containsEntry(this.family, this.selectedVillagerUuid)) {
                  this.minecraft
                     .setScreen(new VillagerLedgerScreen.ConfirmRemoveScreen(this, selectedEntry.getNode().getDisplayName(), this.selectedVillagerUuid));
                  return true;
               }

               this.minecraft.player.displayClientMessage(Component.translatable("slimpatch.message.ledger.family_cannot_remove"), true);
               return true;
            }

            return true;
         }

         VillagerLedgerEntryPayload entry = this.getEntryAt(mouseX, mouseY);
         if (entry != null) {
            if (this.isOverActionButton(mouseX, mouseY, entry, VillagerLedgerScreen.LedgerAction.TELEPORT_TO_PLAYER)) {
               this.sendAction(entry, VillagerLedgerActionPacket.Action.TELEPORT_TO_PLAYER);
               return true;
            }

            if (this.isOverActionButton(mouseX, mouseY, entry, VillagerLedgerScreen.LedgerAction.SEND_HOME)) {
               this.sendAction(entry, VillagerLedgerActionPacket.Action.SEND_HOME);
               return true;
            }

            if (this.shouldRenderActionButton(entry, VillagerLedgerScreen.LedgerAction.HIDE_TOGGLE)
               && this.isOverActionButton(mouseX, mouseY, entry, VillagerLedgerScreen.LedgerAction.HIDE_TOGGLE)) {
               this.sendAction(
                  entry,
                  this.isEntryHidden(entry) ? VillagerLedgerActionPacket.Action.SHOW_IN_COMPANIONS : VillagerLedgerActionPacket.Action.HIDE_FROM_COMPANIONS
               );
               return true;
            }

            if (this.isOverActionButton(mouseX, mouseY, entry, VillagerLedgerScreen.LedgerAction.LOCATE)) {
               this.sendAction(entry, VillagerLedgerActionPacket.Action.LOCATE);
               return true;
            }

            this.selectedVillagerUuid = entry.getNode().getUuid();
            this.dragVillagerUuid = this.selectedVillagerUuid;
            this.dragStartX = mouseX;
            this.dragStartY = mouseY;
            this.draggingRow = false;
            this.dragTargetVillagerUuid = null;
            this.dragPlaceAfter = false;
            return true;
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (button == 0 && this.dragVillagerUuid != null && this.isReorderableTab()) {
         if (!this.draggingRow) {
            double dx = mouseX - this.dragStartX;
            double dy = mouseY - this.dragStartY;
            if (dx * dx + dy * dy >= 16.0) {
               this.draggingRow = true;
            }
         }

         if (this.draggingRow) {
            this.updateDragTarget(mouseX, mouseY);
            return true;
         }
      }

      return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (button == 0 && this.dragVillagerUuid != null) {
         if (this.draggingRow && this.dragTargetVillagerUuid != null && !this.dragVillagerUuid.equals(this.dragTargetVillagerUuid)) {
            ModNetworking.sendToServer(
               new VillagerLedgerActionPacket(
                  this.dragVillagerUuid,
                  VillagerLedgerActionPacket.Action.REORDER,
                  this.dragTargetVillagerUuid,
                  this.getCurrentTabPacketType(),
                  this.dragPlaceAfter
               )
            );
         }

         this.resetDragState();
         return true;
      } else {
         return super.mouseReleased(mouseX, mouseY, button);
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
      if (!this.isOverList(mouseX, mouseY)) {
         return super.mouseScrolled(mouseX, mouseY, scrollDelta);
      }

      int next = Math.max(0, Math.min(this.getMaxScroll(this.getActiveEntries()), this.getActiveScroll() - (int)Math.signum(scrollDelta)));
      this.setActiveScroll(next);
      return true;
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics);
      this.hpTooltip = null;
      int left = this.getLeft();
      int top = this.getTop();
      graphics.fill(left, top, left + 300, top + 196, -14803426);
      graphics.fill(left + 1, top + 1, left + 300 - 1, top + 196 - 1, -3750202);
      graphics.fill(left + 2, top + 2, left + 300 - 2, top + 196 - 2, -9737365);
      graphics.fill(left + 3, top + 3, left + 300 - 3, top + 196 - 3, -4934476);
      graphics.drawCenteredString(this.font, this.title, left + 150, top + 8, 16777215);
      this.renderTab(graphics, this.getTabX(left, VillagerLedgerScreen.Tab.COMPANIONS), top + 24, VillagerLedgerScreen.Tab.COMPANIONS);
      this.renderTab(graphics, this.getTabX(left, VillagerLedgerScreen.Tab.FAMILY), top + 24, VillagerLedgerScreen.Tab.FAMILY);
      this.renderTab(graphics, this.getTabX(left, VillagerLedgerScreen.Tab.HIDDEN), top + 24, VillagerLedgerScreen.Tab.HIDDEN);
      this.renderDeleteButton(graphics, mouseX, mouseY, left + 258, top + 24 + 1);
      this.renderListPanel(graphics, left + 10, top + 48);
      this.renderEntries(graphics, mouseX, mouseY, left + 10, top + 48);
      super.render(graphics, mouseX, mouseY, partialTick);
      if (this.hpTooltip != null) {
         graphics.renderTooltip(this.font, this.hpTooltip, mouseX, mouseY);
      }
   }

   private void renderTab(GuiGraphics graphics, int x, int y, VillagerLedgerScreen.Tab tab) {
      boolean active = this.activeTab == tab;
      graphics.fill(x, y, x + 76, y + 20, active ? -4671304 : -11184811);
      graphics.fill(x + 1, y + 1, x + 76 - 1, y + 20 - 1, active ? -7697782 : -8947849);
      graphics.drawCenteredString(this.font, Component.translatable(tab.translationKey), x + 38, y + 6, active ? 16777215 : 14737632);
   }

   private void renderListPanel(GuiGraphics graphics, int x, int y) {
      graphics.fill(x, y, x + 280, y + 120, -14408668);
      graphics.fill(x + 1, y + 1, x + 280 - 1, y + 120 - 1, -15724528);
   }

   private void renderEntries(GuiGraphics graphics, int mouseX, int mouseY, int x, int y) {
      List<VillagerLedgerEntryPayload> entries = this.getActiveEntries();
      if (entries.isEmpty()) {
         graphics.drawCenteredString(this.font, Component.translatable(this.activeTab.emptyKey), x + 140, y + 60 - 9 / 2, 12632256);
      } else {
         int scroll = this.getActiveScroll();
         int startIndex = scroll;
         int endIndex = Math.min(entries.size(), startIndex + this.getVisibleRows());

         for (int i = startIndex; i < endIndex; i++) {
            int rowY = y + 2 + (i - startIndex) * 33;
            this.renderRow(graphics, entries.get(i), x + 2, rowY, mouseX, mouseY);
         }
      }
   }

   private void renderRow(GuiGraphics graphics, VillagerLedgerEntryPayload entry, int x, int y, int mouseX, int mouseY) {
      boolean hovered = mouseX >= x && mouseX <= x + 280 - 4 && mouseY >= y && mouseY <= y + 33 - 3;
      graphics.fill(x, y, x + 280 - 4, y + 33 - 3, hovered ? -14013910 : -14935012);
      if (this.isSelected(entry)) {
         graphics.fill(x, y, x + 280 - 4, y + 1, -1999054632);
         graphics.fill(x, y + 33 - 4, x + 280 - 4, y + 33 - 3, -1999054632);
         graphics.fill(x, y, x + 1, y + 33 - 3, 1725487320);
         graphics.fill(x + 280 - 5, y, x + 280 - 4, y + 33 - 3, 1725487320);
      }

      this.renderPortrait(graphics, entry, x + 2, y + 5);
      graphics.drawString(this.font, entry.getNode().getDisplayName(), x + 28, y + 5, -989262, false);
      graphics.drawString(this.font, this.getTagComponent(entry), x + 28, y + 18, this.getTagColor(entry), false);
      this.renderActionButton(graphics, entry, x, y, mouseX, mouseY, VillagerLedgerScreen.LedgerAction.TELEPORT_TO_PLAYER);
      this.renderActionButton(graphics, entry, x, y, mouseX, mouseY, VillagerLedgerScreen.LedgerAction.SEND_HOME);
      if (this.shouldRenderActionButton(entry, VillagerLedgerScreen.LedgerAction.HIDE_TOGGLE)) {
         this.renderActionButton(graphics, entry, x, y, mouseX, mouseY, VillagerLedgerScreen.LedgerAction.HIDE_TOGGLE);
      }

      this.renderActionButton(graphics, entry, x, y, mouseX, mouseY, VillagerLedgerScreen.LedgerAction.LOCATE);
      String hpText = null;
      if (entry.hasHealth() && entry.getNode().isAlive()) {
         hpText = String.format(Locale.ROOT, "%.0f/%.0f", entry.getHealth(), entry.getMaxHealth());
      } else if (entry.getNode().isAlive()) {
         hpText = "?/?";
      } else {
         hpText = "0/0";
      }

      int hpX = x + 74;
      int hpWidth = this.font.width(hpText);
      int dataX = Math.max(x + 110, hpX + hpWidth + 8);
      int buttonX = this.getActionButtonX(x, VillagerLedgerScreen.LedgerAction.TELEPORT_TO_PLAYER);
      int maxDataX = buttonX - 8 - this.font.width(this.getHomeComponent(entry));
      if (maxDataX > x + 110) {
         dataX = Math.min(dataX, maxDataX);
      }

      graphics.drawString(this.font, hpText, hpX, y + 11, -986896, false);
      if (entry.hasHealth() && entry.getNode().isAlive() && mouseX >= hpX && mouseX <= hpX + hpWidth && mouseY >= y + 11 && mouseY <= y + 11 + 9) {
         this.hpTooltip = Component.translatable("slimpatch.screen.ledger.hp.tooltip");
      }

      String aliveText = Component.translatable(entry.getNode().isAlive() ? "slimpatch.screen.ledger.alive" : "slimpatch.screen.ledger.deceased").getString();
      graphics.drawString(this.font, aliveText, dataX, y + 5, entry.getNode().isAlive() ? -9910422 : -2794918, false);
      graphics.drawString(this.font, this.getHomeComponent(entry), dataX, y + 18, this.getHomeColor(entry), false);
      if (hovered && !entry.getNode().isAlive()) {
         this.hpTooltip = Component.translatable("slimpatch.screen.family_tree.deceased");
      }

      if (this.draggingRow && this.dragTargetVillagerUuid != null && this.dragTargetVillagerUuid.equals(entry.getNode().getUuid())) {
         int lineY = this.dragPlaceAfter ? y + 33 - 3 : y;
         graphics.fill(x, lineY, x + 280 - 4, lineY + 2, -1713315616);
      }
   }

   private void renderDeleteButton(GuiGraphics graphics, int mouseX, int mouseY, int x, int y) {
      boolean hovered = mouseX >= x && mouseX <= x + 18 && mouseY >= y && mouseY <= y + 18;
      graphics.fill(x, y, x + 18, y + 18, hovered ? -7631989 : -10066330);
      graphics.fill(x + 1, y + 1, x + 18 - 1, y + 18 - 1, -2565928);
      if (Minecraft.getInstance().getResourceManager().getResource(DELETE_ICON).isPresent()) {
         graphics.blit(DELETE_ICON, x + 1, y + 1, 0.0F, 0.0F, 16, 16, 16, 16);
      } else {
         graphics.drawCenteredString(this.font, "X", x + 9, y + 4, -1);
      }

      if (hovered) {
         this.hpTooltip = Component.translatable("slimpatch.screen.ledger.action.remove_from_ledger");
      }
   }

   private void renderActionButton(
      GuiGraphics graphics, VillagerLedgerEntryPayload entry, int rowX, int rowY, int mouseX, int mouseY, VillagerLedgerScreen.LedgerAction action
   ) {
      int x = this.getActionButtonX(rowX, action);
      int y = rowY + 7;
      boolean active = this.isActionEnabled(entry, action);
      boolean hovered = mouseX >= x && mouseX <= x + 18 && mouseY >= y && mouseY <= y + 18;
      graphics.fill(x, y, x + 18, y + 18, active ? (hovered ? -7631989 : -10066330) : -10790053);
      graphics.fill(x + 1, y + 1, x + 18 - 1, y + 18 - 1, active ? -2565928 : -6776680);

      ResourceLocation icon = switch (action) {
         case TELEPORT_TO_PLAYER -> TELEPORT_ICON;
         case SEND_HOME -> SEND_HOME_ICON;
         case HIDE_TOGGLE -> this.isEntryHidden(entry) ? HIDE_ON_ICON : HIDE_OFF_ICON;
         case LOCATE -> LOCATE_ICON;
      };
      if (Minecraft.getInstance().getResourceManager().getResource(icon).isPresent()) {
         graphics.blit(icon, x + 1, y + 1, 0.0F, 0.0F, 16, 16, 16, 16);
      } else {
         graphics.drawCenteredString(
            this.font,
            action == VillagerLedgerScreen.LedgerAction.TELEPORT_TO_PLAYER
               ? "T"
               : (action == VillagerLedgerScreen.LedgerAction.SEND_HOME ? "H" : (action == VillagerLedgerScreen.LedgerAction.LOCATE ? "L" : "-")),
            x + 9,
            y + 4,
            active ? -1 : -8750470
         );
      }

      if (hovered) {
         this.hpTooltip = switch (action) {
            case TELEPORT_TO_PLAYER -> Component.translatable("slimpatch.screen.ledger.action.teleport_to_player");
            case SEND_HOME -> Component.translatable("slimpatch.screen.ledger.action.send_home");
            case HIDE_TOGGLE -> Component.translatable(
               this.isEntryHidden(entry) ? "slimpatch.screen.ledger.action.show_in_companions" : "slimpatch.screen.ledger.action.hide_from_companions"
            );
            case LOCATE -> Component.translatable("slimpatch.screen.ledger.action.locate");
         };
      }
   }

   private void renderPortrait(GuiGraphics graphics, VillagerLedgerEntryPayload entry, int x, int y) {
      graphics.fill(x - 1, y - 1, x + 20 + 1, y + 20 + 1, -16250872);
      graphics.fill(x, y, x + 20, y + 20, -3750202);
      ResourceLocation portraitTexture = FamilyTreePortraitResolver.resolve(Minecraft.getInstance(), entry.getNode(), this.portraitCache);
      if (portraitTexture == null) {
         graphics.fill(x + 1, y + 1, x + 20 - 1, y + 20 - 1, entry.getNode().isAlive() ? -10855846 : -11908534);
         String marker = entry.getNode().getDisplayName().isEmpty() ? "?" : entry.getNode().getDisplayName().substring(0, 1).toUpperCase(Locale.ROOT);
         graphics.drawCenteredString(this.font, marker, x + 10, y + (20 - 9) / 2, -1);
      } else {
         graphics.blit(portraitTexture, x, y, 20, 20, 8.0F, 8.0F, 8, 8, 64, 64);
         graphics.blit(portraitTexture, x, y, 20, 20, 40.0F, 8.0F, 8, 8, 64, 64);
         if (!entry.getNode().isAlive() || entry.getNode().getPortrait().isDeadGraySuggested()) {
            graphics.fill(x, y, x + 20, y + 20, -867546550);
         }
      }
   }

   private Component getTagComponent(VillagerLedgerEntryPayload entry) {
      return switch (entry.getEntryType()) {
         case FOLLOW -> Component.translatable("slimpatch.screen.ledger.status.follow");
         case STAY -> Component.translatable("slimpatch.screen.ledger.status.stay");
         case SPOUSE -> Component.translatable("slimpatch.screen.ledger.family.spouse");
         case CHILD -> Component.translatable("slimpatch.screen.ledger.family.child");
         case GRANDCHILD -> Component.translatable("slimpatch.screen.ledger.family.grandchild");
         case DESCENDANT -> Component.translatable("slimpatch.screen.ledger.family.descendant");
         case FAMILY -> Component.translatable("slimpatch.screen.ledger.family.family");
      };
   }

   private int getTagColor(VillagerLedgerEntryPayload entry) {
      return switch (entry.getEntryType()) {
         case FOLLOW -> -8857218;
         case STAY -> -7416065;
         case SPOUSE -> -928630;
         case CHILD -> -1907998;
         case GRANDCHILD -> -2960686;
         case DESCENDANT, FAMILY -> -3684409;
      };
   }

   private Component getHomeComponent(VillagerLedgerEntryPayload entry) {
      return switch (entry.getHomeState()) {
         case HOME_SET -> Component.translatable("slimpatch.screen.ledger.home.set");
         case NO_HOME -> Component.translatable("slimpatch.screen.ledger.home.none");
         case UNKNOWN -> Component.translatable("slimpatch.screen.ledger.home.unknown");
      };
   }

   private int getHomeColor(VillagerLedgerEntryPayload entry) {
      return switch (entry.getHomeState()) {
         case HOME_SET -> -8403457;
         case NO_HOME -> -6645094;
         case UNKNOWN -> -8618884;
      };
   }

   private VillagerLedgerEntryPayload getEntryAt(double mouseX, double mouseY) {
      if (!this.isOverList(mouseX, mouseY)) {
         return null;
      }

      int listTop = this.getTop() + 48 + 2;
      int relativeY = (int)mouseY - listTop;
      if (relativeY < 0) {
         return null;
      }

      int rowIndex = relativeY / 33;
      int entryIndex = this.getActiveScroll() + rowIndex;
      List<VillagerLedgerEntryPayload> entries = this.getActiveEntries();
      return rowIndex < this.getVisibleRows() && entryIndex >= 0 && entryIndex < entries.size() ? entries.get(entryIndex) : null;
   }

   private boolean isOverActionButton(double mouseX, double mouseY, VillagerLedgerEntryPayload entry, VillagerLedgerScreen.LedgerAction action) {
      int rowIndex = this.getActiveEntries().indexOf(entry) - this.getActiveScroll();
      if (rowIndex >= 0 && rowIndex < this.getVisibleRows()) {
         int rowX = this.getLeft() + 10 + 2;
         int rowY = this.getTop() + 48 + 2 + rowIndex * 33;
         int x = this.getActionButtonX(rowX, action);
         int y = rowY + 7;
         return mouseX >= x && mouseX <= x + 18 && mouseY >= y && mouseY <= y + 18;
      } else {
         return false;
      }
   }

   private int getActionButtonX(int rowX, VillagerLedgerScreen.LedgerAction action) {
      int baseX = rowX + 190;
      if (this.activeTab == VillagerLedgerScreen.Tab.FAMILY) {
         return switch (action) {
            case TELEPORT_TO_PLAYER -> baseX + 18 + 4;
            case SEND_HOME -> baseX + 44;
            case HIDE_TOGGLE -> baseX + 44;
            case LOCATE -> baseX + 66;
         };
      } else {
         return switch (action) {
            case TELEPORT_TO_PLAYER -> baseX;
            case SEND_HOME -> baseX + 18 + 4;
            case HIDE_TOGGLE -> baseX + 44;
            case LOCATE -> baseX + 66;
         };
      }
   }

   private boolean isActionEnabled(VillagerLedgerEntryPayload entry, VillagerLedgerScreen.LedgerAction action) {
      if (action == VillagerLedgerScreen.LedgerAction.HIDE_TOGGLE) {
         return this.canToggleHidden(entry);
      } else if (action == VillagerLedgerScreen.LedgerAction.LOCATE) {
         return entry.getNode().isAlive();
      } else if (!entry.getNode().isAlive()) {
         return false;
      } else {
         return action == VillagerLedgerScreen.LedgerAction.SEND_HOME ? entry.getHomeState() == VillagerLedgerEntryPayload.HomeState.HOME_SET : true;
      }
   }

   private boolean isSelected(VillagerLedgerEntryPayload entry) {
      return this.selectedVillagerUuid != null && this.selectedVillagerUuid.equals(entry.getNode().getUuid());
   }

   private boolean shouldRenderActionButton(VillagerLedgerEntryPayload entry, VillagerLedgerScreen.LedgerAction action) {
      return action != VillagerLedgerScreen.LedgerAction.HIDE_TOGGLE ? true : this.activeTab != VillagerLedgerScreen.Tab.FAMILY && this.canToggleHidden(entry);
   }

   private boolean canToggleHidden(VillagerLedgerEntryPayload entry) {
      UUID villagerUuid = entry.getNode().getUuid();
      return villagerUuid == null ? false : this.containsEntry(this.companions, villagerUuid) || this.containsEntry(this.hidden, villagerUuid);
   }

   private boolean isEntryHidden(VillagerLedgerEntryPayload entry) {
      UUID villagerUuid = entry.getNode().getUuid();
      return villagerUuid != null && this.containsEntry(this.hidden, villagerUuid);
   }

   private boolean containsEntry(List<VillagerLedgerEntryPayload> entries, UUID villagerUuid) {
      for (VillagerLedgerEntryPayload candidate : entries) {
         if (villagerUuid.equals(candidate.getNode().getUuid())) {
            return true;
         }
      }

      return false;
   }

   private void sendAction(VillagerLedgerEntryPayload entry, VillagerLedgerActionPacket.Action action) {
      if (this.minecraft != null && this.minecraft.player != null && entry.getNode().getUuid() != null) {
         ModNetworking.sendToServer(new VillagerLedgerActionPacket(entry.getNode().getUuid(), action));
      }
   }

   private boolean isOverDeleteButton(double mouseX, double mouseY) {
      int x = this.getLeft() + 258;
      int y = this.getTop() + 24 + 1;
      return mouseX >= x && mouseX <= x + 18 && mouseY >= y && mouseY <= y + 18;
   }

   private boolean isOverTab(double mouseX, double mouseY, VillagerLedgerScreen.Tab tab) {
      int left = this.getLeft();
      int top = this.getTop();
      int x = this.getTabX(left, tab);
      int y = top + 24;
      return mouseX >= x && mouseX <= x + 76 && mouseY >= y && mouseY <= y + 20;
   }

   private boolean isOverList(double mouseX, double mouseY) {
      int left = this.getLeft() + 10;
      int top = this.getTop() + 48;
      return mouseX >= left && mouseX <= left + 280 && mouseY >= top && mouseY <= top + 120;
   }

   private boolean isReorderableTab() {
      return this.activeTab == VillagerLedgerScreen.Tab.COMPANIONS
         || this.activeTab == VillagerLedgerScreen.Tab.HIDDEN
         || this.activeTab == VillagerLedgerScreen.Tab.FAMILY;
   }

   private void updateDragTarget(double mouseX, double mouseY) {
      this.dragTargetVillagerUuid = null;
      this.dragPlaceAfter = false;
      if (this.isOverList(mouseX, mouseY)) {
         VillagerLedgerEntryPayload entry = this.getEntryAt(mouseX, mouseY);
         if (entry != null) {
            this.dragTargetVillagerUuid = entry.getNode().getUuid();
            int rowIndex = this.getActiveEntries().indexOf(entry) - this.getActiveScroll();
            int rowTop = this.getTop() + 48 + 2 + rowIndex * 33;
            this.dragPlaceAfter = mouseY >= rowTop + 16.5;
         }
      }
   }

   private VillagerLedgerActionPacket.LedgerTab getCurrentTabPacketType() {
      return switch (this.activeTab) {
         case COMPANIONS -> VillagerLedgerActionPacket.LedgerTab.COMPANIONS;
         case FAMILY -> VillagerLedgerActionPacket.LedgerTab.FAMILY;
         case HIDDEN -> VillagerLedgerActionPacket.LedgerTab.HIDDEN;
      };
   }

   private void clearSelection() {
      this.selectedVillagerUuid = null;
      this.resetDragState();
   }

   private VillagerLedgerEntryPayload getSelectedEntry() {
      return this.findEntry(
         this.companions,
         this.selectedVillagerUuid,
         this.findEntry(this.hidden, this.selectedVillagerUuid, this.findEntry(this.family, this.selectedVillagerUuid, null))
      );
   }

   private VillagerLedgerEntryPayload findEntry(List<VillagerLedgerEntryPayload> entries, UUID villagerUuid, VillagerLedgerEntryPayload fallback) {
      if (villagerUuid == null) {
         return fallback;
      }

      for (VillagerLedgerEntryPayload entry : entries) {
         if (villagerUuid.equals(entry.getNode().getUuid())) {
            return entry;
         }
      }

      return fallback;
   }

   private void confirmRemove(UUID villagerUuid) {
      if (villagerUuid != null) {
         ModNetworking.sendToServer(new VillagerLedgerActionPacket(villagerUuid, VillagerLedgerActionPacket.Action.REMOVE_FROM_LEDGER));
      }
   }

   private void resetDragState() {
      this.dragVillagerUuid = null;
      this.draggingRow = false;
      this.dragTargetVillagerUuid = null;
      this.dragPlaceAfter = false;
   }

   private int getVisibleRows() {
      return Math.max(1, 3);
   }

   private int getMaxScroll(List<VillagerLedgerEntryPayload> entries) {
      return Math.max(0, entries.size() - this.getVisibleRows());
   }

   private int getActiveScroll() {
      return switch (this.activeTab) {
         case COMPANIONS -> this.companionScroll;
         case FAMILY -> this.familyScroll;
         case HIDDEN -> this.hiddenScroll;
      };
   }

   private void setActiveScroll(int value) {
      switch (this.activeTab) {
         case COMPANIONS:
            this.companionScroll = value;
            break;
         case FAMILY:
            this.familyScroll = value;
            break;
         case HIDDEN:
            this.hiddenScroll = value;
      }
   }

   private List<VillagerLedgerEntryPayload> getActiveEntries() {
      return switch (this.activeTab) {
         case COMPANIONS -> this.companions;
         case FAMILY -> this.family;
         case HIDDEN -> this.hidden;
      };
   }

   private int getLeft() {
      return (this.width - 300) / 2;
   }

   private int getTop() {
      return (this.height - 196) / 2;
   }

   private int getTabX(int left, VillagerLedgerScreen.Tab tab) {
      return left + 10 + tab.ordinal() * 82;
   }

   private static class ConfirmRemoveScreen extends Screen {
      private final VillagerLedgerScreen parent;
      private final Component message;
      private final UUID villagerUuid;

      protected ConfirmRemoveScreen(VillagerLedgerScreen parent, String villagerName, UUID villagerUuid) {
         super(Component.empty());
         this.parent = parent;
         this.message = Component.translatable("slimpatch.screen.ledger.remove_confirm", new Object[]{villagerName});
         this.villagerUuid = villagerUuid;
      }

      protected void init() {
         int centerX = this.width / 2;
         int centerY = this.height / 2;
         this.addRenderableWidget(Button.builder(Component.translatable("slimpatch.screen.confirm.yes"), button -> {
            if (this.minecraft != null) {
               this.minecraft.setScreen(this.parent);
               this.parent.confirmRemove(this.villagerUuid);
            }
         }).bounds(centerX - 90, centerY + 10, 80, 20).build());
         this.addRenderableWidget(Button.builder(Component.translatable("slimpatch.screen.confirm.no"), button -> {
            if (this.minecraft != null) {
               this.minecraft.setScreen(this.parent);
            }
         }).bounds(centerX + 10, centerY + 10, 80, 20).build());
      }

      public void renderBackground(GuiGraphics graphics) {
         graphics.fill(0, 0, this.width, this.height, 1879048192);
      }

      public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
         this.renderBackground(graphics);
         int left = this.width / 2 - 110;
         int top = this.height / 2 - 35;
         graphics.fill(left, top, left + 220, top + 70, -14803426);
         graphics.fill(left + 1, top + 1, left + 219, top + 69, -3750202);
         graphics.fill(left + 2, top + 2, left + 218, top + 68, -9737365);
         graphics.fill(left + 3, top + 3, left + 217, top + 67, -4934476);
         graphics.drawCenteredString(this.font, this.message, this.width / 2, top + 16, 16777215);
         super.render(graphics, mouseX, mouseY, partialTick);
      }

      public boolean isPauseScreen() {
         return false;
      }
   }

   private enum LedgerAction {
      TELEPORT_TO_PLAYER,
      SEND_HOME,
      HIDE_TOGGLE,
      LOCATE;
   }

   private enum Tab {
      COMPANIONS("slimpatch.screen.ledger.tab.companions", "slimpatch.screen.ledger.empty.companions"),
      FAMILY("slimpatch.screen.ledger.tab.family", "slimpatch.screen.ledger.empty.family"),
      HIDDEN("slimpatch.screen.ledger.tab.hidden", "slimpatch.screen.ledger.empty.hidden");

      private final String translationKey;
      private final String emptyKey;

      Tab(String translationKey, String emptyKey) {
         this.translationKey = translationKey;
         this.emptyKey = emptyKey;
      }
   }
}
