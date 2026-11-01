package com.javic.slimpatch.network;

import com.javic.slimpatch.entity.AbstractRomanceVillagerEntity;
import com.javic.slimpatch.entity.VillagerCommandHandler;
import com.javic.slimpatch.menu.VillagerEquipmentMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class VillagerMutePacket {
   private static final double MAX_COMMAND_DISTANCE_SQR = 64.0;
   private final int entityId;
   private final boolean muted;

   public VillagerMutePacket(int entityId, boolean muted) {
      this.entityId = entityId;
      this.muted = muted;
   }

   public static void encode(VillagerMutePacket packet, FriendlyByteBuf buf) {
      buf.writeVarInt(packet.entityId);
      buf.writeBoolean(packet.muted);
   }

   public static VillagerMutePacket decode(FriendlyByteBuf buf) {
      return new VillagerMutePacket(buf.readVarInt(), buf.readBoolean());
   }

   public static void handle(VillagerMutePacket msg, ServerPlayer player) {

         if (player != null && player.containerMenu instanceof VillagerEquipmentMenu equipmentMenu) {
            if (equipmentMenu.isForVillager(msg.entityId) && equipmentMenu.stillValid(player)) {
               if (player.serverLevel().getEntity(msg.entityId) instanceof AbstractRomanceVillagerEntity villager && villager.isAlive()) {
                  if (!(player.distanceToSqr(villager) > 64.0)) {
                     if (VillagerCommandHandler.canUseProtectedAction(villager, villager, player)) {
                        villager.setMuted(msg.muted);
                     }
                  }
               }
            }
         }
      
   }
}
