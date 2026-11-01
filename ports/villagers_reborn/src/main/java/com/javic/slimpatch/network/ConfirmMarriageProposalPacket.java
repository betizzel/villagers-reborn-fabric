package com.javic.slimpatch.network;

import com.javic.slimpatch.entity.AbstractRomanceVillagerEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class ConfirmMarriageProposalPacket {
   private static final double MAX_MARRIAGE_DISTANCE_SQR = 64.0;
   private final int entityId;

   public ConfirmMarriageProposalPacket(int entityId) {
      this.entityId = entityId;
   }

   public static void encode(ConfirmMarriageProposalPacket packet, FriendlyByteBuf buf) {
      buf.writeVarInt(packet.entityId);
   }

   public static ConfirmMarriageProposalPacket decode(FriendlyByteBuf buf) {
      return new ConfirmMarriageProposalPacket(buf.readVarInt());
   }

   public static void handle(ConfirmMarriageProposalPacket msg, ServerPlayer player) {

         if (player != null) {
            Entity entity = player.serverLevel().getEntity(msg.entityId);
            if (entity instanceof AbstractRomanceVillagerEntity villager && entity.isAlive()) {
               if (!(player.distanceToSqr(villager) > 64.0)) {
                  villager.confirmMarriageProposal(player);
               }
            }
         }
      
   }
}
