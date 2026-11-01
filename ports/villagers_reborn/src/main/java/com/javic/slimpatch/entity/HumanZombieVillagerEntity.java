package com.javic.slimpatch.entity;

import com.javic.slimpatch.fabric.PersistentEntityData;

import com.javic.slimpatch.memory.CuredVillagerMemory;
import com.javic.slimpatch.sounds.HumanZombieVillagerSounds;
import com.javic.slimpatch.util.SkinPathHelper;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.village.ReputationEventType;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

public class HumanZombieVillagerEntity extends ZombieVillager {
   private static final String CURING_PLAYER_TAG = "SlimPatchCuringPlayer";
   private static final float CURING_RELATIONSHIP_BONUS = 2.0F;
   private static final EntityDataAccessor<String> DATA_GENDER = SynchedEntityData.defineId(HumanZombieVillagerEntity.class, EntityDataSerializers.STRING);
   private int conversionTicks = -1;

   public HumanZombieVillagerEntity(EntityType<? extends ZombieVillager> type, Level level) {
      super(type, level);
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(DATA_GENDER, "male");
   }

   public String getGender() {
      return (String)this.entityData.get(DATA_GENDER);
   }

   public void setGender(String gender) {
      this.entityData.set(DATA_GENDER, gender);
      PersistentEntityData.get(this).putString("slimpatch_gender", gender);
   }

   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      if (tag.contains("slimpatch_saved_data")) {
         PersistentEntityData.get(this).put("slimpatch_saved_data", tag.getCompound("slimpatch_saved_data").copy());
      }

      String gender = getSavedGender(PersistentEntityData.get(this).getCompound("slimpatch_saved_data"));
      if (gender == null && tag.contains("slimpatch_gender")) {
         gender = normalizeGender(tag.getString("slimpatch_gender"));
      }

      if (gender != null) {
         this.setGender(gender);
      }

      if (tag.hasUUID("SlimPatchCuringPlayer")) {
         PersistentEntityData.get(this).putUUID("SlimPatchCuringPlayer", tag.getUUID("SlimPatchCuringPlayer"));
      }

      this.conversionTicks = tag.contains("SlimPatchConversionTicks") ? tag.getInt("SlimPatchConversionTicks") : -1;
   }

   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      if (PersistentEntityData.get(this).contains("slimpatch_saved_data")) {
         tag.put("slimpatch_saved_data", PersistentEntityData.get(this).getCompound("slimpatch_saved_data").copy());
      }

      String gender = getSavedGender(PersistentEntityData.get(this).getCompound("slimpatch_saved_data"));
      tag.putString("slimpatch_gender", gender != null ? gender : normalizeGender(this.getGender()));
      if (PersistentEntityData.get(this).hasUUID("SlimPatchCuringPlayer")) {
         tag.putUUID("SlimPatchCuringPlayer", PersistentEntityData.get(this).getUUID("SlimPatchCuringPlayer"));
      }

      if (this.conversionTicks >= 0) {
         tag.putInt("SlimPatchConversionTicks", this.conversionTicks);
      }
   }

   public ResourceLocation getSkinTexture() {
      return SkinPathHelper.getSkinForType("human_zombie_villager", this.getGender(), 1, this.level());
   }

   @Nullable
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag
   ) {
      SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
      String gender = getSavedGender(PersistentEntityData.get(this).getCompound("slimpatch_saved_data"));
      if (gender == null && PersistentEntityData.get(this).contains("slimpatch_gender")) {
         gender = normalizeGender(PersistentEntityData.get(this).getString("slimpatch_gender"));
      }

      if (gender != null) {
         this.setGender(gender);
      }

      return data;
   }

   public SoundEvent getAmbientSound() {
      return "female".equalsIgnoreCase(this.getGender()) ? HumanZombieVillagerSounds.femaleAmbient() : SoundEvents.ZOMBIE_VILLAGER_AMBIENT;
   }

   public SoundEvent getHurtSound(DamageSource source) {
      return "female".equalsIgnoreCase(this.getGender()) ? HumanZombieVillagerSounds.femaleHurt() : SoundEvents.ZOMBIE_VILLAGER_HURT;
   }

   public SoundEvent getDeathSound() {
      return "female".equalsIgnoreCase(this.getGender()) ? HumanZombieVillagerSounds.femaleDeath() : SoundEvents.ZOMBIE_VILLAGER_DEATH;
   }

   public InteractionResult mobInteract(Player player, InteractionHand hand) {
      ItemStack item = player.getItemInHand(hand);
      if (item.is(Items.GOLDEN_APPLE) && this.hasEffect(MobEffects.WEAKNESS)) {
         if (!this.level().isClientSide) {
            if (!player.getAbilities().instabuild) {
               item.shrink(1);
            }

            if (player instanceof ServerPlayer serverPlayer) {
               PersistentEntityData.get(this).putUUID("SlimPatchCuringPlayer", serverPlayer.getUUID());
            }

            this.startManualConversion();
            this.level().levelEvent(null, 1027, this.blockPosition(), 0);
         }

         return InteractionResult.SUCCESS;
      } else {
         return super.mobInteract(player, hand);
      }
   }

   private void startManualConversion() {
      this.conversionTicks = 200;
      this.removeEffect(MobEffects.WEAKNESS);
      this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, this.conversionTicks, 1));
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide && this.isAlive() && this.conversionTicks > 0) {
         this.conversionTicks--;
         if (this.conversionTicks == 0) {
            this.finishManualConversion((ServerLevel)this.level());
         }
      }
   }

   private void finishManualConversion(ServerLevel level) {
      CompoundTag saved = PersistentEntityData.get(this).getCompound("slimpatch_saved_data");
      if ((saved == null || saved.isEmpty()) && this.getUUID() != null) {
         saved = CuredVillagerMemory.peek(this.getUUID());
      }

      if (saved != null && !saved.isEmpty()) {
         String gender = getSavedGender(saved);
         if (gender == null) {
            gender = normalizeGender(this.getGender());
         }

         double x = this.getX();
         double y = this.getY();
         double z = this.getZ();
         float yRot = this.getYRot();
         float xRot = this.getXRot();
         this.discard();
         CompoundTag nbt = saved.copy();
         nbt.putString("id", "slimpatch:" + (gender.equalsIgnoreCase("female") ? "female_villager" : "male_villager"));
         nbt.putString("slimpatch_gender", gender);
         nbt.putBoolean("slimpatch_restored", true);
         nbt.put("slimpatch_saved_data", saved.copy());
         nbt.remove("DeathTime");
         nbt.remove("HurtTime");
         nbt.remove("HurtByTimestamp");
         nbt.remove("Fire");
         nbt.remove("Air");
         if (!nbt.contains("Health") || nbt.getFloat("Health") <= 0.0F) {
            nbt.putFloat("Health", 20.0F);
         }

         Entity villager = EntityType.loadEntityRecursive(nbt, level, entity -> {
            entity.moveTo(x, y, z, yRot, xRot);
            return entity;
         });
         if (villager != null) {
            if (villager instanceof LivingEntity living && living.getHealth() <= 0.0F) {
               living.setHealth(living.getMaxHealth());
            }

            level.addFreshEntity(villager);
            if (villager instanceof Villager restoredVillager) {
               this.applyCuringRewards(level, restoredVillager);
            }

            level.levelEvent(null, 1027, this.blockPosition(), 0);
            CuredVillagerMemory.consume(this.getUUID());
         }
      }
   }

   private void applyCuringRewards(ServerLevel level, Villager villager) {
      if (PersistentEntityData.get(this).hasUUID("SlimPatchCuringPlayer") && level.getServer() != null) {
         UUID playerUuid = PersistentEntityData.get(this).getUUID("SlimPatchCuringPlayer");
         ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerUuid);
         if (player != null) {
            if (villager.isAlive() && !villager.isRemoved()) {
               level.onReputationEvent(ReputationEventType.ZOMBIE_VILLAGER_CURED, player, villager);
            }

            this.applyRelationshipReward(villager, player);
         }
      }
   }

   private static String getSavedGender(CompoundTag saved) {
      return saved != null && !saved.isEmpty() && saved.contains("slimpatch_gender") ? normalizeGender(saved.getString("slimpatch_gender")) : null;
   }

   private static String normalizeGender(String gender) {
      return "female".equalsIgnoreCase(gender) ? "female" : "male";
   }

   private void applyRelationshipReward(Villager villager, ServerPlayer player) {
      if (villager instanceof MaleVillagerEntity maleVillager) {
         float current = VillagerRelationshipData.getRelationshipForPlayer(maleVillager, player.getUUID(), maleVillager.getRelationship());
         float updated = VillagerRelationshipData.setRelationshipForPlayer(maleVillager, player.getUUID(), current + 2.0F, maleVillager.getRelationship());
         if (!VillagerRelationshipData.usesPerPlayerRelationships(maleVillager)) {
            maleVillager.setRelationship(updated);
         }
      } else if (villager instanceof FemaleVillagerEntity femaleVillager) {
         float current = VillagerRelationshipData.getRelationshipForPlayer(femaleVillager, player.getUUID(), femaleVillager.getRelationship());
         float updated = VillagerRelationshipData.setRelationshipForPlayer(femaleVillager, player.getUUID(), current + 2.0F, femaleVillager.getRelationship());
         if (!VillagerRelationshipData.usesPerPlayerRelationships(femaleVillager)) {
            femaleVillager.setRelationship(updated);
         }
      }
   }
}
