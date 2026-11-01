package com.javic.slimpatch.graves;

import com.javic.slimpatch.ModEntities;
import com.javic.slimpatch.entity.FemaleVillagerEntity;
import com.javic.slimpatch.entity.MaleVillagerEntity;
import com.javic.slimpatch.entity.VillagerAgeStage;
import com.javic.slimpatch.entity.VillagerCommandHandler;
import com.javic.slimpatch.entity.VillagerCommandState;
import com.javic.slimpatch.entity.VillagerHealthHandler;
import com.javic.slimpatch.entity.VillagerPersonality;
import com.javic.slimpatch.entity.VillagerRelationshipData;
import com.javic.slimpatch.entity.VillagerRelationshipStage;
import com.javic.slimpatch.fabric.PersistentEntityData;
import com.javic.slimpatch.familytree.FamilyTreeTracker;
import java.util.Locale;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffers;

public final class VillagerResurrectionService {
   public static final String HISTORICAL_ENTITY_UUID_TAG = "SlimPatchHistoricalEntityUuid";

   private VillagerResurrectionService() {
   }

   public static boolean tryResurrect(ServerPlayer player, ServerLevel level, VillagerDeathRecord record, ItemStack stack, BlockPos gravePos) {
      if (player != null && level != null && record != null && stack != null && gravePos != null) {
         if (record.getState() != VillagerDeathRecordState.ACTIVE) {
            player.displayClientMessage(Component.translatable("message.slimpatch.grave.already_restored"), true);
            return false;
         }

         if (findLivingEntity(level.getServer(), record.getHistoricalEntityUuid()) != null) {
            player.displayClientMessage(Component.translatable("message.slimpatch.grave.already_restored"), true);
            return false;
         }

         if (record.getRestoredLivingUuid() != null && findLivingEntity(level.getServer(), record.getRestoredLivingUuid()) != null) {
            player.displayClientMessage(Component.translatable("message.slimpatch.grave.already_restored"), true);
            return false;
         }

         Villager villager = createVillager(level, record);
         if (villager == null) {
            player.displayClientMessage(Component.translatable("message.slimpatch.grave.resurrection_failed"), true);
            return false;
         }

         BlockPos spawnPos = findSpawnPos(level, gravePos);
         if (spawnPos == null) {
            player.displayClientMessage(Component.translatable("message.slimpatch.grave.no_space"), true);
            return false;
         }

         applyRecord(villager, record);
         PersistentEntityData.get(villager).putUUID("SlimPatchHistoricalEntityUuid", record.getHistoricalEntityUuid());
         villager.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0.0F, 0.0F);
         if (!level.addFreshEntity(villager)) {
            player.displayClientMessage(Component.translatable("message.slimpatch.grave.resurrection_failed"), true);
            return false;
         }

         finalizeRestoredVillager(villager, record, level);
         FamilyTreeTracker.upsertVillager(level.getServer(), villager);
         if (!player.getAbilities().instabuild) {
            stack.shrink(1);
         }

         record.setState(VillagerDeathRecordState.RESTORED);
         record.setRestoredLivingUuid(villager.getUUID());
         GraveSavedData.get(level.getServer()).putRecord(record);
         level.removeBlock(gravePos, false);
         level.sendParticles(ParticleTypes.ENCHANT, villager.getX(), villager.getY() + 1.0, villager.getZ(), 32, 0.45, 0.65, 0.45, 0.02);
         level.sendParticles(ParticleTypes.HAPPY_VILLAGER, villager.getX(), villager.getY() + 1.0, villager.getZ(), 12, 0.35, 0.45, 0.35, 0.0);
         level.playSound(null, gravePos, SoundEvents.TOTEM_USE, SoundSource.BLOCKS, 0.75F, 1.0F);
         player.displayClientMessage(
            Component.translatable(
               "message.slimpatch.grave.resurrected",
               new Object[]{
                  record.getDisplayName().isEmpty() ? Component.translatable("gui.slimpatch.grave.unknown") : Component.literal(record.getDisplayName())
               }
            ),
            true
         );
         return true;
      } else {
         return false;
      }
   }

   @Nullable
   private static Villager createVillager(ServerLevel level, VillagerDeathRecord record) {
      if ("female".equalsIgnoreCase(record.getEntityKind())) {
         return ModEntities.FEMALE_VILLAGER.create(level);
      } else if ("male".equalsIgnoreCase(record.getEntityKind())) {
         return ModEntities.MALE_VILLAGER.create(level);
      } else {
         return "female".equalsIgnoreCase(record.getGender())
            ? ModEntities.FEMALE_VILLAGER.create(level)
            : ModEntities.MALE_VILLAGER.create(level);
      }
   }

   private static void applyRecord(Villager villager, VillagerDeathRecord record) {
      if (villager != null) {
         applyVanillaVillagerSnapshot(villager, record);
         if (!record.getDisplayName().isEmpty()) {
            villager.setCustomName(Component.literal(record.getDisplayName()));
            villager.setCustomNameVisible(true);
         }

         if (villager instanceof MaleVillagerEntity maleVillager) {
            applyToMale(maleVillager, record);
         } else if (villager instanceof FemaleVillagerEntity femaleVillager) {
            applyToFemale(femaleVillager, record);
         }
      }
   }

   private static void applyToMale(MaleVillagerEntity villager, VillagerDeathRecord record) {
      applySharedVillagerData(villager, record);
      applyPersonality(villager, record.getPersonality());
      villager.setSkinIndex(record.getSkinIndex());
      villager.setSavedSkinInput(record.getSavedSkinInput());
      villager.setCustomSkinPath(record.getSkinResourcePath());
      villager.setRelationship(record.getRelationship());
      villager.setGoldenRelationship(record.getGoldenRelationship());
      villager.setParentPlayerUuid(record.getParentPlayerUuid());
      villager.setParentPlayerName(record.getParentPlayerName());
      villager.setFamilyOwnerPlayerUuid(record.getFamilyOwnerPlayerUuid());
      villager.setFamilyOwnerPlayerName(record.getFamilyOwnerPlayerName());
      villager.setSpousePlayerUuid(record.getSpousePlayerUuid());
      villager.setSpousePlayerName(record.getSpousePlayerName());
      villager.setAgeStage(parseAgeStage(record.getAgeStage()));
      villager.setRelationshipStage(parseRelationshipStage(record.getRelationshipStage()));
      VillagerRelationshipData.load(record.getRelationshipData(), villager);
      VillagerCommandHandler.load(record.getCommandData(), villager);
      normalizeStayCommand(villager);
      villager.setBaseMaxHealth(record.getBaseMaxHealth());
      villager.setBonusHealth(record.getBonusHealth());
      VillagerHealthHandler.reapplyBonusHealth(villager, villager);
      villager.setHealth(villager.getMaxHealth());
   }

   private static void applyToFemale(FemaleVillagerEntity villager, VillagerDeathRecord record) {
      applySharedVillagerData(villager, record);
      applyPersonality(villager, record.getPersonality());
      villager.setSkinIndex(record.getSkinIndex());
      villager.setSavedSkinInput(record.getSavedSkinInput());
      villager.setCustomSkinPath(record.getSkinResourcePath());
      villager.setRelationship(record.getRelationship());
      villager.setGoldenRelationship(record.getGoldenRelationship());
      villager.setParentPlayerUuid(record.getParentPlayerUuid());
      villager.setParentPlayerName(record.getParentPlayerName());
      villager.setFamilyOwnerPlayerUuid(record.getFamilyOwnerPlayerUuid());
      villager.setFamilyOwnerPlayerName(record.getFamilyOwnerPlayerName());
      villager.setSpousePlayerUuid(record.getSpousePlayerUuid());
      villager.setSpousePlayerName(record.getSpousePlayerName());
      villager.setAgeStage(parseAgeStage(record.getAgeStage()));
      villager.setRelationshipStage(parseRelationshipStage(record.getRelationshipStage()));
      VillagerRelationshipData.load(record.getRelationshipData(), villager);
      VillagerCommandHandler.load(record.getCommandData(), villager);
      normalizeStayCommand(villager);
      villager.setBaseMaxHealth(record.getBaseMaxHealth());
      villager.setBonusHealth(record.getBonusHealth());
      VillagerHealthHandler.reapplyBonusHealth(villager, villager);
      villager.setHealth(villager.getMaxHealth());
   }

   private static void finalizeRestoredVillager(Villager villager, VillagerDeathRecord record, ServerLevel level) {
      if (record.getVanillaVillagerData().isEmpty()) {
         applySharedVillagerData(villager, record);
      }

      villager.refreshBrain(level);
      if (villager instanceof MaleVillagerEntity maleVillager) {
         normalizeStayCommand(maleVillager);
      } else if (villager instanceof FemaleVillagerEntity femaleVillager) {
         normalizeStayCommand(femaleVillager);
      }
   }

   private static void applyVanillaVillagerSnapshot(Villager villager, VillagerDeathRecord record) {
      if (record.getVanillaVillagerData().isEmpty()) {
         applySharedVillagerData(villager, record);
      } else {
         villager.readAdditionalSaveData(record.getVanillaVillagerData());
      }
   }

   private static void applySharedVillagerData(Villager villager, VillagerDeathRecord record) {
      VillagerData villagerData = null;
      if (!record.getVillagerDataTag().isEmpty() && record.getVillagerDataTag().contains("VillagerData")) {
         villagerData = (VillagerData)VillagerData.CODEC.parse(NbtOps.INSTANCE, record.getVillagerDataTag().get("VillagerData")).result().orElse(null);
      }

      if (villagerData == null) {
         VillagerType villagerType = VillagerType.PLAINS;
         if (!record.getVillagerType().isEmpty()) {
            ResourceLocation villagerTypeId = ResourceLocation.tryParse(record.getVillagerType());
            if (villagerTypeId != null) {
               villagerType = BuiltInRegistries.VILLAGER_TYPE.getOptional(villagerTypeId).orElse(VillagerType.PLAINS);
            }
         }

         VillagerProfession villagerProfession = VillagerProfession.NONE;
         if (!record.getVillagerProfession().isEmpty()) {
            ResourceLocation professionId = ResourceLocation.tryParse(record.getVillagerProfession());
            if (professionId != null) {
               villagerProfession = BuiltInRegistries.VILLAGER_PROFESSION.getOptional(professionId).orElse(VillagerProfession.NONE);
            }
         }

         int villagerLevel = Math.max(1, record.getVillagerLevel());
         villagerData = new VillagerData(villagerType, villagerProfession, villagerLevel);
      }

      villager.setVillagerData(villagerData);
      villager.setVillagerXp(Math.max(0, record.getVillagerXp()));
      if (!record.getOffersData().isEmpty() && record.getOffersData().contains("Offers")) {
         villager.setOffers(new MerchantOffers(record.getOffersData().getCompound("Offers")));
      }
   }

   private static void applyPersonality(MaleVillagerEntity villager, String personality) {
      if (personality != null && !personality.isEmpty()) {
         try {
            villager.setPersonality(VillagerPersonality.valueOf(personality));
         } catch (IllegalArgumentException var3) {
         }
      }
   }

   private static void applyPersonality(FemaleVillagerEntity villager, String personality) {
      if (personality != null && !personality.isEmpty()) {
         try {
            villager.setPersonality(VillagerPersonality.valueOf(personality));
         } catch (IllegalArgumentException var3) {
         }
      }
   }

   private static void normalizeStayCommand(MaleVillagerEntity villager) {
      if (villager.getCommandState() == VillagerCommandState.STAY) {
         villager.setCommandState(VillagerCommandState.NONE);
         villager.setStayPos(null);
         villager.setStayAnchorPos(null);
         villager.setCommandTargetUuid(null);
      }
   }

   private static void normalizeStayCommand(FemaleVillagerEntity villager) {
      if (villager.getCommandState() == VillagerCommandState.STAY) {
         villager.setCommandState(VillagerCommandState.NONE);
         villager.setStayPos(null);
         villager.setStayAnchorPos(null);
         villager.setCommandTargetUuid(null);
      }
   }

   private static VillagerAgeStage parseAgeStage(String ageStage) {
      if (ageStage != null && !ageStage.isEmpty()) {
         try {
            return VillagerAgeStage.valueOf(ageStage.toUpperCase(Locale.ROOT));
         } catch (IllegalArgumentException e) {
            return VillagerAgeStage.ADULT;
         }
      } else {
         return VillagerAgeStage.ADULT;
      }
   }

   private static VillagerRelationshipStage parseRelationshipStage(String relationshipStage) {
      if (relationshipStage != null && !relationshipStage.isEmpty()) {
         try {
            return VillagerRelationshipStage.valueOf(relationshipStage.toUpperCase(Locale.ROOT));
         } catch (IllegalArgumentException e) {
            return VillagerRelationshipStage.FRIENDSHIP;
         }
      } else {
         return VillagerRelationshipStage.FRIENDSHIP;
      }
   }

   @Nullable
   private static Entity findLivingEntity(MinecraftServer server, UUID uuid) {
      if (server != null && uuid != null) {
         for (ServerLevel serverLevel : server.getAllLevels()) {
            Entity entity = serverLevel.getEntity(uuid);
            if (entity != null && entity.isAlive() && !entity.isRemoved()) {
               return entity;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   @Nullable
   private static BlockPos findSpawnPos(ServerLevel level, BlockPos gravePos) {
      BlockPos above = gravePos.above();
      if (isSafeSpawnPos(level, above)) {
         return above;
      }

      for (int radius = 1; radius <= 2; radius++) {
         for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
               BlockPos candidate = gravePos.offset(x, 0, z).above();
               if (isSafeSpawnPos(level, candidate)) {
                  return candidate;
               }
            }
         }
      }

      return null;
   }

   private static boolean isSafeSpawnPos(ServerLevel level, BlockPos pos) {
      if (level.getBlockState(pos).liquid() || level.getBlockState(pos.above()).liquid()) {
         return false;
      } else {
         return level.getBlockState(pos).canBeReplaced() && level.getBlockState(pos.above()).canBeReplaced()
            ? level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
            : false;
      }
   }
}
