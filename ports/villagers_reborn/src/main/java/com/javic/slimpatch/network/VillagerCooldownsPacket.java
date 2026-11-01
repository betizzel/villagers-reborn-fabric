package com.javic.slimpatch.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.network.FriendlyByteBuf;

public class VillagerCooldownsPacket {
   private final UUID uuid;
   private final UUID playerUuid;
   private final int entityId;
   private final Map<String, Integer> cooldowns;
   private final long giftCooldownRemainingMs;

   public VillagerCooldownsPacket(UUID uuid, UUID playerUuid, int entityId, Map<String, Integer> cooldowns, long giftCooldownRemainingMs) {
      this.uuid = uuid;
      this.playerUuid = playerUuid;
      this.entityId = entityId;
      this.cooldowns = cooldowns;
      this.giftCooldownRemainingMs = giftCooldownRemainingMs;
   }

   public static void encode(VillagerCooldownsPacket packet, FriendlyByteBuf buf) {
      buf.writeUUID(packet.uuid);
      buf.writeUUID(packet.playerUuid);
      buf.writeVarInt(packet.entityId);
      buf.writeVarLong(packet.giftCooldownRemainingMs);
      buf.writeVarInt(packet.cooldowns.size());

      for (Entry<String, Integer> entry : packet.cooldowns.entrySet()) {
         buf.writeUtf(entry.getKey());
         buf.writeVarInt(entry.getValue());
      }
   }

   public static VillagerCooldownsPacket decode(FriendlyByteBuf buf) {
      UUID uuid = buf.readUUID();
      UUID playerUuid = buf.readUUID();
      int entityId = buf.readVarInt();
      long giftCooldownRemainingMs = buf.readVarLong();
      int size = ModNetworking.readCount(buf, Integer.MAX_VALUE);
      Map<String, Integer> cooldowns = new HashMap<>();

      for (int i = 0; i < size; i++) {
         cooldowns.put(buf.readUtf(), buf.readVarInt());
      }

      return new VillagerCooldownsPacket(uuid, playerUuid, entityId, cooldowns, giftCooldownRemainingMs);
   }

   public static void handle(VillagerCooldownsPacket msg) {
         VillagerCooldownsStorage.ensureLinked(msg.entityId, msg.uuid);
         VillagerCooldownsStorage.setCooldowns(msg.uuid, msg.playerUuid, msg.cooldowns);
         VillagerCooldownsStorage.setGiftCooldownRemaining(msg.uuid, msg.playerUuid, msg.giftCooldownRemainingMs);
      
   }
}
