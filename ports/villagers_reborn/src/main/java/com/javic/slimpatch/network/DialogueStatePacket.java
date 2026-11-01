package com.javic.slimpatch.network;

import com.javic.slimpatch.dialogue.DialogueManager;
import com.javic.slimpatch.entity.FamilyVillager;
import com.javic.slimpatch.entity.FemaleVillagerEntity;
import com.javic.slimpatch.entity.MaleVillagerEntity;
import com.javic.slimpatch.entity.SpouseCookingHandler;
import com.javic.slimpatch.entity.VillagerCooldownData;
import com.javic.slimpatch.entity.VillagerRelationshipData;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;

public class DialogueStatePacket {
   private static final double MAX_DIALOGUE_DISTANCE_SQR = 64.0;
   private final int entityId;
   private final boolean active;

   public DialogueStatePacket(int entityId, boolean active) {
      this.entityId = entityId;
      this.active = active;
   }

   public static void encode(DialogueStatePacket packet, FriendlyByteBuf buf) {
      buf.writeVarInt(packet.entityId);
      buf.writeBoolean(packet.active);
   }

   public static DialogueStatePacket decode(FriendlyByteBuf buf) {
      return new DialogueStatePacket(buf.readVarInt(), buf.readBoolean());
   }

   public static void handle(DialogueStatePacket msg, ServerPlayer player) {

            if (player != null) {
               Entity entity = player.serverLevel().getEntity(msg.entityId);
               if (entity instanceof Villager villager && (entity instanceof MaleVillagerEntity || entity instanceof FemaleVillagerEntity) && entity.isAlive()) {
                  if (msg.active) {
                     if (player.distanceToSqr(villager) > 64.0) {
                        return;
                     }

                     UUID currentDialoguePlayer = DialogueManager.getDialoguePlayer(villager);
                     if (currentDialoguePlayer != null && !player.getUUID().equals(currentDialoguePlayer)) {
                        return;
                     }

                     DialogueManager.startDialogue(villager, player);
                     if (villager instanceof MaleVillagerEntity male) {
                        VillagerCooldownData.syncDialogueCooldownsToPlayer(male, player, male.getOptionCooldowns());
                     } else if (villager instanceof FemaleVillagerEntity female) {
                        VillagerCooldownData.syncDialogueCooldownsToPlayer(female, player, female.getOptionCooldowns());
                     }

                     float relationship = villager instanceof MaleVillagerEntity male
                        ? VillagerRelationshipData.getRelationshipForPlayer(male, player.getUUID(), male.getRelationship())
                        : (
                           villager instanceof FemaleVillagerEntity female
                              ? VillagerRelationshipData.getRelationshipForPlayer(female, player.getUUID(), female.getRelationship())
                              : 0.5F
                        );
                     float goldenRelationship = villager instanceof MaleVillagerEntity male
                        ? VillagerRelationshipData.getGoldenRelationshipForPlayer(male, player.getUUID(), male.getGoldenRelationship())
                        : (
                           villager instanceof FemaleVillagerEntity female
                              ? VillagerRelationshipData.getGoldenRelationshipForPlayer(female, player.getUUID(), female.getGoldenRelationship())
                              : 0.0F
                        );
                     ModNetworking.sendToClient(new RelationshipSyncPacket(villager.getId(), relationship, goldenRelationship), player);
                     String introLine = DialogueManager.getRandomLine("Intro", villager, player, true);
                     ModNetworking.sendToClient(new DialogueResultPacket(villager.getId(), "Intro", true, introLine), player);
                     if (villager instanceof FamilyVillager familyVillager) {
                        String lineKey = SpouseCookingHandler.tryServeMeal(villager, familyVillager, player);
                        if (lineKey != null && !lineKey.isEmpty()) {
                           ModNetworking.sendToClient(new SpouseCookingLinePacket(villager.getId(), lineKey), player);
                        }
                     }
                  } else {
                     DialogueManager.endDialogue(villager, player);
                  }
               }
            }
         
   }
}
