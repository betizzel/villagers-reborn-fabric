package com.javic.slimpatch.entity;

import com.javic.slimpatch.network.ModNetworking;
import com.javic.slimpatch.network.RelationshipSyncPacket;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class RomanticGiftHandler {
   private static final Map<Item, Float> ROMANTIC_GIFTS = Map.of(
      Items.AMETHYST_SHARD,
      0.5F,
      Items.EMERALD,
      0.5F,
      Items.GOLD_INGOT,
      0.5F,
      Items.CAKE,
      0.75F,
      Items.DIAMOND,
      0.75F,
      Items.GOLDEN_APPLE,
      1.0F,
      Items.HEART_OF_THE_SEA,
      1.5F
   );

   private RomanticGiftHandler() {
   }

   public static boolean isRomanticGift(ItemStack stack) {
      return !stack.isEmpty() && ROMANTIC_GIFTS.containsKey(stack.getItem());
   }

   public static InteractionResult handleGift(Villager villager, FamilyVillager familyVillager, ServerPlayer player, ItemStack stack, Runnable positiveFeedback) {
      if (!VillagerFamilyData.canUseRomanticInteraction(villager, player)) {
         player.displayClientMessage(Component.translatable("slimpatch.message.parent_romance_blocked"), true);
         return InteractionResult.SUCCESS;
      }

      boolean usePerPlayerGolden = VillagerRelationshipData.usesPerPlayerRelationships(villager);
      boolean isDatingForPlayer = usePerPlayerGolden
         ? VillagerRelationshipData.getGoldenRelationshipForPlayer(villager, player.getUUID(), familyVillager.getGoldenRelationship()) > 0.0F
         : familyVillager.getRelationshipStage() == VillagerRelationshipStage.DATING || familyVillager.getGoldenRelationship() > 0.0F;
      if (!isDatingForPlayer) {
         return InteractionResult.PASS;
      }

      Float goldenDelta = ROMANTIC_GIFTS.get(stack.getItem());
      if (goldenDelta == null) {
         return InteractionResult.PASS;
      }

      float goldenRelationship = usePerPlayerGolden
         ? VillagerRelationshipData.getGoldenRelationshipForPlayer(villager, player.getUUID(), familyVillager.getGoldenRelationship())
         : familyVillager.getGoldenRelationship();
      if (goldenRelationship >= 5.0F) {
         player.displayClientMessage(Component.translatable("slimpatch.message.golden_relationship_max"), true);
         return InteractionResult.SUCCESS;
      }

      if (VillagerCooldownData.hasRomanticGiftCooldown(villager, player.getUUID())) {
         player.displayClientMessage(Component.translatable("slimpatch.message.romantic_gift_cooldown"), true);
         return InteractionResult.SUCCESS;
      }

      float updatedGoldenRelationship;
      if (usePerPlayerGolden) {
         updatedGoldenRelationship = VillagerRelationshipData.setGoldenRelationshipForPlayer(
            villager, player.getUUID(), goldenRelationship + goldenDelta, familyVillager.getGoldenRelationship()
         );
      } else {
         familyVillager.setGoldenRelationship(goldenRelationship + goldenDelta);
         updatedGoldenRelationship = familyVillager.getGoldenRelationship();
      }

      if (!player.getAbilities().instabuild) {
         stack.shrink(1);
      }

      VillagerCooldownData.setRomanticGiftCooldown(villager, player.getUUID());
      positiveFeedback.run();
      float relationship = villager instanceof FamilyVillager ? VillagerRelationshipData.getRelationshipForPlayer(villager, player.getUUID(), 0.5F) : 0.5F;
      ModNetworking.sendToClient(new RelationshipSyncPacket(villager.getId(), relationship, updatedGoldenRelationship), player);
      return InteractionResult.SUCCESS;
   }
}
