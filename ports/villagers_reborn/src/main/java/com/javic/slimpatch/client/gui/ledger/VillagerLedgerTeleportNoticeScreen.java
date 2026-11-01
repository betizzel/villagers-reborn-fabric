package com.javic.slimpatch.client.gui.ledger;

import com.javic.slimpatch.network.OpenVillagerLedgerTeleportNoticePacket;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public class VillagerLedgerTeleportNoticeScreen extends Screen {
   private static final int MAX_PANEL_WIDTH = 420;
   private static final int PANEL_MARGIN = 40;
   private static final int MESSAGE_HORIZONTAL_PADDING = 40;
   private static final int MIN_MESSAGE_WIDTH = 80;
   private static final int LINE_HEIGHT = 12;
   private static final int CHECKBOX_SIZE = 10;
   private static final int TITLE_COLOR = -9882;
   private static boolean hideForCurrentSession;
   private static boolean teleportToPlayerNoticeShownForCurrentSession;
   private static boolean sendHomeNoticeShownForCurrentSession;
   private static Object currentLevel;
   private final Screen parent;
   private final Component message;
   private final Component dontShowAgainText;
   private boolean dontShowAgain;

   public VillagerLedgerTeleportNoticeScreen(Screen parent) {
      super(Component.translatable("slimpatch.screen.villager_ledger.teleport_notice.title"));
      this.parent = parent;
      this.message = Component.translatable("slimpatch.screen.villager_ledger.teleport_notice.message");
      this.dontShowAgainText = Component.translatable("slimpatch.screen.villager_ledger.teleport_notice.dont_show_again");
   }

   public static void refreshSession(Object level) {
      if (currentLevel != level) {
         currentLevel = level;
         hideForCurrentSession = false;
         teleportToPlayerNoticeShownForCurrentSession = false;
         sendHomeNoticeShownForCurrentSession = false;
      }
   }

   public static boolean shouldShow(OpenVillagerLedgerTeleportNoticePacket.NoticeAction action, boolean risky) {
      if (hideForCurrentSession) {
         return false;
      }

      if (action == OpenVillagerLedgerTeleportNoticePacket.NoticeAction.TELEPORT_TO_PLAYER) {
         if (!teleportToPlayerNoticeShownForCurrentSession) {
            teleportToPlayerNoticeShownForCurrentSession = true;
            return true;
         } else {
            return risky;
         }
      } else if (!sendHomeNoticeShownForCurrentSession) {
         sendHomeNoticeShownForCurrentSession = true;
         return true;
      } else {
         return risky;
      }
   }

   protected void init() {
      int centerX = this.width / 2;
      int panelHeight = this.getPanelHeight(this.getMessageLines().size());
      int top = this.height / 2 - panelHeight / 2;
      this.addRenderableWidget(
         Button.builder(Component.translatable("slimpatch.screen.villager_ledger.teleport_notice.close"), button -> this.onClose())
            .bounds(centerX - 40, top + panelHeight - 32, 80, 20)
            .build()
      );
   }

   public void onClose() {
      if (this.dontShowAgain) {
         hideForCurrentSession = true;
      }

      if (this.minecraft != null) {
         this.minecraft.setScreen(this.parent);
      }
   }

   public void renderBackground(GuiGraphics graphics) {
      graphics.fill(0, 0, this.width, this.height, 1879048192);
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics);
      List<FormattedCharSequence> lines = this.getMessageLines();
      int panelWidth = this.getPanelWidth();
      int panelHeight = this.getPanelHeight(lines.size());
      int left = this.width / 2 - panelWidth / 2;
      int top = this.height / 2 - panelHeight / 2;
      graphics.fill(left, top, left + panelWidth, top + panelHeight, -14803426);
      graphics.fill(left + 1, top + 1, left + panelWidth - 1, top + panelHeight - 1, -3750202);
      graphics.fill(left + 2, top + 2, left + panelWidth - 2, top + panelHeight - 2, -9737365);
      graphics.fill(left + 3, top + 3, left + panelWidth - 3, top + panelHeight - 3, -4934476);
      graphics.drawCenteredString(this.font, this.title, this.width / 2, top + 14, -9882);
      int messageY = top + 34;

      for (FormattedCharSequence line : lines) {
         graphics.drawCenteredString(this.font, line, this.width / 2, messageY, 16777215);
         messageY += 12;
      }

      this.renderCheckbox(graphics, mouseX, mouseY, top, panelHeight);
      super.render(graphics, mouseX, mouseY, partialTick);
   }

   private void renderCheckbox(GuiGraphics graphics, int mouseX, int mouseY, int top, int panelHeight) {
      int checkboxX = this.getCheckboxX();
      int checkboxY = this.getCheckboxY(top, panelHeight);
      boolean hovered = this.isCheckboxHovered(mouseX, mouseY, checkboxX, checkboxY);
      graphics.fill(checkboxX, checkboxY, checkboxX + 10, checkboxY + 10, -14803426);
      graphics.fill(checkboxX + 1, checkboxY + 1, checkboxX + 10 - 1, checkboxY + 10 - 1, hovered ? -2039584 : -3750202);
      graphics.fill(checkboxX + 2, checkboxY + 2, checkboxX + 10 - 2, checkboxY + 10 - 2, -9737365);
      if (this.dontShowAgain) {
         graphics.fill(checkboxX + 3, checkboxY + 3, checkboxX + 10 - 3, checkboxY + 10 - 3, -9882);
      }

      graphics.drawString(this.font, this.dontShowAgainText, checkboxX + 10 + 6, checkboxY + 1, 16777215, false);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      List<FormattedCharSequence> lines = this.getMessageLines();
      int panelHeight = this.getPanelHeight(lines.size());
      int top = this.height / 2 - panelHeight / 2;
      int checkboxX = this.getCheckboxX();
      int checkboxY = this.getCheckboxY(top, panelHeight);
      if (this.isCheckboxHovered(mouseX, mouseY, checkboxX, checkboxY)) {
         this.dontShowAgain = !this.dontShowAgain;
         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   private boolean isCheckboxHovered(double mouseX, double mouseY, int checkboxX, int checkboxY) {
      int width = 16 + this.font.width(this.dontShowAgainText);
      return mouseX >= checkboxX && mouseX <= checkboxX + width && mouseY >= checkboxY && mouseY <= checkboxY + 10;
   }

   private int getCheckboxX() {
      int width = 16 + this.font.width(this.dontShowAgainText);
      return this.width / 2 - width / 2;
   }

   private int getCheckboxY(int top, int panelHeight) {
      return top + panelHeight - 55;
   }

   private List<FormattedCharSequence> getMessageLines() {
      return this.font.split(this.message, this.getMessageWidth());
   }

   private int getPanelWidth() {
      return Math.min(420, Math.max(120, this.width - 40));
   }

   private int getMessageWidth() {
      return Math.max(80, this.getPanelWidth() - 40);
   }

   private int getPanelHeight(int lineCount) {
      return Math.max(132, 96 + lineCount * 12);
   }

   public boolean isPauseScreen() {
      return false;
   }
}
