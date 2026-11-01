package com.javic.slimpatch.config;

import com.javic.slimpatch.entity.VillagerPersonality;
import com.mojang.logging.LogUtils;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

public final class GiftPoolConfig {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("villagersreborn");
   private static final Path CONFIG_FILE = CONFIG_DIR.resolve("gift_pools.toml");
   private static final Map<VillagerPersonality, Map<GiftPoolConfig.GiftSlot, Item>> DEFAULT_ITEMS = createDefaultItems();
   private static volatile Map<VillagerPersonality, Map<GiftPoolConfig.GiftSlot, Item>> configuredItems = DEFAULT_ITEMS;
   private static volatile long lastLoadedMillis = Long.MIN_VALUE;

   private GiftPoolConfig() {
   }

   public static Item getItem(VillagerPersonality personality, GiftPoolConfig.GiftSlot slot) {
      loadIfNeeded();
      return configuredItems.getOrDefault(personality, DEFAULT_ITEMS.get(VillagerPersonality.FRIENDLY)).get(slot);
   }

   private static synchronized void loadIfNeeded() {
      try {
         Files.createDirectories(CONFIG_DIR);
         if (Files.notExists(CONFIG_FILE)) {
            Files.writeString(CONFIG_FILE, buildDefaultConfig(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
         }

         long modifiedMillis = Files.getLastModifiedTime(CONFIG_FILE).toMillis();
         if (modifiedMillis == lastLoadedMillis) {
            return;
         }

         String content = Files.readString(CONFIG_FILE, StandardCharsets.UTF_8);
         Map<VillagerPersonality, Map<GiftPoolConfig.GiftSlot, Item>> loaded = new EnumMap<>(VillagerPersonality.class);

         for (VillagerPersonality personality : VillagerPersonality.values()) {
            loaded.put(personality, parsePersonality(content, personality));
         }

         configuredItems = loaded;
         lastLoadedMillis = modifiedMillis;
      } catch (Exception e) {
         configuredItems = DEFAULT_ITEMS;
         lastLoadedMillis = Long.MIN_VALUE;
         LOGGER.warn("Failed to load villager gift pools from {}. Using built-in defaults.", CONFIG_FILE, e);
      }
   }

   private static Map<GiftPoolConfig.GiftSlot, Item> parsePersonality(String content, VillagerPersonality personality) {
      Map<GiftPoolConfig.GiftSlot, Item> fallback = DEFAULT_ITEMS.get(personality);
      Map<GiftPoolConfig.GiftSlot, Item> items = new EnumMap<>(GiftPoolConfig.GiftSlot.class);
      String sectionName = personality.name().toLowerCase();
      Matcher sectionMatcher = Pattern.compile("(?ms)^\\s*\\[" + Pattern.quote(sectionName) + "]\\s*(.*?)(?=^\\s*\\[|\\z)").matcher(content);
      String section = sectionMatcher.find() ? sectionMatcher.group(1) : null;

      for (GiftPoolConfig.GiftSlot slot : GiftPoolConfig.GiftSlot.values()) {
         Item fallbackItem = fallback.get(slot);
         if (section == null) {
            items.put(slot, fallbackItem);
         } else {
            Matcher valueMatcher = Pattern.compile("(?m)^\\s*" + Pattern.quote(slot.key()) + "\\s*=\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(section);
            if (!valueMatcher.find()) {
               items.put(slot, fallbackItem);
            } else {
               ResourceLocation itemId = ResourceLocation.tryParse(unescape(valueMatcher.group(1)).trim());
               Item item = itemId == null ? null : (Item)BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
               items.put(slot, item == null ? fallbackItem : item);
            }
         }
      }

      return items;
   }

   private static Map<VillagerPersonality, Map<GiftPoolConfig.GiftSlot, Item>> createDefaultItems() {
      Map<VillagerPersonality, Map<GiftPoolConfig.GiftSlot, Item>> defaults = new EnumMap<>(VillagerPersonality.class);
      defaults.put(VillagerPersonality.FRIENDLY, mapOf(Items.BREAD, Items.CAKE, Items.EMERALD, Items.HONEY_BOTTLE, Items.GOLDEN_APPLE));
      defaults.put(VillagerPersonality.MEAN, mapOf(Items.ROTTEN_FLESH, Items.COAL, Items.TNT, Items.IRON_INGOT, Items.NETHERITE_SCRAP));
      defaults.put(VillagerPersonality.SHY, mapOf(Items.PUMPKIN_PIE, Items.APPLE, Items.HONEY_BOTTLE, Items.DIAMOND, Items.POTION));
      defaults.put(VillagerPersonality.BRAVE, mapOf(Items.ARROW, Items.SHIELD, Items.POTION, Items.DIAMOND_SWORD, Items.TOTEM_OF_UNDYING));
      defaults.put(VillagerPersonality.GRUMPY, mapOf(Items.POTATO, Items.IRON_INGOT, Items.EMERALD, Items.ENDER_PEARL, Items.DIAMOND_CHESTPLATE));
      defaults.put(VillagerPersonality.GREEDY, mapOf(Items.COPPER_INGOT, Items.GOLD_NUGGET, Items.GOLD_INGOT, Items.EMERALD, Items.DIAMOND));
      defaults.put(VillagerPersonality.ROMANTIC, mapOf(Items.POPPY, Items.APPLE, Items.EMERALD, Items.BLAZE_POWDER, Items.HEART_OF_THE_SEA));
      defaults.put(VillagerPersonality.WISE, mapOf(Items.BOOK, Items.LAPIS_LAZULI, Items.POTION, Items.POTION, Items.SHULKER_SHELL));
      return defaults;
   }

   private static Map<GiftPoolConfig.GiftSlot, Item> mapOf(Item common, Item uncommon, Item rare, Item veryRare, Item legendary) {
      Map<GiftPoolConfig.GiftSlot, Item> items = new EnumMap<>(GiftPoolConfig.GiftSlot.class);
      items.put(GiftPoolConfig.GiftSlot.COMMON, common);
      items.put(GiftPoolConfig.GiftSlot.UNCOMMON, uncommon);
      items.put(GiftPoolConfig.GiftSlot.RARE, rare);
      items.put(GiftPoolConfig.GiftSlot.VERY_RARE, veryRare);
      items.put(GiftPoolConfig.GiftSlot.LEGENDARY, legendary);
      return items;
   }

   private static String buildDefaultConfig() {
      StringBuilder builder = new StringBuilder();
      appendSection(builder, "friendly", DEFAULT_ITEMS.get(VillagerPersonality.FRIENDLY));
      appendSection(builder, "mean", DEFAULT_ITEMS.get(VillagerPersonality.MEAN));
      appendSection(builder, "shy", DEFAULT_ITEMS.get(VillagerPersonality.SHY));
      appendSection(builder, "brave", DEFAULT_ITEMS.get(VillagerPersonality.BRAVE));
      appendSection(builder, "grumpy", DEFAULT_ITEMS.get(VillagerPersonality.GRUMPY));
      appendSection(builder, "greedy", DEFAULT_ITEMS.get(VillagerPersonality.GREEDY));
      appendSection(builder, "romantic", DEFAULT_ITEMS.get(VillagerPersonality.ROMANTIC));
      appendSection(builder, "wise", DEFAULT_ITEMS.get(VillagerPersonality.WISE));
      return builder.toString();
   }

   private static void appendSection(StringBuilder builder, String sectionName, Map<GiftPoolConfig.GiftSlot, Item> items) {
      if (!builder.isEmpty()) {
         builder.append('\n');
      }

      builder.append('[').append(sectionName).append("]\n");

      for (GiftPoolConfig.GiftSlot slot : GiftPoolConfig.GiftSlot.values()) {
         builder.append(slot.key()).append(" = \"").append(BuiltInRegistries.ITEM.getKey(items.get(slot))).append("\"\n");
      }
   }

   private static String unescape(String value) {
      StringBuilder builder = new StringBuilder(value.length());
      boolean escaping = false;

      for (int i = 0; i < value.length(); i++) {
         char c = value.charAt(i);
         if (escaping) {
            builder.append(c);
            escaping = false;
         } else if (c == '\\') {
            escaping = true;
         } else {
            builder.append(c);
         }
      }

      if (escaping) {
         builder.append('\\');
      }

      return builder.toString();
   }

   public enum GiftSlot {
      COMMON("common"),
      UNCOMMON("uncommon"),
      RARE("rare"),
      VERY_RARE("veryRare"),
      LEGENDARY("legendary");

      private final String key;

      GiftSlot(String key) {
         this.key = key;
      }

      public String key() {
         return this.key;
      }
   }
}
