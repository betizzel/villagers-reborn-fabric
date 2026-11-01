package com.javic.slimpatch;

import com.javic.slimpatch.menu.VillagerEquipmentMenu;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
   public static final MenuType<VillagerEquipmentMenu> VILLAGER_EQUIPMENT = Registry.register(
      BuiltInRegistries.MENU, new ResourceLocation("slimpatch", "villager_equipment"),
      new MenuType<>(VillagerEquipmentMenu::new, FeatureFlags.DEFAULT_FLAGS));

   public static void init() { }
   private ModMenus() { }
}
