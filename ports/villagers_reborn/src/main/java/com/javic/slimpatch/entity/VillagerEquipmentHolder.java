package com.javic.slimpatch.entity;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

public interface VillagerEquipmentHolder {
   SimpleContainer getEquipmentInventory();

   ItemStack getPersistentMainHandItem();

   void setPersistentMainHandItem(ItemStack var1);

   void syncPersistentMainHand();
}
