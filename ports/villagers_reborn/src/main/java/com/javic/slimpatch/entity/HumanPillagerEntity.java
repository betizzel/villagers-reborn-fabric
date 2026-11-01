package com.javic.slimpatch.entity;

import com.javic.slimpatch.fabric.PersistentEntityData;

import com.javic.slimpatch.Config;
import com.javic.slimpatch.sounds.HumanIllagerSounds;
import com.javic.slimpatch.util.SkinPathHelper;
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
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

public class HumanPillagerEntity extends Pillager {
   private static final int MALE_SKINS = 8;
   private static final int FEMALE_SKINS = 8;
   private static final EntityDataAccessor<Integer> DATA_SKIN = SynchedEntityData.defineId(HumanPillagerEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<String> DATA_GENDER = SynchedEntityData.defineId(HumanPillagerEntity.class, EntityDataSerializers.STRING);

   public HumanPillagerEntity(EntityType<? extends Pillager> type, Level level) {
      super(type, level);
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(DATA_SKIN, 0);
      this.entityData.define(DATA_GENDER, "male");
   }

   public boolean isFemale() {
      return "female".equalsIgnoreCase((String)this.entityData.get(DATA_GENDER));
   }

   public void setGender(String gender) {
      this.entityData.set(DATA_GENDER, gender);
      PersistentEntityData.get(this).putString("slimpatch_gender", gender);
   }

   public String getGender() {
      return (String)this.entityData.get(DATA_GENDER);
   }

   public void setSkinIndex(int index) {
      this.entityData.set(DATA_SKIN, index);
      PersistentEntityData.get(this).putInt("slimpatch_skin", index);
   }

   public int getSkinIndex() {
      return (Integer)this.entityData.get(DATA_SKIN);
   }

   public ResourceLocation getSkinTexture() {
      return SkinPathHelper.getSkinForType("human_pillager", this.getGender(), this.getSkinIndex(), this.level());
   }

   protected ResourceLocation getDefaultLootTable() {
      return new ResourceLocation("minecraft", "entities/pillager");
   }

   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag tag
   ) {
      SpawnGroupData groupData = super.finalizeSpawn(level, difficulty, reason, spawnData, tag);
      CompoundTag data = PersistentEntityData.get(this);
      if (!data.contains("slimpatch_initialized")) {
         boolean female = this.getRandom().nextBoolean();
         String gender = female ? "female" : "male";
         this.setGender(gender);
         int skinIndex = this.getRandom().nextInt(female ? 8 : 8) + 1;
         this.setSkinIndex(skinIndex);
         data.putString("slimpatch_gender", gender);
         data.putInt("slimpatch_skin", skinIndex);
         data.putBoolean("slimpatch_initialized", true);
         this.setCustomName(null);
         this.setCustomNameVisible(false);
      } else {
         if (data.contains("slimpatch_gender")) {
            this.setGender(data.getString("slimpatch_gender"));
         }

         if (data.contains("slimpatch_skin")) {
            this.setSkinIndex(data.getInt("slimpatch_skin"));
         }
      }

      return groupData;
   }

   protected SoundEvent getAmbientSound() {
      return Config.CUSTOM_ILLAGER_SOUNDS.get()
         ? (this.isFemale() ? HumanIllagerSounds.femaleAmbient() : HumanIllagerSounds.maleAmbient())
         : super.getAmbientSound();
   }

   protected SoundEvent getHurtSound(DamageSource source) {
      return Config.CUSTOM_ILLAGER_SOUNDS.get()
         ? (this.isFemale() ? HumanIllagerSounds.femaleHurt() : HumanIllagerSounds.maleHurt())
         : super.getHurtSound(source);
   }

   protected SoundEvent getDeathSound() {
      return Config.CUSTOM_ILLAGER_SOUNDS.get() ? (this.isFemale() ? HumanIllagerSounds.femaleDeath() : HumanIllagerSounds.maleDeath()) : super.getDeathSound();
   }

   public InteractionResult mobInteract(Player player, InteractionHand hand) {
      if (!this.level().isClientSide && hand == InteractionHand.MAIN_HAND) {
         this.level().playSound(null, this.blockPosition(), this.getAmbientSound(), SoundSource.HOSTILE, 1.0F, 1.0F);
      }

      return InteractionResult.SUCCESS;
   }

   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      if (tag.contains("slimpatch_skin")) {
         this.setSkinIndex(tag.getInt("slimpatch_skin"));
      }

      if (tag.contains("slimpatch_gender")) {
         this.setGender(tag.getString("slimpatch_gender"));
      }
   }

   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putInt("slimpatch_skin", this.getSkinIndex());
      tag.putString("slimpatch_gender", this.getGender());
   }

   protected void registerGoals() {
      super.registerGoals();
   }

   public boolean shouldShowName() {
      return false;
   }

   public boolean isCustomNameVisible() {
      return false;
   }

   public Component getName() {
      return this.getType().getDescription();
   }
}
