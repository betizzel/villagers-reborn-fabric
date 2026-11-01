package com.javic.slimpatch.entity;

import java.util.UUID;

public interface FamilyVillager {
   float getGoldenRelationship();

   void setGoldenRelationship(float var1);

   UUID getSpousePlayerUuid();

   void setSpousePlayerUuid(UUID var1);

   String getSpousePlayerName();

   void setSpousePlayerName(String var1);

   UUID getFormerSpousePlayerUuid();

   void setFormerSpousePlayerUuid(UUID var1);

   String getFormerSpousePlayerName();

   void setFormerSpousePlayerName(String var1);

   UUID getSpouseVillagerUuid();

   void setSpouseVillagerUuid(UUID var1);

   String getSpouseVillagerName();

   void setSpouseVillagerName(String var1);

   VillagerRelationshipStage getRelationshipStage();

   void setRelationshipStage(VillagerRelationshipStage var1);

   VillagerAgeStage getAgeStage();

   void setAgeStage(VillagerAgeStage var1);

   long getAgeStageStartedAt();

   void setAgeStageStartedAt(long var1);

   long getNextAgeStageAt();

   void setNextAgeStageAt(long var1);

   boolean isExpectingChild();

   void setExpectingChild(boolean var1);

   UUID getExpectingParentPlayerUuid();

   void setExpectingParentPlayerUuid(UUID var1);

   String getExpectingParentPlayerName();

   void setExpectingParentPlayerName(String var1);

   UUID getExpectingOtherParentVillagerUuid();

   void setExpectingOtherParentVillagerUuid(UUID var1);

   String getExpectingOtherParentVillagerName();

   void setExpectingOtherParentVillagerName(String var1);

   String getForcedBabyGender();

   void setForcedBabyGender(String var1);

   long getExpectingStartedAt();

   void setExpectingStartedAt(long var1);

   long getExpectingEndsAt();

   void setExpectingEndsAt(long var1);

   boolean isBornFromFamilySystem();

   void setBornFromFamilySystem(boolean var1);

   UUID getParentPlayerUuid();

   void setParentPlayerUuid(UUID var1);

   String getParentPlayerName();

   void setParentPlayerName(String var1);

   UUID getParentVillagerUuid();

   void setParentVillagerUuid(UUID var1);

   String getParentVillagerName();

   void setParentVillagerName(String var1);

   UUID getParentVillager2Uuid();

   void setParentVillager2Uuid(UUID var1);

   String getParentVillager2Name();

   void setParentVillager2Name(String var1);

   UUID getNaturalFamilyGroupId();

   void setNaturalFamilyGroupId(UUID var1);

   boolean isNaturalFamilyMember();

   void setNaturalFamilyMember(boolean var1);

   UUID getFamilyOwnerPlayerUuid();

   void setFamilyOwnerPlayerUuid(UUID var1);

   String getFamilyOwnerPlayerName();

   void setFamilyOwnerPlayerName(String var1);

   default boolean hasSpouse() {
      return this.getSpousePlayerUuid() != null || this.getSpouseVillagerUuid() != null;
   }

   default boolean isDating() {
      return this.getRelationshipStage() == VillagerRelationshipStage.DATING;
   }

   default boolean isMarried() {
      return this.getRelationshipStage() == VillagerRelationshipStage.MARRIED;
   }
}
