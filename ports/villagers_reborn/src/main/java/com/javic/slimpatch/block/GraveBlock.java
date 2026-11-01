package com.javic.slimpatch.block;

import com.javic.slimpatch.blockentity.GraveBlockEntity;
import com.javic.slimpatch.familytree.FamilyTreePortraitPayload;
import com.javic.slimpatch.graves.GraveSavedData;
import com.javic.slimpatch.graves.VillagerDeathRecord;
import com.javic.slimpatch.graves.VillagerResurrectionService;
import com.javic.slimpatch.item.GraveBlockItem;
import com.javic.slimpatch.item.ModItems;
import com.javic.slimpatch.network.ModNetworking;
import com.javic.slimpatch.network.OpenGraveScreenPacket;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.slf4j.Logger;

public class GraveBlock extends Block implements EntityBlock {
   private static final Logger LOGGER = LogUtils.getLogger();

   public GraveBlock(Properties properties) { super(properties); }

   @Override
   public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

   @Override
   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new GraveBlockEntity(pos, state); }

   @Override
   public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
      BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
      return blockEntity instanceof GraveBlockEntity grave && grave.getRecordId() != null
         ? List.of(GraveBlockItem.createStackWithRecord(this, grave.getRecordId(), grave.getCachedDisplayName()))
         : super.getDrops(state, params);
   }

   @Override
   public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
      super.setPlacedBy(level, pos, state, placer, stack);
      if (!level.isClientSide && level.getBlockEntity(pos) instanceof GraveBlockEntity grave && grave.getRecordId() != null) {
         GraveSavedData data = GraveSavedData.get(level.getServer());
         VillagerDeathRecord record = data.getRecord(grave.getRecordId());
         if (record == null) {
            grave.setRecordId(null);
            grave.setCachedDisplayName("");
         } else {
            if (!record.getDisplayName().isEmpty()) grave.setCachedDisplayName(record.getDisplayName());
            if (record.isGravePlaced() && !this.isOfficialGraveMissing(level, record, pos)) {
               grave.setRecordId(null);
               grave.setCachedDisplayName("");
            } else {
               record.setGravePlaced(true);
               record.setGraveDimension(level.dimension().location().toString());
               record.setGraveX(pos.getX());
               record.setGraveY(pos.getY());
               record.setGraveZ(pos.getZ());
               data.putRecord(record);
            }
         }
      }
   }

   @Override
   public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
      if (!state.is(newState.getBlock()) && !level.isClientSide
         && level.getBlockEntity(pos) instanceof GraveBlockEntity grave && grave.getRecordId() != null) {
         GraveSavedData data = GraveSavedData.get(level.getServer());
         VillagerDeathRecord record = data.getRecord(grave.getRecordId());
         if (record != null && record.isGravePlaced() && level.dimension().location().toString().equals(record.getGraveDimension())
            && pos.getX() == record.getGraveX() && pos.getY() == record.getGraveY() && pos.getZ() == record.getGraveZ()) {
            record.setGravePlaced(false);
            data.putRecord(record);
         }
      }
      super.onRemove(state, level, pos, newState, movedByPiston);
   }

   @Override
   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
      if (level.isClientSide) return InteractionResult.SUCCESS;
      if (player instanceof ServerPlayer serverPlayer) {
         BlockEntity blockEntity = level.getBlockEntity(pos);
         if (blockEntity instanceof GraveBlockEntity grave) {
            if (grave.getRecordId() == null) {
               LOGGER.warn("Grave interaction failed because recordId was null: pos={}, level={}, cachedDisplayName={}", pos, level.dimension().location(), grave.getCachedDisplayName());
               serverPlayer.displayClientMessage(Component.translatable("gui.slimpatch.grave.invalid"), true);
               return InteractionResult.CONSUME;
            }
            VillagerDeathRecord record = GraveSavedData.get(serverPlayer.serverLevel().getServer()).getRecord(grave.getRecordId());
            if (record == null) {
               LOGGER.warn("Grave interaction failed because no death record was found: recordId={}, pos={}, level={}", grave.getRecordId(), pos, level.dimension().location());
               serverPlayer.displayClientMessage(Component.translatable("gui.slimpatch.grave.invalid"), true);
               return InteractionResult.CONSUME;
            }
            ItemStack mainHand = player.getMainHandItem();
            if (mainHand.is(ModItems.SOUL_GEM)) {
               VillagerResurrectionService.tryResurrect(serverPlayer, serverPlayer.serverLevel(), record, mainHand, pos);
            } else {
               ModNetworking.sendToClient(new OpenGraveScreenPacket(record.getRecordId(), record.getDisplayName(), record.getGender(), record.getAgeStage(), record.getEntityKind(), this.createPortrait(record)), serverPlayer);
            }
            return InteractionResult.CONSUME;
         }
         LOGGER.warn("Grave interaction failed because no GraveBlockEntity was found: pos={}, level={}, blockEntityClass={}", pos, level.dimension().location(), blockEntity == null ? "null" : blockEntity.getClass().getName());
         serverPlayer.displayClientMessage(Component.translatable("gui.slimpatch.grave.invalid"), true);
      }
      return InteractionResult.CONSUME;
   }

   private FamilyTreePortraitPayload createPortrait(VillagerDeathRecord record) {
      String skinResourcePath = record.getSkinResourcePath();
      FamilyTreePortraitPayload.PortraitType portraitType = FamilyTreePortraitPayload.PortraitType.VILLAGER_DEFAULT;
      if (!skinResourcePath.isEmpty()) {
         if (ResourceLocation.tryParse(skinResourcePath) != null || new File(skinResourcePath).isFile()) {
            portraitType = FamilyTreePortraitPayload.PortraitType.VILLAGER_RESOURCE;
         }
      }
      if (record.hasCustomSkin() && !record.getSavedSkinInput().isEmpty() && !record.getSkinResourcePath().isEmpty()) {
         portraitType = FamilyTreePortraitPayload.PortraitType.VILLAGER_RESOURCE;
      }
      return new FamilyTreePortraitPayload(portraitType, record.getSkinIndex(), record.getSkinResourcePath(), record.getSavedSkinInput(), true, new byte[0]);
   }

   private boolean isOfficialGraveMissing(Level level, VillagerDeathRecord record, BlockPos currentPos) {
      if (record.isGravePlaced() && !record.getGraveDimension().isEmpty()) {
         if (level.dimension().location().toString().equals(record.getGraveDimension()) && currentPos.getX() == record.getGraveX()
            && currentPos.getY() == record.getGraveY() && currentPos.getZ() == record.getGraveZ()) return true;
         ResourceLocation dimensionId = ResourceLocation.tryParse(record.getGraveDimension());
         if (dimensionId != null && level.getServer() != null) {
            ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimensionId);
            Level officialLevel = level.getServer().getLevel(dimensionKey);
            if (officialLevel == null) return false;
            BlockPos officialPos = new BlockPos(record.getGraveX(), record.getGraveY(), record.getGraveZ());
            return officialLevel.hasChunkAt(officialPos) && !(officialLevel.getBlockEntity(officialPos) instanceof GraveBlockEntity grave
               && record.getRecordId().equals(grave.getRecordId()));
         }
         return false;
      }
      return true;
   }
}
