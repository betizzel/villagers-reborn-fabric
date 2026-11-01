package com.javic.slimpatch.blockentity;

import com.javic.slimpatch.ModBlockEntities;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class GraveBlockEntity extends BlockEntity {
   public static final String RECORD_ID_TAG = "RecordId";
   public static final String CACHED_DISPLAY_NAME_TAG = "CachedDisplayName";
   private UUID recordId;
   private String cachedDisplayName = "";

   public GraveBlockEntity(BlockPos pos, BlockState blockState) {
      super(ModBlockEntities.GRAVE, pos, blockState);
   }

   public UUID getRecordId() {
      return this.recordId;
   }

   public void setRecordId(UUID recordId) {
      this.recordId = recordId;
      this.setChanged();
   }

   public String getCachedDisplayName() {
      return this.cachedDisplayName;
   }

   public void setCachedDisplayName(String cachedDisplayName) {
      this.cachedDisplayName = cachedDisplayName == null ? "" : cachedDisplayName;
      this.setChanged();
   }

   @Override
   public void load(CompoundTag tag) {
      super.load(tag);
      this.recordId = tag.hasUUID("RecordId") ? tag.getUUID("RecordId") : null;
      this.cachedDisplayName = tag.contains("CachedDisplayName") ? tag.getString("CachedDisplayName") : "";
   }

   @Override
   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      if (this.recordId != null) {
         tag.putUUID("RecordId", this.recordId);
      }
      if (!this.cachedDisplayName.isEmpty()) {
         tag.putString("CachedDisplayName", this.cachedDisplayName);
      }
   }
}
