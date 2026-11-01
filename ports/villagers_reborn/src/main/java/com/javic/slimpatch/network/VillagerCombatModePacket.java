package com.javic.slimpatch.network;

import com.javic.slimpatch.entity.AbstractRomanceVillagerEntity;
import com.javic.slimpatch.entity.VillagerCombatMode;
import com.javic.slimpatch.entity.VillagerCommandHandler;
import com.javic.slimpatch.menu.VillagerEquipmentMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class VillagerCombatModePacket {
   private static final double MAX_COMMAND_DISTANCE_SQR = 64.0;
   private final int entityId;
   private final VillagerCombatMode combatMode;

   public VillagerCombatModePacket(int entityId, VillagerCombatMode combatMode) {
      this.entityId = entityId;
      this.combatMode = combatMode == null ? VillagerCombatMode.DEFENSIVE : combatMode;
   }

   public static void encode(VillagerCombatModePacket packet, FriendlyByteBuf buf) {
      buf.writeVarInt(packet.entityId);
      buf.writeEnum(packet.combatMode);
   }

   public static VillagerCombatModePacket decode(FriendlyByteBuf buf) {
      return new VillagerCombatModePacket(buf.readVarInt(), (VillagerCombatMode)buf.readEnum(VillagerCombatMode.class));
   }

   public static void handle(VillagerCombatModePacket msg, ServerPlayer player) {

         if (player != null && player.containerMenu instanceof VillagerEquipmentMenu equipmentMenu) {
            if (equipmentMenu.isForVillager(msg.entityId) && equipmentMenu.stillValid(player)) {
               if (player.serverLevel().getEntity(msg.entityId) instanceof AbstractRomanceVillagerEntity villager && villager.isAlive()) {
                  if (!(player.distanceToSqr(villager) > 64.0)) {
                     if (VillagerCommandHandler.canUseProtectedAction(villager, villager, player)) {
                        villager.setCombatMode(msg.combatMode);
                     }
                  }
               }
            }
         }
      
   }
}
