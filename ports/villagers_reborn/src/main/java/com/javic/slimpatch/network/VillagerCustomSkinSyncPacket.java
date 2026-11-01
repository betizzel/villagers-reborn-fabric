package com.javic.slimpatch.network;

import com.javic.slimpatch.client.ClientSkinTextureHandler;
import com.mojang.logging.LogUtils;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import org.slf4j.Logger;

public class VillagerCustomSkinSyncPacket {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final int entityId;
   private final UUID villagerUuid;
   private final String savedSkinInput;
   private final byte[] pngData;
   private final boolean clear;

   public VillagerCustomSkinSyncPacket(int entityId, UUID villagerUuid, String savedSkinInput, byte[] pngData, boolean clear) {
      this.entityId = entityId;
      this.villagerUuid = villagerUuid;
      this.savedSkinInput = savedSkinInput;
      this.pngData = pngData;
      this.clear = clear;
   }

   public static void encode(VillagerCustomSkinSyncPacket packet, FriendlyByteBuf buf) {
      buf.writeVarInt(packet.entityId);
      buf.writeUUID(packet.villagerUuid);
      buf.writeUtf(packet.savedSkinInput == null ? "" : packet.savedSkinInput);
      buf.writeBoolean(packet.clear);
      buf.writeByteArray(packet.pngData == null ? new byte[0] : packet.pngData);
   }

   public static VillagerCustomSkinSyncPacket decode(FriendlyByteBuf buf) {
      int entityId = buf.readVarInt();
      UUID villagerUuid = buf.readUUID();
      String savedSkinInput = buf.readUtf();
      boolean clear = buf.readBoolean();
      byte[] pngData = buf.readByteArray();
      return new VillagerCustomSkinSyncPacket(entityId, villagerUuid, savedSkinInput, pngData, clear);
   }

   public static void handle(VillagerCustomSkinSyncPacket msg) {
applyClient(msg);
   }

   private static void applyClient(VillagerCustomSkinSyncPacket msg) {
      try {
         ClientSkinTextureHandler.applySyncedSkin(msg.entityId, msg.villagerUuid, msg.savedSkinInput, msg.pngData, msg.clear);
      } catch (Exception e) {
         LOGGER.error("Failed to apply synced custom skin for villager {}", msg.villagerUuid, e);
      }
   }
}
