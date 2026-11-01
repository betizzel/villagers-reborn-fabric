package com.javic.slimpatch.entity;

import com.javic.slimpatch.item.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class DivorcePapersHandler {
   private DivorcePapersHandler() {
   }

   public static boolean isDivorcePapers(ItemStack stack) {
      return stack.is((Item)ModItems.DIVORCE_PAPERS);
   }

   public static ItemStack findDivorcePapersStack(Player player) {
      ItemStack mainHand = player.getMainHandItem();
      if (isDivorcePapers(mainHand)) {
         return mainHand;
      }

      ItemStack offHand = player.getOffhandItem();
      return isDivorcePapers(offHand) ? offHand : ItemStack.EMPTY;
   }
}
