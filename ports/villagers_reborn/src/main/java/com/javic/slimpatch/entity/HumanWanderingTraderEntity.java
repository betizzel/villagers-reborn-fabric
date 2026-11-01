package com.javic.slimpatch.entity;

import com.javic.slimpatch.fabric.PersistentEntityData;

import com.javic.slimpatch.ModEntities;
import com.javic.slimpatch.sounds.HumanVillagerSounds;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

public class HumanWanderingTraderEntity extends WanderingTrader {
   private static final int LEGACY_CLEANUP_DESPAWN_DELAY = 2400;
   private static final int MALE_SKINS = 3;
   private static final int FEMALE_SKINS = 3;
   private static final EntityDataAccessor<Integer> DATA_SKIN = SynchedEntityData.defineId(HumanWanderingTraderEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> DATA_IS_FEMALE = SynchedEntityData.defineId(HumanWanderingTraderEntity.class, EntityDataSerializers.BOOLEAN);

   public HumanWanderingTraderEntity(EntityType<? extends WanderingTrader> type, Level level) {
      super(type, level);
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(DATA_SKIN, 0);
      this.entityData.define(DATA_IS_FEMALE, false);
   }

   public void setSkinIndex(int index) {
      this.entityData.set(DATA_SKIN, index);
      PersistentEntityData.get(this).putInt("hv_skin", index);
   }

   public int getSkinIndex() {
      return (Integer)this.entityData.get(DATA_SKIN);
   }

   public void setFemale(boolean female) {
      this.entityData.set(DATA_IS_FEMALE, female);
      PersistentEntityData.get(this).putBoolean("hv_isFemale", female);
   }

   public boolean isFemale() {
      return (Boolean)this.entityData.get(DATA_IS_FEMALE);
   }

   public ResourceLocation getSkinTexture() {
      String gender = this.isFemale() ? "female" : "male";
      return new ResourceLocation(
         "slimpatch", "textures/entity/human_trader/" + gender + "/skin_" + String.format("%02d", this.getSkinIndex()) + ".png"
      );
   }

   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag tag
   ) {
      SpawnGroupData groupData = super.finalizeSpawn(level, difficulty, reason, spawnData, tag);
      CompoundTag data = PersistentEntityData.get(this);
      if (!data.contains("hv_initialized")) {
         boolean female = level.getRandom().nextBoolean();
         this.setFemale(female);
         int maxSkins = female ? 3 : 3;
         int skinIndex = this.getRandom().nextInt(maxSkins) + 1;
         this.setSkinIndex(skinIndex);
         if (!this.hasCustomName()) {
            String name = female ? "Wanderer" : "Traveler";
            this.setCustomName(Component.literal(name));
            this.setCustomNameVisible(true);
         }

         data.putBoolean("hv_initialized", true);
      }

      return groupData;
   }

   public InteractionResult mobInteract(Player player, InteractionHand hand) {
      if (!this.level().isClientSide && hand == InteractionHand.MAIN_HAND) {
         this.level()
            .playSound(
               null,
               this.blockPosition(),
               this.isFemale() ? HumanVillagerSounds.femaleClick() : HumanVillagerSounds.maleClick(),
               SoundSource.NEUTRAL,
               1.0F,
               1.0F
            );
      }

      return super.mobInteract(player, hand);
   }

   protected SoundEvent getAmbientSound() {
      return this.isFemale() ? HumanVillagerSounds.femaleClick() : HumanVillagerSounds.maleClick();
   }

   public int getAmbientSoundInterval() {
      return 400;
   }

   public SoundEvent getNotifyTradeSound() {
      return this.isFemale() ? HumanVillagerSounds.femaleClick() : HumanVillagerSounds.maleClick();
   }

   protected SoundEvent getHurtSound(DamageSource source) {
      return this.isFemale() ? HumanVillagerSounds.femaleHurt() : HumanVillagerSounds.maleHurt();
   }

   protected SoundEvent getDeathSound() {
      return this.isFemale() ? HumanVillagerSounds.femaleDeath() : HumanVillagerSounds.maleDeath();
   }

   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      if (tag.contains("hv_skin")) {
         this.setSkinIndex(tag.getInt("hv_skin"));
      }

      if (tag.contains("hv_isFemale")) {
         this.setFemale(tag.getBoolean("hv_isFemale"));
      }
   }

   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putInt("hv_skin", this.getSkinIndex());
      tag.putBoolean("hv_isFemale", this.isFemale());
   }

   public void aiStep() {
      if (!this.level().isClientSide
         && (this.getType() == ModEntities.HUMAN_WANDERING_TRADER || this.getType() == ModEntities.HUMAN_TRADER_NATURAL)
         && this.getDespawnDelay() <= 0) {
         this.setDespawnDelay(2400);
      }

      super.aiStep();
   }
}
