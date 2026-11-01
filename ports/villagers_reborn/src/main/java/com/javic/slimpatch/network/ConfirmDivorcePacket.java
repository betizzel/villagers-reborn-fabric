package com.javic.slimpatch.network;

import com.javic.slimpatch.entity.AbstractRomanceVillagerEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class ConfirmDivorcePacket {
   private static final double MAX_DIVORCE_DISTANCE_SQR = 64.0;
   private final int entityId;

   public ConfirmDivorcePacket(int entityId) {
      this.entityId = entityId;
   }

   public static void encode(ConfirmDivorcePacket packet, FriendlyByteBuf buf) {
      buf.writeVarInt(packet.entityId);
   }

   public static ConfirmDivorcePacket decode(FriendlyByteBuf buf) {
      return new ConfirmDivorcePacket(buf.readVarInt());
   }

   public static void handle(ConfirmDivorcePacket msg, ServerPlayer player) {

         if (player != null) {
            Entity entity = player.serverLevel().getEntity(msg.entityId);
            if (entity instanceof AbstractRomanceVillagerEntity villager && entity.isAlive()) {
               if (!(player.distanceToSqr(villager) > 64.0)) {
                  villager.confirmDivorce(player);
               }
            }
         }
      
   }
}
