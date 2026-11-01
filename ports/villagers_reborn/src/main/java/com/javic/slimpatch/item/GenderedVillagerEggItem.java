package com.javic.slimpatch.item;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

public class GenderedVillagerEggItem extends SpawnEggItem {
   private final boolean isMale;

   public GenderedVillagerEggItem(EntityType<? extends Mob> type, boolean isMale, Properties properties) {
      super(type, 0, 0, properties);
      this.isMale = isMale;
   }

   @Override
   public ItemStack getDefaultInstance() {
      ItemStack stack = super.getDefaultInstance();
      CompoundTag entityTag = stack.getOrCreateTagElement("EntityTag");
      CompoundTag forgeData = entityTag.getCompound("ForgeData");
      forgeData.putInt("slimpatch_skin", ThreadLocalRandom.current().nextInt(35) + 1);
      forgeData.putString("slimpatch_gender", isMale ? "male" : "female");
      forgeData.putBoolean("slimpatch_forced", true);
      entityTag.put("ForgeData", forgeData);
      return stack;
   }
}
