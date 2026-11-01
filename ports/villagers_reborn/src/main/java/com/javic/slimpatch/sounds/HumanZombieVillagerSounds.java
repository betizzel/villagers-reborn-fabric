package com.javic.slimpatch.sounds;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class HumanZombieVillagerSounds {
   public static final SoundEvent FEMALE_AMBIENT = register("zombie_villager.female.ambient");
   public static final SoundEvent FEMALE_HURT = register("zombie_villager.female.hurt");
   public static final SoundEvent FEMALE_DEATH = register("zombie_villager.female.death");

   private static SoundEvent register(String name) {
      ResourceLocation id = new ResourceLocation("slimpatch", name);
      return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
   }

   public static SoundEvent femaleAmbient() { return FEMALE_AMBIENT; }
   public static SoundEvent femaleHurt() { return FEMALE_HURT; }
   public static SoundEvent femaleDeath() { return FEMALE_DEATH; }

   public static void init() { }
   private HumanZombieVillagerSounds() { }
}
