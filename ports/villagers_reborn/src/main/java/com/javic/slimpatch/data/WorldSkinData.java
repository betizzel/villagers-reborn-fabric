package com.javic.slimpatch.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

public class WorldSkinData extends SavedData {
   private String skinTheme = "";
   private boolean guiShown = false;

   public static WorldSkinData load(CompoundTag tag) {
      WorldSkinData data = new WorldSkinData();
      if (tag.contains("SkinTheme")) {
         data.skinTheme = normalizeTheme(tag.getString("SkinTheme"));
      }
      if (tag.contains("GuiShown")) {
         data.guiShown = tag.getBoolean("GuiShown");
      }
      return data;
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      tag.putString("SkinTheme", this.skinTheme);
      tag.putBoolean("GuiShown", this.guiShown);
      return tag;
   }

   public static WorldSkinData get(MinecraftServer server) {
      WorldSkinData data = server.overworld().getDataStorage().computeIfAbsent(WorldSkinData::load, WorldSkinData::new, "slimpatch_skin_data");
      if (!data.isInitialized()) {
         String migratedTheme = findLegacyTheme(server);
         if (!migratedTheme.isEmpty()) {
            data.skinTheme = migratedTheme;
            data.guiShown = true;
            data.setDirty();
         }
      }
      return data;
   }

   public static WorldSkinData get(ServerLevel level) {
      return get(level.getServer());
   }

   public void setTheme(String theme) {
      String normalizedTheme = normalizeTheme(theme);
      if (!normalizedTheme.isEmpty()) {
         this.skinTheme = normalizedTheme;
         this.setDirty();
      }
   }

   public String getTheme() { return this.skinTheme; }
   public boolean isInitialized() { return this.skinTheme != null && !this.skinTheme.isEmpty(); }
   public boolean isGuiShown() { return this.guiShown; }

   public void setGuiShown(boolean shown) {
      this.guiShown = shown;
      this.setDirty();
   }

   public static String normalizeTheme(String theme) {
      if ("modern".equalsIgnoreCase(theme)) {
         return "modern";
      }
      return "fantasy".equalsIgnoreCase(theme) ? "fantasy" : "";
   }

   private static String findLegacyTheme(MinecraftServer server) {
      String netherTheme = getLegacyTheme(server.getLevel(Level.NETHER));
      return !netherTheme.isEmpty() ? netherTheme : getLegacyTheme(server.getLevel(Level.END));
   }

   private static String getLegacyTheme(ServerLevel level) {
      if (level == null) {
         return "";
      }
      WorldSkinData data = level.getDataStorage().computeIfAbsent(WorldSkinData::load, WorldSkinData::new, "slimpatch_skin_data");
      return data.getTheme();
   }
}
