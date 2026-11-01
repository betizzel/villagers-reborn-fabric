package com.javic.slimpatch.client.gui;

import com.javic.slimpatch.Config;
import com.javic.slimpatch.config.NewWorldSkinTypeMode;
import com.javic.slimpatch.config.SlimPatchConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class SlimPatchConfigScreen extends Screen {
   private static final int ROW_HEIGHT = 24;
   private static final int TOP_MARGIN = 48;
   private static final int BOTTOM_MARGIN = 36;
   private static final int SCROLL_STEP = 24;
   private final Screen parent;
   private final SlimPatchConfigScreen.ViewMode viewMode;
   private final SlimPatchConfigScreen.Category category;
   private final List<SlimPatchConfigScreen.OptionRow> rows = new ArrayList<>();
   private int scrollOffset;
   private int contentHeight;

   public SlimPatchConfigScreen(Screen parent) {
      this(parent, SlimPatchConfigScreen.ViewMode.ROOT, null);
   }

   private SlimPatchConfigScreen(Screen parent, SlimPatchConfigScreen.ViewMode viewMode, SlimPatchConfigScreen.Category category) {
      super(resolveTitle(viewMode, category));
      this.parent = parent;
      this.viewMode = viewMode;
      this.category = category;
   }

   private static Component resolveTitle(SlimPatchConfigScreen.ViewMode viewMode, SlimPatchConfigScreen.Category category) {
      if (viewMode == SlimPatchConfigScreen.ViewMode.ROOT) {
         return Component.translatable("slimpatch.configuration.title");
      } else if (viewMode == SlimPatchConfigScreen.ViewMode.MOD_SETTINGS) {
         return Component.translatable("slimpatch.configuration.mod_settings_title");
      } else {
         return category == SlimPatchConfigScreen.Category.COMMON
            ? Component.translatable("slimpatch.configuration.common_settings_title")
            : Component.literal(
               Component.translatable("slimpatch.configuration.mod_settings_title").getString() + " > " + Component.translatable(category.titleKey).getString()
            );
      }
   }

   protected void init() {
      super.init();
      this.rows.clear();
      this.scrollOffset = 0;
      if (this.viewMode == SlimPatchConfigScreen.ViewMode.ROOT) {
         this.initRoot();
      } else if (this.viewMode == SlimPatchConfigScreen.ViewMode.MOD_SETTINGS) {
         this.initModSettings();
      } else {
         this.initCategory();
      }
   }

   private void initRoot() {
      int centerX = this.width / 2 - 100;
      int y = this.height / 2 - 10;
      this.addRenderableWidget(
         Button.builder(
               Component.translatable("slimpatch.configuration.common_settings"),
               button -> this.minecraft
                  .setScreen(new SlimPatchConfigScreen(this, SlimPatchConfigScreen.ViewMode.CATEGORY, SlimPatchConfigScreen.Category.COMMON))
            )
            .bounds(centerX, y, 200, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(
               Component.translatable("slimpatch.configuration.mod_settings"),
               button -> this.minecraft.setScreen(new SlimPatchConfigScreen(this, SlimPatchConfigScreen.ViewMode.MOD_SETTINGS, null))
            )
            .bounds(centerX, y + 28, 200, 20)
            .build()
      );
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).bounds(centerX, this.height - 26, 200, 20).build());
   }

   private void initModSettings() {
      int labelX = this.width / 2 - 100;
      int buttonX = this.width / 2 + 56;
      int startY = this.height / 2 - 28;
      this.addCategoryEntry(SlimPatchConfigScreen.Category.APPEARANCE, labelX, buttonX, startY);
      this.addCategoryEntry(SlimPatchConfigScreen.Category.GAMEPLAY, labelX, buttonX, startY + 28);
      this.addCategoryEntry(SlimPatchConfigScreen.Category.FAMILY, labelX, buttonX, startY + 56);
      this.updateRowPositions();
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).bounds(this.width / 2 - 100, this.height - 26, 200, 20).build()
      );
   }

   private void addCategoryEntry(SlimPatchConfigScreen.Category entryCategory, int labelX, int buttonX, int y) {
      this.addRenderableWidget(
         Button.builder(
               Component.translatable("slimpatch.configuration.edit"),
               button -> this.minecraft.setScreen(new SlimPatchConfigScreen(this, SlimPatchConfigScreen.ViewMode.CATEGORY, entryCategory))
            )
            .bounds(buttonX, y, 90, 20)
            .build()
      );
      this.rows
         .add(new SlimPatchConfigScreen.CategoryLabelRow(Component.literal(Component.translatable(entryCategory.labelKey).getString() + "..."), y, labelX));
   }

   private void initCategory() {
      if (this.category != null) {
         switch (this.category) {
            case COMMON:
               this.buildCommonRows();
               break;
            case APPEARANCE:
               this.buildAppearanceRows();
               break;
            case GAMEPLAY:
               this.buildGameplayRows();
               break;
            case FAMILY:
               this.buildFamilyRows();
         }

         this.contentHeight = this.rows.isEmpty() ? 0 : this.rows.get(this.rows.size() - 1).bottom();
         this.addRowWidgets();
         this.updateRowPositions();
         this.addRenderableWidget(
            Button.builder(Component.translatable("slimpatch.configuration.undo"), button -> this.undoCategory())
               .bounds(this.width / 2 - 152, this.height - 26, 98, 20)
               .build()
         );
         this.addRenderableWidget(
            Button.builder(Component.translatable("slimpatch.configuration.reset"), button -> this.resetCategory())
               .bounds(this.width / 2 - 50, this.height - 26, 98, 20)
               .build()
         );
         this.addRenderableWidget(
            Button.builder(CommonComponents.GUI_DONE, button -> this.saveAndClose()).bounds(this.width / 2 + 52, this.height - 26, 98, 20).build()
         );
      }
   }

   private void buildCommonRows() {
      this.addBoolean(
         "slimpatch.configuration.allowMultiplayerCustomSkins",
         (Boolean)SlimPatchConfig.SERVER.allowMultiplayerCustomSkins.get(),
         SlimPatchConfig.SERVER.allowMultiplayerCustomSkins::set,
         true
      );
      this.addInt(
         "slimpatch.configuration.maxCustomSkinSizeKb",
         (Integer)SlimPatchConfig.SERVER.maxCustomSkinSizeKb.get(),
         SlimPatchConfig.SERVER.maxCustomSkinSizeKb::set,
         256,
         16,
         2048
      );
   }

   private void buildAppearanceRows() {
      this.addEnum(
         "slimpatch.config.newWorldSkinType",
         (NewWorldSkinTypeMode)Config.NEW_WORLD_SKIN_TYPE.get(),
         Config.NEW_WORLD_SKIN_TYPE::set,
         NewWorldSkinTypeMode.SELECTION_SCREEN,
         NewWorldSkinTypeMode.values(),
         mode -> Component.translatable(mode.getTranslationKey())
      );
      this.addBoolean("slimpatch.config.customVillagerSkins", (Boolean)Config.CUSTOM_VILLAGER_SKINS.get(), Config.CUSTOM_VILLAGER_SKINS::set, true);
      this.addBoolean("slimpatch.config.customIllagerSkins", (Boolean)Config.CUSTOM_ILLAGER_SKINS.get(), Config.CUSTOM_ILLAGER_SKINS::set, true);
      this.addBoolean("slimpatch.config.customWitchSkin", (Boolean)Config.CUSTOM_WITCH_SKIN.get(), Config.CUSTOM_WITCH_SKIN::set, true);
      this.addBoolean("slimpatch.config.customPillagerModel", (Boolean)Config.CUSTOM_PILLAGER_MODEL.get(), Config.CUSTOM_PILLAGER_MODEL::set, true);
      this.addBoolean("slimpatch.config.customVindicatorModel", (Boolean)Config.CUSTOM_VINDICATOR_MODEL.get(), Config.CUSTOM_VINDICATOR_MODEL::set, true);
      this.addBoolean("slimpatch.config.customEvokerModel", (Boolean)Config.CUSTOM_EVOKER_MODEL.get(), Config.CUSTOM_EVOKER_MODEL::set, true);
      this.addBoolean("slimpatch.config.customVillagerSounds", (Boolean)Config.CUSTOM_VILLAGER_SOUNDS.get(), Config.CUSTOM_VILLAGER_SOUNDS::set, true);
      this.addBoolean("slimpatch.config.customIllagerSounds", (Boolean)Config.CUSTOM_ILLAGER_SOUNDS.get(), Config.CUSTOM_ILLAGER_SOUNDS::set, true);
      this.addBoolean("slimpatch.config.villagerNameTag", (Boolean)Config.VILLAGER_NAME_TAG.get(), Config.VILLAGER_NAME_TAG::set, true);
      this.addBoolean("slimpatch.config.pressRToTalkIndicator", (Boolean)Config.PRESS_R_TO_TALK_INDICATOR.get(), Config.PRESS_R_TO_TALK_INDICATOR::set, true);
   }

   private void buildGameplayRows() {
      this.addDouble(
         "slimpatch.config.dialogueCameraSensitivity", (Double)Config.DIALOGUE_CAMERA_SENSITIVITY.get(), Config.DIALOGUE_CAMERA_SENSITIVITY::set, 1.0, 0.5, 1.5
      );
      this.addBoolean("slimpatch.config.invertDialogueCameraX", (Boolean)Config.INVERT_DIALOGUE_CAMERA_X.get(), Config.INVERT_DIALOGUE_CAMERA_X::set, false);
      this.addBoolean("slimpatch.config.invertDialogueCameraY", (Boolean)Config.INVERT_DIALOGUE_CAMERA_Y.get(), Config.INVERT_DIALOGUE_CAMERA_Y::set, false);
      this.addBoolean("slimpatch.config.dialogueScreenBlur", (Boolean)Config.DIALOGUE_SCREEN_BLUR.get(), Config.DIALOGUE_SCREEN_BLUR::set, false);
      this.addBoolean(
         "slimpatch.config.forceFirstPersonInDialogue", (Boolean)Config.FORCE_FIRST_PERSON_IN_DIALOGUE.get(), Config.FORCE_FIRST_PERSON_IN_DIALOGUE::set, false
      );
      this.addDouble(
         "slimpatch.config.healthPerGoldenCarrot", (Double)Config.HEALTH_PER_GOLDEN_CARROT.get(), Config.HEALTH_PER_GOLDEN_CARROT::set, 5.0, 0.1, 100.0
      );
      this.addDouble("slimpatch.config.maxBonusHealth", (Double)Config.MAX_BONUS_HEALTH.get(), Config.MAX_BONUS_HEALTH::set, 100.0, 0.0, 1000.0);
      this.addBoolean("slimpatch.config.replaceVanillaEntities", (Boolean)Config.REPLACE_VANILLA_ENTITIES.get(), Config.REPLACE_VANILLA_ENTITIES::set, true);
   }

   private void buildFamilyRows() {
      this.addBoolean(
         "slimpatch.config.genderedRelationshipLabels", (Boolean)Config.GENDERED_RELATIONSHIP_LABELS.get(), Config.GENDERED_RELATIONSHIP_LABELS::set, true
      );
      this.addBoolean(
         "slimpatch.config.allowMultiplePlayerSpouses", (Boolean)Config.ALLOW_MULTIPLE_PLAYER_SPOUSES.get(), Config.ALLOW_MULTIPLE_PLAYER_SPOUSES::set, true
      );
      this.addBoolean("slimpatch.config.spouseCookingEnabled", (Boolean)Config.SPOUSE_COOKING_ENABLED.get(), Config.SPOUSE_COOKING_ENABLED::set, true);
      this.addInt(
         "slimpatch.config.spouseCookingCooldownTicks",
         (Integer)Config.SPOUSE_COOKING_COOLDOWN_TICKS.get(),
         Config.SPOUSE_COOKING_COOLDOWN_TICKS::set,
         24000,
         1200,
         240000
      );
      this.addBoolean("slimpatch.config.enableWeddingCutscene", (Boolean)Config.ENABLE_WEDDING_CUTSCENE.get(), Config.ENABLE_WEDDING_CUTSCENE::set, true);
      this.addBoolean("slimpatch.config.enableFamilyCutscene", (Boolean)Config.ENABLE_FAMILY_CUTSCENE.get(), Config.ENABLE_FAMILY_CUTSCENE::set, true);
      this.addInt(
         "slimpatch.config.pregnancyDurationTicks", (Integer)Config.PREGNANCY_DURATION_TICKS.get(), Config.PREGNANCY_DURATION_TICKS::set, 144000, 20, 24000000
      );
      this.addBoolean("slimpatch.config.enablePregnancyBelly", (Boolean)Config.ENABLE_PREGNANCY_BELLY.get(), Config.ENABLE_PREGNANCY_BELLY::set, true);
      this.addBoolean("slimpatch.config.enableHavingChildren", (Boolean)Config.ENABLE_HAVING_CHILDREN.get(), Config.ENABLE_HAVING_CHILDREN::set, true);
      this.addInt(
         "slimpatch.config.familyCharmMaxChildrenPerPair",
         (Integer)Config.FAMILY_CHARM_MAX_CHILDREN_PER_PAIR.get(),
         Config.FAMILY_CHARM_MAX_CHILDREN_PER_PAIR::set,
         3,
         1,
         20
      );
      this.addBoolean(
         "slimpatch.config.enableNaturalFamilySpawns", (Boolean)Config.ENABLE_NATURAL_FAMILY_SPAWNS.get(), Config.ENABLE_NATURAL_FAMILY_SPAWNS::set, true
      );
      this.addBoolean("slimpatch.config.enableAging", (Boolean)Config.ENABLE_AGING.get(), Config.ENABLE_AGING::set, true);
      this.addInt(
         "slimpatch.config.toddlerDurationTicks", (Integer)Config.TODDLER_DURATION_TICKS.get(), Config.TODDLER_DURATION_TICKS::set, 144000, 20, 24000000
      );
      this.addInt("slimpatch.config.childDurationTicks", (Integer)Config.CHILD_DURATION_TICKS.get(), Config.CHILD_DURATION_TICKS::set, 336000, 20, 24000000);
      this.addInt("slimpatch.config.teenDurationTicks", (Integer)Config.TEEN_DURATION_TICKS.get(), Config.TEEN_DURATION_TICKS::set, 336000, 20, 24000000);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
      if (this.viewMode != SlimPatchConfigScreen.ViewMode.CATEGORY) {
         return super.mouseScrolled(mouseX, mouseY, scrollY);
      } else {
         int nextOffset = Math.max(0, Math.min(this.getMaxScroll(), this.scrollOffset - (int)Math.signum(scrollY) * 24));
         if (nextOffset != this.scrollOffset) {
            this.scrollOffset = nextOffset;
            this.updateRowPositions();
            return true;
         } else {
            return super.mouseScrolled(mouseX, mouseY, scrollY);
         }
      }
   }

   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(guiGraphics);
      int left = 12;
      int right = this.width - 12;
      guiGraphics.fill(left, 34, right, 35, -2130706433);
      guiGraphics.fill(left, this.height - 34, right, this.height - 33, -2141891243);
      guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 14, 16777215);
      if (this.viewMode == SlimPatchConfigScreen.ViewMode.ROOT) {
         guiGraphics.drawString(
            this.font, Component.translatable("slimpatch.configuration.common_options"), this.width / 2 - 100, this.height / 2 - 32, 16765823, false
         );
      }

      super.render(guiGraphics, mouseX, mouseY, partialTick);

      for (SlimPatchConfigScreen.OptionRow row : this.rows) {
         row.renderLabel(guiGraphics, mouseX, mouseY);
         row.renderTooltip(guiGraphics, mouseX, mouseY);
      }
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void onClose() {
      if (this.minecraft != null) {
         this.minecraft.setScreen(this.parent);
      }
   }

   private void saveAndClose() {
      for (SlimPatchConfigScreen.OptionRow row : this.rows) {
         row.save();
      }

      Config.SPEC.save();
      SlimPatchConfig.SERVER_SPEC.save();
      this.onClose();
   }

   private void undoCategory() {
      for (SlimPatchConfigScreen.OptionRow row : this.rows) {
         row.undo();
      }
   }

   private void resetCategory() {
      for (SlimPatchConfigScreen.OptionRow row : this.rows) {
         row.reset();
      }
   }

   private void addBoolean(String translationKey, boolean currentValue, Consumer<Boolean> setter, boolean defaultValue) {
      int top = this.nextRowTop();
      SlimPatchConfigScreen.BooleanRow row = new SlimPatchConfigScreen.BooleanRow(
         Component.translatable(translationKey), this.tooltipFor(translationKey), top, currentValue, setter, defaultValue
      );
      this.rows.add(row);
   }

   private void addInt(String translationKey, int currentValue, Consumer<Integer> setter, int defaultValue, int min, int max) {
      int top = this.nextRowTop();
      SlimPatchConfigScreen.IntRow row = new SlimPatchConfigScreen.IntRow(
         Component.translatable(translationKey), this.tooltipFor(translationKey), top, Integer.toString(currentValue), setter, defaultValue, min, max
      );
      this.rows.add(row);
   }

   private void addDouble(String translationKey, double currentValue, Consumer<Double> setter, double defaultValue, double min, double max) {
      int top = this.nextRowTop();
      SlimPatchConfigScreen.DoubleRow row = new SlimPatchConfigScreen.DoubleRow(
         Component.translatable(translationKey),
         this.tooltipFor(translationKey),
         top,
         String.format(Locale.ROOT, "%s", currentValue),
         setter,
         defaultValue,
         min,
         max
      );
      this.rows.add(row);
   }

   private void addString(String translationKey, String currentValue, Consumer<String> setter, String defaultValue) {
      int top = this.nextRowTop();
      SlimPatchConfigScreen.StringRow row = new SlimPatchConfigScreen.StringRow(
         Component.translatable(translationKey), this.tooltipFor(translationKey), top, currentValue, setter, defaultValue
      );
      this.rows.add(row);
   }

   private <T extends Enum<T>> void addEnum(
      String translationKey, T currentValue, Consumer<T> setter, T defaultValue, T[] values, Function<T, Component> labelFunction
   ) {
      int top = this.nextRowTop();
      SlimPatchConfigScreen.EnumRow<T> row = new SlimPatchConfigScreen.EnumRow<>(
         Component.translatable(translationKey), this.tooltipFor(translationKey), top, currentValue, setter, defaultValue, values, labelFunction
      );
      this.rows.add(row);
   }

   private int nextRowTop() {
      return this.rows.isEmpty() ? 48 : this.rows.get(this.rows.size() - 1).bottom() + 2;
   }

   private Component tooltipFor(String translationKey) {
      return Component.translatable(translationKey + ".tooltip");
   }

   private void addRowWidgets() {
      for (SlimPatchConfigScreen.OptionRow row : this.rows) {
         row.addWidgets();
      }
   }

   private void updateRowPositions() {
      for (SlimPatchConfigScreen.OptionRow row : this.rows) {
         row.updatePosition(this.scrollOffset);
      }
   }

   private int getMaxScroll() {
      return Math.max(0, this.contentHeight - (this.height - 36));
   }

   private final class BooleanRow extends SlimPatchConfigScreen.OptionRow {
      private final Button button;
      private final Consumer<Boolean> setter;
      private final boolean originalValue;
      private final boolean defaultValue;
      private boolean value;

      private BooleanRow(Component label, Component tooltip, int top, boolean currentValue, Consumer<Boolean> setter, boolean defaultValue) {
         super(label, tooltip, top);
         this.value = currentValue;
         this.originalValue = currentValue;
         this.setter = setter;
         this.defaultValue = defaultValue;
         this.button = Button.builder(this.getButtonText(), button -> {
            this.value = !this.value;
            button.setMessage(this.getButtonText());
         }).bounds(0, 0, 90, 20).build();
      }

      private Component getButtonText() {
         return Component.literal(this.value ? "ON" : "OFF");
      }

      @Override
      protected void addWidgets() {
         SlimPatchConfigScreen.this.addRenderableWidget(this.button);
      }

      @Override
      protected void updatePosition(int scrollOffset) {
         this.y = this.top - scrollOffset;
         this.button.setX(SlimPatchConfigScreen.this.width - 114);
         this.button.setY(this.y + 2);
         this.button.visible = this.y >= 40 && this.y <= SlimPatchConfigScreen.this.height - 36;
         this.button.active = this.button.visible;
      }

      @Override
      protected void save() {
         this.setter.accept(this.value);
      }

      @Override
      protected void undo() {
         this.value = this.originalValue;
         this.button.setMessage(this.getButtonText());
      }

      @Override
      protected void reset() {
         this.value = this.defaultValue;
         this.button.setMessage(this.getButtonText());
      }
   }

   private enum Category {
      COMMON("slimpatch.configuration.common_settings", "slimpatch.configuration.common_settings_title"),
      APPEARANCE("slimpatch.configuration.appearance", "slimpatch.configuration.appearance.title"),
      GAMEPLAY("slimpatch.configuration.gameplay", "slimpatch.configuration.gameplay.title"),
      FAMILY("slimpatch.configuration.family", "slimpatch.configuration.family.title");

      private final String labelKey;
      private final String titleKey;

      Category(String labelKey, String titleKey) {
         this.labelKey = labelKey;
         this.titleKey = titleKey;
      }
   }

   private final class CategoryLabelRow extends SlimPatchConfigScreen.OptionRow {
      private final int x;

      private CategoryLabelRow(Component label, int top, int x) {
         super(label, null, top);
         this.x = x;
      }

      @Override
      protected void addWidgets() {
      }

      @Override
      protected void updatePosition(int scrollOffset) {
         this.y = this.top;
      }

      @Override
      protected void save() {
      }

      @Override
      protected void undo() {
      }

      @Override
      protected void reset() {
      }

      @Override
      protected void renderLabel(GuiGraphics guiGraphics, int mouseX, int mouseY) {
         guiGraphics.drawString(SlimPatchConfigScreen.this.font, this.label, this.x, this.y + 6, 16777215, false);
      }
   }

   private final class DoubleRow extends SlimPatchConfigScreen.TextRow<Double> {
      private final Consumer<Double> setter;
      private final double min;
      private final double max;

      private DoubleRow(Component label, Component tooltip, int top, String currentValue, Consumer<Double> setter, double defaultValue, double min, double max) {
         super(label, tooltip, top, currentValue, Double.parseDouble(currentValue), defaultValue);
         this.setter = setter;
         this.min = min;
         this.max = max;
      }

      @Override
      protected void save() {
         try {
            double value = Double.parseDouble(this.field.getValue().trim());
            this.setter.accept(Math.max(this.min, Math.min(this.max, value)));
         } catch (NumberFormatException ignored) {
            this.setter.accept(this.originalValue);
         }
      }

      protected String formatValue(Double value) {
         return value == null ? "" : String.format(Locale.ROOT, "%s", value);
      }
   }

   private final class EnumRow<T extends Enum<T>> extends SlimPatchConfigScreen.OptionRow {
      private final Button button;
      private final Consumer<T> setter;
      private final T originalValue;
      private final T defaultValue;
      private final T[] values;
      private final Function<T, Component> labelFunction;
      private T value;

      private EnumRow(
         Component label, Component tooltip, int top, T currentValue, Consumer<T> setter, T defaultValue, T[] values, Function<T, Component> labelFunction
      ) {
         super(label, tooltip, top);
         this.value = currentValue;
         this.originalValue = currentValue;
         this.setter = setter;
         this.defaultValue = defaultValue;
         this.values = values;
         this.labelFunction = labelFunction;
         this.button = Button.builder(this.getButtonText(), button -> this.cycle()).bounds(0, 0, 120, 20).build();
      }

      private void cycle() {
         int nextIndex = 0;

         for (int i = 0; i < this.values.length; i++) {
            if (this.values[i] == this.value) {
               nextIndex = (i + 1) % this.values.length;
               break;
            }
         }

         this.value = this.values[nextIndex];
         this.button.setMessage(this.getButtonText());
      }

      private Component getButtonText() {
         return this.labelFunction.apply(this.value);
      }

      @Override
      protected void addWidgets() {
         SlimPatchConfigScreen.this.addRenderableWidget(this.button);
      }

      @Override
      protected void updatePosition(int scrollOffset) {
         this.y = this.top - scrollOffset;
         this.button.setX(SlimPatchConfigScreen.this.width - 144);
         this.button.setY(this.y + 2);
         this.button.visible = this.y >= 40 && this.y <= SlimPatchConfigScreen.this.height - 36;
         this.button.active = this.button.visible;
      }

      @Override
      protected void save() {
         this.setter.accept(this.value);
      }

      @Override
      protected void undo() {
         this.value = this.originalValue;
         this.button.setMessage(this.getButtonText());
      }

      @Override
      protected void reset() {
         this.value = this.defaultValue;
         this.button.setMessage(this.getButtonText());
      }
   }

   private final class IntRow extends SlimPatchConfigScreen.TextRow<Integer> {
      private final Consumer<Integer> setter;
      private final int min;
      private final int max;

      private IntRow(Component label, Component tooltip, int top, String currentValue, Consumer<Integer> setter, int defaultValue, int min, int max) {
         super(label, tooltip, top, currentValue, Integer.parseInt(currentValue), defaultValue);
         this.setter = setter;
         this.min = min;
         this.max = max;
      }

      @Override
      protected void save() {
         try {
            int value = Integer.parseInt(this.field.getValue().trim());
            this.setter.accept(Math.max(this.min, Math.min(this.max, value)));
         } catch (NumberFormatException ignored) {
            this.setter.accept(this.originalValue);
         }
      }
   }

   private abstract class OptionRow {
      protected final Component label;
      protected final Component tooltip;
      protected final int top;
      protected int y;

      protected OptionRow(Component label, Component tooltip, int top) {
         this.label = label;
         this.tooltip = tooltip;
         this.top = top;
      }

      protected int height() {
         return 24;
      }

      protected int bottom() {
         return this.top + this.height();
      }

      protected abstract void addWidgets();

      protected abstract void updatePosition(int var1);

      protected abstract void save();

      protected abstract void undo();

      protected abstract void reset();

      protected void renderLabel(GuiGraphics guiGraphics, int mouseX, int mouseY) {
         if (this.y >= 40 && this.y <= SlimPatchConfigScreen.this.height - 36) {
            guiGraphics.drawString(SlimPatchConfigScreen.this.font, this.label, 24, this.y + 6, 16777215, false);
         }
      }

      protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
         if (this.tooltip != null && this.y >= 40 && this.y <= SlimPatchConfigScreen.this.height - 36) {
            if (mouseX >= 24 && mouseX <= 220 && mouseY >= this.y && mouseY <= this.y + this.height()) {
               guiGraphics.renderTooltip(SlimPatchConfigScreen.this.font, this.tooltip, mouseX, mouseY);
            }
         }
      }
   }

   private final class StringRow extends SlimPatchConfigScreen.TextRow<String> {
      private final Consumer<String> setter;

      private StringRow(Component label, Component tooltip, int top, String currentValue, Consumer<String> setter, String defaultValue) {
         super(label, tooltip, top, currentValue, currentValue == null ? "" : currentValue, defaultValue);
         this.setter = setter;
      }

      @Override
      protected void save() {
         String value = this.field.getValue().trim();
         this.setter.accept(value.isEmpty() ? this.originalValue : value);
      }
   }

   private abstract class TextRow<T> extends SlimPatchConfigScreen.OptionRow {
      protected final EditBox field;
      protected final T originalValue;
      protected final T defaultValue;

      protected TextRow(Component label, Component tooltip, int top, String currentValue, T originalValue, T defaultValue) {
         super(label, tooltip, top);
         this.originalValue = originalValue;
         this.defaultValue = defaultValue;
         this.field = new EditBox(SlimPatchConfigScreen.this.font, 0, 0, 90, 20, label);
         this.field.setValue(currentValue == null ? "" : currentValue);
         this.field.setMaxLength(64);
      }

      @Override
      protected void addWidgets() {
         SlimPatchConfigScreen.this.addRenderableWidget(this.field);
      }

      @Override
      protected void updatePosition(int scrollOffset) {
         this.y = this.top - scrollOffset;
         this.field.setX(SlimPatchConfigScreen.this.width - 114);
         this.field.setY(this.y + 2);
         this.field.visible = this.y >= 40 && this.y <= SlimPatchConfigScreen.this.height - 36;
         this.field.setEditable(this.field.visible);
      }

      @Override
      protected void undo() {
         this.field.setValue(this.formatValue(this.originalValue));
      }

      @Override
      protected void reset() {
         this.field.setValue(this.formatValue(this.defaultValue));
      }

      protected String formatValue(T value) {
         return value == null ? "" : value.toString();
      }
   }

   private enum ViewMode {
      ROOT,
      MOD_SETTINGS,
      CATEGORY;
   }
}
