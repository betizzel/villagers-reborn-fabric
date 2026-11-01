package com.javic.slimpatch.item;

import com.javic.slimpatch.ModBlockEntities;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class GraveBlockItem extends BlockItem {
   public GraveBlockItem(Block block, Properties properties) {
      super(block, properties);
   }

   public static ItemStack createStackWithRecord(Block block, UUID recordId, String cachedDisplayName) {
      ItemStack stack = new ItemStack(block);
      CompoundTag data = new CompoundTag();
      if (recordId != null) data.putUUID("RecordId", recordId);
      if (cachedDisplayName != null && !cachedDisplayName.isEmpty()) data.putString("CachedDisplayName", cachedDisplayName);
      BlockItem.setBlockEntityData(stack, ModBlockEntities.GRAVE, data);
      return stack;
   }

   @Override
   public Component getName(ItemStack stack) {
      CompoundTag data = BlockItem.getBlockEntityData(stack);
      if (data != null && data.contains("CachedDisplayName")) {
         String cachedDisplayName = data.getString("CachedDisplayName");
         if (!cachedDisplayName.isEmpty()) return Component.translatable("item.slimpatch.grave.named", cachedDisplayName);
      }
      return super.getName(stack);
   }
}
