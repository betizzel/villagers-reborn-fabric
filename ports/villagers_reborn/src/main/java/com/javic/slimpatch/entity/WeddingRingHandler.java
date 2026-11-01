package com.javic.slimpatch.entity;

import com.javic.slimpatch.Config;
import com.javic.slimpatch.familytree.FamilyTreeSavedData;
import com.javic.slimpatch.familytree.FamilyTreeVillagerSnapshot;
import com.javic.slimpatch.item.ModItems;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class WeddingRingHandler {
   private WeddingRingHandler() {
   }

   private static boolean isAlreadyCommitted(FamilyVillager familyVillager) {
      return familyVillager.getRelationshipStage() == VillagerRelationshipStage.MARRIED
         || familyVillager.getSpousePlayerUuid() != null
         || familyVillager.getSpouseVillagerUuid() != null
         || familyVillager.hasSpouse();
   }

   public static boolean isWeddingRing(ItemStack stack) {
      return stack.is((Item)ModItems.IRON_WEDDING_RING)
         || stack.is((Item)ModItems.GOLD_WEDDING_RING)
         || stack.is((Item)ModItems.EMERALD_WEDDING_RING)
         || stack.is((Item)ModItems.DIAMOND_WEDDING_RING)
         || stack.is((Item)ModItems.REDSTONE_WEDDING_RING)
         || stack.is((Item)ModItems.LAPISLAZULI_WEDDING_RING);
   }

   public static String getMarriageFailureMessage(FamilyVillager familyVillager) {
      if (isAlreadyCommitted(familyVillager)) {
         return "slimpatch.message.marriage_already_married";
      } else if (familyVillager.getAgeStage() != VillagerAgeStage.ADULT) {
         return "slimpatch.message.marriage_requires_adult";
      } else if (familyVillager.getRelationshipStage() != VillagerRelationshipStage.DATING) {
         return "slimpatch.message.marriage_requires_dating";
      } else {
         return familyVillager.getGoldenRelationship() < 5.0F ? "slimpatch.message.marriage_requires_golden_hearts" : null;
      }
   }

   public static String getMarriageFailureMessage(FamilyVillager familyVillager, Villager villager, ServerPlayer player) {
      if (isAlreadyCommitted(familyVillager)) {
         return "slimpatch.message.marriage_already_married";
      } else if (VillagerFamilyData.isUnderage(familyVillager)) {
         return "slimpatch.message.marriage_requires_adult";
      } else if (!VillagerFamilyData.canUseRomanticInteraction(villager, player)) {
         return "slimpatch.message.parent_romance_blocked";
      } else {
         float goldenRelationship = VillagerRelationshipData.getGoldenRelationshipForPlayer(villager, player.getUUID(), familyVillager.getGoldenRelationship());
         if (goldenRelationship <= 0.0F) {
            return "slimpatch.message.marriage_requires_dating";
         } else if (goldenRelationship < 5.0F) {
            return "slimpatch.message.marriage_requires_golden_hearts";
         } else {
            return !Config.ALLOW_MULTIPLE_PLAYER_SPOUSES.get() && playerHasLivingVillagerSpouse(player, villager.getUUID())
               ? "slimpatch.message.marriage_player_already_has_spouse"
               : null;
         }
      }
   }

   private static boolean playerHasLivingVillagerSpouse(ServerPlayer player, UUID targetVillagerUuid) {
      MinecraftServer server = player.getServer();
      if (server == null) {
         return false;
      }

      UUID playerUuid = player.getUUID();
      FamilyTreeSavedData data = FamilyTreeSavedData.get(server);

      for (FamilyTreeVillagerSnapshot snapshot : data.getVillagers()) {
         if (snapshot != null && playerUuid.equals(snapshot.getSpousePlayerUuid())) {
            UUID villagerUuid = snapshot.getVillagerUuid();
            if (villagerUuid != null && !villagerUuid.equals(targetVillagerUuid) && isVillagerAlive(server, snapshot)) {
               return true;
            }
         }
      }

      return false;
   }

   private static boolean isVillagerAlive(MinecraftServer server, FamilyTreeVillagerSnapshot snapshot) {
      if (server != null && snapshot != null && snapshot.getVillagerUuid() != null) {
         for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(snapshot.getVillagerUuid()) instanceof Villager villager) {
               return villager.isAlive();
            }
         }

         return snapshot.isAlive();
      } else {
         return false;
      }
   }

   public static ItemStack findWeddingRingStack(Player player) {
      ItemStack mainHand = player.getMainHandItem();
      if (isWeddingRing(mainHand)) {
         return mainHand;
      }

      ItemStack offHand = player.getOffhandItem();
      return isWeddingRing(offHand) ? offHand : ItemStack.EMPTY;
   }
}
