package com.javic.slimpatch.network;

import com.javic.slimpatch.entity.AbstractRomanceVillagerEntity;
import com.javic.slimpatch.entity.VillagerCommandHandler;
import com.javic.slimpatch.familytree.FamilyTreeTracker;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class VillagerEditDataPacket {
   private static final double MAX_EDIT_DISTANCE_SQR = 64.0;
   private static final int MAX_SKIN_FIELD_LENGTH = 500;
   private final UUID villagerId;
   private final String savedSkinInput;
   private final String customSkinPath;
   private final int height;
   private final int width;

   public VillagerEditDataPacket(UUID villagerId, String savedSkinInput, String customSkinPath, int height, int width) {
      this.villagerId = villagerId;
      this.savedSkinInput = savedSkinInput == null ? "" : savedSkinInput;
      this.customSkinPath = customSkinPath == null ? "" : customSkinPath;
      this.height = height;
      this.width = width;
   }

   public static void encode(VillagerEditDataPacket packet, FriendlyByteBuf buf) {
      buf.writeUUID(packet.villagerId);
      buf.writeUtf(packet.savedSkinInput, 500);
      buf.writeUtf(packet.customSkinPath, 500);
      buf.writeInt(packet.height);
      buf.writeInt(packet.width);
   }

   public static VillagerEditDataPacket decode(FriendlyByteBuf buf) {
      return new VillagerEditDataPacket(buf.readUUID(), buf.readUtf(500), buf.readUtf(500), buf.readInt(), buf.readInt());
   }

   public static void handle(VillagerEditDataPacket msg, ServerPlayer player) {

         if (player != null) {
            if (player.serverLevel().getEntity(msg.villagerId) instanceof AbstractRomanceVillagerEntity villager && villager.isAlive()) {
               if (!(player.distanceToSqr(villager) > 64.0)) {
                  if (VillagerCommandHandler.canUseProtectedAction(villager, villager, player)) {
                     boolean allowCustomSkin = !player.serverLevel().getServer().isDedicatedServer() && !player.serverLevel().getServer().isPublished();
                     if (allowCustomSkin) {
                        villager.setSavedSkinInput(msg.savedSkinInput);
                        villager.setCustomSkinPath(msg.customSkinPath);
                     }

                     villager.setVisualHeight(msg.height);
                     villager.setVisualWidth(msg.width);
                     FamilyTreeTracker.onVillagerSkinChanged(player.serverLevel().getServer(), villager);
                  }
               }
            }
         }
      
   }
}
