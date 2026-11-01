package com.javic.slimpatch.entity;

import com.javic.slimpatch.config.GiftPoolConfig;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;

public final class GiftReactionHandler {
   private GiftReactionHandler() {
   }

   public static ItemStack getRandomGift(VillagerPersonality personality, RandomSource random) {
      GiftPoolConfig.GiftSlot slot = rollSlot(random);

      return switch (personality) {
         case FRIENDLY -> {
            switch (slot) {
               case COMMON:
                  yield stack(personality, slot, random.nextInt(5) + 2);
               case UNCOMMON:
                  yield stack(personality, slot, 1);
               case RARE:
                  yield stack(personality, slot, random.nextInt(5) + 1);
               case VERY_RARE:
                  yield stack(personality, slot, random.nextInt(3) + 1);
               case LEGENDARY:
                  yield stack(personality, slot, random.nextInt(3) + 1);
               default:
                  throw new IncompatibleClassChangeError();
            }
         }
         case MEAN -> {
            switch (slot) {
               case COMMON:
                  yield stack(personality, slot, random.nextInt(4) + 2);
               case UNCOMMON:
                  yield stack(personality, slot, random.nextInt(7) + 4);
               case RARE:
                  yield stack(personality, slot, random.nextInt(5) + 1);
               case VERY_RARE:
                  yield stack(personality, slot, random.nextInt(6) + 1);
               case LEGENDARY:
                  yield stack(personality, slot, random.nextInt(4) + 1);
               default:
                  throw new IncompatibleClassChangeError();
            }
         }
         case SHY -> {
            switch (slot) {
               case COMMON:
                  yield stack(personality, slot, random.nextInt(2) + 1);
               case UNCOMMON:
                  yield stack(personality, slot, random.nextInt(5) + 2);
               case RARE:
                  yield stack(personality, slot, random.nextInt(3) + 1);
               case VERY_RARE:
                  yield stack(personality, slot, random.nextInt(2) + 1);
               case LEGENDARY:
                  yield potionOrItem(VillagerPersonality.SHY, slot, random.nextInt(2) + 1, random, Potions.INVISIBILITY);
               default:
                  throw new IncompatibleClassChangeError();
            }
         }
         case BRAVE -> {
            switch (slot) {
               case COMMON:
                  yield stack(personality, slot, random.nextInt(11) + 5);
               case UNCOMMON:
                  yield stack(personality, slot, 1);
               case RARE:
                  yield potionOrItem(
                     VillagerPersonality.BRAVE, slot, random.nextInt(3) + 1, random, random.nextBoolean() ? Potions.STRENGTH : Potions.FIRE_RESISTANCE
                  );
               case VERY_RARE:
                  yield stack(personality, slot, 1);
               case LEGENDARY:
                  yield stack(personality, slot, 1);
               default:
                  throw new IncompatibleClassChangeError();
            }
         }
         case GRUMPY -> {
            switch (slot) {
               case COMMON:
                  yield stack(personality, slot, random.nextInt(6) + 3);
               case UNCOMMON:
                  yield stack(personality, slot, random.nextInt(3) + 1);
               case RARE:
                  yield stack(personality, slot, random.nextInt(3) + 1);
               case VERY_RARE:
                  yield stack(personality, slot, random.nextInt(4) + 1);
               case LEGENDARY:
                  yield grumpyLegendaryGift(random);
               default:
                  throw new IncompatibleClassChangeError();
            }
         }
         case GREEDY -> {
            switch (slot) {
               case COMMON:
                  yield stack(personality, slot, random.nextInt(3) + 2);
               case UNCOMMON:
                  yield stack(personality, slot, random.nextInt(7) + 3);
               case RARE:
                  yield stack(personality, slot, random.nextInt(3) + 1);
               case VERY_RARE:
                  yield stack(personality, slot, random.nextInt(6) + 1);
               case LEGENDARY:
                  yield stack(personality, slot, random.nextInt(3) + 1);
               default:
                  throw new IncompatibleClassChangeError();
            }
         }
         case ROMANTIC -> {
            switch (slot) {
               case COMMON:
                  yield romanticCommonGift(random);
               case UNCOMMON:
                  yield stack(personality, slot, random.nextInt(6) + 3);
               case RARE:
                  yield stack(personality, slot, random.nextInt(5) + 2);
               case VERY_RARE:
                  yield stack(personality, slot, random.nextInt(4) + 1);
               case LEGENDARY:
                  yield stack(personality, slot, 1);
               default:
                  throw new IncompatibleClassChangeError();
            }
         }
         case WISE -> {
            switch (slot) {
               case COMMON:
                  yield stack(personality, slot, random.nextInt(8) + 1);
               case UNCOMMON:
                  yield stack(personality, slot, random.nextInt(9) + 4);
               case RARE:
                  yield potionOrItem(VillagerPersonality.WISE, slot, random.nextInt(3) + 1, random, Potions.FIRE_RESISTANCE);
               case VERY_RARE:
                  yield potionOrItem(VillagerPersonality.WISE, slot, random.nextInt(3) + 1, random, Potions.REGENERATION);
               case LEGENDARY:
                  yield stack(personality, slot, 1);
               default:
                  throw new IncompatibleClassChangeError();
            }
         }
      };
   }

   private static GiftPoolConfig.GiftSlot rollSlot(RandomSource random) {
      float roll = random.nextFloat();
      if (roll < 0.42F) {
         return GiftPoolConfig.GiftSlot.COMMON;
      } else if (roll < 0.69F) {
         return GiftPoolConfig.GiftSlot.UNCOMMON;
      } else if (roll < 0.86F) {
         return GiftPoolConfig.GiftSlot.RARE;
      } else {
         return roll < 0.95F ? GiftPoolConfig.GiftSlot.VERY_RARE : GiftPoolConfig.GiftSlot.LEGENDARY;
      }
   }

   private static ItemStack stack(VillagerPersonality personality, GiftPoolConfig.GiftSlot slot, int count) {
      return new ItemStack(GiftPoolConfig.getItem(personality, slot), count);
   }

   private static ItemStack potionOrItem(VillagerPersonality personality, GiftPoolConfig.GiftSlot slot, int count, RandomSource random, Potion potion) {
      Item item = GiftPoolConfig.getItem(personality, slot);
      return item != Items.POTION ? new ItemStack(item, count) : PotionUtils.setPotion(new ItemStack(Items.POTION, count), potion);
   }

   private static ItemStack grumpyLegendaryGift(RandomSource random) {
      Item item = GiftPoolConfig.getItem(VillagerPersonality.GRUMPY, GiftPoolConfig.GiftSlot.LEGENDARY);
      if (item != Items.DIAMOND_CHESTPLATE) {
         return new ItemStack(item);
      }

      ItemStack[] armorPieces = new ItemStack[]{
         new ItemStack(Items.DIAMOND_HELMET),
         new ItemStack(Items.DIAMOND_CHESTPLATE),
         new ItemStack(Items.DIAMOND_LEGGINGS),
         new ItemStack(Items.DIAMOND_BOOTS)
      };
      return armorPieces[random.nextInt(armorPieces.length)];
   }

   private static ItemStack romanticCommonGift(RandomSource random) {
      Item item = GiftPoolConfig.getItem(VillagerPersonality.ROMANTIC, GiftPoolConfig.GiftSlot.COMMON);
      return item != Items.POPPY
         ? new ItemStack(item, random.nextInt(3) + 1)
         : new ItemStack(random.nextBoolean() ? Items.PINK_TULIP : Items.POPPY, random.nextInt(3) + 1);
   }
}
