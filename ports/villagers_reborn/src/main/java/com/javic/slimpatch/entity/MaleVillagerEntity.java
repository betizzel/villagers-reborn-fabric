package com.javic.slimpatch.entity;

import com.javic.slimpatch.fabric.PersistentEntityData;

import com.javic.slimpatch.ModEntities;
import com.javic.slimpatch.config.VillagerNameConfig;
import com.javic.slimpatch.memory.CuredVillagerMemory;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;

public class MaleVillagerEntity extends AbstractRomanceVillagerEntity {
   private static final int MALE_SKINS = 150;

   public MaleVillagerEntity(EntityType<? extends Villager> type, Level level) {
      super(type, level);
   }

   @Override
   protected String villagerGender() {
      return "male";
   }

   @Override
   protected int skinCount() {
      return 150;
   }

   @Override
   protected String randomName() {
      return VillagerNameConfig.getRandomMaleName(this.getRandom());
   }

   @Override
   protected void tickGenderSpecific() {
      FamilyPregnancyHandler.tickPregnancy(this, this);
   }

   public static MaleVillagerEntity restoreFromCured(ServerLevel level, UUID zombieUUID, double x, double y, double z, float yRot, float xRot) {
      CompoundTag saved = CuredVillagerMemory.consume(zombieUUID);
      if (saved == null) {
         saved = new CompoundTag();
      }

      MaleVillagerEntity villager = (MaleVillagerEntity)((EntityType)ModEntities.MALE_VILLAGER).create(level);
      if (villager == null) {
         return null;
      }

      villager.moveTo(x, y, z, yRot, xRot);
      PersistentEntityData.get(villager).putBoolean("slimpatch_restored", true);
      villager.restoreFromSavedData(saved);
      return villager;
   }
}
