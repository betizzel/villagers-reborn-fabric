package com.javic.slimpatch.network;

import com.javic.slimpatch.dialogue.DialogueManager;
import com.javic.slimpatch.entity.FemaleVillagerEntity;
import com.javic.slimpatch.entity.MaleVillagerEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;

public class GiftPacket {
   private static final double MAX_GIFT_DISTANCE_SQR = 64.0;
   private final int entityId;

   public GiftPacket(int entityId) {
      this.entityId = entityId;
   }

   public static void encode(GiftPacket packet, FriendlyByteBuf buf) {
      buf.writeVarInt(packet.entityId);
   }

   public static GiftPacket decode(FriendlyByteBuf buf) {
      return new GiftPacket(buf.readVarInt());
   }

   public static void handle(GiftPacket msg, ServerPlayer player) {

         if (player != null) {
            Entity entity = player.serverLevel().getEntity(msg.entityId);
            if (entity instanceof Villager villager && entity.isAlive()) {
               if (!(player.distanceToSqr(villager) > 64.0)) {
                  if (player.getUUID().equals(DialogueManager.getDialoguePlayer(villager))) {
                     if (entity instanceof MaleVillagerEntity male) {
                        male.giveGiftToPlayer(player);
                     } else if (entity instanceof FemaleVillagerEntity female) {
                        female.giveGiftToPlayer(player);
                     }
                  }
               }
            }
         }
      
   }
}
