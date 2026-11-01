package com.javic.slimpatch.client.gui.grave;

import com.javic.slimpatch.client.gui.familytree.FamilyTreePortraitResolver;
import com.javic.slimpatch.familytree.FamilyTreeNodePayload;
import com.javic.slimpatch.familytree.FamilyTreePortraitPayload;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class GraveScreen extends Screen {
   private static final int WINDOW_WIDTH = 176;
   private static final int WINDOW_HEIGHT = 166;
   private static final int PORTRAIT_SIZE = 48;
   private final UUID recordId;
   private final String displayName;
   private final String gender;
   private final String ageStage;
   private final String entityKind;
   private final FamilyTreePortraitPayload portrait;
   private final Map<String, ResourceLocation> portraitCache = new HashMap<>();

   public GraveScreen(UUID recordId, String displayName, String gender, String ageStage, String entityKind, FamilyTreePortraitPayload portrait) {
      super(Component.translatable("gui.slimpatch.grave.title", new Object[]{sanitizeDisplayName(displayName)}));
      this.recordId = recordId;
      this.displayName = sanitizeDisplayName(displayName);
      this.gender = gender == null ? "" : gender;
      this.ageStage = ageStage == null ? "" : ageStage;
      this.entityKind = entityKind == null ? "" : entityKind;
      this.portrait = portrait == null
         ? new FamilyTreePortraitPayload(FamilyTreePortraitPayload.PortraitType.PLAYER_PLACEHOLDER, 0, "", "", true, new byte[0])
         : portrait;
   }

   protected void init() {
      super.init();
      int left = (this.width - 176) / 2;
      int top = (this.height - 166) / 2;
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.slimpatch.grave.close"), button -> this.onClose()).bounds(left + 48, top + 138, 80, 20).build()
      );
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics);
      int left = (this.width - 176) / 2;
      int top = (this.height - 166) / 2;
      graphics.fill(left, top, left + 176, top + 166, -267382768);
      graphics.fill(left + 1, top + 1, left + 176 - 1, top + 166 - 1, -533712848);
      graphics.drawCenteredString(this.font, this.title, left + 88, top + 10, 16777215);
      this.renderPortrait(graphics, left + 64, top + 34);
      graphics.drawCenteredString(this.font, this.getDetailsComponent(), left + 88, top + 92, 14211288);
      Component hint = Component.translatable("gui.slimpatch.grave.hint");
      int hintX = left + 88;
      int hintY = top + 112;
      graphics.drawCenteredString(this.font, hint, hintX, hintY, 13092863);

      super.render(graphics, mouseX, mouseY, partialTick);

      if (mouseX >= hintX - this.font.width(hint) / 2 && mouseX <= hintX + this.font.width(hint) / 2 && mouseY >= hintY && mouseY <= hintY + 9) {
         graphics.renderTooltip(this.font, Component.translatable("gui.slimpatch.grave.hint.tooltip"), mouseX, mouseY);
      }
   }

   private void renderPortrait(GuiGraphics graphics, int x, int y) {
      graphics.fill(x - 2, y - 2, x + 48 + 2, y + 48 + 2, -16250872);
      graphics.fill(x - 1, y - 1, x + 48 + 1, y + 48 + 1, -3750202);
      FamilyTreeNodePayload node = this.createPortraitNode();
      ResourceLocation portraitTexture = FamilyTreePortraitResolver.resolve(Minecraft.getInstance(), node, this.portraitCache);
      if (portraitTexture != null) {
         graphics.blit(portraitTexture, x, y, 48, 48, 8.0F, 8.0F, 8, 8, 64, 64);
         graphics.blit(portraitTexture, x, y, 48, 48, 40.0F, 8.0F, 8, 8, 64, 64);
      } else {
         graphics.fill(x, y, x + 48, y + 48, -10855846);
         String marker = this.displayName.isEmpty() ? "?" : this.displayName.substring(0, 1).toUpperCase(Locale.ROOT);
         graphics.drawCenteredString(this.font, marker, x + 24, y + (48 - 9) / 2, -1);
      }
   }

   private FamilyTreeNodePayload createPortraitNode() {
      return new FamilyTreeNodePayload(
         "grave:" + this.recordId,
         FamilyTreeNodePayload.NodeType.VILLAGER,
         this.recordId,
         this.displayName,
         this.gender,
         this.ageStage,
         false,
         false,
         false,
         this.portrait
      );
   }

   private Component getDetailsComponent() {
      return Component.translatable("gui.slimpatch.grave.details", new Object[]{this.resolveGenderLabel(), this.resolveAgeStageLabel()});
   }

   private String resolveGenderLabel() {
      if ("female".equalsIgnoreCase(this.gender) || "female".equalsIgnoreCase(this.entityKind)) {
         return Component.translatable("slimpatch.dialogue.gender.female").getString();
      } else {
         return !"male".equalsIgnoreCase(this.gender) && !"male".equalsIgnoreCase(this.entityKind)
            ? Component.translatable("gui.slimpatch.grave.unknown").getString()
            : Component.translatable("slimpatch.dialogue.gender.male").getString();
      }
   }

   private String resolveAgeStageLabel() {
      return switch (this.ageStage == null ? "" : this.ageStage) {
         case "TODDLER" -> Component.translatable("slimpatch.life_stage.toddler").getString();
         case "CHILD" -> Component.translatable("slimpatch.life_stage.child").getString();
         case "TEEN" -> Component.translatable("slimpatch.life_stage.teen").getString();
         case "ADULT" -> Component.translatable("slimpatch.life_stage.adult").getString();
         default -> Component.translatable("gui.slimpatch.grave.unknown").getString();
      };
   }

   private static String sanitizeDisplayName(String displayName) {
      return displayName != null && !displayName.isEmpty() ? displayName : Component.translatable("gui.slimpatch.grave.unknown").getString();
   }
}
