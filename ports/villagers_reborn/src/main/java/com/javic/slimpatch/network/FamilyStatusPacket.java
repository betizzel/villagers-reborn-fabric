package com.javic.slimpatch.network;

import com.javic.slimpatch.dialogue.DialogueManager;
import com.javic.slimpatch.entity.AbstractRomanceVillagerEntity;
import com.javic.slimpatch.entity.FamilyVillager;
import com.javic.slimpatch.entity.VillagerRelationshipStage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class FamilyStatusPacket {
   private static final double MAX_FAMILY_DISTANCE_SQR = 64.0;
   private final int entityId;

   public FamilyStatusPacket(int entityId) {
      this.entityId = entityId;
   }

   public static void encode(FamilyStatusPacket packet, FriendlyByteBuf buf) {
      buf.writeVarInt(packet.entityId);
   }

   public static FamilyStatusPacket decode(FriendlyByteBuf buf) {
      return new FamilyStatusPacket(buf.readVarInt());
   }

   public static void handle(FamilyStatusPacket msg, ServerPlayer player) {

         if (player != null) {
            Entity entity = player.serverLevel().getEntity(msg.entityId);
            if (entity instanceof AbstractRomanceVillagerEntity villager && entity instanceof FamilyVillager familyVillager && villager.isAlive()) {
               if (!(player.distanceToSqr(villager) > 64.0)) {
                  if (player.getUUID().equals(DialogueManager.getDialoguePlayer(villager))) {
                     if (familyVillager.getRelationshipStage() == VillagerRelationshipStage.MARRIED) {
                        if (!player.getUUID().equals(familyVillager.getSpousePlayerUuid())) {
                           player.displayClientMessage(Component.translatable("slimpatch.message.family_not_spouse"), true);
                        } else if (familyVillager.isExpectingChild()) {
                           long remainingTicks = familyVillager.getExpectingEndsAt() - villager.level().getGameTime();
                           if (remainingTicks <= 0L) {
                              ModNetworking.sendToClient(new FamilyBirthReadyPacket(villager.getId(), "slimpatch.dialogue.family.birth_ready"), player);
                           } else {
                              String lineKey;
                              if (remainingTicks > 16000L) {
                                 lineKey = "slimpatch.dialogue.family.status_early";
                              } else if (remainingTicks > 8000L) {
                                 lineKey = "slimpatch.dialogue.family.status_mid";
                              } else {
                                 lineKey = "slimpatch.dialogue.family.status_soon";
                              }

                              ModNetworking.sendToClient(new FamilyDialogueLinePacket(villager.getId(), lineKey), player);
                           }
                        }
                     }
                  }
               }
            }
         }
      
   }
}
