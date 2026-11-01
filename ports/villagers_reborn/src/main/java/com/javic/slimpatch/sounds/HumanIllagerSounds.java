package com.javic.slimpatch.sounds;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class HumanIllagerSounds {
   public static final SoundEvent MALE_AMBIENT = register("illager.male.ambient");
   public static final SoundEvent MALE_HURT = register("illager.male.hurt");
   public static final SoundEvent MALE_DEATH = register("illager.male.death");
   public static final SoundEvent FEMALE_AMBIENT = register("illager.female.ambient");
   public static final SoundEvent FEMALE_HURT = register("illager.female.hurt");
   public static final SoundEvent FEMALE_DEATH = register("illager.female.death");

   private static SoundEvent register(String name) {
      ResourceLocation id = new ResourceLocation("slimpatch", name);
      return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
   }

   public static SoundEvent maleAmbient() { return MALE_AMBIENT; }
   public static SoundEvent femaleAmbient() { return FEMALE_AMBIENT; }
   public static SoundEvent maleHurt() { return MALE_HURT; }
   public static SoundEvent femaleHurt() { return FEMALE_HURT; }
   public static SoundEvent maleDeath() { return MALE_DEATH; }
   public static SoundEvent femaleDeath() { return FEMALE_DEATH; }

   public static void init() { }
   private HumanIllagerSounds() { }
}
