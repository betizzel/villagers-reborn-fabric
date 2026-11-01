package com.javic.slimpatch.network;

import com.javic.slimpatch.entity.AbstractRomanceVillagerEntity;
import com.javic.slimpatch.entity.VillagerCommandHandler;
import com.javic.slimpatch.entity.VillagerFollowMode;
import com.javic.slimpatch.menu.VillagerEquipmentMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class VillagerFollowModePacket {
   private static final double MAX_COMMAND_DISTANCE_SQR = 64.0;
   private final int entityId;
   private final VillagerFollowMode followMode;

   public VillagerFollowModePacket(int entityId, VillagerFollowMode followMode) {
      this.entityId = entityId;
      this.followMode = followMode == null ? VillagerFollowMode.CLOSE : followMode;
   }

   public static void encode(VillagerFollowModePacket packet, FriendlyByteBuf buf) {
      buf.writeVarInt(packet.entityId);
      buf.writeEnum(packet.followMode);
   }

   public static VillagerFollowModePacket decode(FriendlyByteBuf buf) {
      return new VillagerFollowModePacket(buf.readVarInt(), (VillagerFollowMode)buf.readEnum(VillagerFollowMode.class));
   }

   public static void handle(VillagerFollowModePacket msg, ServerPlayer player) {

         if (player != null && player.containerMenu instanceof VillagerEquipmentMenu equipmentMenu) {
            if (equipmentMenu.isForVillager(msg.entityId) && equipmentMenu.stillValid(player)) {
               if (player.serverLevel().getEntity(msg.entityId) instanceof AbstractRomanceVillagerEntity villager && villager.isAlive()) {
                  if (!(player.distanceToSqr(villager) > 64.0)) {
                     if (VillagerCommandHandler.canUseProtectedAction(villager, villager, player)) {
                        villager.setFollowMode(msg.followMode);
                     }
                  }
               }
            }
         }
      
   }
}
