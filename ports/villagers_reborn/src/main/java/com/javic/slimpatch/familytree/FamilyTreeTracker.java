package com.javic.slimpatch.familytree;

import com.javic.slimpatch.entity.FamilyVillager;
import com.javic.slimpatch.entity.FemaleVillagerEntity;
import com.javic.slimpatch.entity.MaleVillagerEntity;
import com.javic.slimpatch.fabric.PersistentEntityData;
import com.javic.slimpatch.graves.GraveIdentityResolver;
import com.javic.slimpatch.util.SkinPathHelper;
import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;

public final class FamilyTreeTracker {
   private FamilyTreeTracker() {
   }

   public static void upsertVillager(MinecraftServer server, Villager villager) {
      if (server != null && villager != null) {
         UUID effectiveVillagerUuid = GraveIdentityResolver.resolveHistoricalUuid(villager);
         if (effectiveVillagerUuid != null) {
            FamilyTreeSavedData data = FamilyTreeSavedData.get(server);
            FamilyTreeVillagerSnapshot snapshot = data.getOrCreateVillager(effectiveVillagerUuid);
            if (snapshot != null) {
               snapshot.setVillagerUuid(effectiveVillagerUuid);
               snapshot.setAlive(true);
               snapshot.setName(villager.getName().getString());
               snapshot.setGender(resolveGender(villager));
               snapshot.setLastKnownDimension(villager.level().dimension().location().toString());
               snapshot.setLastKnownX(villager.blockPosition().getX());
               snapshot.setLastKnownY(villager.blockPosition().getY());
               snapshot.setLastKnownZ(villager.blockPosition().getZ());
               if (villager instanceof MaleVillagerEntity maleVillager) {
                  snapshot.setSkinIndex(maleVillager.getSkinIndex());
                  snapshot.setSavedSkinInput(maleVillager.getSavedSkinInput());
               } else if (villager instanceof FemaleVillagerEntity femaleVillager) {
                  snapshot.setSkinIndex(femaleVillager.getSkinIndex());
                  snapshot.setSavedSkinInput(femaleVillager.getSavedSkinInput());
               } else {
                  snapshot.setSkinIndex(PersistentEntityData.get(villager).contains("slimpatch_skin") ? PersistentEntityData.get(villager).getInt("slimpatch_skin") : 0);
                  snapshot.setSavedSkinInput(
                     PersistentEntityData.get(villager).contains("SavedSkinInput") ? PersistentEntityData.get(villager).getString("SavedSkinInput") : ""
                  );
               }

               snapshot.setSkinResourcePath(resolveSkinResourcePath(villager, snapshot.getGender(), snapshot.getSkinIndex(), snapshot.getSavedSkinInput()));
               snapshot.setHasCustomSkin(!snapshot.getSavedSkinInput().isEmpty());
               boolean historicalAlias = !effectiveVillagerUuid.equals(villager.getUUID());
               if (villager instanceof FamilyVillager familyVillager) {
                  snapshot.setAgeStage(familyVillager.getAgeStage());
                  snapshot.setBornFromFamilySystem(familyVillager.isBornFromFamilySystem());
                  snapshot.setParentPlayerUuid(familyVillager.getParentPlayerUuid());
                  snapshot.setParentPlayerName(familyVillager.getParentPlayerName());
                  UUID parentVillagerUuid = GraveIdentityResolver.resolveHistoricalUuid(server, familyVillager.getParentVillagerUuid());
                  if (parentVillagerUuid != null || !historicalAlias) {
                     snapshot.setParentVillagerUuid(parentVillagerUuid);
                  }

                  if (!familyVillager.getParentVillagerName().isEmpty() || !historicalAlias) {
                     snapshot.setParentVillagerName(familyVillager.getParentVillagerName());
                  }

                  UUID parentVillager2Uuid = GraveIdentityResolver.resolveHistoricalUuid(server, familyVillager.getParentVillager2Uuid());
                  if (parentVillager2Uuid != null || !historicalAlias) {
                     snapshot.setParentVillager2Uuid(parentVillager2Uuid);
                  }

                  if (!familyVillager.getParentVillager2Name().isEmpty() || !historicalAlias) {
                     snapshot.setParentVillager2Name(familyVillager.getParentVillager2Name());
                  }

                  snapshot.setSpousePlayerUuid(familyVillager.getSpousePlayerUuid());
                  snapshot.setSpousePlayerName(familyVillager.getSpousePlayerName());
                  snapshot.setFormerSpousePlayerUuid(familyVillager.getFormerSpousePlayerUuid());
                  snapshot.setFormerSpousePlayerName(familyVillager.getFormerSpousePlayerName());
                  UUID spouseVillagerUuid = GraveIdentityResolver.resolveHistoricalUuid(server, familyVillager.getSpouseVillagerUuid());
                  if (spouseVillagerUuid != null || !historicalAlias) {
                     snapshot.setSpouseVillagerUuid(spouseVillagerUuid);
                  }

                  if (!familyVillager.getSpouseVillagerName().isEmpty() || !historicalAlias) {
                     snapshot.setSpouseVillagerName(familyVillager.getSpouseVillagerName());
                  }

                  snapshot.setNaturalFamilyGroupId(familyVillager.getNaturalFamilyGroupId());
                  snapshot.setNaturalFamilyMember(familyVillager.isNaturalFamilyMember());
                  if (familyVillager.getParentPlayerUuid() != null) {
                     upsertPlayer(server, familyVillager.getParentPlayerUuid(), familyVillager.getParentPlayerName());
                  }

                  if (familyVillager.getSpousePlayerUuid() != null) {
                     upsertPlayer(server, familyVillager.getSpousePlayerUuid(), familyVillager.getSpousePlayerName());
                  }

                  if (familyVillager.getFormerSpousePlayerUuid() != null) {
                     upsertPlayer(server, familyVillager.getFormerSpousePlayerUuid(), familyVillager.getFormerSpousePlayerName());
                  }
               }

               data.putVillager(snapshot);
               linkChildToParents(server, villager);
               linkVillagerSpouses(server, villager);
            }
         }
      }
   }

   public static void markVillagerDead(MinecraftServer server, UUID villagerUuid) {
      if (server != null && villagerUuid != null) {
         UUID effectiveVillagerUuid = GraveIdentityResolver.resolveHistoricalUuid(server, villagerUuid);
         FamilyTreeSavedData data = FamilyTreeSavedData.get(server);
         FamilyTreeVillagerSnapshot snapshot = data.getOrCreateVillager(effectiveVillagerUuid);
         if (snapshot != null) {
            snapshot.setAlive(false);
            data.putVillager(snapshot);
         }
      }
   }

   public static void upsertPlayer(MinecraftServer server, UUID playerUuid, String playerName) {
      if (server != null && playerUuid != null) {
         FamilyTreeSavedData data = FamilyTreeSavedData.get(server);
         FamilyTreePlayerSnapshot snapshot = data.getOrCreatePlayer(playerUuid);
         if (snapshot != null) {
            snapshot.setPlayerUuid(playerUuid);
            snapshot.setName(playerName);
            data.putPlayer(snapshot);
         }
      }
   }

   public static void linkChildToParents(MinecraftServer server, Villager child) {
      if (server != null && child instanceof FamilyVillager familyVillager) {
         UUID childUuid = GraveIdentityResolver.resolveHistoricalUuid(child);
         UUID parentVillagerUuid = GraveIdentityResolver.resolveHistoricalUuid(server, familyVillager.getParentVillagerUuid());
         if (parentVillagerUuid != null) {
            FamilyTreeSavedData data = FamilyTreeSavedData.get(server);
            FamilyTreeVillagerSnapshot parentSnapshot = data.getOrCreateVillager(parentVillagerUuid);
            if (parentSnapshot != null && childUuid != null && parentSnapshot.getChildVillagerUuids().add(childUuid)) {
               data.putVillager(parentSnapshot);
            }
         }

         UUID parentVillager2Uuid = GraveIdentityResolver.resolveHistoricalUuid(server, familyVillager.getParentVillager2Uuid());
         if (parentVillager2Uuid != null) {
            FamilyTreeSavedData data = FamilyTreeSavedData.get(server);
            FamilyTreeVillagerSnapshot parentSnapshot = data.getOrCreateVillager(parentVillager2Uuid);
            if (parentSnapshot != null && childUuid != null && parentSnapshot.getChildVillagerUuids().add(childUuid)) {
               data.putVillager(parentSnapshot);
            }
         }

         if (familyVillager.getParentPlayerUuid() != null) {
            upsertPlayer(server, familyVillager.getParentPlayerUuid(), familyVillager.getParentPlayerName());
         }
      }
   }

   public static void linkVillagerSpouses(MinecraftServer server, Villager villager) {
      if (server != null && villager instanceof FamilyVillager familyVillager) {
         UUID spouseVillagerUuid = GraveIdentityResolver.resolveHistoricalUuid(server, familyVillager.getSpouseVillagerUuid());
         if (spouseVillagerUuid != null) {
            FamilyTreeSavedData data = FamilyTreeSavedData.get(server);
            FamilyTreeVillagerSnapshot spouseSnapshot = data.getOrCreateVillager(spouseVillagerUuid);
            if (spouseSnapshot != null && spouseSnapshot.getName().isEmpty() && !familyVillager.getSpouseVillagerName().isEmpty()) {
               spouseSnapshot.setName(familyVillager.getSpouseVillagerName());
               data.putVillager(spouseSnapshot);
            }
         }
      }
   }

   public static void onMarriageToPlayer(MinecraftServer server, Villager villager, UUID playerUuid, String playerName) {
      if (server != null && villager != null && playerUuid != null) {
         upsertPlayer(server, playerUuid, playerName);
         normalizeMarriageToPlayer(server, villager, playerUuid, playerName);
         upsertVillager(server, villager);
      }
   }

   public static void onDivorceFromPlayer(MinecraftServer server, Villager villager, UUID playerUuid, String playerName) {
      if (server != null && villager != null && playerUuid != null) {
         upsertPlayer(server, playerUuid, playerName);
         upsertVillager(server, villager);
      }
   }

   public static void onVillagerNameChanged(MinecraftServer server, Villager villager) {
      upsertVillager(server, villager);
   }

   public static void onVillagerSkinChanged(MinecraftServer server, Villager villager) {
      upsertVillager(server, villager);
   }

   public static void onVillagerAgeStageChanged(MinecraftServer server, Villager villager) {
      upsertVillager(server, villager);
   }

   private static String resolveGender(Villager villager) {
      if (villager instanceof MaleVillagerEntity) {
         return "male";
      } else {
         return villager instanceof FemaleVillagerEntity ? "female" : "";
      }
   }

   private static void normalizeMarriageToPlayer(MinecraftServer server, Villager spouseVillager, UUID playerUuid, String playerName) {
      if (spouseVillager instanceof FamilyVillager spouseFamilyVillager) {
         FamilyTreeSavedData data = FamilyTreeSavedData.get(server);
         UUID spouseUuid = GraveIdentityResolver.resolveHistoricalUuid(spouseVillager);
         FamilyTreeVillagerSnapshot spouseSnapshot = data.getOrCreateVillager(spouseUuid);
         if (spouseSnapshot != null && !spouseSnapshot.getChildVillagerUuids().isEmpty()) {
            boolean clearedFormerVillagerSpouse = false;

            for (UUID childUuid : new ArrayList<>(spouseSnapshot.getChildVillagerUuids())) {
               if (childUuid != null) {
                  FamilyTreeVillagerSnapshot childSnapshot = data.getVillager(childUuid);
                  if (childSnapshot != null && childSnapshot.getParentPlayerUuid() == null) {
                     UUID firstParentUuid = childSnapshot.getParentVillagerUuid();
                     UUID secondParentUuid = childSnapshot.getParentVillager2Uuid();
                     boolean spouseIsFirstParent = spouseUuid.equals(firstParentUuid);
                     boolean spouseIsSecondParent = spouseUuid.equals(secondParentUuid);
                     if (spouseIsFirstParent || spouseIsSecondParent) {
                        UUID oldOtherParentUuid = spouseIsFirstParent ? secondParentUuid : firstParentUuid;
                        if (oldOtherParentUuid != null) {
                           String spouseName = spouseVillager.getName().getString();
                           childSnapshot.setParentPlayerUuid(playerUuid);
                           childSnapshot.setParentPlayerName(playerName);
                           childSnapshot.setParentVillagerUuid(spouseUuid);
                           childSnapshot.setParentVillagerName(spouseName);
                           childSnapshot.setParentVillager2Uuid(null);
                           childSnapshot.setParentVillager2Name("");
                           data.putVillager(childSnapshot);
                           spouseSnapshot.getChildVillagerUuids().add(childUuid);
                           oldOtherParentUuid = GraveIdentityResolver.resolveHistoricalUuid(server, oldOtherParentUuid);
                           FamilyTreeVillagerSnapshot oldOtherParentSnapshot = data.getOrCreateVillager(oldOtherParentUuid);
                           if (oldOtherParentSnapshot != null) {
                              oldOtherParentSnapshot.getChildVillagerUuids().remove(childUuid);
                              if (spouseUuid.equals(oldOtherParentSnapshot.getSpouseVillagerUuid())) {
                                 oldOtherParentSnapshot.setSpouseVillagerUuid(null);
                                 oldOtherParentSnapshot.setSpouseVillagerName("");
                                 data.putVillager(oldOtherParentSnapshot);
                                 clearedFormerVillagerSpouse = true;
                              } else {
                                 data.putVillager(oldOtherParentSnapshot);
                              }
                           }

                           if (spouseFamilyVillager.getSpouseVillagerUuid() != null && spouseFamilyVillager.getSpouseVillagerUuid().equals(oldOtherParentUuid)) {
                              spouseFamilyVillager.setSpouseVillagerUuid(null);
                              spouseFamilyVillager.setSpouseVillagerName("");
                              spouseSnapshot.setSpouseVillagerUuid(null);
                              spouseSnapshot.setSpouseVillagerName("");
                              clearedFormerVillagerSpouse = true;
                           }

                           if (findVillager(server, childUuid) instanceof FamilyVillager childFamilyVillager) {
                              childFamilyVillager.setParentPlayerUuid(playerUuid);
                              childFamilyVillager.setParentPlayerName(playerName);
                              childFamilyVillager.setParentVillagerUuid(spouseUuid);
                              childFamilyVillager.setParentVillagerName(spouseName);
                              childFamilyVillager.setParentVillager2Uuid(null);
                              childFamilyVillager.setParentVillager2Name("");
                           }

                           if (findVillager(server, oldOtherParentUuid) instanceof FamilyVillager oldOtherParentFamilyVillager
                              && spouseUuid.equals(oldOtherParentFamilyVillager.getSpouseVillagerUuid())) {
                              oldOtherParentFamilyVillager.setSpouseVillagerUuid(null);
                              oldOtherParentFamilyVillager.setSpouseVillagerName("");
                           }
                        }
                     }
                  }
               }
            }

            if (clearedFormerVillagerSpouse) {
               data.putVillager(spouseSnapshot);
            }
         }
      }
   }

   private static Villager findVillager(MinecraftServer server, UUID villagerUuid) {
      if (server != null && villagerUuid != null) {
         for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(villagerUuid) instanceof Villager villager) {
               return villager;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static String resolveSkinResourcePath(Villager villager, String gender, int skinIndex, String savedSkinInput) {
      String customSkinPath = PersistentEntityData.get(villager).contains("CustomSkinPath") ? PersistentEntityData.get(villager).getString("CustomSkinPath") : "";
      if (!customSkinPath.isEmpty()) {
         ResourceLocation parsed = ResourceLocation.tryParse(customSkinPath);
         if (parsed != null) {
            return parsed.toString();
         }

         if (!savedSkinInput.isEmpty()) {
            return customSkinPath;
         }
      }

      if (!gender.isEmpty() && skinIndex > 0) {
         ResourceLocation resourceLocation = SkinPathHelper.getSkin(gender, skinIndex, villager.level());
         if (resourceLocation != null) {
            return resourceLocation.toString();
         }
      }

      return "";
   }
}
