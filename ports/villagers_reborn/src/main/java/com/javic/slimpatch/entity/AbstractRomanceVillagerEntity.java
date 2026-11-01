package com.javic.slimpatch.entity;

import com.javic.slimpatch.fabric.PersistentEntityData;

import com.javic.slimpatch.Config;
import com.javic.slimpatch.ModEntities;
import com.javic.slimpatch.dialogue.DialogueGoal;
import com.javic.slimpatch.dialogue.DialogueManager;
import com.javic.slimpatch.familytree.FamilyTreeTracker;
import com.javic.slimpatch.item.ModItems;
import com.javic.slimpatch.memory.CuredVillagerMemory;
import com.javic.slimpatch.network.ModNetworking;
import com.javic.slimpatch.network.OpenDivorceConfirmationScreenPacket;
import com.javic.slimpatch.network.OpenMarriageProposalScreenPacket;
import com.javic.slimpatch.network.RelationshipSyncPacket;
import com.javic.slimpatch.network.StartWeddingCutscenePacket;
import com.javic.slimpatch.sounds.HumanVillagerSounds;
import com.javic.slimpatch.util.SkinPathHelper;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractRomanceVillagerEntity extends Villager implements CommandableVillager, VillagerEquipmentHolder, FamilyVillager {
   private static final double RELAXED_OWNER_LOOK_SUPPRESS_DISTANCE_SQR = 64.0;
   private static final double RELAXED_OWNER_LOOK_NEAR_DISTANCE_SQR = 16.0;
   private static final double RELAXED_OWNER_LOOK_MOVE_AWAY_SPEED_SQR = 0.01;
   private static final double RELAXED_OWNER_LOOK_MOVE_AWAY_DOT = 0.08;
   private static final Map<String, Long> OPTION_COOLDOWNS = Map.of("Friendly", 60000L, "Mean", 30000L, "Joke", 120000L, "Flirt", 180000L);
   protected static final EntityDataAccessor<Integer> DATA_SKIN = SynchedEntityData.defineId(AbstractRomanceVillagerEntity.class, EntityDataSerializers.INT);
   protected static final EntityDataAccessor<String> DATA_PERSONALITY = SynchedEntityData.defineId(
      AbstractRomanceVillagerEntity.class, EntityDataSerializers.STRING
   );
   protected static final EntityDataAccessor<Float> DATA_RELATIONSHIP = SynchedEntityData.defineId(
      AbstractRomanceVillagerEntity.class, EntityDataSerializers.FLOAT
   );
   public static final EntityDataAccessor<Boolean> DATA_HAS_QUEST = SynchedEntityData.defineId(
      AbstractRomanceVillagerEntity.class, EntityDataSerializers.BOOLEAN
   );
   public static final EntityDataAccessor<String> DATA_QUEST_ID = SynchedEntityData.defineId(AbstractRomanceVillagerEntity.class, EntityDataSerializers.STRING);
   protected static final EntityDataAccessor<String> DATA_CUSTOM_SKIN_PATH = SynchedEntityData.defineId(
      AbstractRomanceVillagerEntity.class, EntityDataSerializers.STRING
   );
   protected static final EntityDataAccessor<String> DATA_SAVED_SKIN_INPUT = SynchedEntityData.defineId(
      AbstractRomanceVillagerEntity.class, EntityDataSerializers.STRING
   );
   protected static final EntityDataAccessor<Integer> DATA_HEIGHT = SynchedEntityData.defineId(AbstractRomanceVillagerEntity.class, EntityDataSerializers.INT);
   protected static final EntityDataAccessor<Integer> DATA_WIDTH = SynchedEntityData.defineId(AbstractRomanceVillagerEntity.class, EntityDataSerializers.INT);
   protected static final EntityDataAccessor<String> DATA_COMMAND_STATE = SynchedEntityData.defineId(
      AbstractRomanceVillagerEntity.class, EntityDataSerializers.STRING
   );
   protected static final EntityDataAccessor<Boolean> DATA_HAS_HOME = SynchedEntityData.defineId(
      AbstractRomanceVillagerEntity.class, EntityDataSerializers.BOOLEAN
   );
   protected static final EntityDataAccessor<String> DATA_COMMAND_OWNER_UUID = SynchedEntityData.defineId(
      AbstractRomanceVillagerEntity.class, EntityDataSerializers.STRING
   );
   protected static final EntityDataAccessor<String> DATA_COMMAND_OWNER_NAME = SynchedEntityData.defineId(
      AbstractRomanceVillagerEntity.class, EntityDataSerializers.STRING
   );
   protected static final EntityDataAccessor<String> DATA_COMBAT_MODE = SynchedEntityData.defineId(
      AbstractRomanceVillagerEntity.class, EntityDataSerializers.STRING
   );
   protected static final EntityDataAccessor<String> DATA_FOLLOW_MODE = SynchedEntityData.defineId(
      AbstractRomanceVillagerEntity.class, EntityDataSerializers.STRING
   );
   protected static final EntityDataAccessor<Boolean> DATA_HIDE_ARMOR = SynchedEntityData.defineId(
      AbstractRomanceVillagerEntity.class, EntityDataSerializers.BOOLEAN
   );
   protected static final EntityDataAccessor<Boolean> DATA_MUTED = SynchedEntityData.defineId(
      AbstractRomanceVillagerEntity.class, EntityDataSerializers.BOOLEAN
   );
   protected static final EntityDataAccessor<Float> DATA_GOLDEN_RELATIONSHIP = SynchedEntityData.defineId(
      AbstractRomanceVillagerEntity.class, EntityDataSerializers.FLOAT
   );
   protected static final EntityDataAccessor<String> DATA_SPOUSE_PLAYER_NAME = SynchedEntityData.defineId(
      AbstractRomanceVillagerEntity.class, EntityDataSerializers.STRING
   );
   protected static final EntityDataAccessor<String> DATA_FORMER_SPOUSE_PLAYER_NAME = SynchedEntityData.defineId(
      AbstractRomanceVillagerEntity.class, EntityDataSerializers.STRING
   );
   protected static final EntityDataAccessor<String> DATA_AGE_STAGE = SynchedEntityData.defineId(
      AbstractRomanceVillagerEntity.class, EntityDataSerializers.STRING
   );
   protected static final EntityDataAccessor<String> DATA_RELATIONSHIP_STAGE = SynchedEntityData.defineId(
      AbstractRomanceVillagerEntity.class, EntityDataSerializers.STRING
   );
   protected static final EntityDataAccessor<Boolean> DATA_EXPECTING_CHILD = SynchedEntityData.defineId(
      AbstractRomanceVillagerEntity.class, EntityDataSerializers.BOOLEAN
   );
   protected static final EntityDataAccessor<Boolean> DATA_BORN_FROM_FAMILY_SYSTEM = SynchedEntityData.defineId(
      AbstractRomanceVillagerEntity.class, EntityDataSerializers.BOOLEAN
   );
   protected static final EntityDataAccessor<String> DATA_PARENT_PLAYER_UUID = SynchedEntityData.defineId(
      AbstractRomanceVillagerEntity.class, EntityDataSerializers.STRING
   );
   private VillagerCommandState commandState = VillagerCommandState.NONE;
   private UUID commandTargetUuid;
   private UUID commandOwnerUuid;
   private UUID spousePlayerUuid;
   private UUID formerSpousePlayerUuid;
   private UUID spouseVillagerUuid;
   private UUID expectingParentPlayerUuid;
   private UUID expectingOtherParentVillagerUuid;
   private UUID parentPlayerUuid;
   private UUID parentVillagerUuid;
   private UUID parentVillager2Uuid;
   private UUID familyOwnerPlayerUuid;
   private UUID naturalFamilyGroupId;
   private BlockPos stayPos;
   private Vec3 stayAnchorPos;
   private BlockPos homePos;
   private ResourceKey<Level> homeDimension;
   private Vec3 dialogueHoldPos;
   private long lastCommandTeleportTick;
   private int combatLockTicks;
   private int lastDamageTick;
   private int lastAggressiveScanTick;
   private int lastBowAttackTick;
   private int bowChargeStartTick = -1;
   private int lastCrossbowAttackTick;
   private int crossbowChargeStartTick = -1;
   private int lastShieldBlockTick;
   private int shieldRaiseUntilTick;
   private float bonusHealth;
   private double baseMaxHealth;
   private final SimpleContainer equipmentInventory = new SimpleContainer(27);
   private ItemStack persistentMainHandItem = ItemStack.EMPTY;

   protected AbstractRomanceVillagerEntity(EntityType<? extends Villager> type, Level level) {
      super(type, level);
   }

   protected abstract String villagerGender();

   protected abstract int skinCount();

   protected abstract String randomName();

   protected abstract void tickGenderSpecific();

   protected SoundEvent clickSound() {
      return "female".equals(this.villagerGender()) ? HumanVillagerSounds.femaleClick() : HumanVillagerSounds.maleClick();
   }

   protected SoundEvent hurtVoice() {
      return "female".equals(this.villagerGender()) ? HumanVillagerSounds.femaleHurt() : HumanVillagerSounds.maleHurt();
   }

   protected SoundEvent deathVoice() {
      return "female".equals(this.villagerGender()) ? HumanVillagerSounds.femaleDeath() : HumanVillagerSounds.maleDeath();
   }

   protected SoundEvent positiveReactionSound() {
      return "female".equals(this.villagerGender()) ? HumanVillagerSounds.femaleReactionPositive() : HumanVillagerSounds.maleReactionPositive();
   }

   protected SoundEvent negativeReactionSound() {
      return "female".equals(this.villagerGender()) ? HumanVillagerSounds.femaleReactionNegative() : HumanVillagerSounds.maleReactionNegative();
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(DATA_SKIN, 0);
      this.entityData.define(DATA_PERSONALITY, VillagerPersonality.FRIENDLY.name());
      this.entityData.define(DATA_RELATIONSHIP, 0.5F);
      this.entityData.define(DATA_HAS_QUEST, false);
      this.entityData.define(DATA_QUEST_ID, "");
      this.entityData.define(DATA_CUSTOM_SKIN_PATH, "");
      this.entityData.define(DATA_SAVED_SKIN_INPUT, "");
      this.entityData.define(DATA_HEIGHT, 100);
      this.entityData.define(DATA_WIDTH, 100);
      this.entityData.define(DATA_COMMAND_STATE, VillagerCommandState.NONE.name());
      this.entityData.define(DATA_HAS_HOME, false);
      this.entityData.define(DATA_COMMAND_OWNER_UUID, "");
      this.entityData.define(DATA_COMMAND_OWNER_NAME, "");
      this.entityData.define(DATA_COMBAT_MODE, VillagerCombatMode.AGGRESSIVE.name());
      this.entityData.define(DATA_FOLLOW_MODE, VillagerFollowMode.CLOSE.name());
      this.entityData.define(DATA_HIDE_ARMOR, false);
      this.entityData.define(DATA_MUTED, false);
      this.entityData.define(DATA_GOLDEN_RELATIONSHIP, 0.0F);
      this.entityData.define(DATA_SPOUSE_PLAYER_NAME, "");
      this.entityData.define(DATA_FORMER_SPOUSE_PLAYER_NAME, "");
      this.entityData.define(DATA_AGE_STAGE, VillagerAgeStage.ADULT.name());
      this.entityData.define(DATA_RELATIONSHIP_STAGE, VillagerRelationshipStage.FRIENDSHIP.name());
      this.entityData.define(DATA_EXPECTING_CHILD, false);
      this.entityData.define(DATA_BORN_FROM_FAMILY_SYSTEM, false);
      this.entityData.define(DATA_PARENT_PLAYER_UUID, "");
   }

   public void setSkinIndex(int index) {
      this.entityData.set(DATA_SKIN, index);
      PersistentEntityData.get(this).putInt("slimpatch_skin", index);
   }

   public int getSkinIndex() {
      return (Integer)this.entityData.get(DATA_SKIN);
   }

   public VillagerPersonality getPersonality() {
      try {
         return VillagerPersonality.valueOf((String)this.entityData.get(DATA_PERSONALITY));
      } catch (IllegalArgumentException e) {
         return VillagerPersonality.FRIENDLY;
      }
   }

   public void setPersonality(VillagerPersonality personality) {
      VillagerPersonality resolved = VillagerFamilyData.sanitizePersonality(this, personality);
      this.entityData.set(DATA_PERSONALITY, resolved.name());
      PersistentEntityData.get(this).putString("slimpatch_personality", resolved.name());
   }

   public float getRelationship() {
      return (Float)this.entityData.get(DATA_RELATIONSHIP);
   }

   public void setRelationship(float value) {
      float clamped = Math.max(0.0F, Math.min(5.0F, value));
      this.entityData.set(DATA_RELATIONSHIP, clamped);
      PersistentEntityData.get(this).putFloat("slimpatch_relationship", clamped);
   }

   public boolean hasQuest() {
      return (Boolean)this.entityData.get(DATA_HAS_QUEST);
   }

   public void setHasQuest(boolean value) {
      this.entityData.set(DATA_HAS_QUEST, value);
      PersistentEntityData.get(this).putBoolean("HasQuest", value);
   }

   public String getQuestId() {
      return (String)this.entityData.get(DATA_QUEST_ID);
   }

   public void setQuestId(String id) {
      String value = id == null ? "" : id;
      this.entityData.set(DATA_QUEST_ID, value);
      if (value.isEmpty()) {
         PersistentEntityData.get(this).remove("QuestId");
      } else {
         PersistentEntityData.get(this).putString("QuestId", value);
      }
   }

   public String getCustomSkinPath() {
      return (String)this.entityData.get(DATA_CUSTOM_SKIN_PATH);
   }

   public void setCustomSkinPath(String path) {
      String value = path == null ? "" : path;
      this.entityData.set(DATA_CUSTOM_SKIN_PATH, value);
      if (value.isEmpty()) {
         PersistentEntityData.get(this).remove("CustomSkinPath");
      } else {
         PersistentEntityData.get(this).putString("CustomSkinPath", value);
      }
   }

   public String getSavedSkinInput() {
      return (String)this.entityData.get(DATA_SAVED_SKIN_INPUT);
   }

   public void setSavedSkinInput(String input) {
      String value = input == null ? "" : input;
      this.entityData.set(DATA_SAVED_SKIN_INPUT, value);
      if (value.isEmpty()) {
         PersistentEntityData.get(this).remove("SavedSkinInput");
      } else {
         PersistentEntityData.get(this).putString("SavedSkinInput", value);
      }
   }

   public int getVisualHeight() {
      return (Integer)this.entityData.get(DATA_HEIGHT);
   }

   public void setVisualHeight(int value) {
      int clamped = Math.max(50, Math.min(150, value));
      this.entityData.set(DATA_HEIGHT, clamped);
      PersistentEntityData.get(this).putInt("Height", clamped);
   }

   public int getVisualWidth() {
      return (Integer)this.entityData.get(DATA_WIDTH);
   }

   public void setVisualWidth(int value) {
      int clamped = Math.max(50, Math.min(150, value));
      this.entityData.set(DATA_WIDTH, clamped);
      PersistentEntityData.get(this).putInt("Width", clamped);
   }

   public ResourceLocation getSkinTexture() {
      CompoundTag data = PersistentEntityData.get(this);
      String customSkinPath = this.getCustomSkinPath();
      if (customSkinPath.isEmpty() && data.contains("CustomSkinPath")) {
         customSkinPath = data.getString("CustomSkinPath");
      }

      if (!customSkinPath.isEmpty()) {
         ResourceLocation internal = ResourceLocation.tryParse(customSkinPath);
         if (internal != null) {
            return internal;
         }

         File external = new File(customSkinPath);
         if (external.exists() && external.isFile()) {
            ResourceLocation loc = SkinPathHelper.loadExternalSkinTexture(this.getUUID(), external);
            if (loc != null) {
               return loc;
            }
         }
      }

      int skin = this.getSkinIndex();
      if (skin <= 0 || skin > this.skinCount()) {
         skin = data.contains("slimpatch_skin") ? data.getInt("slimpatch_skin") : this.getRandom().nextInt(this.skinCount()) + 1;
         this.setSkinIndex(skin);
      }

      return SkinPathHelper.getSkin(this.villagerGender(), skin, this.level());
   }

   @Override
   public VillagerCombatMode getCombatMode() {
      return VillagerCombatMode.fromName((String)this.entityData.get(DATA_COMBAT_MODE));
   }

   @Override
   public void setCombatMode(VillagerCombatMode combatMode) {
      VillagerCombatMode resolved = combatMode == null ? VillagerCombatMode.AGGRESSIVE : combatMode;
      this.entityData.set(DATA_COMBAT_MODE, resolved.name());
      PersistentEntityData.get(this).putString("SlimPatchCombatMode", resolved.name());
   }

   @Override
   public VillagerFollowMode getFollowMode() {
      return VillagerFollowMode.fromName((String)this.entityData.get(DATA_FOLLOW_MODE));
   }

   @Override
   public void setFollowMode(VillagerFollowMode followMode) {
      VillagerFollowMode resolved = followMode == null ? VillagerFollowMode.CLOSE : followMode;
      this.entityData.set(DATA_FOLLOW_MODE, resolved.name());
      PersistentEntityData.get(this).putString("SlimPatchFollowMode", resolved.name());
   }

   @Override
   public boolean isArmorHidden() {
      return (Boolean)this.entityData.get(DATA_HIDE_ARMOR);
   }

   @Override
   public void setArmorHidden(boolean hidden) {
      this.entityData.set(DATA_HIDE_ARMOR, hidden);
      PersistentEntityData.get(this).putBoolean("SlimPatchHideArmor", hidden);
   }

   @Override
   public boolean isMuted() {
      return (Boolean)this.entityData.get(DATA_MUTED);
   }

   @Override
   public void setMuted(boolean muted) {
      this.entityData.set(DATA_MUTED, muted);
      PersistentEntityData.get(this).putBoolean("SlimPatchMuted", muted);
   }

   @Override
   public float getGoldenRelationship() {
      return (Float)this.entityData.get(DATA_GOLDEN_RELATIONSHIP);
   }

   @Override
   public void setGoldenRelationship(float value) {
      float clamped = VillagerFamilyData.clampGoldenRelationship(value);
      this.entityData.set(DATA_GOLDEN_RELATIONSHIP, clamped);
      PersistentEntityData.get(this).putFloat("SlimPatchGoldenRelationship", clamped);
      if (clamped <= 0.0F && this.getRelationshipStage() == VillagerRelationshipStage.DATING) {
         this.setRelationshipStage(VillagerRelationshipStage.FRIENDSHIP);
      }
   }

   @Override
   public UUID getSpousePlayerUuid() {
      return this.spousePlayerUuid;
   }

   @Override
   public void setSpousePlayerUuid(UUID uuid) {
      this.spousePlayerUuid = uuid;
      VillagerFamilyData.writeSpousePlayerUuid(PersistentEntityData.get(this), uuid);
   }

   @Override
   public String getSpousePlayerName() {
      return (String)this.entityData.get(DATA_SPOUSE_PLAYER_NAME);
   }

   @Override
   public void setSpousePlayerName(String name) {
      String value = name == null ? "" : name;
      this.entityData.set(DATA_SPOUSE_PLAYER_NAME, value);
      VillagerFamilyData.writeOptionalString(PersistentEntityData.get(this), "SlimPatchSpousePlayerName", value);
   }

   @Override
   public UUID getFormerSpousePlayerUuid() {
      return this.formerSpousePlayerUuid;
   }

   @Override
   public void setFormerSpousePlayerUuid(UUID uuid) {
      this.formerSpousePlayerUuid = uuid;
      VillagerFamilyData.writeFormerSpousePlayerUuid(PersistentEntityData.get(this), uuid);
   }

   @Override
   public String getFormerSpousePlayerName() {
      return (String)this.entityData.get(DATA_FORMER_SPOUSE_PLAYER_NAME);
   }

   @Override
   public void setFormerSpousePlayerName(String name) {
      String value = name == null ? "" : name;
      this.entityData.set(DATA_FORMER_SPOUSE_PLAYER_NAME, value);
      VillagerFamilyData.writeOptionalString(PersistentEntityData.get(this), "SlimPatchFormerSpousePlayerName", value);
   }

   @Override
   public UUID getSpouseVillagerUuid() {
      return this.spouseVillagerUuid;
   }

   @Override
   public void setSpouseVillagerUuid(UUID uuid) {
      this.spouseVillagerUuid = uuid;
      VillagerFamilyData.writeSpouseVillagerUuid(PersistentEntityData.get(this), uuid);
   }

   @Override
   public String getSpouseVillagerName() {
      return VillagerFamilyData.readOptionalString(PersistentEntityData.get(this), "SlimPatchSpouseVillagerName");
   }

   @Override
   public void setSpouseVillagerName(String name) {
      VillagerFamilyData.writeOptionalString(PersistentEntityData.get(this), "SlimPatchSpouseVillagerName", name);
   }

   @Override
   public VillagerRelationshipStage getRelationshipStage() {
      return VillagerFamilyData.parseRelationshipStage((String)this.entityData.get(DATA_RELATIONSHIP_STAGE));
   }

   @Override
   public void setRelationshipStage(VillagerRelationshipStage relationshipStage) {
      VillagerRelationshipStage resolved = relationshipStage == null ? VillagerRelationshipStage.FRIENDSHIP : relationshipStage;
      this.entityData.set(DATA_RELATIONSHIP_STAGE, resolved.name());
      PersistentEntityData.get(this).putString("SlimPatchRelationshipStage", resolved.name());
   }

   @Override
   public VillagerAgeStage getAgeStage() {
      return VillagerFamilyData.parseAgeStage((String)this.entityData.get(DATA_AGE_STAGE));
   }

   @Override
   public void setAgeStage(VillagerAgeStage ageStage) {
      VillagerAgeStage resolved = ageStage == null ? VillagerAgeStage.ADULT : ageStage;
      this.entityData.set(DATA_AGE_STAGE, resolved.name());
      PersistentEntityData.get(this).putString("SlimPatchAgeStage", resolved.name());
      this.refreshDimensions();
      if (!VillagerFamilyData.canHaveRomanticPersonality(this) && this.getPersonality() == VillagerPersonality.ROMANTIC) {
         this.setPersonality(VillagerPersonality.FRIENDLY);
      }

      if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
         FamilyTreeTracker.onVillagerAgeStageChanged(serverLevel.getServer(), this);
      }
   }

   @Override
   public long getAgeStageStartedAt() {
      return PersistentEntityData.get(this).contains("SlimPatchAgeStageStartedAt") ? PersistentEntityData.get(this).getLong("SlimPatchAgeStageStartedAt") : 0L;
   }

   @Override
   public void setAgeStageStartedAt(long startedAt) {
      if (startedAt <= 0L) {
         PersistentEntityData.get(this).remove("SlimPatchAgeStageStartedAt");
      } else {
         PersistentEntityData.get(this).putLong("SlimPatchAgeStageStartedAt", startedAt);
      }
   }

   @Override
   public long getNextAgeStageAt() {
      return PersistentEntityData.get(this).contains("SlimPatchNextAgeStageAt") ? PersistentEntityData.get(this).getLong("SlimPatchNextAgeStageAt") : 0L;
   }

   @Override
   public void setNextAgeStageAt(long nextAgeStageAt) {
      if (nextAgeStageAt <= 0L) {
         PersistentEntityData.get(this).remove("SlimPatchNextAgeStageAt");
      } else {
         PersistentEntityData.get(this).putLong("SlimPatchNextAgeStageAt", nextAgeStageAt);
      }
   }

   @Override
   public boolean isExpectingChild() {
      return (Boolean)this.entityData.get(DATA_EXPECTING_CHILD);
   }

   @Override
   public void setExpectingChild(boolean expectingChild) {
      this.entityData.set(DATA_EXPECTING_CHILD, expectingChild);
      PersistentEntityData.get(this).putBoolean("SlimPatchExpectingChild", expectingChild);
      if (!expectingChild) {
         this.setExpectingParentPlayerUuid(null);
         this.setExpectingParentPlayerName("");
         this.setExpectingOtherParentVillagerUuid(null);
         this.setExpectingOtherParentVillagerName("");
         this.setForcedBabyGender("");
         this.setExpectingStartedAt(0L);
         this.setExpectingEndsAt(0L);
      }
   }

   @Override
   public UUID getExpectingParentPlayerUuid() {
      return this.expectingParentPlayerUuid;
   }

   @Override
   public void setExpectingParentPlayerUuid(UUID uuid) {
      this.expectingParentPlayerUuid = uuid;
      VillagerFamilyData.writeExpectingParentPlayerUuid(PersistentEntityData.get(this), uuid);
   }

   @Override
   public String getExpectingParentPlayerName() {
      return VillagerFamilyData.readOptionalString(PersistentEntityData.get(this), "SlimPatchExpectingParentPlayerName");
   }

   @Override
   public void setExpectingParentPlayerName(String name) {
      VillagerFamilyData.writeOptionalString(PersistentEntityData.get(this), "SlimPatchExpectingParentPlayerName", name);
   }

   @Override
   public UUID getExpectingOtherParentVillagerUuid() {
      return this.expectingOtherParentVillagerUuid;
   }

   @Override
   public void setExpectingOtherParentVillagerUuid(UUID uuid) {
      this.expectingOtherParentVillagerUuid = uuid;
      VillagerFamilyData.writeExpectingOtherParentVillagerUuid(PersistentEntityData.get(this), uuid);
   }

   @Override
   public String getExpectingOtherParentVillagerName() {
      return VillagerFamilyData.readOptionalString(PersistentEntityData.get(this), "SlimPatchExpectingOtherParentVillagerName");
   }

   @Override
   public void setExpectingOtherParentVillagerName(String name) {
      VillagerFamilyData.writeOptionalString(PersistentEntityData.get(this), "SlimPatchExpectingOtherParentVillagerName", name);
   }

   @Override
   public String getForcedBabyGender() {
      return VillagerFamilyData.readOptionalString(PersistentEntityData.get(this), "SlimPatchForcedBabyGender");
   }

   @Override
   public void setForcedBabyGender(String gender) {
      VillagerFamilyData.writeOptionalString(
         PersistentEntityData.get(this), "SlimPatchForcedBabyGender", gender != null && !gender.isEmpty() ? BirthScreenData.normalizeGender(gender) : ""
      );
   }

   @Override
   public long getExpectingStartedAt() {
      return PersistentEntityData.get(this).contains("SlimPatchExpectingStartedAt") ? PersistentEntityData.get(this).getLong("SlimPatchExpectingStartedAt") : 0L;
   }

   @Override
   public void setExpectingStartedAt(long startedAt) {
      if (startedAt <= 0L) {
         PersistentEntityData.get(this).remove("SlimPatchExpectingStartedAt");
      } else {
         PersistentEntityData.get(this).putLong("SlimPatchExpectingStartedAt", startedAt);
      }
   }

   @Override
   public long getExpectingEndsAt() {
      return PersistentEntityData.get(this).contains("SlimPatchExpectingEndsAt") ? PersistentEntityData.get(this).getLong("SlimPatchExpectingEndsAt") : 0L;
   }

   @Override
   public void setExpectingEndsAt(long endsAt) {
      if (endsAt <= 0L) {
         PersistentEntityData.get(this).remove("SlimPatchExpectingEndsAt");
      } else {
         PersistentEntityData.get(this).putLong("SlimPatchExpectingEndsAt", endsAt);
      }
   }

   @Override
   public boolean isBornFromFamilySystem() {
      return (Boolean)this.entityData.get(DATA_BORN_FROM_FAMILY_SYSTEM);
   }

   @Override
   public void setBornFromFamilySystem(boolean bornFromFamilySystem) {
      this.entityData.set(DATA_BORN_FROM_FAMILY_SYSTEM, bornFromFamilySystem);
      PersistentEntityData.get(this).putBoolean("SlimPatchBornFromFamilySystem", bornFromFamilySystem);
   }

   @Override
   public UUID getParentPlayerUuid() {
      return this.parentPlayerUuid;
   }

   @Override
   public void setParentPlayerUuid(UUID uuid) {
      this.parentPlayerUuid = uuid;
      this.entityData.set(DATA_PARENT_PLAYER_UUID, uuid == null ? "" : uuid.toString());
      VillagerFamilyData.writeParentPlayerUuid(PersistentEntityData.get(this), uuid);
   }

   @Override
   public String getParentPlayerName() {
      return VillagerFamilyData.readOptionalString(PersistentEntityData.get(this), "SlimPatchParentPlayerName");
   }

   @Override
   public void setParentPlayerName(String name) {
      VillagerFamilyData.writeOptionalString(PersistentEntityData.get(this), "SlimPatchParentPlayerName", name);
   }

   @Override
   public UUID getParentVillagerUuid() {
      return this.parentVillagerUuid;
   }

   @Override
   public void setParentVillagerUuid(UUID uuid) {
      this.parentVillagerUuid = uuid;
      VillagerFamilyData.writeParentVillagerUuid(PersistentEntityData.get(this), uuid);
   }

   @Override
   public String getParentVillagerName() {
      return VillagerFamilyData.readOptionalString(PersistentEntityData.get(this), "SlimPatchParentVillagerName");
   }

   @Override
   public void setParentVillagerName(String name) {
      VillagerFamilyData.writeOptionalString(PersistentEntityData.get(this), "SlimPatchParentVillagerName", name);
   }

   @Override
   public UUID getParentVillager2Uuid() {
      return this.parentVillager2Uuid;
   }

   @Override
   public void setParentVillager2Uuid(UUID uuid) {
      this.parentVillager2Uuid = uuid;
      VillagerFamilyData.writeParentVillager2Uuid(PersistentEntityData.get(this), uuid);
   }

   @Override
   public String getParentVillager2Name() {
      return VillagerFamilyData.readOptionalString(PersistentEntityData.get(this), "SlimPatchParentVillager2Name");
   }

   @Override
   public void setParentVillager2Name(String name) {
      VillagerFamilyData.writeOptionalString(PersistentEntityData.get(this), "SlimPatchParentVillager2Name", name);
   }

   @Override
   public UUID getNaturalFamilyGroupId() {
      return this.naturalFamilyGroupId;
   }

   @Override
   public void setNaturalFamilyGroupId(UUID uuid) {
      this.naturalFamilyGroupId = uuid;
      VillagerFamilyData.writeNaturalFamilyGroupId(PersistentEntityData.get(this), uuid);
   }

   @Override
   public boolean isNaturalFamilyMember() {
      return PersistentEntityData.get(this).getBoolean("SlimPatchNaturalFamilyMember");
   }

   @Override
   public void setNaturalFamilyMember(boolean naturalFamilyMember) {
      PersistentEntityData.get(this).putBoolean("SlimPatchNaturalFamilyMember", naturalFamilyMember);
   }

   @Override
   public UUID getFamilyOwnerPlayerUuid() {
      return this.familyOwnerPlayerUuid;
   }

   @Override
   public void setFamilyOwnerPlayerUuid(UUID uuid) {
      this.familyOwnerPlayerUuid = uuid;
      VillagerFamilyData.writeFamilyOwnerPlayerUuid(PersistentEntityData.get(this), uuid);
   }

   @Override
   public String getFamilyOwnerPlayerName() {
      return VillagerFamilyData.readOptionalString(PersistentEntityData.get(this), "SlimPatchFamilyOwnerPlayerName");
   }

   @Override
   public void setFamilyOwnerPlayerName(String name) {
      VillagerFamilyData.writeOptionalString(PersistentEntityData.get(this), "SlimPatchFamilyOwnerPlayerName", name);
   }

   @Override
   public VillagerCommandState getCommandState() {
      try {
         return VillagerCommandState.valueOf((String)this.entityData.get(DATA_COMMAND_STATE));
      } catch (IllegalArgumentException e) {
         return VillagerCommandState.NONE;
      }
   }

   @Override
   public void setCommandState(VillagerCommandState commandState) {
      this.commandState = commandState == null ? VillagerCommandState.NONE : commandState;
      this.entityData.set(DATA_COMMAND_STATE, this.commandState.name());
   }

   @Override
   public UUID getCommandTargetUuid() {
      return this.commandTargetUuid;
   }

   @Override
   public void setCommandTargetUuid(UUID commandTargetUuid) {
      this.commandTargetUuid = commandTargetUuid;
   }

   @Override
   public UUID getCommandOwnerUuid() {
      return this.commandOwnerUuid;
   }

   @Override
   public void setCommandOwnerUuid(UUID commandOwnerUuid) {
      this.commandOwnerUuid = commandOwnerUuid;
      this.entityData.set(DATA_COMMAND_OWNER_UUID, commandOwnerUuid == null ? "" : commandOwnerUuid.toString());
   }

   @Override
   public String getCommandOwnerName() {
      return (String)this.entityData.get(DATA_COMMAND_OWNER_NAME);
   }

   @Override
   public void setCommandOwnerName(String commandOwnerName) {
      this.entityData.set(DATA_COMMAND_OWNER_NAME, commandOwnerName == null ? "" : commandOwnerName);
   }

   @Override
   public BlockPos getStayPos() {
      return this.stayPos;
   }

   @Override
   public void setStayPos(BlockPos stayPos) {
      this.stayPos = stayPos;
   }

   @Override
   public Vec3 getStayAnchorPos() {
      return this.stayAnchorPos;
   }

   @Override
   public void setStayAnchorPos(Vec3 stayAnchorPos) {
      this.stayAnchorPos = stayAnchorPos;
   }

   @Override
   public boolean hasHome() {
      return (Boolean)this.entityData.get(DATA_HAS_HOME);
   }

   @Override
   public void setHome(BlockPos homePos, ResourceKey<Level> homeDimension) {
      this.homePos = homePos;
      this.homeDimension = homeDimension;
      this.entityData.set(DATA_HAS_HOME, homePos != null && homeDimension != null);
   }

   @Override
   public void clearHome() {
      this.homePos = null;
      this.homeDimension = null;
      this.entityData.set(DATA_HAS_HOME, false);
   }

   @Override
   public BlockPos getHomePos() {
      return this.homePos;
   }

   @Override
   public ResourceKey<Level> getHomeDimension() {
      return this.homeDimension;
   }

   @Override
   public long getLastCommandTeleportTick() {
      return this.lastCommandTeleportTick;
   }

   @Override
   public void setLastCommandTeleportTick(long tick) {
      this.lastCommandTeleportTick = tick;
   }

   @Override
   public int getCombatLockTicks() {
      return this.combatLockTicks;
   }

   @Override
   public void setCombatLockTicks(int ticks) {
      this.combatLockTicks = ticks;
   }

   @Override
   public int getLastDamageTick() {
      return this.lastDamageTick;
   }

   @Override
   public void setLastDamageTick(int tick) {
      this.lastDamageTick = tick;
   }

   @Override
   public int getLastAggressiveScanTick() {
      return this.lastAggressiveScanTick;
   }

   @Override
   public void setLastAggressiveScanTick(int tick) {
      this.lastAggressiveScanTick = tick;
   }

   @Override
   public int getLastBowAttackTick() {
      return this.lastBowAttackTick;
   }

   @Override
   public void setLastBowAttackTick(int tick) {
      this.lastBowAttackTick = tick;
   }

   @Override
   public int getBowChargeStartTick() {
      return this.bowChargeStartTick;
   }

   @Override
   public void setBowChargeStartTick(int tick) {
      this.bowChargeStartTick = tick;
   }

   @Override
   public int getLastCrossbowAttackTick() {
      return this.lastCrossbowAttackTick;
   }

   @Override
   public void setLastCrossbowAttackTick(int tick) {
      this.lastCrossbowAttackTick = tick;
   }

   @Override
   public int getCrossbowChargeStartTick() {
      return this.crossbowChargeStartTick;
   }

   @Override
   public void setCrossbowChargeStartTick(int tick) {
      this.crossbowChargeStartTick = tick;
   }

   @Override
   public int getLastShieldBlockTick() {
      return this.lastShieldBlockTick;
   }

   @Override
   public void setLastShieldBlockTick(int tick) {
      this.lastShieldBlockTick = tick;
   }

   @Override
   public int getShieldRaiseUntilTick() {
      return this.shieldRaiseUntilTick;
   }

   @Override
   public void setShieldRaiseUntilTick(int tick) {
      this.shieldRaiseUntilTick = tick;
   }

   @Override
   public float getBonusHealth() {
      return this.bonusHealth;
   }

   @Override
   public void setBonusHealth(float bonusHealth) {
      this.bonusHealth = Math.max(0.0F, bonusHealth);
      PersistentEntityData.get(this).putFloat("SlimPatchBonusHealth", this.bonusHealth);
   }

   @Override
   public double getBaseMaxHealth() {
      return this.baseMaxHealth;
   }

   @Override
   public void setBaseMaxHealth(double baseMaxHealth) {
      this.baseMaxHealth = Math.max(0.0, baseMaxHealth);
      PersistentEntityData.get(this).putDouble("SlimPatchBaseMaxHealth", this.baseMaxHealth);
   }

   @Override
   public SimpleContainer getEquipmentInventory() {
      return this.equipmentInventory;
   }

   @Override
   public ItemStack getPersistentMainHandItem() {
      return this.persistentMainHandItem.copy();
   }

   @Override
   public void setPersistentMainHandItem(ItemStack stack) {
      this.persistentMainHandItem = stack.copy();
      if (!this.level().isClientSide) {
         if (this.persistentMainHandItem.isEmpty()) {
            PersistentEntityData.get(this).remove("PersistentMainHand");
         } else {
            PersistentEntityData.get(this).put("PersistentMainHand", this.persistentMainHandItem.save(new CompoundTag()));
         }
      }
   }

   @Override
   public void syncPersistentMainHand() {
      if (!ItemStack.matches(this.getMainHandItem(), this.persistentMainHandItem)) {
         super.setItemInHand(InteractionHand.MAIN_HAND, this.persistentMainHandItem.copy());
      }
   }

   public void applyRelationshipChange(String option) {
      this.applyRelationshipChange(option, DialogueManager.calculateSuccess(this.getPersonality(), option));
   }

   public void applyRelationshipChange(String option, boolean success) {
      this.setRelationship(this.getRelationship() + DialogueManager.getRelationshipChange(this.getPersonality(), option, success));
   }

   public void applyRelationshipChange(ServerPlayer player, String option, boolean success) {
      if (!option.equalsIgnoreCase("Flirt") || VillagerFamilyData.canUseRomanticInteraction(this, player)) {
         if (!VillagerCooldownData.isDialogueOptionOnCooldown(this, player.getUUID(), option)) {
            VillagerCooldownData.setDialogueCooldown(this, player.getUUID(), option, OPTION_COOLDOWNS.getOrDefault(option, 20000L));
            VillagerCooldownData.syncDialogueCooldownsToPlayer(this, player, OPTION_COOLDOWNS);
            boolean perPlayer = VillagerRelationshipData.usesPerPlayerRelationships(this);
            float golden = VillagerRelationshipData.getGoldenRelationshipForPlayer(this, player.getUUID(), this.getGoldenRelationship());
            boolean isDating = perPlayer ? golden > 0.0F : this.getRelationshipStage() == VillagerRelationshipStage.DATING;
            if (isDating && option.equalsIgnoreCase("Flirt")) {
               float goldenDelta = success ? 0.5F : -0.25F;
               float updatedGolden = perPlayer
                  ? VillagerRelationshipData.setGoldenRelationshipForPlayer(this, player.getUUID(), golden + goldenDelta, this.getGoldenRelationship())
                  : this.updateGlobalGoldenRelationship(goldenDelta);
               ModNetworking.sendToClient(
                  new RelationshipSyncPacket(
                     this.getId(), VillagerRelationshipData.getRelationshipForPlayer(this, player.getUUID(), this.getRelationship()), updatedGolden
                  ),
                  player
               );
               if (goldenDelta > 0.0F) {
                  this.playPositiveReactionFeedback();
               } else if (goldenDelta < 0.0F) {
                  this.playNegativeReactionFeedback();
               }
            } else {
               float delta = DialogueManager.getRelationshipChange(this.getPersonality(), option, success);
               float updatedGolden = perPlayer ? golden : this.getGoldenRelationship();
               if (isDating && option.equalsIgnoreCase("Mean") && !success) {
                  updatedGolden = perPlayer
                     ? VillagerRelationshipData.setGoldenRelationshipForPlayer(this, player.getUUID(), golden - 0.25F, this.getGoldenRelationship())
                     : this.updateGlobalGoldenRelationship(-0.25F);
               }

               float relationship;
               if (perPlayer) {
                  float old = VillagerRelationshipData.getRelationshipForPlayer(this, player.getUUID(), this.getRelationship());
                  relationship = VillagerRelationshipData.setRelationshipForPlayer(this, player.getUUID(), old + delta, this.getRelationship());
               } else {
                  this.setRelationship(this.getRelationship() + delta);
                  relationship = this.getRelationship();
               }

               ModNetworking.sendToClient(new RelationshipSyncPacket(this.getId(), relationship, updatedGolden), player);
               if (delta > 0.0F) {
                  this.playPositiveReactionFeedback();
               } else if (delta < 0.0F) {
                  this.playNegativeReactionFeedback();
               }
            }
         }
      }
   }

   public void applyRelationshipChange(ServerPlayer player, String option) {
      this.applyRelationshipChange(player, option, DialogueManager.calculateSuccess(this.getPersonality(), option));
   }

   private float updateGlobalGoldenRelationship(float delta) {
      this.setGoldenRelationship(this.getGoldenRelationship() + delta);
      return this.getGoldenRelationship();
   }

   protected void playPositiveReactionFeedback() {
      if ((Boolean)Config.CUSTOM_VILLAGER_SOUNDS.get()) {
         this.level().playSound(null, this.blockPosition(), this.positiveReactionSound(), SoundSource.NEUTRAL, 1.0F, VillagerFamilyData.getAgeStagePitch(this));
      }

      for (int i = 0; i < 7; i++) {
         ((ServerLevel)this.level())
            .sendParticles(
               ParticleTypes.HAPPY_VILLAGER,
               this.getX() + this.random.nextGaussian() * 0.3,
               this.getY() + 1.0,
               this.getZ() + this.random.nextGaussian() * 0.3,
               1,
               0.0,
               0.0,
               0.0,
               0.0
            );
      }
   }

   protected void playNegativeReactionFeedback() {
      if ((Boolean)Config.CUSTOM_VILLAGER_SOUNDS.get()) {
         this.level().playSound(null, this.blockPosition(), this.negativeReactionSound(), SoundSource.NEUTRAL, 1.0F, VillagerFamilyData.getAgeStagePitch(this));
      }

      for (int i = 0; i < 4; i++) {
         ((ServerLevel)this.level())
            .sendParticles(
               ParticleTypes.ANGRY_VILLAGER,
               this.getX() + this.random.nextGaussian() * 0.3,
               this.getY() + 1.0,
               this.getZ() + this.random.nextGaussian() * 0.3,
               1,
               0.0,
               0.0,
               0.0,
               0.0
            );
      }
   }

   private InteractionResult handleFlowerInteraction(Player player, ItemStack stack, float goldenDelta) {
      if (player instanceof ServerPlayer serverPlayer) {
         if (!VillagerFamilyData.canUseRomanticInteraction(this, serverPlayer)) {
            this.playNegativeReactionFeedback();
            return InteractionResult.SUCCESS;
         }

         float relationship = VillagerRelationshipData.getRelationshipForPlayer(this, serverPlayer.getUUID(), this.getRelationship());
         boolean perPlayer = VillagerRelationshipData.usesPerPlayerRelationships(this);
         float golden = VillagerRelationshipData.getGoldenRelationshipForPlayer(this, serverPlayer.getUUID(), this.getGoldenRelationship());
         boolean married = this.getRelationshipStage() == VillagerRelationshipStage.MARRIED || this.hasSpouse();
         if (!married && (perPlayer ? !(golden > 0.0F) : this.getRelationshipStage() != VillagerRelationshipStage.DATING)) {
            if (!VillagerFamilyData.canStartDating(this, this, serverPlayer, relationship)) {
               this.playNegativeReactionFeedback();
               return InteractionResult.SUCCESS;
            } else {
               consumeHeldItemIfNeeded(player, stack);
               this.setRelationshipStage(VillagerRelationshipStage.DATING);
               float updatedGolden = perPlayer
                  ? VillagerRelationshipData.setGoldenRelationshipForPlayer(this, serverPlayer.getUUID(), golden + goldenDelta, this.getGoldenRelationship())
                  : this.updateGlobalGoldenRelationship(goldenDelta);
               ModNetworking.sendToClient(new RelationshipSyncPacket(this.getId(), relationship, updatedGolden), serverPlayer);
               this.playPositiveReactionFeedback();
               return InteractionResult.SUCCESS;
            }
         } else {
            consumeHeldItemIfNeeded(player, stack);
            this.playPositiveReactionFeedback();
            return InteractionResult.SUCCESS;
         }
      } else {
         return InteractionResult.SUCCESS;
      }
   }

   private InteractionResult handleWeddingRingInteraction(Player player) {
      if (player instanceof ServerPlayer serverPlayer) {
         String failureMessage = WeddingRingHandler.getMarriageFailureMessage(this, this, serverPlayer);
         if (failureMessage != null) {
            serverPlayer.displayClientMessage(Component.translatable(failureMessage), true);
            return InteractionResult.SUCCESS;
         } else {
            ModNetworking.sendToClient(new OpenMarriageProposalScreenPacket(this.getId(), this.getName().getString()), serverPlayer);
            return InteractionResult.SUCCESS;
         }
      } else {
         return InteractionResult.SUCCESS;
      }
   }

   private InteractionResult handleDivorcePapersInteraction(Player player) {
      if (player instanceof ServerPlayer serverPlayer) {
         if (this.getRelationshipStage() == VillagerRelationshipStage.MARRIED && this.hasSpouse()) {
            if (!serverPlayer.getUUID().equals(this.getSpousePlayerUuid())) {
               serverPlayer.displayClientMessage(Component.translatable("slimpatch.message.divorce_not_spouse"), true);
               return InteractionResult.SUCCESS;
            } else {
               ModNetworking.sendToClient(new OpenDivorceConfirmationScreenPacket(this.getId(), this.getName().getString()), serverPlayer);
               return InteractionResult.SUCCESS;
            }
         } else {
            serverPlayer.displayClientMessage(Component.translatable("slimpatch.message.divorce_requires_marriage"), true);
            return InteractionResult.SUCCESS;
         }
      } else {
         return InteractionResult.SUCCESS;
      }
   }

   public void confirmMarriageProposal(ServerPlayer player) {
      String failureMessage = WeddingRingHandler.getMarriageFailureMessage(this, this, player);
      if (failureMessage != null) {
         player.displayClientMessage(Component.translatable(failureMessage), true);
      } else {
         ItemStack ringStack = WeddingRingHandler.findWeddingRingStack(player);
         if (!ringStack.isEmpty()) {
            this.setRelationshipStage(VillagerRelationshipStage.MARRIED);
            this.setSpousePlayerUuid(player.getUUID());
            this.setSpousePlayerName(player.getGameProfile().getName());
            if (player.getUUID().equals(this.getFormerSpousePlayerUuid())) {
               this.setFormerSpousePlayerUuid(null);
               this.setFormerSpousePlayerName("");
            }

            consumeHeldItemIfNeeded(player, ringStack);
            SpouseCookingHandler.markMarriageCooldownUsed(this, player);
            this.playPositiveReactionFeedback();
            if ((Boolean)Config.ENABLE_WEDDING_CUTSCENE.get()) {
               WeddingCutsceneServerHandler.start(this, player);
            }

            FamilyTreeTracker.onMarriageToPlayer(player.serverLevel().getServer(), this, player.getUUID(), player.getGameProfile().getName());
            ModNetworking.sendToClient(new StartWeddingCutscenePacket(this.getId(), this.getName().getString()), player);
         }
      }
   }

   public void confirmDivorce(ServerPlayer player) {
      if (this.getRelationshipStage() == VillagerRelationshipStage.MARRIED && this.hasSpouse()) {
         if (!player.getUUID().equals(this.getSpousePlayerUuid())) {
            player.displayClientMessage(Component.translatable("slimpatch.message.divorce_not_spouse"), true);
         } else {
            ItemStack divorcePapers = DivorcePapersHandler.findDivorcePapersStack(player);
            if (!divorcePapers.isEmpty()) {
               this.setFormerSpousePlayerUuid(player.getUUID());
               this.setFormerSpousePlayerName(player.getGameProfile().getName());
               this.setSpousePlayerUuid(null);
               this.setSpousePlayerName("");
               this.setRelationshipStage(VillagerRelationshipStage.FRIENDSHIP);
               this.setGoldenRelationship(0.0F);
               float relationship;
               float goldenRelationship;
               if (VillagerRelationshipData.usesPerPlayerRelationships(this)) {
                  relationship = VillagerRelationshipData.setRelationshipForPlayer(this, player.getUUID(), 2.0F, this.getRelationship());
                  goldenRelationship = VillagerRelationshipData.setGoldenRelationshipForPlayer(this, player.getUUID(), 0.0F, this.getGoldenRelationship());
               } else {
                  this.setRelationship(2.0F);
                  relationship = this.getRelationship();
                  goldenRelationship = this.getGoldenRelationship();
               }

               ModNetworking.sendToClient(new RelationshipSyncPacket(this.getId(), relationship, goldenRelationship), player);
               consumeHeldItemIfNeeded(player, divorcePapers);
               FamilyTreeTracker.onDivorceFromPlayer(player.serverLevel().getServer(), this, player.getUUID(), player.getGameProfile().getName());
               player.displayClientMessage(Component.translatable("slimpatch.message.divorce_success", new Object[]{this.getName().getString()}), true);
            }
         }
      } else {
         player.displayClientMessage(Component.translatable("slimpatch.message.divorce_requires_marriage"), true);
      }
   }

   private static void consumeHeldItemIfNeeded(Player player, ItemStack stack) {
      if (!player.getAbilities().instabuild) {
         stack.shrink(1);
      }
   }

   public void giveGiftToPlayer(Player player) {
      if (!this.level().isClientSide && !VillagerCooldownData.hasGiftCooldown(this, player.getUUID())) {
         float relationship = player instanceof ServerPlayer serverPlayer
            ? VillagerRelationshipData.getRelationshipForPlayer(this, serverPlayer.getUUID(), this.getRelationship())
            : this.getRelationship();
         if (!(relationship < 5.0F)) {
            ItemStack gift = GiftReactionHandler.getRandomGift(this.getPersonality(), this.getRandom());
            if (!player.addItem(gift)) {
               this.spawnAtLocation(gift);
            }

            this.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.8F, 1.0F);
            VillagerCooldownData.setGiftCooldown(this, player.getUUID());
            ((ServerLevel)this.level()).sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 1.0, this.getZ(), 8, 0.5, 0.5, 0.5, 0.0);
            if ((Boolean)Config.CUSTOM_VILLAGER_SOUNDS.get()) {
               this.level()
                  .playSound(null, this.blockPosition(), this.positiveReactionSound(), SoundSource.NEUTRAL, 1.0F, VillagerFamilyData.getAgeStagePitch(this));
            }
         }
      }
   }

   protected void registerGoals() {
      super.registerGoals();
      VillagerCommandHandler.updateNavigationForCommand(this, this.getCommandState());
      this.goalSelector.addGoal(0, new DialogueGoal(this));
      this.goalSelector.addGoal(4, new ArmedVillagerMeleeAttackGoal(this, this, 0.75));
      this.goalSelector.addGoal(5, new VillagerCommandGoal(this, this));
      this.goalSelector.addGoal(6, new ReturnHomeAtNightGoal(this, this));
   }

   protected void customServerAiStep() {
      VillagerCommandHandler.suppressBedBehaviorForHomeAtNight(this, this);
      VillagerCombatHandler.tickCombat(this, this);
      if (WeddingCutsceneServerHandler.tick(this) || FamilyCutsceneServerHandler.tick(this)) {
         this.dialogueHoldPos = null;
      } else if (DialogueManager.isInDialogue(this)) {
         if (this.dialogueHoldPos == null) {
            this.dialogueHoldPos = this.position();
         }

         VillagerCommandHandler.holdForDialogue(this, this.dialogueHoldPos);
      } else if (this.getCommandState() != VillagerCommandState.NONE && !VillagerCombatHandler.isActivelyDefending(this, this)) {
         this.dialogueHoldPos = null;
         VillagerCommandHandler.tick(this, this);
      } else {
         this.dialogueHoldPos = null;
      }

      super.customServerAiStep();
      if (!VillagerCombatHandler.isActivelyDefending(this, this)) {
         VillagerCommandHandler.enforceHomeRadius(this, this);
      }

      this.suppressRelaxedOwnerLookTarget();
   }

   private void suppressRelaxedOwnerLookTarget() {
      if (this.getCommandState() == VillagerCommandState.FOLLOW && this.getFollowMode() == VillagerFollowMode.RELAXED) {
         if (!this.isTrading() && !DialogueManager.isInDialogue(this) && !VillagerCombatHandler.isActivelyDefending(this, this)) {
            UUID targetUuid = this.getCommandTargetUuid();
            if (targetUuid != null) {
               Player player = this.level().getPlayerByUUID(targetUuid);
               if (player != null && player.isAlive() && player.level() == this.level()) {
                  double distanceToPlayerSqr = this.distanceToSqr(player);
                  if (!(distanceToPlayerSqr > 64.0)) {
                     if (!(distanceToPlayerSqr > 16.0) || !this.isOwnerMovingAwayClearly(player)) {
                        this.getBrain().getMemory(MemoryModuleType.LOOK_TARGET).ifPresent(lookTarget -> {
                           if (lookTarget instanceof EntityTracker entityTracker && entityTracker.getEntity() == player) {
                              this.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
                           }
                        });
                     }
                  }
               }
            }
         }
      }
   }

   private boolean isOwnerMovingAwayClearly(Player player) {
      Vec3 motion = player.getDeltaMovement();
      double horizontalSpeedSqr = motion.x * motion.x + motion.z * motion.z;
      if (horizontalSpeedSqr < 0.01) {
         return false;
      }

      Vec3 toPlayer = player.position().subtract(this.position());
      double horizontalLength = Math.sqrt(toPlayer.x * toPlayer.x + toPlayer.z * toPlayer.z);
      if (horizontalLength < 1.0E-4) {
         return false;
      }

      double dot = (motion.x * toPlayer.x + motion.z * toPlayer.z) / horizontalLength;
      return dot > 0.08;
   }

   public boolean hurt(DamageSource source, float amount) {
      if (!VillagerCommandHandler.shouldIgnoreDamage(this, this, source) && !VillagerCombatHandler.tryBlockDamage(this, this, source, amount)) {
         boolean hurt = super.hurt(source, amount);
         if (hurt) {
            VillagerPassiveHealHandler.recordDamage(this, this);
            VillagerCombatHandler.onVillagerHurt(this, this, source);
         }

         return hurt;
      } else {
         return false;
      }
   }

   public void die(DamageSource source) {
      if (!this.level().isClientSide) {
         this.dropBackpackAndEquipment();
      }

      super.die(source);
   }

   private void dropBackpackAndEquipment() {
      for (int i = 0; i < this.equipmentInventory.getContainerSize(); i++) {
         ItemStack stack = this.equipmentInventory.getItem(i);
         if (!stack.isEmpty()) {
            this.spawnAtLocation(stack.copy());
            this.equipmentInventory.setItem(i, ItemStack.EMPTY);
         }
      }

      this.dropAndClearEquipmentSlot(EquipmentSlot.HEAD);
      this.dropAndClearEquipmentSlot(EquipmentSlot.CHEST);
      this.dropAndClearEquipmentSlot(EquipmentSlot.LEGS);
      this.dropAndClearEquipmentSlot(EquipmentSlot.FEET);
      this.dropAndClearOffhand();
      this.dropAndClearMainHand();
   }

   private void dropAndClearEquipmentSlot(EquipmentSlot slot) {
      ItemStack stack = this.getItemBySlot(slot);
      if (!stack.isEmpty()) {
         this.spawnAtLocation(stack.copy());
         this.setItemSlot(slot, ItemStack.EMPTY);
      }
   }

   private void dropAndClearOffhand() {
      ItemStack stack = this.getOffhandItem();
      if (!stack.isEmpty()) {
         this.spawnAtLocation(stack.copy());
         this.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
      }
   }

   private void dropAndClearMainHand() {
      ItemStack stack = this.getMainHandItem();
      if (stack.isEmpty()) {
         stack = this.getPersistentMainHandItem();
      }

      if (!stack.isEmpty()) {
         this.spawnAtLocation(stack.copy());
      }

      super.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
      this.setPersistentMainHandItem(ItemStack.EMPTY);
   }

   public boolean isBaby() {
      return false;
   }

   @Nullable
   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag
   ) {
      SpawnGroupData groupData = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
      CompoundTag data = PersistentEntityData.get(this);
      if (reason != MobSpawnType.CONVERSION && !data.getBoolean("slimpatch_restored") && !data.contains("slimpatch_saved_data")) {
         if (!data.contains("slimpatch_initialized")) {
            data.putString("slimpatch_gender", this.villagerGender());
            data.putBoolean("slimpatch_forced", true);
            this.setSkinIndex(data.contains("slimpatch_skin") ? data.getInt("slimpatch_skin") : this.getRandom().nextInt(this.skinCount()) + 1);
            if (!this.hasCustomName()) {
               this.setCustomName(Component.literal(this.randomName()));
               this.setCustomNameVisible(true);
            }

            if (!data.contains("slimpatch_personality")) {
               this.setPersonality(VillagerPersonality.getRandom(this.getRandom()));
            } else {
               try {
                  this.setPersonality(VillagerPersonality.valueOf(data.getString("slimpatch_personality")));
               } catch (IllegalArgumentException e) {
                  this.setPersonality(VillagerPersonality.getRandom(this.getRandom()));
               }
            }

            this.setRelationship(0.5F);
            data.putBoolean("slimpatch_named", true);
            data.putBoolean("slimpatch_initialized", true);
         }

         NaturalFamilySpawnHandler.maybeSpawnNaturalFamily(this, this, level, reason);
         return groupData;
      } else {
         this.loadPersistentFields(data);
         if (data.contains("SavedName") && !data.getString("SavedName").isEmpty()) {
            this.setCustomName(Component.literal(data.getString("SavedName")));
            this.setCustomNameVisible(true);
         }

         return groupData;
      }
   }

   public boolean shouldShowName() {
      if (!(Boolean)Config.VILLAGER_NAME_TAG.get()) {
         return false;
      }

      Player nearestPlayer = this.level().getNearestPlayer(this, 8.0);
      return nearestPlayer != null && this.hasCustomName();
   }

   public InteractionResult mobInteract(Player player, InteractionHand hand) {
      InteractionResult healthResult = VillagerHealthHandler.tryHandleGoldenCarrotInteraction(this, this, player, hand);
      if (healthResult != InteractionResult.PASS) {
         return healthResult;
      }

      ItemStack held = player.getItemInHand(hand);
      if (DivorcePapersHandler.isDivorcePapers(held)) {
         return this.level().isClientSide ? InteractionResult.SUCCESS : this.handleDivorcePapersInteraction(player);
      }

      if (hand == InteractionHand.MAIN_HAND && WeddingRingHandler.isWeddingRing(held)) {
         return this.level().isClientSide ? InteractionResult.SUCCESS : this.handleWeddingRingInteraction(player);
      }

      if (hand == InteractionHand.MAIN_HAND && held.is(ItemTags.FLOWERS)) {
         return this.level().isClientSide ? InteractionResult.SUCCESS : this.handleFlowerInteraction(player, held, 0.5F);
      }

      if (hand == InteractionHand.MAIN_HAND && held.is((Item)ModItems.ROMANTIC_BOUQUET)) {
         return this.level().isClientSide ? InteractionResult.SUCCESS : this.handleFlowerInteraction(player, held, 1.5F);
      }

      if (hand == InteractionHand.MAIN_HAND && held.is((Item)ModItems.FAMILY_CHARM)) {
         return this.level().isClientSide
            ? InteractionResult.SUCCESS
            : (player instanceof ServerPlayer serverPlayer ? FamilyCharmHandler.handleUseOnVillager(serverPlayer, held, this) : InteractionResult.SUCCESS);
      }

      if (hand != InteractionHand.MAIN_HAND || !held.is((Item)ModItems.SUN_CHARM) && !held.is((Item)ModItems.MOON_CHARM)) {
         if (hand == InteractionHand.MAIN_HAND
            && (
               VillagerRelationshipData.usesPerPlayerRelationships(this)
                  ? VillagerRelationshipData.getGoldenRelationshipForPlayer(this, player.getUUID(), this.getGoldenRelationship()) > 0.0F
                  : this.getRelationshipStage() == VillagerRelationshipStage.DATING
            )
            && RomanticGiftHandler.isRomanticGift(held)) {
            return this.level().isClientSide
               ? InteractionResult.SUCCESS
               : (
                  player instanceof ServerPlayer serverPlayer
                     ? RomanticGiftHandler.handleGift(this, this, serverPlayer, held, this::playPositiveReactionFeedback)
                     : InteractionResult.SUCCESS
               );
         }

         if (!this.level().isClientSide && hand == InteractionHand.MAIN_HAND && (Boolean)Config.CUSTOM_VILLAGER_SOUNDS.get()) {
            this.level().playSound(null, this.blockPosition(), this.clickSound(), SoundSource.NEUTRAL, 1.0F, VillagerFamilyData.getAgeStagePitch(this));
         }

         return super.mobInteract(player, hand);
      } else {
         return this.level().isClientSide
            ? InteractionResult.SUCCESS
            : (player instanceof ServerPlayer serverPlayer ? BabyGenderCharmHandler.handleUseOnVillager(serverPlayer, held, this) : InteractionResult.SUCCESS);
      }
   }

   public float getVoicePitch() {
      return VillagerFamilyData.getAgeStagePitch(this);
   }

   protected SoundEvent getAmbientSound() {
      return Config.CUSTOM_VILLAGER_SOUNDS.get() ? (this.isMuted() ? SoundEvents.EMPTY : this.clickSound()) : super.getAmbientSound();
   }

   public int getAmbientSoundInterval() {
      return 500;
   }

   public SoundEvent getNotifyTradeSound() {
      return Config.CUSTOM_VILLAGER_SOUNDS.get() ? this.clickSound() : super.getNotifyTradeSound();
   }

   protected SoundEvent getHurtSound(DamageSource source) {
      return Config.CUSTOM_VILLAGER_SOUNDS.get() ? this.hurtVoice() : super.getHurtSound(source);
   }

   protected SoundEvent getDeathSound() {
      return Config.CUSTOM_VILLAGER_SOUNDS.get() ? this.deathVoice() : super.getDeathSound();
   }

   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.loadPersistentFields(tag);
   }

   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      this.savePersistentFields(tag);
   }

   public Map<String, Long> getOptionCooldowns() {
      return OPTION_COOLDOWNS;
   }

   public Map<String, Integer> getCooldownsForClient() {
      return this.getCooldownsForClient(null);
   }

   public Map<String, Integer> getCooldownsForClient(UUID playerUuid) {
      Map<String, Integer> result = new HashMap<>();
      if (playerUuid == null) {
         return result;
      }

      CompoundTag playerCooldowns = PersistentEntityData.get(this).getCompound("SlimPatchDialogueCooldowns").getCompound(playerUuid.toString());
      long now = System.currentTimeMillis();

      for (String key : playerCooldowns.getAllKeys()) {
         long elapsed = now - playerCooldowns.getLong(key);
         long remaining = Math.max(0L, OPTION_COOLDOWNS.getOrDefault(key, 20000L) - elapsed);
         result.put(key, (int)(remaining / 1000L));
      }

      return result;
   }

   public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
      super.onSyncedDataUpdated(key);
      if (DATA_COMMAND_STATE.equals(key)) {
         this.commandState = this.getCommandState();
      }

      if (DATA_HAS_HOME.equals(key) && !this.hasHome()) {
         this.homePos = null;
         this.homeDimension = null;
      }

      if (DATA_COMMAND_OWNER_UUID.equals(key)) {
         String ownerUuid = (String)this.entityData.get(DATA_COMMAND_OWNER_UUID);
         this.commandOwnerUuid = ownerUuid.isEmpty() ? null : UUID.fromString(ownerUuid);
      }

      if (DATA_PARENT_PLAYER_UUID.equals(key)) {
         String uuid = (String)this.entityData.get(DATA_PARENT_PLAYER_UUID);
         this.parentPlayerUuid = uuid.isEmpty() ? null : UUID.fromString(uuid);
         VillagerFamilyData.writeParentPlayerUuid(PersistentEntityData.get(this), this.parentPlayerUuid);
      }

      if (DATA_SKIN.equals(key)) {
         int skin = this.getSkinIndex();
         if (skin <= 0 || skin > this.skinCount()) {
            this.setSkinIndex(this.getRandom().nextInt(this.skinCount()) + 1);
         }
      }
   }

   public EntityDimensions getDimensions(Pose pose) {
      return VillagerAgeStageDimensions.forStage(this.getAgeStage(), super.getDimensions(pose));
   }

   public <T extends Mob> T convertTo(EntityType<T> type, boolean keepEquipment) {
      if (type == EntityType.ZOMBIE_VILLAGER) {
         CompoundTag saved = new CompoundTag();
         this.addAdditionalSaveData(saved);
         saved.putString("slimpatch_gender", this.villagerGender());
         saved.putUUID("SlimPatchHistoricalEntityUuid", this.getUUID());
         Mob mob = super.convertTo((EntityType)ModEntities.HUMAN_ZOMBIE_VILLAGER, keepEquipment);
         if (mob instanceof HumanZombieVillagerEntity zombie) {
            PersistentEntityData.get(zombie).put("slimpatch_saved_data", saved.copy());
            CuredVillagerMemory.store(zombie.getUUID(), saved);
            zombie.setGender(this.villagerGender());
            zombie.setCustomName(null);
            zombie.setCustomNameVisible(false);
         }

         return (T)mob;
      } else {
         return (T)super.convertTo(type, keepEquipment);
      }
   }

   protected void restoreFromSavedData(CompoundTag saved) {
      if (saved.contains("slimpatch_skin")) {
         this.setSkinIndex(saved.getInt("slimpatch_skin"));
      }

      if (saved.contains("slimpatch_personality")) {
         try {
            this.setPersonality(VillagerPersonality.valueOf(saved.getString("slimpatch_personality")));
         } catch (IllegalArgumentException e) {
            this.setPersonality(VillagerPersonality.FRIENDLY);
         }
      }

      if (saved.contains("slimpatch_relationship")) {
         this.setRelationship(saved.getFloat("slimpatch_relationship"));
      }

      if (saved.contains("SavedName") && !saved.getString("SavedName").isEmpty()) {
         this.setCustomName(Component.literal(saved.getString("SavedName")));
         this.setCustomNameVisible(true);
      }

      if (saved.contains("SlimPatchCombatMode")) {
         this.setCombatMode(VillagerCombatMode.fromName(saved.getString("SlimPatchCombatMode")));
      }

      if (saved.contains("SlimPatchFollowMode")) {
         this.setFollowMode(VillagerFollowMode.fromName(saved.getString("SlimPatchFollowMode")));
      }

      if (saved.contains("SlimPatchHideArmor")) {
         this.setArmorHidden(saved.getBoolean("SlimPatchHideArmor"));
      }

      if (saved.contains("SlimPatchMuted")) {
         this.setMuted(saved.getBoolean("SlimPatchMuted"));
      }

      if (saved.contains("SlimPatchBonusHealth")) {
         this.setBaseMaxHealth(saved.contains("SlimPatchBaseMaxHealth") ? saved.getDouble("SlimPatchBaseMaxHealth") : 0.0);
         this.setBonusHealth(saved.getFloat("SlimPatchBonusHealth"));
         VillagerHealthHandler.reapplyBonusHealth(this, this);
      }

      this.loadPersistentFields(saved);
   }

   public void tick() {
      if (!this.level().isClientSide && this.tickCount == 1) {
         CompoundTag data = PersistentEntityData.get(this);
         CompoundTag saved = data.getCompound("slimpatch_saved_data");
         if (!saved.isEmpty()) {
            if (saved.contains("slimpatch_gender")) {
               data.putString("slimpatch_gender", saved.getString("slimpatch_gender"));
            }

            if (saved.contains("CustomSkinPath")) {
               data.putString("CustomSkinPath", saved.getString("CustomSkinPath"));
            }

            this.restoreFromSavedData(saved);
            PersistentEntityData.get(this).putBoolean("slimpatch_restored", true);
         } else {
            if (data.contains("SavedName") && !data.getString("SavedName").isEmpty()) {
               this.setCustomName(Component.literal(data.getString("SavedName")));
               this.setCustomNameVisible(true);
            }

            if (data.contains("CustomSkinPath") && !data.getString("CustomSkinPath").isEmpty()) {
               PersistentEntityData.get(this).putString("CustomSkinPath", data.getString("CustomSkinPath"));
            }
         }
      }

      super.tick();
      if (!this.level().isClientSide) {
         this.tickGenderSpecific();
         FamilyAgingHandler.tick(this);
         VillagerPassiveHealHandler.tickHealing(this, this);
         this.syncPersistentMainHand();
      }
   }

   private void loadPersistentFields(CompoundTag tag) {
      this.setSkinIndex(tag.contains("slimpatch_skin") ? tag.getInt("slimpatch_skin") : this.getRandom().nextInt(this.skinCount()) + 1);
      if (tag.contains("slimpatch_personality")) {
         try {
            this.setPersonality(VillagerPersonality.valueOf(tag.getString("slimpatch_personality")));
         } catch (IllegalArgumentException e) {
            this.setPersonality(VillagerPersonality.getRandom(this.getRandom()));
         }
      }

      this.setRelationship(tag.contains("slimpatch_relationship") ? tag.getFloat("slimpatch_relationship") : 0.5F);
      this.setGoldenRelationship(tag.contains("SlimPatchGoldenRelationship") ? tag.getFloat("SlimPatchGoldenRelationship") : 0.0F);
      this.setSpousePlayerUuid(VillagerFamilyData.readSpousePlayerUuid(tag));
      this.setSpousePlayerName(VillagerFamilyData.readOptionalString(tag, "SlimPatchSpousePlayerName"));
      this.setFormerSpousePlayerUuid(VillagerFamilyData.readFormerSpousePlayerUuid(tag));
      this.setFormerSpousePlayerName(VillagerFamilyData.readOptionalString(tag, "SlimPatchFormerSpousePlayerName"));
      this.setRelationshipStage(
         VillagerFamilyData.parseRelationshipStage(tag.contains("SlimPatchRelationshipStage") ? tag.getString("SlimPatchRelationshipStage") : null)
      );
      this.setAgeStage(VillagerFamilyData.parseAgeStage(tag.contains("SlimPatchAgeStage") ? tag.getString("SlimPatchAgeStage") : null));
      this.setAgeStageStartedAt(tag.contains("SlimPatchAgeStageStartedAt") ? tag.getLong("SlimPatchAgeStageStartedAt") : 0L);
      this.setNextAgeStageAt(tag.contains("SlimPatchNextAgeStageAt") ? tag.getLong("SlimPatchNextAgeStageAt") : 0L);
      this.setExpectingChild(tag.getBoolean("SlimPatchExpectingChild"));
      this.setExpectingParentPlayerUuid(VillagerFamilyData.readExpectingParentPlayerUuid(tag));
      this.setExpectingParentPlayerName(VillagerFamilyData.readOptionalString(tag, "SlimPatchExpectingParentPlayerName"));
      this.setExpectingOtherParentVillagerUuid(VillagerFamilyData.readExpectingOtherParentVillagerUuid(tag));
      this.setExpectingOtherParentVillagerName(VillagerFamilyData.readOptionalString(tag, "SlimPatchExpectingOtherParentVillagerName"));
      this.setForcedBabyGender(VillagerFamilyData.readOptionalString(tag, "SlimPatchForcedBabyGender"));
      this.setExpectingStartedAt(tag.contains("SlimPatchExpectingStartedAt") ? tag.getLong("SlimPatchExpectingStartedAt") : 0L);
      this.setExpectingEndsAt(tag.contains("SlimPatchExpectingEndsAt") ? tag.getLong("SlimPatchExpectingEndsAt") : 0L);
      this.setBornFromFamilySystem(tag.getBoolean("SlimPatchBornFromFamilySystem"));
      this.setParentPlayerUuid(VillagerFamilyData.readParentPlayerUuid(tag));
      this.setParentPlayerName(VillagerFamilyData.readOptionalString(tag, "SlimPatchParentPlayerName"));
      this.setParentVillagerUuid(VillagerFamilyData.readParentVillagerUuid(tag));
      this.setParentVillagerName(VillagerFamilyData.readOptionalString(tag, "SlimPatchParentVillagerName"));
      this.setParentVillager2Uuid(VillagerFamilyData.readParentVillager2Uuid(tag));
      this.setParentVillager2Name(VillagerFamilyData.readOptionalString(tag, "SlimPatchParentVillager2Name"));
      this.setNaturalFamilyGroupId(VillagerFamilyData.readNaturalFamilyGroupId(tag));
      this.setNaturalFamilyMember(tag.getBoolean("SlimPatchNaturalFamilyMember"));
      this.setFamilyOwnerPlayerUuid(VillagerFamilyData.readFamilyOwnerPlayerUuid(tag));
      this.setFamilyOwnerPlayerName(VillagerFamilyData.readOptionalString(tag, "SlimPatchFamilyOwnerPlayerName"));
      if (tag.contains("HasQuest")) {
         this.setHasQuest(tag.getBoolean("HasQuest"));
      }

      if (tag.contains("QuestId")) {
         this.setQuestId(tag.getString("QuestId"));
      }

      if (tag.contains("SavedName") && !tag.getString("SavedName").isEmpty()) {
         Component parsed = Serializer.fromJson(tag.getString("SavedName"));
         if (parsed != null) {
            this.setCustomName(parsed);
            this.setCustomNameVisible(true);
         }
      }

      this.setCustomSkinPath(tag.contains("CustomSkinPath") ? tag.getString("CustomSkinPath") : "");
      this.setSavedSkinInput(tag.contains("SavedSkinInput") ? tag.getString("SavedSkinInput") : "");
      this.setVisualHeight(tag.contains("Height") ? tag.getInt("Height") : 100);
      this.setVisualWidth(tag.contains("Width") ? tag.getInt("Width") : 100);
      VillagerRelationshipData.load(tag, this);
      VillagerCommandHandler.load(tag, this);
      this.setBaseMaxHealth(tag.contains("SlimPatchBaseMaxHealth") ? tag.getDouble("SlimPatchBaseMaxHealth") : 0.0);
      this.setBonusHealth(tag.contains("SlimPatchBonusHealth") ? tag.getFloat("SlimPatchBonusHealth") : 0.0F);
      VillagerHealthHandler.reapplyBonusHealth(this, this);
      this.setCombatMode(
         tag.contains("SlimPatchCombatMode") ? VillagerCombatMode.fromName(tag.getString("SlimPatchCombatMode")) : VillagerCombatMode.AGGRESSIVE
      );
      this.setFollowMode(tag.contains("SlimPatchFollowMode") ? VillagerFollowMode.fromName(tag.getString("SlimPatchFollowMode")) : VillagerFollowMode.CLOSE);
      this.setArmorHidden(tag.getBoolean("SlimPatchHideArmor"));
      this.setMuted(tag.getBoolean("SlimPatchMuted"));
      this.persistentMainHandItem = tag.contains("PersistentMainHand") ? ItemStack.of(tag.getCompound("PersistentMainHand")) : ItemStack.EMPTY;
      this.syncPersistentMainHand();
      this.equipmentInventory.clearContent();
      if (tag.contains("EquipmentInventory")) {
         NonNullList<ItemStack> items = NonNullList.withSize(this.equipmentInventory.getContainerSize(), ItemStack.EMPTY);
         ContainerHelper.loadAllItems(tag.getCompound("EquipmentInventory"), items);

         for (int i = 0; i < items.size(); i++) {
            this.equipmentInventory.setItem(i, (ItemStack)items.get(i));
         }
      }
   }

   private void savePersistentFields(CompoundTag tag) {
      tag.putInt("slimpatch_skin", this.getSkinIndex());
      tag.putString("slimpatch_personality", this.getPersonality().name());
      tag.putFloat("slimpatch_relationship", this.getRelationship());
      tag.putFloat("SlimPatchGoldenRelationship", this.getGoldenRelationship());
      VillagerFamilyData.writeSpousePlayerUuid(tag, this.getSpousePlayerUuid());
      VillagerFamilyData.writeOptionalString(tag, "SlimPatchSpousePlayerName", this.getSpousePlayerName());
      VillagerFamilyData.writeFormerSpousePlayerUuid(tag, this.getFormerSpousePlayerUuid());
      VillagerFamilyData.writeOptionalString(tag, "SlimPatchFormerSpousePlayerName", this.getFormerSpousePlayerName());
      tag.putString("SlimPatchRelationshipStage", this.getRelationshipStage().name());
      tag.putString("SlimPatchAgeStage", this.getAgeStage().name());
      if (this.getAgeStageStartedAt() > 0L) {
         tag.putLong("SlimPatchAgeStageStartedAt", this.getAgeStageStartedAt());
      }

      if (this.getNextAgeStageAt() > 0L) {
         tag.putLong("SlimPatchNextAgeStageAt", this.getNextAgeStageAt());
      }

      tag.putBoolean("SlimPatchExpectingChild", this.isExpectingChild());
      VillagerFamilyData.writeExpectingParentPlayerUuid(tag, this.getExpectingParentPlayerUuid());
      VillagerFamilyData.writeOptionalString(tag, "SlimPatchExpectingParentPlayerName", this.getExpectingParentPlayerName());
      VillagerFamilyData.writeExpectingOtherParentVillagerUuid(tag, this.getExpectingOtherParentVillagerUuid());
      VillagerFamilyData.writeOptionalString(tag, "SlimPatchExpectingOtherParentVillagerName", this.getExpectingOtherParentVillagerName());
      VillagerFamilyData.writeOptionalString(tag, "SlimPatchForcedBabyGender", this.getForcedBabyGender());
      if (this.getExpectingStartedAt() > 0L) {
         tag.putLong("SlimPatchExpectingStartedAt", this.getExpectingStartedAt());
      }

      if (this.getExpectingEndsAt() > 0L) {
         tag.putLong("SlimPatchExpectingEndsAt", this.getExpectingEndsAt());
      }

      if (this.isBornFromFamilySystem()) {
         tag.putBoolean("SlimPatchBornFromFamilySystem", true);
      }

      VillagerFamilyData.writeParentPlayerUuid(tag, this.getParentPlayerUuid());
      VillagerFamilyData.writeOptionalString(tag, "SlimPatchParentPlayerName", this.getParentPlayerName());
      VillagerFamilyData.writeParentVillagerUuid(tag, this.getParentVillagerUuid());
      VillagerFamilyData.writeOptionalString(tag, "SlimPatchParentVillagerName", this.getParentVillagerName());
      VillagerFamilyData.writeParentVillager2Uuid(tag, this.getParentVillager2Uuid());
      VillagerFamilyData.writeOptionalString(tag, "SlimPatchParentVillager2Name", this.getParentVillager2Name());
      VillagerFamilyData.writeSpouseVillagerUuid(tag, this.getSpouseVillagerUuid());
      VillagerFamilyData.writeOptionalString(tag, "SlimPatchSpouseVillagerName", this.getSpouseVillagerName());
      VillagerFamilyData.writeFamilyOwnerPlayerUuid(tag, this.getFamilyOwnerPlayerUuid());
      VillagerFamilyData.writeOptionalString(tag, "SlimPatchFamilyOwnerPlayerName", this.getFamilyOwnerPlayerName());
      VillagerFamilyData.writeNaturalFamilyGroupId(tag, this.getNaturalFamilyGroupId());
      tag.putBoolean("SlimPatchNaturalFamilyMember", this.isNaturalFamilyMember());
      tag.putBoolean("HasQuest", this.hasQuest());
      tag.putString("QuestId", this.getQuestId());
      if (this.hasCustomName()) {
         tag.putString("SavedName", Serializer.toJson(this.getCustomName()));
      }

      if (!this.getCustomSkinPath().isEmpty()) {
         tag.putString("CustomSkinPath", this.getCustomSkinPath());
      }

      if (!this.getSavedSkinInput().isEmpty()) {
         tag.putString("SavedSkinInput", this.getSavedSkinInput());
      }

      tag.putInt("Height", this.getVisualHeight());
      tag.putInt("Width", this.getVisualWidth());
      VillagerRelationshipData.save(tag, this);
      VillagerCommandHandler.save(tag, this);
      tag.putString("SlimPatchCombatMode", this.getCombatMode().name());
      tag.putString("SlimPatchFollowMode", this.getFollowMode().name());
      tag.putBoolean("SlimPatchHideArmor", this.isArmorHidden());
      tag.putBoolean("SlimPatchMuted", this.isMuted());
      tag.putDouble("SlimPatchBaseMaxHealth", this.getBaseMaxHealth());
      tag.putFloat("SlimPatchBonusHealth", this.getBonusHealth());
      if (this.persistentMainHandItem.isEmpty()) {
         tag.remove("PersistentMainHand");
      } else {
         tag.put("PersistentMainHand", this.persistentMainHandItem.save(new CompoundTag()));
      }

      CompoundTag equipmentTag = new CompoundTag();
      NonNullList<ItemStack> items = NonNullList.withSize(this.equipmentInventory.getContainerSize(), ItemStack.EMPTY);

      for (int i = 0; i < this.equipmentInventory.getContainerSize(); i++) {
         items.set(i, this.equipmentInventory.getItem(i));
      }

      ContainerHelper.saveAllItems(equipmentTag, items);
      tag.put("EquipmentInventory", equipmentTag);
   }
}
