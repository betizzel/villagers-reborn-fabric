package com.javic.slimpatch.network;

import com.javic.slimpatch.entity.AbstractRomanceVillagerEntity;
import com.javic.slimpatch.entity.VillagerCommandHandler;
import com.javic.slimpatch.familytree.FamilyTreeTracker;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class VillagerNamePacket {
   private static final double MAX_NAME_DISTANCE_SQR = 64.0;
   private static final int MAX_NAME_LENGTH = 64;
   private final UUID villagerId;
   private final String newName;

   public VillagerNamePacket(UUID villagerId, String newName) {
      this.villagerId = villagerId;
      this.newName = newName == null ? "" : newName;
   }

   public static void encode(VillagerNamePacket packet, FriendlyByteBuf buf) {
      buf.writeUUID(packet.villagerId);
      buf.writeUtf(packet.newName, 64);
   }

   public static VillagerNamePacket decode(FriendlyByteBuf buf) {
      return new VillagerNamePacket(buf.readUUID(), buf.readUtf(64));
   }

   public static void handle(VillagerNamePacket msg, ServerPlayer player) {

         if (player != null) {
            if (player.serverLevel().getEntity(msg.villagerId) instanceof AbstractRomanceVillagerEntity villager && villager.isAlive()) {
               if (!(player.distanceToSqr(villager) > 64.0)) {
                  if (VillagerCommandHandler.canUseProtectedAction(villager, villager, player)) {
                     String sanitizedName = msg.newName == null ? "" : msg.newName.trim();
                     if (!sanitizedName.isEmpty()) {
                        if (sanitizedName.length() > 64) {
                           sanitizedName = sanitizedName.substring(0, 64);
                        }

                        villager.setCustomName(Component.literal(sanitizedName));
                        villager.setCustomNameVisible(true);
                        com.javic.slimpatch.fabric.PersistentEntityData.get(villager).putString("SavedName", sanitizedName);
                        FamilyTreeTracker.onVillagerNameChanged(player.serverLevel().getServer(), villager);
                     }
                  }
               }
            }
         }
      
   }
}
