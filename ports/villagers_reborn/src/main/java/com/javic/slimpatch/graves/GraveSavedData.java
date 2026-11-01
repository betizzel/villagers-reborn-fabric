package com.javic.slimpatch.graves;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public class GraveSavedData extends SavedData {
   private static final String DATA_ID = "slimpatch_graves";
   private final Map<UUID, VillagerDeathRecord> recordsById = new LinkedHashMap<>();
   private final Map<UUID, UUID> recordIdsByHistoricalEntityUuid = new LinkedHashMap<>();
   private final Map<UUID, UUID> recordIdsByRestoredLivingUuid = new LinkedHashMap<>();

   public static GraveSavedData get(MinecraftServer server) {
      return server.overworld().getDataStorage().computeIfAbsent(GraveSavedData::load, GraveSavedData::new, DATA_ID);
   }

   public static GraveSavedData load(CompoundTag tag) {
      GraveSavedData data = new GraveSavedData();
      ListTag records = tag.getList("Records", 10);
      for (int i = 0; i < records.size(); i++) {
         VillagerDeathRecord record = VillagerDeathRecord.load(records.getCompound(i));
         if (record.getRecordId() != null) {
            data.recordsById.put(record.getRecordId(), record);
            if (record.getHistoricalEntityUuid() != null) data.recordIdsByHistoricalEntityUuid.put(record.getHistoricalEntityUuid(), record.getRecordId());
            if (record.getRestoredLivingUuid() != null) data.recordIdsByRestoredLivingUuid.put(record.getRestoredLivingUuid(), record.getRecordId());
         }
      }
      return data;
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      ListTag records = new ListTag();
      for (VillagerDeathRecord record : this.recordsById.values()) records.add(record.save());
      tag.put("Records", records);
      return tag;
   }

   public VillagerDeathRecord getRecord(UUID recordId) {
      return recordId == null ? null : this.recordsById.get(recordId);
   }

   public VillagerDeathRecord getRecordByHistoricalEntityUuid(UUID historicalEntityUuid) {
      if (historicalEntityUuid == null) return null;
      UUID recordId = this.recordIdsByHistoricalEntityUuid.get(historicalEntityUuid);
      return recordId == null ? null : this.recordsById.get(recordId);
   }

   public VillagerDeathRecord getRecordByRestoredLivingUuid(UUID restoredLivingUuid) {
      if (restoredLivingUuid == null) return null;
      UUID recordId = this.recordIdsByRestoredLivingUuid.get(restoredLivingUuid);
      return recordId == null ? null : this.recordsById.get(recordId);
   }

   public void putRecord(VillagerDeathRecord record) {
      if (record != null && record.getRecordId() != null) {
         this.recordsById.put(record.getRecordId(), record);
         if (record.getHistoricalEntityUuid() != null) this.recordIdsByHistoricalEntityUuid.put(record.getHistoricalEntityUuid(), record.getRecordId());
         if (record.getRestoredLivingUuid() != null) this.recordIdsByRestoredLivingUuid.put(record.getRestoredLivingUuid(), record.getRecordId());
         this.setDirty();
      }
   }

   public void removeRecord(UUID recordId) {
      if (recordId != null) {
         VillagerDeathRecord removed = this.recordsById.remove(recordId);
         if (removed != null) {
            if (removed.getHistoricalEntityUuid() != null) this.recordIdsByHistoricalEntityUuid.remove(removed.getHistoricalEntityUuid());
            if (removed.getRestoredLivingUuid() != null) this.recordIdsByRestoredLivingUuid.remove(removed.getRestoredLivingUuid());
            this.setDirty();
         }
      }
   }
}
