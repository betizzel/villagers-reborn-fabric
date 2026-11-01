package com.javic.slimpatch.network;

import com.javic.slimpatch.dialogue.DialogueManager;
import com.javic.slimpatch.entity.FemaleVillagerEntity;
import com.javic.slimpatch.entity.MaleVillagerEntity;
import com.javic.slimpatch.entity.VillagerCooldownData;
import com.javic.slimpatch.entity.VillagerFamilyData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;

public class RelationshipPacket {
   private static final double MAX_RELATIONSHIP_DISTANCE_SQR = 64.0;
   private final int entityId;
   private final String option;

   public RelationshipPacket(int entityId, String option) {
      this.entityId = entityId;
      this.option = option;
   }

   public static void encode(RelationshipPacket packet, FriendlyByteBuf buf) {
      buf.writeVarInt(packet.entityId);
      buf.writeUtf(packet.option);
   }

   public static RelationshipPacket decode(FriendlyByteBuf buf) {
      return new RelationshipPacket(buf.readVarInt(), buf.readUtf());
   }

   public static void handle(RelationshipPacket msg, ServerPlayer player) {

         if (player != null) {
            Entity entity = player.serverLevel().getEntity(msg.entityId);
            if (entity instanceof Villager villager && (entity instanceof MaleVillagerEntity || entity instanceof FemaleVillagerEntity) && entity.isAlive()) {
               if (!(player.distanceToSqr(villager) > 64.0)) {
                  if (player.getUUID().equals(DialogueManager.getDialoguePlayer(villager))) {
                     if (isSupportedOption(msg.option)) {
                        if (!VillagerCooldownData.isDialogueOptionOnCooldown(villager, player.getUUID(), msg.option)) {
                           if (!msg.option.equalsIgnoreCase("Flirt") || VillagerFamilyData.canUseRomanticInteraction(villager, player)) {
                              boolean success;
                              if (entity instanceof MaleVillagerEntity male) {
                                 success = DialogueManager.calculateSuccess(male.getPersonality(), msg.option);
                                 male.applyRelationshipChange(player, msg.option, success);
                              } else {
                                 if (!(entity instanceof FemaleVillagerEntity female)) {
                                    return;
                                 }

                                 success = DialogueManager.calculateSuccess(female.getPersonality(), msg.option);
                                 female.applyRelationshipChange(player, msg.option, success);
                              }

                              String line = DialogueManager.getRandomLine(msg.option, villager, player, success);
                              ModNetworking.sendToClient(new DialogueResultPacket(villager.getId(), msg.option, success, line), player);
                           }
                        }
                     }
                  }
               }
            }
         }
      
   }

   private static boolean isSupportedOption(String option) {
      return "Friendly".equalsIgnoreCase(option) || "Mean".equalsIgnoreCase(option) || "Joke".equalsIgnoreCase(option) || "Flirt".equalsIgnoreCase(option);
   }
}
