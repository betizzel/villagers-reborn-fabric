package com.javic.slimpatch;

import com.javic.slimpatch.entity.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
   public static final EntityType<HumanWanderingTraderEntity> HUMAN_WANDERING_TRADER = register("wandering_trader", EntityType.Builder.of(HumanWanderingTraderEntity::new, MobCategory.CREATURE).sized(0.6F, 1.95F).build("minecraft:wandering_trader"));
   public static final EntityType<HumanWanderingTraderEntity> HUMAN_TRADER_NATURAL = register("human_trader", EntityType.Builder.of(HumanWanderingTraderEntity::new, MobCategory.CREATURE).sized(0.6F, 1.95F).build("slimpatch:human_trader"));
   public static final EntityType<MaleVillagerEntity> MALE_VILLAGER = register("male_villager", EntityType.Builder.of(MaleVillagerEntity::new, MobCategory.CREATURE).sized(0.6F, 1.95F).build("slimpatch:male_villager"));
   public static final EntityType<FemaleVillagerEntity> FEMALE_VILLAGER = register("female_villager", EntityType.Builder.of(FemaleVillagerEntity::new, MobCategory.CREATURE).sized(0.6F, 1.95F).build("slimpatch:female_villager"));
   public static final EntityType<HumanPillagerEntity> HUMAN_PILLAGER = register("pillager", EntityType.Builder.of(HumanPillagerEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).build("minecraft:pillager"));
   public static final EntityType<HumanVindicatorEntity> HUMAN_VINDICATOR = register("vindicator", EntityType.Builder.of(HumanVindicatorEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).build("minecraft:vindicator"));
   public static final EntityType<HumanEvokerEntity> HUMAN_EVOKER = register("evoker", EntityType.Builder.of(HumanEvokerEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).build("minecraft:evoker"));
   public static final EntityType<HumanZombieVillagerEntity> HUMAN_ZOMBIE_VILLAGER = register("human_zombie_villager", EntityType.Builder.of(HumanZombieVillagerEntity::new, MobCategory.MONSTER).sized(0.6F, 1.95F).build("slimpatch:human_zombie_villager"));

   private static <T extends net.minecraft.world.entity.Entity> EntityType<T> register(String name, EntityType<T> type) {
      return Registry.register(BuiltInRegistries.ENTITY_TYPE, new ResourceLocation("slimpatch", name), type);
   }

   public static void init() { }
   private ModEntities() { }
}
