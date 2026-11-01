package com.javic.slimpatch.network;

import com.javic.slimpatch.entity.AbstractRomanceVillagerEntity;
import com.javic.slimpatch.entity.FamilyTreeViewHandler;
import com.javic.slimpatch.familytree.FamilyTreeGraphBuilder;
import com.javic.slimpatch.familytree.FamilyTreeTracker;
import com.javic.slimpatch.graves.GraveIdentityResolver;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class RequestFamilyTreePacket {
   private static final double MAX_DISTANCE_SQR = 64.0;
   private final int villagerEntityId;

   public RequestFamilyTreePacket(int villagerEntityId) {
      this.villagerEntityId = villagerEntityId;
   }

   public static void encode(RequestFamilyTreePacket packet, FriendlyByteBuf buf) {
      buf.writeVarInt(packet.villagerEntityId);
   }

   public static RequestFamilyTreePacket decode(FriendlyByteBuf buf) {
      return new RequestFamilyTreePacket(buf.readVarInt());
   }

   public static void handle(RequestFamilyTreePacket msg, ServerPlayer player) {

            if (player != null) {
               if (player.serverLevel().getEntity(msg.villagerEntityId) instanceof AbstractRomanceVillagerEntity villager && villager.isAlive()) {
                  if (!(player.distanceToSqr(villager) > 64.0)) {
                     FamilyTreeViewHandler.beginViewing(player, villager);
                     FamilyTreeTracker.upsertVillager(player.serverLevel().getServer(), villager);
                     ModNetworking.sendToClient(
                        new FamilyTreeDataPacket(
                           FamilyTreeGraphBuilder.build(player.serverLevel().getServer(), GraveIdentityResolver.resolveHistoricalUuid(villager))
                        ),
                        player
                     );
                  }
               }
            }
         
   }
}
