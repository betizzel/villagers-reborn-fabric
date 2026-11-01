package com.javic.slimpatch;

import com.javic.slimpatch.config.ConfigStore;
import com.javic.slimpatch.config.ConfigStore.Value;
import com.javic.slimpatch.config.NewWorldSkinTypeMode;

public final class Config {
    public static final ConfigStore SPEC = new ConfigStore("villagersreborn.toml");
    public static final Value<NewWorldSkinTypeMode> NEW_WORLD_SKIN_TYPE = SPEC.enumeration("newWorldSkinType", NewWorldSkinTypeMode.SELECTION_SCREEN, NewWorldSkinTypeMode.class);
    public static final Value<Boolean> CUSTOM_VILLAGER_SKINS = SPEC.bool("customVillagerSkins", true);
    public static final Value<Boolean> CUSTOM_ILLAGER_SKINS = SPEC.bool("customIllagerSkins", true);
    public static final Value<Boolean> CUSTOM_WITCH_SKIN = SPEC.bool("customWitchSkin", true);
    public static final Value<Boolean> CUSTOM_PILLAGER_MODEL = SPEC.bool("customPillagerModel", true);
    public static final Value<Boolean> CUSTOM_VINDICATOR_MODEL = SPEC.bool("customVindicatorModel", true);
    public static final Value<Boolean> CUSTOM_EVOKER_MODEL = SPEC.bool("customEvokerModel", true);
    public static final Value<Boolean> CUSTOM_VILLAGER_SOUNDS = SPEC.bool("customVillagerSounds", true);
    public static final Value<Boolean> CUSTOM_ILLAGER_SOUNDS = SPEC.bool("customIllagerSounds", true);
    public static final Value<Boolean> VILLAGER_NAME_TAG = SPEC.bool("villagerNameTag", true);
    public static final Value<Boolean> PRESS_R_TO_TALK_INDICATOR = SPEC.bool("pressRToTalkIndicator", true);
    public static final Value<Double> DIALOGUE_CAMERA_SENSITIVITY = SPEC.decimal("dialogueCameraSensitivity", 1.0, 0.5, 1.5);
    public static final Value<Boolean> INVERT_DIALOGUE_CAMERA_X = SPEC.bool("invertDialogueCameraX", false);
    public static final Value<Boolean> INVERT_DIALOGUE_CAMERA_Y = SPEC.bool("invertDialogueCameraY", false);
    public static final Value<Boolean> DIALOGUE_SCREEN_BLUR = SPEC.bool("dialogueScreenBlur", false);
    public static final Value<Boolean> FORCE_FIRST_PERSON_IN_DIALOGUE = SPEC.bool("forceFirstPersonInDialogue", false);
    public static final Value<Double> HEALTH_PER_GOLDEN_CARROT = SPEC.decimal("healthPerGoldenCarrot", 5.0, 0.1, 100.0);
    public static final Value<Double> MAX_BONUS_HEALTH = SPEC.decimal("maxBonusHealth", 100.0, 0.0, 1000.0);
    public static final Value<Boolean> REPLACE_VANILLA_ENTITIES = SPEC.bool("replaceVanillaEntities", true);
    public static final Value<Boolean> GENDERED_RELATIONSHIP_LABELS = SPEC.bool("genderedRelationshipLabels", true);
    public static final Value<Boolean> ALLOW_MULTIPLE_PLAYER_SPOUSES = SPEC.bool("allowMultiplePlayerSpouses", true);
    public static final Value<Boolean> SPOUSE_COOKING_ENABLED = SPEC.bool("spouseCookingEnabled", true);
    public static final Value<Integer> SPOUSE_COOKING_COOLDOWN_TICKS = SPEC.integer("spouseCookingCooldownTicks", 24000, 1200, 240000);
    public static final Value<Boolean> ENABLE_WEDDING_CUTSCENE = SPEC.bool("enableWeddingCutscene", true);
    public static final Value<Boolean> ENABLE_FAMILY_CUTSCENE = SPEC.bool("enableFamilyCutscene", true);
    public static final Value<Integer> PREGNANCY_DURATION_TICKS = SPEC.integer("pregnancyDurationTicks", 144000, 20, 24000000);
    public static final Value<Boolean> ENABLE_PREGNANCY_BELLY = SPEC.bool("enablePregnancyBelly", true);
    public static final Value<Boolean> ENABLE_HAVING_CHILDREN = SPEC.bool("enableHavingChildren", true);
    public static final Value<Integer> FAMILY_CHARM_MAX_CHILDREN_PER_PAIR = SPEC.integer("familyCharmMaxChildrenPerPair", 3, 1, 20);
    public static final Value<Boolean> ENABLE_NATURAL_FAMILY_SPAWNS = SPEC.bool("enableNaturalFamilySpawns", true);
    public static final Value<Boolean> ENABLE_AGING = SPEC.bool("enableAging", true);
    public static final Value<Integer> TODDLER_DURATION_TICKS = SPEC.integer("toddlerDurationTicks", 144000, 20, 24000000);
    public static final Value<Integer> CHILD_DURATION_TICKS = SPEC.integer("childDurationTicks", 336000, 20, 24000000);
    public static final Value<Integer> TEEN_DURATION_TICKS = SPEC.integer("teenDurationTicks", 336000, 20, 24000000);

    static { SPEC.save(); }
    private Config() {}
}
