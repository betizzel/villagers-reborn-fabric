package com.javic.slimpatch.network;

import com.javic.slimpatch.entity.AbstractRomanceVillagerEntity;
import com.javic.slimpatch.entity.VillagerCommandHandler;
import com.javic.slimpatch.entity.VillagerEquipmentHolder;
import com.javic.slimpatch.menu.VillagerEquipmentMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;

public class VillagerEquipmentPacket {
   private static final double MAX_OPEN_DISTANCE_SQR = 64.0;
   private final int entityId;

   public VillagerEquipmentPacket(int entityId) {
      this.entityId = entityId;
   }

   public static void encode(VillagerEquipmentPacket packet, FriendlyByteBuf buf) {
      buf.writeVarInt(packet.entityId);
   }

   public static VillagerEquipmentPacket decode(FriendlyByteBuf buf) {
      return new VillagerEquipmentPacket(buf.readVarInt());
   }

   public static void handle(VillagerEquipmentPacket msg, ServerPlayer player) {

            if (player != null) {
               Entity entity = player.serverLevel().getEntity(msg.entityId);
               if (entity instanceof AbstractRomanceVillagerEntity villager && entity instanceof VillagerEquipmentHolder equipmentHolder) {
                  if (villager.isAlive() && !(player.distanceToSqr(villager) > 64.0)) {
                     if (VillagerCommandHandler.canUseProtectedAction(villager, villager, player)) {
                        player.openMenu(
                           new SimpleMenuProvider(
                              (containerId, playerInventory, serverPlayer) -> new VillagerEquipmentMenu(
                                 containerId, playerInventory, villager, equipmentHolder.getEquipmentInventory()
                              ),
                              villager.getDisplayName()
                           )
                        );
                     }
                  }
               }
            }
         
   }
}
