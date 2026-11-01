package com.javic.slimpatch.graves;

import com.javic.slimpatch.ModBlocks;
import com.javic.slimpatch.blockentity.GraveBlockEntity;
import com.javic.slimpatch.entity.CommandableVillager;
import com.javic.slimpatch.entity.FamilyVillager;
import com.javic.slimpatch.entity.FemaleVillagerEntity;
import com.javic.slimpatch.entity.HumanZombieVillagerEntity;
import com.javic.slimpatch.entity.MaleVillagerEntity;
import com.javic.slimpatch.entity.VillagerCommandHandler;
import com.javic.slimpatch.entity.VillagerCommandState;
import com.javic.slimpatch.entity.VillagerFamilyData;
import com.javic.slimpatch.entity.VillagerRelationshipData;
import com.javic.slimpatch.entity.VillagerRelationshipStage;
import com.javic.slimpatch.fabric.PersistentEntityData;
import com.javic.slimpatch.familytree.FamilyTreeTracker;
import com.javic.slimpatch.item.GraveBlockItem;
import com.javic.slimpatch.util.SkinPathHelper;
import com.mojang.logging.LogUtils;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import org.slf4j.Logger;

public final class GravePlacementHandler {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int LOCAL_PLACEMENT_RADIUS = 10;
   private static final int[] LOCAL_VERTICAL_OFFSETS = new int[]{1, -1, 2};
   private static final int[] SURFACE_FALLBACK_RADII = new int[]{16, 24, 32, 48, 64};

   private GravePlacementHandler() {
   }

   public static void handleZombieVillagerDeath(ServerLevel level, HumanZombieVillagerEntity zombieVillager) {
      if (level != null && zombieVillager != null) {
         CompoundTag savedData = PersistentEntityData.get(zombieVillager).getCompound("slimpatch_saved_data");
         if (!savedData.isEmpty() && shouldCreateDeathRecord(savedData)) {
            try {
               UUID historicalEntityUuid = savedData.hasUUID("SlimPatchHistoricalEntityUuid") ? savedData.getUUID("SlimPatchHistoricalEntityUuid") : null;
               if (historicalEntityUuid == null) {
                  return;
               }

               GraveSavedData data = GraveSavedData.get(level.getServer());
               VillagerDeathRecord existing = data.getRecordByHistoricalEntityUuid(historicalEntityUuid);
               if (existing != null && existing.getState() == VillagerDeathRecordState.ACTIVE) {
                  return;
               }

               if (existing != null) {
                  data.removeRecord(existing.getRecordId());
               }

               VillagerDeathRecord record = createZombieRecord(level, zombieVillager, savedData, historicalEntityUuid);
               handleGravePlacementOrFallback(level, zombieVillager.blockPosition(), record, historicalEntityUuid);
               data.putRecord(record);
               if (record.isGravePlaced()) {
                  notifyRelatedPlayers(level, record);
               }
            } catch (Exception e) {
               LOGGER.error("Failed to create villager grave record from zombie villager {}", zombieVillager.getUUID(), e);
            }
         }
      }
   }

   public static void clearGraveForHistoricalEntityUuid(ServerLevel level, UUID historicalEntityUuid) {
      if (level != null && historicalEntityUuid != null) {
         GraveSavedData data = GraveSavedData.get(level.getServer());
         VillagerDeathRecord record = data.getRecordByHistoricalEntityUuid(historicalEntityUuid);
         if (record != null) {
            if (record.isGravePlaced() && !record.getGraveDimension().isEmpty()) {
               ResourceLocation dimensionId = ResourceLocation.tryParse(record.getGraveDimension());
               if (dimensionId != null) {
                  ServerLevel graveLevel = level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
                  if (graveLevel != null) {
                     BlockPos gravePos = new BlockPos(record.getGraveX(), record.getGraveY(), record.getGraveZ());
                     if (graveLevel.hasChunkAt(gravePos)
                        && graveLevel.getBlockEntity(gravePos) instanceof GraveBlockEntity graveBlockEntity
                        && record.getRecordId().equals(graveBlockEntity.getRecordId())) {
                        graveLevel.removeBlock(gravePos, false);
                     }
                  }
               }
            }

            data.removeRecord(record.getRecordId());
         }
      }
   }

   public static void handleVillagerDeath(ServerLevel level, Villager villager) {
      if (level != null && villager != null && VillagerGraveEligibility.shouldCreateDeathRecord(villager)) {
         try {
            GraveSavedData data = GraveSavedData.get(level.getServer());
            UUID historicalEntityUuid = GraveIdentityResolver.resolveHistoricalUuid(villager);
            if (historicalEntityUuid == null) {
               return;
            }

            VillagerDeathRecord existing = data.getRecordByHistoricalEntityUuid(historicalEntityUuid);
            if (existing != null && existing.getState() == VillagerDeathRecordState.ACTIVE) {
               return;
            }

            if (existing != null) {
               data.removeRecord(existing.getRecordId());
            }

            FamilyTreeTracker.markVillagerDead(level.getServer(), historicalEntityUuid);
            VillagerDeathRecord record = createRecord(level, villager, historicalEntityUuid);
            handleGravePlacementOrFallback(level, villager.blockPosition(), record, historicalEntityUuid);
            data.putRecord(record);
            if (record.isGravePlaced()) {
               notifyRelatedPlayers(level, record);
            }
         } catch (Exception e) {
            LOGGER.error("Failed to create villager grave record for {}", villager.getUUID(), e);
         }
      }
   }

   private static VillagerDeathRecord createRecord(ServerLevel level, Villager villager, UUID historicalEntityUuid) {
      VillagerDeathRecord record = new VillagerDeathRecord();
      record.setRecordId(UUID.randomUUID());
      record.setHistoricalEntityUuid(historicalEntityUuid);
      record.setState(VillagerDeathRecordState.ACTIVE);
      record.setCreatedGameTime(level.getGameTime());
      record.setDisplayName(villager.getName().getString());
      record.setEntityKind(villager instanceof FemaleVillagerEntity ? "female" : "male");
      record.setGender(villager instanceof FemaleVillagerEntity ? "female" : "male");
      record.setLastKnownDimension(level.dimension().location().toString());
      record.setLastKnownX(villager.blockPosition().getX());
      record.setLastKnownY(villager.blockPosition().getY());
      record.setLastKnownZ(villager.blockPosition().getZ());
      record.setGravePlaced(false);
      if (villager instanceof MaleVillagerEntity maleVillager) {
         populateFromMale(record, maleVillager);
      } else if (villager instanceof FemaleVillagerEntity femaleVillager) {
         populateFromFemale(record, femaleVillager);
      }

      if (villager instanceof FamilyVillager familyVillager) {
         record.setAgeStage(familyVillager.getAgeStage().name());
         record.setParentPlayerUuid(familyVillager.getParentPlayerUuid());
         record.setParentPlayerName(familyVillager.getParentPlayerName());
         record.setFamilyOwnerPlayerUuid(familyVillager.getFamilyOwnerPlayerUuid());
         record.setFamilyOwnerPlayerName(familyVillager.getFamilyOwnerPlayerName());
         record.setSpousePlayerUuid(familyVillager.getSpousePlayerUuid());
         record.setSpousePlayerName(familyVillager.getSpousePlayerName());
         record.setRelationshipStage(familyVillager.getRelationshipStage().name());
      }

      return record;
   }

   private static VillagerDeathRecord createZombieRecord(
      ServerLevel level, HumanZombieVillagerEntity zombieVillager, CompoundTag savedData, UUID historicalEntityUuid
   ) {
      VillagerDeathRecord record = new VillagerDeathRecord();
      record.setRecordId(UUID.randomUUID());
      record.setHistoricalEntityUuid(historicalEntityUuid);
      record.setState(VillagerDeathRecordState.ACTIVE);
      record.setCreatedGameTime(level.getGameTime());
      record.setDisplayName(resolveSavedDisplayName(savedData));
      String gender = savedData.contains("slimpatch_gender") ? savedData.getString("slimpatch_gender") : zombieVillager.getGender();
      record.setEntityKind("female".equalsIgnoreCase(gender) ? "female" : "male");
      record.setGender(record.getEntityKind());
      record.setLastKnownDimension(level.dimension().location().toString());
      record.setLastKnownX(zombieVillager.blockPosition().getX());
      record.setLastKnownY(zombieVillager.blockPosition().getY());
      record.setLastKnownZ(zombieVillager.blockPosition().getZ());
      record.setGravePlaced(false);
      populateFromSavedData(record, savedData);
      return record;
   }

   private static void populateFromMale(VillagerDeathRecord record, MaleVillagerEntity villager) {
      record.setRelationship(villager.getRelationship());
      record.setGoldenRelationship(villager.getGoldenRelationship());
      record.setPersonality(villager.getPersonality().name());
      record.setSkinIndex(villager.getSkinIndex());
      record.setSavedSkinInput(villager.getSavedSkinInput());
      record.setHasCustomSkin(!villager.getSavedSkinInput().isEmpty());
      record.setSkinResourcePath(resolveSkinResourcePath(villager, "male", villager.getSkinIndex(), villager.getSavedSkinInput(), villager.getCustomSkinPath()));
      populateSharedData(record, villager, villager);
   }

   private static void populateFromFemale(VillagerDeathRecord record, FemaleVillagerEntity villager) {
      record.setRelationship(villager.getRelationship());
      record.setGoldenRelationship(villager.getGoldenRelationship());
      record.setPersonality(villager.getPersonality().name());
      record.setSkinIndex(villager.getSkinIndex());
      record.setSavedSkinInput(villager.getSavedSkinInput());
      record.setHasCustomSkin(!villager.getSavedSkinInput().isEmpty());
      record.setSkinResourcePath(
         resolveSkinResourcePath(villager, "female", villager.getSkinIndex(), villager.getSavedSkinInput(), villager.getCustomSkinPath())
      );
      populateSharedData(record, villager, villager);
   }

   private static void populateSharedData(VillagerDeathRecord record, Villager villager, CommandableVillager commandableVillager) {
      CompoundTag saved = new CompoundTag();
      villager.addAdditionalSaveData(saved);
      CompoundTag vanillaVillagerData = new CompoundTag();
      if (saved.contains("VillagerData")) {
         vanillaVillagerData.put("VillagerData", saved.get("VillagerData").copy());
      }

      if (saved.contains("Xp")) {
         vanillaVillagerData.putInt("Xp", saved.getInt("Xp"));
      }

      if (saved.contains("Offers")) {
         vanillaVillagerData.put("Offers", saved.get("Offers").copy());
      }

      if (saved.contains("FoodLevel")) {
         vanillaVillagerData.putByte("FoodLevel", saved.getByte("FoodLevel"));
      }

      record.setVanillaVillagerData(vanillaVillagerData);
      CompoundTag relationshipData = new CompoundTag();
      VillagerRelationshipData.save(relationshipData, villager);
      record.setRelationshipData(relationshipData);
      CompoundTag commandData = new CompoundTag();
      VillagerCommandHandler.save(commandData, commandableVillager);
      record.setCommandData(commandData);
      record.setBaseMaxHealth(commandableVillager.getBaseMaxHealth());
      record.setBonusHealth(commandableVillager.getBonusHealth());
      VillagerData villagerData = villager.getVillagerData();
      record.setVillagerType(BuiltInRegistries.VILLAGER_TYPE.getKey(villagerData.getType()).toString());
      record.setVillagerProfession(BuiltInRegistries.VILLAGER_PROFESSION.getKey(villagerData.getProfession()).toString());
      record.setVillagerLevel(villagerData.getLevel());
      record.setVillagerXp(villager.getVillagerXp());
      CompoundTag villagerDataTag = new CompoundTag();
      villagerDataTag.put("VillagerData", (Tag)VillagerData.CODEC.encodeStart(NbtOps.INSTANCE, villagerData).getOrThrow(false, error -> {}));
      record.setVillagerDataTag(villagerDataTag);
      MerchantOffers offers = villager.getOffers();
      if (!offers.isEmpty()) {
         CompoundTag offersData = new CompoundTag();
         offersData.put("Offers", offers.createTag().copy());
         record.setOffersData(offersData);
      }
   }

   private static String resolveSkinResourcePath(Villager villager, String gender, int skinIndex, String savedSkinInput, String customSkinPath) {
      if (customSkinPath != null && !customSkinPath.isEmpty()) {
         ResourceLocation parsed = ResourceLocation.tryParse(customSkinPath);
         if (parsed != null) {
            return parsed.toString();
         }

         if (savedSkinInput != null && !savedSkinInput.isEmpty()) {
            return customSkinPath;
         }
      }

      if (gender != null && !gender.isEmpty() && skinIndex > 0) {
         ResourceLocation resourceLocation = SkinPathHelper.getSkin(gender, skinIndex, villager.level());
         if (resourceLocation != null) {
            return resourceLocation.toString();
         }
      }

      return "";
   }

   private static void populateFromSavedData(VillagerDeathRecord record, CompoundTag savedData) {
      record.setRelationship(savedData.contains("slimpatch_relationship") ? savedData.getFloat("slimpatch_relationship") : 0.0F);
      record.setGoldenRelationship(savedData.contains("SlimPatchGoldenRelationship") ? savedData.getFloat("SlimPatchGoldenRelationship") : 0.0F);
      record.setPersonality(savedData.contains("slimpatch_personality") ? savedData.getString("slimpatch_personality") : "");
      record.setSkinIndex(savedData.contains("slimpatch_skin") ? savedData.getInt("slimpatch_skin") : 0);
      record.setSavedSkinInput(savedData.contains("SavedSkinInput") ? savedData.getString("SavedSkinInput") : "");
      record.setSkinResourcePath(savedData.contains("CustomSkinPath") ? savedData.getString("CustomSkinPath") : "");
      record.setHasCustomSkin(!record.getSavedSkinInput().isEmpty());
      record.setAgeStage(savedData.contains("SlimPatchAgeStage") ? savedData.getString("SlimPatchAgeStage") : "ADULT");
      record.setParentPlayerUuid(VillagerFamilyData.readParentPlayerUuid(savedData));
      record.setParentPlayerName(VillagerFamilyData.readOptionalString(savedData, "SlimPatchParentPlayerName"));
      record.setFamilyOwnerPlayerUuid(VillagerFamilyData.readFamilyOwnerPlayerUuid(savedData));
      record.setFamilyOwnerPlayerName(VillagerFamilyData.readOptionalString(savedData, "SlimPatchFamilyOwnerPlayerName"));
      record.setSpousePlayerUuid(VillagerFamilyData.readSpousePlayerUuid(savedData));
      record.setSpousePlayerName(VillagerFamilyData.readOptionalString(savedData, "SlimPatchSpousePlayerName"));
      record.setRelationshipStage(savedData.contains("SlimPatchRelationshipStage") ? savedData.getString("SlimPatchRelationshipStage") : "");
      record.setBaseMaxHealth(savedData.contains("SlimPatchBaseMaxHealth") ? savedData.getDouble("SlimPatchBaseMaxHealth") : 0.0);
      record.setBonusHealth(savedData.contains("SlimPatchBonusHealth") ? savedData.getFloat("SlimPatchBonusHealth") : 0.0F);
      if (savedData.contains("VanillaVillagerData", 10)) {
         record.setVanillaVillagerData(savedData.getCompound("VanillaVillagerData"));
      }

      if (savedData.contains("VillagerDataTag", 10)) {
         record.setVillagerDataTag(savedData.getCompound("VillagerDataTag"));
      }

      if (savedData.contains("OffersData", 10)) {
         record.setOffersData(savedData.getCompound("OffersData"));
      }

      if (savedData.contains("RelationshipData", 10)) {
         record.setRelationshipData(savedData.getCompound("RelationshipData"));
      }

      if (savedData.contains("CommandData", 10)) {
         record.setCommandData(savedData.getCompound("CommandData"));
      }

      if (savedData.contains("VillagerType")) {
         record.setVillagerType(savedData.getString("VillagerType"));
      }

      if (savedData.contains("VillagerProfession")) {
         record.setVillagerProfession(savedData.getString("VillagerProfession"));
      }

      if (savedData.contains("VillagerLevel")) {
         record.setVillagerLevel(savedData.getInt("VillagerLevel"));
      }

      if (savedData.contains("VillagerXp")) {
         record.setVillagerXp(savedData.getInt("VillagerXp"));
      }
   }

   private static boolean shouldCreateDeathRecord(CompoundTag savedData) {
      if (savedData != null && !savedData.isEmpty()) {
         CompoundTag relationships = savedData.getCompound("SlimPatchRelationships");

         for (String key : relationships.getAllKeys()) {
            if (relationships.getFloat(key) >= 3.0F) {
               return true;
            }
         }

         if (savedData.contains("slimpatch_relationship") && savedData.getFloat("slimpatch_relationship") >= 3.0F) {
            return true;
         }

         if (VillagerFamilyData.readParentPlayerUuid(savedData) == null
            && VillagerFamilyData.readFamilyOwnerPlayerUuid(savedData) == null
            && VillagerFamilyData.readSpousePlayerUuid(savedData) == null) {
            CompoundTag goldenRelationships = savedData.getCompound("SlimPatchGoldenRelationships");

            for (String key : goldenRelationships.getAllKeys()) {
               if (goldenRelationships.getFloat(key) > 0.0F) {
                  return true;
               }
            }

            if (savedData.contains("SlimPatchRelationshipStage")) {
               String relationshipStage = savedData.getString("SlimPatchRelationshipStage");
               if (VillagerRelationshipStage.DATING.name().equals(relationshipStage)
                  || VillagerFamilyData.readSpousePlayerUuid(savedData) != null && VillagerRelationshipStage.MARRIED.name().equals(relationshipStage)) {
                  return true;
               }
            }

            if (savedData.contains("SlimPatchCommand")) {
               String commandState = savedData.getString("SlimPatchCommand");
               if ((VillagerCommandState.FOLLOW.name().equals(commandState) || VillagerCommandState.STAY.name().equals(commandState))
                  && savedData.hasUUID("SlimPatchCommandOwnerUuid")) {
                  return true;
               }
            }

            return false;
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   private static String resolveSavedDisplayName(CompoundTag savedData) {
      if (savedData.contains("SavedName") && !savedData.getString("SavedName").isEmpty()) {
         Component parsed = Serializer.fromJson(savedData.getString("SavedName"));
         if (parsed != null && !parsed.getString().isEmpty()) {
            return parsed.getString();
         }
      }

      return "";
   }

   private static void notifyRelatedPlayers(ServerLevel level, VillagerDeathRecord record) {
      if (level != null && level.getServer() != null && record != null) {
         Set<UUID> playerUuids = collectRelatedPlayerUuids(record);
         Component villagerName = record.getDisplayName().isEmpty()
            ? Component.translatable("gui.slimpatch.grave.unknown")
            : Component.literal(record.getDisplayName());

         for (UUID playerUuid : playerUuids) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerUuid);
            if (player != null) {
               player.displayClientMessage(Component.translatable("message.slimpatch.grave.villager_died", new Object[]{villagerName}), true);
               player.playNotifySound(SoundEvents.SOUL_ESCAPE, SoundSource.NEUTRAL, 1.2F, 0.8F);
            }
         }
      }
   }

   private static void handleGravePlacementOrFallback(ServerLevel level, BlockPos deathPos, VillagerDeathRecord record, UUID villagerUuid) {
      BlockPos gravePos = findPlacementPos(level, deathPos);
      if (!tryPlaceGraveBlock(level, gravePos, record)) {
         if (gravePos != null) {
            LOGGER.warn(
               "Failed to place villager grave block for villager {} at {} in {}",
               new Object[]{villagerUuid, formatPos(gravePos), level.dimension().location()}
            );
         }

         ServerPlayer recipient = findPreferredOnlineRecipient(level, record);
         if (recipient != null) {
            ItemStack graveStack = GraveBlockItem.createStackWithRecord(ModBlocks.GRAVE, record.getRecordId(), record.getDisplayName());
            if (recipient.getInventory().add(graveStack)) {
               LOGGER.warn(
                  "Delivered villager grave item to {} because the grave block could not be placed for villager {}",
                  recipient.getGameProfile().getName(),
                  villagerUuid
               );
            } else {
               ItemEntity itemEntity = recipient.drop(graveStack, false);
               if (itemEntity == null) {
                  itemEntity = new ItemEntity(level, recipient.getX(), recipient.getY(), recipient.getZ(), graveStack);
                  level.addFreshEntity(itemEntity);
               }

               LOGGER.warn(
                  "Dropped villager grave item near {} because their inventory was full for villager {}", recipient.getGameProfile().getName(), villagerUuid
               );
            }
         } else {
            LOGGER.warn(
               "No online player found for villager {} grave item fallback near {} in {}",
               new Object[]{villagerUuid, formatPos(deathPos), level.dimension().location()}
            );
            BlockPos surfaceFallbackPos = findSurfaceFallbackPos(level, deathPos);
            if (tryPlaceGraveBlock(level, surfaceFallbackPos, record)) {
               LOGGER.warn(
                  "Placed villager grave using fallback search for villager {} from {} to {} in {}",
                  new Object[]{villagerUuid, formatPos(deathPos), formatPos(surfaceFallbackPos), level.dimension().location()}
               );
            } else {
               LOGGER.warn(
                  "No valid grave position found for villager {} near {} in {}", new Object[]{villagerUuid, formatPos(deathPos), level.dimension().location()}
               );
            }
         }
      }
   }

   private static boolean tryPlaceGraveBlock(ServerLevel level, BlockPos gravePos, VillagerDeathRecord record) {
      if (level != null && gravePos != null && record != null) {
         if (!level.setBlock(gravePos, ModBlocks.GRAVE.defaultBlockState(), 3)) {
            return false;
         }

         record.setGraveDimension(level.dimension().location().toString());
         record.setGraveX(gravePos.getX());
         record.setGraveY(gravePos.getY());
         record.setGraveZ(gravePos.getZ());
         record.setGravePlaced(true);
         if (level.getBlockEntity(gravePos) instanceof GraveBlockEntity graveBlockEntity) {
            graveBlockEntity.setRecordId(record.getRecordId());
            graveBlockEntity.setCachedDisplayName(record.getDisplayName());
         }

         return true;
      } else {
         return false;
      }
   }

   private static ServerPlayer findPreferredOnlineRecipient(ServerLevel level, VillagerDeathRecord record) {
      if (level != null && level.getServer() != null && record != null) {
         CompoundTag commandData = record.getCommandData();
         if (commandData.hasUUID("SlimPatchCommandOwnerUuid")) {
            ServerPlayer commandOwner = level.getServer().getPlayerList().getPlayer(commandData.getUUID("SlimPatchCommandOwnerUuid"));
            if (commandOwner != null) {
               return commandOwner;
            }
         }

         Set<UUID> relatedPlayers = collectRelatedPlayerUuids(record);
         if (commandData.hasUUID("SlimPatchCommandOwnerUuid")) {
            relatedPlayers.remove(commandData.getUUID("SlimPatchCommandOwnerUuid"));
         }

         for (UUID playerUuid : relatedPlayers) {
            ServerPlayer relatedPlayer = level.getServer().getPlayerList().getPlayer(playerUuid);
            if (relatedPlayer != null) {
               return relatedPlayer;
            }
         }

         ServerPlayer closestPlayer = null;
         double closestDistance = Double.MAX_VALUE;
         BlockPos lastKnownPos = new BlockPos(record.getLastKnownX(), record.getLastKnownY(), record.getLastKnownZ());

         for (ServerPlayer player : level.players()) {
            double distance = player.blockPosition().distSqr(lastKnownPos);
            if (distance < closestDistance) {
               closestDistance = distance;
               closestPlayer = player;
            }
         }

         return closestPlayer;
      } else {
         return null;
      }
   }

   private static Set<UUID> collectRelatedPlayerUuids(VillagerDeathRecord record) {
      Set<UUID> playerUuids = new LinkedHashSet<>();
      addPlayerUuid(playerUuids, record.getParentPlayerUuid());
      addPlayerUuid(playerUuids, record.getFamilyOwnerPlayerUuid());
      addPlayerUuid(playerUuids, record.getSpousePlayerUuid());
      CompoundTag relationshipData = record.getRelationshipData();
      CompoundTag relationships = relationshipData.getCompound("SlimPatchRelationships");

      for (String key : relationships.getAllKeys()) {
         if (relationships.getFloat(key) >= 3.0F) {
            addPlayerUuid(playerUuids, key);
         }
      }

      CompoundTag goldenRelationships = relationshipData.getCompound("SlimPatchGoldenRelationships");

      for (String key : goldenRelationships.getAllKeys()) {
         if (goldenRelationships.getFloat(key) > 0.0F) {
            addPlayerUuid(playerUuids, key);
         }
      }

      CompoundTag commandData = record.getCommandData();
      if (commandData.contains("SlimPatchCommand")) {
         String commandState = commandData.getString("SlimPatchCommand");
         if (VillagerCommandState.FOLLOW.name().equals(commandState) || VillagerCommandState.STAY.name().equals(commandState)) {
            if (commandData.hasUUID("SlimPatchCommandOwnerUuid")) {
               addPlayerUuid(playerUuids, commandData.getUUID("SlimPatchCommandOwnerUuid"));
            }

            if (VillagerCommandState.FOLLOW.name().equals(commandState) && commandData.hasUUID("SlimPatchCommandTarget")) {
               addPlayerUuid(playerUuids, commandData.getUUID("SlimPatchCommandTarget"));
            }
         }
      }

      return playerUuids;
   }

   private static void addPlayerUuid(Set<UUID> playerUuids, UUID playerUuid) {
      if (playerUuid != null) {
         playerUuids.add(playerUuid);
      }
   }

   private static void addPlayerUuid(Set<UUID> playerUuids, String playerUuid) {
      if (playerUuid != null && !playerUuid.isEmpty()) {
         try {
            playerUuids.add(UUID.fromString(playerUuid));
         } catch (IllegalArgumentException var3) {
         }
      }
   }

   private static BlockPos findPlacementPos(ServerLevel level, BlockPos deathPos) {
      if (isValidPlacementPos(level, deathPos)) {
         return deathPos;
      }

      for (int radius = 1; radius <= 10; radius++) {
         BlockPos sameY = findValidPlacementInRing(level, deathPos, radius, 0);
         if (sameY != null) {
            return sameY;
         }
      }

      for (int yOffset : LOCAL_VERTICAL_OFFSETS) {
         BlockPos adjustedCenter = deathPos.offset(0, yOffset, 0);
         if (isValidPlacementPos(level, adjustedCenter)) {
            return adjustedCenter;
         }

         for (int radius = 1; radius <= 10; radius++) {
            BlockPos candidate = findValidPlacementInRing(level, deathPos, radius, yOffset);
            if (candidate != null) {
               return candidate;
            }
         }
      }

      return null;
   }

   private static BlockPos findSurfaceFallbackPos(ServerLevel level, BlockPos deathPos) {
      for (int radius : SURFACE_FALLBACK_RADII) {
         for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
               if (Math.max(Math.abs(dx), Math.abs(dz)) == radius) {
                  int x = deathPos.getX() + dx;
                  int z = deathPos.getZ() + dz;
                  int y = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                  BlockPos candidate = new BlockPos(x, y, z);
                  if (isValidPlacementPos(level, candidate)) {
                     return candidate;
                  }
               }
            }
         }
      }

      return null;
   }

   private static BlockPos findValidPlacementInRing(ServerLevel level, BlockPos center, int radius, int yOffset) {
      for (int dx = -radius; dx <= radius; dx++) {
         for (int dz = -radius; dz <= radius; dz++) {
            if (Math.max(Math.abs(dx), Math.abs(dz)) == radius) {
               BlockPos candidate = center.offset(dx, yOffset, dz);
               if (isValidPlacementPos(level, candidate)) {
                  return candidate;
               }
            }
         }
      }

      return null;
   }

   private static String formatPos(BlockPos pos) {
      return pos == null ? "null" : pos.getX() + "," + pos.getY() + "," + pos.getZ();
   }

   private static boolean isValidPlacementPos(ServerLevel level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      BlockState belowState = level.getBlockState(pos.below());
      if (state.liquid()) {
         return false;
      } else if (!state.canBeReplaced()) {
         return false;
      } else if (state.hasBlockEntity() || state.getBlock() instanceof BedBlock) {
         return false;
      } else {
         return !belowState.isAir() && !belowState.liquid() ? belowState.isFaceSturdy(level, pos.below(), Direction.UP) : false;
      }
   }
}
