package com.javic.slimpatch.sounds;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class HumanVillagerSounds {
   public static final SoundEvent MALE_CLICK = register("male_click");
   public static final SoundEvent FEMALE_CLICK = register("female_click");
   public static final SoundEvent MALE_REACTION_POSITIVE = register("villager.male.reaction.positive");
   public static final SoundEvent MALE_REACTION_NEGATIVE = register("villager.male.reaction.negative");
   public static final SoundEvent FEMALE_REACTION_POSITIVE = register("villager.female.reaction.positive");
   public static final SoundEvent FEMALE_REACTION_NEGATIVE = register("villager.female.reaction.negative");
   public static final SoundEvent MALE_HURT = register("male_hurt");
   public static final SoundEvent FEMALE_HURT = register("female_hurt");
   public static final SoundEvent MALE_DEATH = register("male_death");
   public static final SoundEvent FEMALE_DEATH = register("female_death");
   public static final SoundEvent WEDDING_CUTSCENE_1 = register("wedding_cutscene_1");
   public static final SoundEvent WEDDING_CUTSCENE_2 = register("wedding_cutscene_2");
   public static final SoundEvent WEDDING_CUTSCENE_3 = register("wedding_cutscene_3");
   public static final SoundEvent WEDDING_CUTSCENE_4 = register("wedding_cutscene_4");
   public static final SoundEvent FAMILY_CUTSCENE_1 = register("family_cutscene_1");
   public static final SoundEvent FAMILY_CUTSCENE_2 = register("family_cutscene_2");
   public static final SoundEvent FAMILY_CUTSCENE_3 = register("family_cutscene_3");
   public static final SoundEvent FAMILY_CUTSCENE_4 = register("family_cutscene_4");

   private static SoundEvent register(String name) {
      ResourceLocation id = new ResourceLocation("slimpatch", name);
      return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
   }

   public static SoundEvent maleClick() { return MALE_CLICK; }
   public static SoundEvent femaleClick() { return FEMALE_CLICK; }
   public static SoundEvent maleReactionPositive() { return MALE_REACTION_POSITIVE; }
   public static SoundEvent maleReactionNegative() { return MALE_REACTION_NEGATIVE; }
   public static SoundEvent femaleReactionPositive() { return FEMALE_REACTION_POSITIVE; }
   public static SoundEvent femaleReactionNegative() { return FEMALE_REACTION_NEGATIVE; }
   public static SoundEvent maleHurt() { return MALE_HURT; }
   public static SoundEvent femaleHurt() { return FEMALE_HURT; }
   public static SoundEvent maleDeath() { return MALE_DEATH; }
   public static SoundEvent femaleDeath() { return FEMALE_DEATH; }
   public static SoundEvent weddingCutscene1() { return WEDDING_CUTSCENE_1; }
   public static SoundEvent weddingCutscene2() { return WEDDING_CUTSCENE_2; }
   public static SoundEvent weddingCutscene3() { return WEDDING_CUTSCENE_3; }
   public static SoundEvent weddingCutscene4() { return WEDDING_CUTSCENE_4; }
   public static SoundEvent familyCutscene1() { return FAMILY_CUTSCENE_1; }
   public static SoundEvent familyCutscene2() { return FAMILY_CUTSCENE_2; }
   public static SoundEvent familyCutscene3() { return FAMILY_CUTSCENE_3; }
   public static SoundEvent familyCutscene4() { return FAMILY_CUTSCENE_4; }

   public static void init() { }
   private HumanVillagerSounds() { }
}
