package com.javic.slimpatch.graves;

import com.javic.slimpatch.fabric.PersistentEntityData;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.npc.Villager;

public final class GraveIdentityResolver {
   public static final String HISTORICAL_ENTITY_UUID_TAG = "SlimPatchHistoricalEntityUuid";

   private GraveIdentityResolver() {}

   public static UUID resolveHistoricalUuid(Villager villager) {
      if (villager == null) return null;
      return PersistentEntityData.get(villager).hasUUID(HISTORICAL_ENTITY_UUID_TAG)
         ? PersistentEntityData.get(villager).getUUID(HISTORICAL_ENTITY_UUID_TAG)
         : resolveHistoricalUuid(villager.getServer(), villager.getUUID());
   }

   public static UUID resolveHistoricalUuid(MinecraftServer server, UUID uuid) {
      if (uuid == null) return null;
      if (server == null) return uuid;
      VillagerDeathRecord record = GraveSavedData.get(server).getRecordByRestoredLivingUuid(uuid);
      return record != null && record.getHistoricalEntityUuid() != null ? record.getHistoricalEntityUuid() : uuid;
   }
}
