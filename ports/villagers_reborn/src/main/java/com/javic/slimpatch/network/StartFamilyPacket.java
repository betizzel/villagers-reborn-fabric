package com.javic.slimpatch.network;

import com.javic.slimpatch.Config;
import com.javic.slimpatch.entity.AbstractRomanceVillagerEntity;
import com.javic.slimpatch.entity.FamilyCutsceneServerHandler;
import com.javic.slimpatch.entity.FamilyPregnancyHandler;
import com.javic.slimpatch.entity.FamilyVillager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class StartFamilyPacket {
   private static final double MAX_FAMILY_DISTANCE_SQR = 64.0;
   private final int entityId;

   public StartFamilyPacket(int entityId) {
      this.entityId = entityId;
   }

   public static void encode(StartFamilyPacket packet, FriendlyByteBuf buf) {
      buf.writeVarInt(packet.entityId);
   }

   public static StartFamilyPacket decode(FriendlyByteBuf buf) {
      return new StartFamilyPacket(buf.readVarInt());
   }

   public static void handle(StartFamilyPacket msg, ServerPlayer player) {

         if (player != null) {
            Entity entity = player.serverLevel().getEntity(msg.entityId);
            if (entity instanceof AbstractRomanceVillagerEntity villager && entity instanceof FamilyVillager familyVillager && villager.isAlive()) {
               if (!(player.distanceToSqr(villager) > 64.0)) {
                  FamilyPregnancyHandler.Result result = FamilyPregnancyHandler.startFamily(villager, familyVillager, player);
                  if (result.success()) {
                     if ((Boolean)Config.ENABLE_FAMILY_CUTSCENE.get()) {
                        FamilyCutsceneServerHandler.start(villager, player);
                     }

                     ModNetworking.sendToClient(new StartFamilyCutscenePacket(villager.getId(), villager.getName().getString()), player);
                  } else {
                     player.displayClientMessage(Component.translatable(result.messageKey()), true);
                  }
               }
            }
         }
      
   }
}
