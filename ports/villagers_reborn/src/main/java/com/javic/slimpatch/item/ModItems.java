package com.javic.slimpatch.item;

import com.javic.slimpatch.ModBlocks;
import com.javic.slimpatch.ModEntities;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public final class ModItems {
   public static final Item MALE_VILLAGER_SPAWN_EGG = register("male_villager_spawn_egg", new GenderedVillagerEggItem(ModEntities.MALE_VILLAGER, true, new Item.Properties()));
   public static final Item FEMALE_VILLAGER_SPAWN_EGG = register("female_villager_spawn_egg", new GenderedVillagerEggItem(ModEntities.FEMALE_VILLAGER, false, new Item.Properties()));
   public static final Item ROMANTIC_BOUQUET = register("romantic_bouquet", new Item(new Item.Properties()));
   public static final Item IRON_WEDDING_RING = register("iron_wedding_ring", new Item(new Item.Properties()));
   public static final Item GOLD_WEDDING_RING = register("gold_wedding_ring", new Item(new Item.Properties()));
   public static final Item EMERALD_WEDDING_RING = register("emerald_wedding_ring", new Item(new Item.Properties()));
   public static final Item DIAMOND_WEDDING_RING = register("diamond_wedding_ring", new Item(new Item.Properties()));
   public static final Item REDSTONE_WEDDING_RING = register("redstone_wedding_ring", new Item(new Item.Properties()));
   public static final Item LAPISLAZULI_WEDDING_RING = register("lapislazuli_wedding_ring", new Item(new Item.Properties()));
   public static final Item DIVORCE_PAPERS = register("divorce_papers", new Item(new Item.Properties()));
   public static final Item FAMILY_CHARM = register("family_charm", new Item(new Item.Properties()));
   public static final Item SUN_CHARM = register("sun_charm", new Item(new Item.Properties()));
   public static final Item MOON_CHARM = register("moon_charm", new Item(new Item.Properties()));
   public static final Item SOUL_GEM = register("soul_gem", new Item(new Item.Properties()));
   public static final Item VILLAGER_LEDGER = register("villager_ledger", new VillagerLedgerItem(new Item.Properties().stacksTo(1)));
   public static final Item GRAVE = register("grave", new GraveBlockItem(ModBlocks.GRAVE, new Item.Properties()));

   private static Item register(String name, Item item) {
      return Registry.register(BuiltInRegistries.ITEM, new ResourceLocation("slimpatch", name), item);
   }

   public static void init() { }
   private ModItems() { }
}
