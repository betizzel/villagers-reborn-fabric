package com.javic.slimpatch.item;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ModCreativeTabs {
   public static final CreativeModeTab VILLAGERS_REBORN = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
      new ResourceLocation("slimpatch", "villagers_reborn"), FabricItemGroup.builder()
         .title(Component.translatable("itemGroup.slimpatch.villagers_reborn"))
         .icon(() -> new ItemStack(ModItems.ROMANTIC_BOUQUET))
         .displayItems((parameters, output) -> {
            output.accept(ModItems.ROMANTIC_BOUQUET);
            output.accept(ModItems.IRON_WEDDING_RING);
            output.accept(ModItems.GOLD_WEDDING_RING);
            output.accept(ModItems.EMERALD_WEDDING_RING);
            output.accept(ModItems.DIAMOND_WEDDING_RING);
            output.accept(ModItems.REDSTONE_WEDDING_RING);
            output.accept(ModItems.LAPISLAZULI_WEDDING_RING);
            output.accept(ModItems.DIVORCE_PAPERS);
            output.accept(ModItems.FAMILY_CHARM);
            output.accept(ModItems.SUN_CHARM);
            output.accept(ModItems.MOON_CHARM);
            output.accept(ModItems.SOUL_GEM);
            output.accept(ModItems.VILLAGER_LEDGER);
            output.accept(ModItems.GRAVE);
            output.accept(ModItems.MALE_VILLAGER_SPAWN_EGG);
            output.accept(ModItems.FEMALE_VILLAGER_SPAWN_EGG);
         }).build());

   public static void init() { }
   private ModCreativeTabs() { }
}
