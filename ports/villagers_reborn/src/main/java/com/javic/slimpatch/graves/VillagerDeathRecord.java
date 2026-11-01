package com.javic.slimpatch.graves;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

public class VillagerDeathRecord {
   private UUID recordId;
   private UUID historicalEntityUuid;
   private UUID restoredLivingUuid;
   private VillagerDeathRecordState state = VillagerDeathRecordState.ACTIVE;
   private long createdGameTime;
   private String displayName = "";
   private String entityKind = "";
   private String gender = "";
   private String ageStage = "";
   private String lastKnownDimension = "";
   private int lastKnownX;
   private int lastKnownY;
   private int lastKnownZ;
   private String graveDimension = "";
   private int graveX;
   private int graveY;
   private int graveZ;
   private boolean gravePlaced;
   private UUID parentPlayerUuid;
   private String parentPlayerName = "";
   private UUID familyOwnerPlayerUuid;
   private String familyOwnerPlayerName = "";
   private UUID spousePlayerUuid;
   private String spousePlayerName = "";
   private String relationshipStage = "";
   private float relationship;
   private float goldenRelationship;
   private String personality = "";
   private int skinIndex;
   private String skinResourcePath = "";
   private String savedSkinInput = "";
   private boolean hasCustomSkin;
   private double baseMaxHealth;
   private float bonusHealth;
   private String villagerType = "";
   private String villagerProfession = "";
   private int villagerLevel;
   private int villagerXp;
   private CompoundTag vanillaVillagerData = new CompoundTag();
   private CompoundTag villagerDataTag = new CompoundTag();
   private CompoundTag offersData = new CompoundTag();
   private CompoundTag relationshipData = new CompoundTag();
   private CompoundTag commandData = new CompoundTag();

   public static VillagerDeathRecord load(CompoundTag tag) {
      VillagerDeathRecord record = new VillagerDeathRecord();
      record.setRecordId(readUuid(tag, "RecordId"));
      record.setHistoricalEntityUuid(readUuid(tag, "HistoricalEntityUuid"));
      record.setRestoredLivingUuid(readUuid(tag, "RestoredLivingUuid"));
      if (tag.contains("State")) {
         try {
            record.setState(VillagerDeathRecordState.valueOf(tag.getString("State")));
         } catch (IllegalArgumentException e) {
            record.setState(VillagerDeathRecordState.ACTIVE);
         }
      }

      record.setCreatedGameTime(tag.contains("CreatedGameTime") ? tag.getLong("CreatedGameTime") : 0L);
      record.setDisplayName(tag.contains("DisplayName") ? tag.getString("DisplayName") : "");
      record.setEntityKind(tag.contains("EntityKind") ? tag.getString("EntityKind") : "");
      record.setGender(tag.contains("Gender") ? tag.getString("Gender") : "");
      record.setAgeStage(tag.contains("AgeStage") ? tag.getString("AgeStage") : "");
      record.setLastKnownDimension(tag.contains("LastKnownDimension") ? tag.getString("LastKnownDimension") : "");
      record.setLastKnownX(tag.contains("LastKnownX") ? tag.getInt("LastKnownX") : 0);
      record.setLastKnownY(tag.contains("LastKnownY") ? tag.getInt("LastKnownY") : 0);
      record.setLastKnownZ(tag.contains("LastKnownZ") ? tag.getInt("LastKnownZ") : 0);
      record.setGraveDimension(tag.contains("GraveDimension") ? tag.getString("GraveDimension") : "");
      record.setGraveX(tag.contains("GraveX") ? tag.getInt("GraveX") : 0);
      record.setGraveY(tag.contains("GraveY") ? tag.getInt("GraveY") : 0);
      record.setGraveZ(tag.contains("GraveZ") ? tag.getInt("GraveZ") : 0);
      record.setGravePlaced(tag.getBoolean("GravePlaced"));
      record.setParentPlayerUuid(readUuid(tag, "ParentPlayerUuid"));
      record.setParentPlayerName(tag.contains("ParentPlayerName") ? tag.getString("ParentPlayerName") : "");
      record.setFamilyOwnerPlayerUuid(readUuid(tag, "FamilyOwnerPlayerUuid"));
      record.setFamilyOwnerPlayerName(tag.contains("FamilyOwnerPlayerName") ? tag.getString("FamilyOwnerPlayerName") : "");
      record.setSpousePlayerUuid(readUuid(tag, "SpousePlayerUuid"));
      record.setSpousePlayerName(tag.contains("SpousePlayerName") ? tag.getString("SpousePlayerName") : "");
      record.setRelationshipStage(tag.contains("RelationshipStage") ? tag.getString("RelationshipStage") : "");
      record.setRelationship(tag.contains("Relationship") ? tag.getFloat("Relationship") : 0.0F);
      record.setGoldenRelationship(tag.contains("GoldenRelationship") ? tag.getFloat("GoldenRelationship") : 0.0F);
      record.setPersonality(tag.contains("Personality") ? tag.getString("Personality") : "");
      record.setSkinIndex(tag.contains("SkinIndex") ? tag.getInt("SkinIndex") : 0);
      record.setSkinResourcePath(tag.contains("SkinResourcePath") ? tag.getString("SkinResourcePath") : "");
      record.setSavedSkinInput(tag.contains("SavedSkinInput") ? tag.getString("SavedSkinInput") : "");
      record.setHasCustomSkin(tag.getBoolean("HasCustomSkin"));
      record.setBaseMaxHealth(tag.contains("BaseMaxHealth") ? tag.getDouble("BaseMaxHealth") : 0.0);
      record.setBonusHealth(tag.contains("BonusHealth") ? tag.getFloat("BonusHealth") : 0.0F);
      record.setVillagerType(tag.contains("VillagerType") ? tag.getString("VillagerType") : "");
      record.setVillagerProfession(tag.contains("VillagerProfession") ? tag.getString("VillagerProfession") : "");
      record.setVillagerLevel(tag.contains("VillagerLevel") ? tag.getInt("VillagerLevel") : 0);
      record.setVillagerXp(tag.contains("VillagerXp") ? tag.getInt("VillagerXp") : 0);
      record.setVanillaVillagerData(tag.contains("VanillaVillagerData", 10) ? tag.getCompound("VanillaVillagerData") : new CompoundTag());
      record.setVillagerDataTag(tag.contains("VillagerDataTag", 10) ? tag.getCompound("VillagerDataTag") : new CompoundTag());
      record.setOffersData(tag.contains("OffersData", 10) ? tag.getCompound("OffersData") : new CompoundTag());
      record.setRelationshipData(tag.contains("RelationshipData", 10) ? tag.getCompound("RelationshipData") : new CompoundTag());
      record.setCommandData(tag.contains("CommandData", 10) ? tag.getCompound("CommandData") : new CompoundTag());
      return record;
   }

   public CompoundTag save() {
      CompoundTag tag = new CompoundTag();
      writeUuid(tag, "RecordId", this.recordId);
      writeUuid(tag, "HistoricalEntityUuid", this.historicalEntityUuid);
      writeUuid(tag, "RestoredLivingUuid", this.restoredLivingUuid);
      tag.putString("State", this.state.name());
      tag.putLong("CreatedGameTime", this.createdGameTime);
      if (!this.displayName.isEmpty()) {
         tag.putString("DisplayName", this.displayName);
      }

      if (!this.entityKind.isEmpty()) {
         tag.putString("EntityKind", this.entityKind);
      }

      if (!this.gender.isEmpty()) {
         tag.putString("Gender", this.gender);
      }

      if (!this.ageStage.isEmpty()) {
         tag.putString("AgeStage", this.ageStage);
      }

      if (!this.lastKnownDimension.isEmpty()) {
         tag.putString("LastKnownDimension", this.lastKnownDimension);
      }

      tag.putInt("LastKnownX", this.lastKnownX);
      tag.putInt("LastKnownY", this.lastKnownY);
      tag.putInt("LastKnownZ", this.lastKnownZ);
      if (!this.graveDimension.isEmpty()) {
         tag.putString("GraveDimension", this.graveDimension);
      }

      tag.putInt("GraveX", this.graveX);
      tag.putInt("GraveY", this.graveY);
      tag.putInt("GraveZ", this.graveZ);
      tag.putBoolean("GravePlaced", this.gravePlaced);
      writeUuid(tag, "ParentPlayerUuid", this.parentPlayerUuid);
      if (!this.parentPlayerName.isEmpty()) {
         tag.putString("ParentPlayerName", this.parentPlayerName);
      }

      writeUuid(tag, "FamilyOwnerPlayerUuid", this.familyOwnerPlayerUuid);
      if (!this.familyOwnerPlayerName.isEmpty()) {
         tag.putString("FamilyOwnerPlayerName", this.familyOwnerPlayerName);
      }

      writeUuid(tag, "SpousePlayerUuid", this.spousePlayerUuid);
      if (!this.spousePlayerName.isEmpty()) {
         tag.putString("SpousePlayerName", this.spousePlayerName);
      }

      if (!this.relationshipStage.isEmpty()) {
         tag.putString("RelationshipStage", this.relationshipStage);
      }

      tag.putFloat("Relationship", this.relationship);
      if (this.goldenRelationship > 0.0F) {
         tag.putFloat("GoldenRelationship", this.goldenRelationship);
      }

      if (!this.personality.isEmpty()) {
         tag.putString("Personality", this.personality);
      }

      tag.putInt("SkinIndex", this.skinIndex);
      if (!this.skinResourcePath.isEmpty()) {
         tag.putString("SkinResourcePath", this.skinResourcePath);
      }

      if (!this.savedSkinInput.isEmpty()) {
         tag.putString("SavedSkinInput", this.savedSkinInput);
      }

      tag.putBoolean("HasCustomSkin", this.hasCustomSkin);
      if (this.baseMaxHealth > 0.0) {
         tag.putDouble("BaseMaxHealth", this.baseMaxHealth);
      }

      if (this.bonusHealth > 0.0F) {
         tag.putFloat("BonusHealth", this.bonusHealth);
      }

      if (!this.villagerType.isEmpty()) {
         tag.putString("VillagerType", this.villagerType);
      }

      if (!this.villagerProfession.isEmpty()) {
         tag.putString("VillagerProfession", this.villagerProfession);
      }

      if (this.villagerLevel > 0) {
         tag.putInt("VillagerLevel", this.villagerLevel);
      }

      if (this.villagerXp > 0) {
         tag.putInt("VillagerXp", this.villagerXp);
      }

      if (!this.vanillaVillagerData.isEmpty()) {
         tag.put("VanillaVillagerData", this.vanillaVillagerData.copy());
      }

      if (!this.villagerDataTag.isEmpty()) {
         tag.put("VillagerDataTag", this.villagerDataTag.copy());
      }

      if (!this.offersData.isEmpty()) {
         tag.put("OffersData", this.offersData.copy());
      }

      if (!this.relationshipData.isEmpty()) {
         tag.put("RelationshipData", this.relationshipData.copy());
      }

      if (!this.commandData.isEmpty()) {
         tag.put("CommandData", this.commandData.copy());
      }

      return tag;
   }

   public UUID getRecordId() {
      return this.recordId;
   }

   public void setRecordId(UUID recordId) {
      this.recordId = recordId;
   }

   public UUID getHistoricalEntityUuid() {
      return this.historicalEntityUuid;
   }

   public void setHistoricalEntityUuid(UUID historicalEntityUuid) {
      this.historicalEntityUuid = historicalEntityUuid;
   }

   public UUID getRestoredLivingUuid() {
      return this.restoredLivingUuid;
   }

   public void setRestoredLivingUuid(UUID restoredLivingUuid) {
      this.restoredLivingUuid = restoredLivingUuid;
   }

   public VillagerDeathRecordState getState() {
      return this.state;
   }

   public void setState(VillagerDeathRecordState state) {
      this.state = state == null ? VillagerDeathRecordState.ACTIVE : state;
   }

   public long getCreatedGameTime() {
      return this.createdGameTime;
   }

   public void setCreatedGameTime(long createdGameTime) {
      this.createdGameTime = createdGameTime;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public void setDisplayName(String displayName) {
      this.displayName = displayName == null ? "" : displayName;
   }

   public String getEntityKind() {
      return this.entityKind;
   }

   public void setEntityKind(String entityKind) {
      this.entityKind = entityKind == null ? "" : entityKind;
   }

   public String getGender() {
      return this.gender;
   }

   public void setGender(String gender) {
      this.gender = gender == null ? "" : gender;
   }

   public String getAgeStage() {
      return this.ageStage;
   }

   public void setAgeStage(String ageStage) {
      this.ageStage = ageStage == null ? "" : ageStage;
   }

   public String getLastKnownDimension() {
      return this.lastKnownDimension;
   }

   public void setLastKnownDimension(String lastKnownDimension) {
      this.lastKnownDimension = lastKnownDimension == null ? "" : lastKnownDimension;
   }

   public int getLastKnownX() {
      return this.lastKnownX;
   }

   public void setLastKnownX(int lastKnownX) {
      this.lastKnownX = lastKnownX;
   }

   public int getLastKnownY() {
      return this.lastKnownY;
   }

   public void setLastKnownY(int lastKnownY) {
      this.lastKnownY = lastKnownY;
   }

   public int getLastKnownZ() {
      return this.lastKnownZ;
   }

   public void setLastKnownZ(int lastKnownZ) {
      this.lastKnownZ = lastKnownZ;
   }

   public String getGraveDimension() {
      return this.graveDimension;
   }

   public void setGraveDimension(String graveDimension) {
      this.graveDimension = graveDimension == null ? "" : graveDimension;
   }

   public int getGraveX() {
      return this.graveX;
   }

   public void setGraveX(int graveX) {
      this.graveX = graveX;
   }

   public int getGraveY() {
      return this.graveY;
   }

   public void setGraveY(int graveY) {
      this.graveY = graveY;
   }

   public int getGraveZ() {
      return this.graveZ;
   }

   public void setGraveZ(int graveZ) {
      this.graveZ = graveZ;
   }

   public boolean isGravePlaced() {
      return this.gravePlaced;
   }

   public void setGravePlaced(boolean gravePlaced) {
      this.gravePlaced = gravePlaced;
   }

   public UUID getParentPlayerUuid() {
      return this.parentPlayerUuid;
   }

   public void setParentPlayerUuid(UUID parentPlayerUuid) {
      this.parentPlayerUuid = parentPlayerUuid;
   }

   public String getParentPlayerName() {
      return this.parentPlayerName;
   }

   public void setParentPlayerName(String parentPlayerName) {
      this.parentPlayerName = parentPlayerName == null ? "" : parentPlayerName;
   }

   public UUID getFamilyOwnerPlayerUuid() {
      return this.familyOwnerPlayerUuid;
   }

   public void setFamilyOwnerPlayerUuid(UUID familyOwnerPlayerUuid) {
      this.familyOwnerPlayerUuid = familyOwnerPlayerUuid;
   }

   public String getFamilyOwnerPlayerName() {
      return this.familyOwnerPlayerName;
   }

   public void setFamilyOwnerPlayerName(String familyOwnerPlayerName) {
      this.familyOwnerPlayerName = familyOwnerPlayerName == null ? "" : familyOwnerPlayerName;
   }

   public UUID getSpousePlayerUuid() {
      return this.spousePlayerUuid;
   }

   public void setSpousePlayerUuid(UUID spousePlayerUuid) {
      this.spousePlayerUuid = spousePlayerUuid;
   }

   public String getSpousePlayerName() {
      return this.spousePlayerName;
   }

   public void setSpousePlayerName(String spousePlayerName) {
      this.spousePlayerName = spousePlayerName == null ? "" : spousePlayerName;
   }

   public String getRelationshipStage() {
      return this.relationshipStage;
   }

   public void setRelationshipStage(String relationshipStage) {
      this.relationshipStage = relationshipStage == null ? "" : relationshipStage;
   }

   public float getRelationship() {
      return this.relationship;
   }

   public void setRelationship(float relationship) {
      this.relationship = relationship;
   }

   public float getGoldenRelationship() {
      return this.goldenRelationship;
   }

   public void setGoldenRelationship(float goldenRelationship) {
      this.goldenRelationship = goldenRelationship;
   }

   public String getPersonality() {
      return this.personality;
   }

   public void setPersonality(String personality) {
      this.personality = personality == null ? "" : personality;
   }

   public int getSkinIndex() {
      return this.skinIndex;
   }

   public void setSkinIndex(int skinIndex) {
      this.skinIndex = skinIndex;
   }

   public String getSkinResourcePath() {
      return this.skinResourcePath;
   }

   public void setSkinResourcePath(String skinResourcePath) {
      this.skinResourcePath = skinResourcePath == null ? "" : skinResourcePath;
   }

   public String getSavedSkinInput() {
      return this.savedSkinInput;
   }

   public void setSavedSkinInput(String savedSkinInput) {
      this.savedSkinInput = savedSkinInput == null ? "" : savedSkinInput;
   }

   public boolean hasCustomSkin() {
      return this.hasCustomSkin;
   }

   public void setHasCustomSkin(boolean hasCustomSkin) {
      this.hasCustomSkin = hasCustomSkin;
   }

   public double getBaseMaxHealth() {
      return this.baseMaxHealth;
   }

   public void setBaseMaxHealth(double baseMaxHealth) {
      this.baseMaxHealth = baseMaxHealth;
   }

   public float getBonusHealth() {
      return this.bonusHealth;
   }

   public void setBonusHealth(float bonusHealth) {
      this.bonusHealth = bonusHealth;
   }

   public String getVillagerType() {
      return this.villagerType;
   }

   public void setVillagerType(String villagerType) {
      this.villagerType = villagerType == null ? "" : villagerType;
   }

   public String getVillagerProfession() {
      return this.villagerProfession;
   }

   public void setVillagerProfession(String villagerProfession) {
      this.villagerProfession = villagerProfession == null ? "" : villagerProfession;
   }

   public int getVillagerLevel() {
      return this.villagerLevel;
   }

   public void setVillagerLevel(int villagerLevel) {
      this.villagerLevel = villagerLevel;
   }

   public int getVillagerXp() {
      return this.villagerXp;
   }

   public void setVillagerXp(int villagerXp) {
      this.villagerXp = villagerXp;
   }

   public CompoundTag getVanillaVillagerData() {
      return this.vanillaVillagerData.copy();
   }

   public void setVanillaVillagerData(CompoundTag vanillaVillagerData) {
      this.vanillaVillagerData = vanillaVillagerData == null ? new CompoundTag() : vanillaVillagerData.copy();
   }

   public CompoundTag getVillagerDataTag() {
      return this.villagerDataTag.copy();
   }

   public void setVillagerDataTag(CompoundTag villagerDataTag) {
      this.villagerDataTag = villagerDataTag == null ? new CompoundTag() : villagerDataTag.copy();
   }

   public CompoundTag getOffersData() {
      return this.offersData.copy();
   }

   public void setOffersData(CompoundTag offersData) {
      this.offersData = offersData == null ? new CompoundTag() : offersData.copy();
   }

   public CompoundTag getRelationshipData() {
      return this.relationshipData.copy();
   }

   public void setRelationshipData(CompoundTag relationshipData) {
      this.relationshipData = relationshipData == null ? new CompoundTag() : relationshipData.copy();
   }

   public CompoundTag getCommandData() {
      return this.commandData.copy();
   }

   public void setCommandData(CompoundTag commandData) {
      this.commandData = commandData == null ? new CompoundTag() : commandData.copy();
   }

   private static UUID readUuid(CompoundTag tag, String key) {
      return tag.hasUUID(key) ? tag.getUUID(key) : null;
   }

   private static void writeUuid(CompoundTag tag, String key, UUID uuid) {
      if (uuid != null) {
         tag.putUUID(key, uuid);
      }
   }
}
