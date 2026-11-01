package com.javic.slimpatch.ledger;

import com.javic.slimpatch.entity.CommandableVillager;
import com.javic.slimpatch.entity.FamilyVillager;
import com.javic.slimpatch.entity.FemaleVillagerEntity;
import com.javic.slimpatch.entity.MaleVillagerEntity;
import com.javic.slimpatch.entity.VillagerCommandState;
import com.javic.slimpatch.familytree.FamilyTreeNodePayload;
import com.javic.slimpatch.familytree.FamilyTreePortraitPayload;
import com.javic.slimpatch.familytree.FamilyTreeSavedData;
import com.javic.slimpatch.familytree.FamilyTreeTracker;
import com.javic.slimpatch.familytree.FamilyTreeVillagerSnapshot;
import com.javic.slimpatch.graves.GraveIdentityResolver;
import com.javic.slimpatch.graves.GraveSavedData;
import com.javic.slimpatch.graves.VillagerDeathRecord;
import com.javic.slimpatch.graves.VillagerDeathRecordState;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;

public final class VillagerLedgerDataBuilder {
   private VillagerLedgerDataBuilder() {
   }

   public static VillagerLedgerDataBuilder.VillagerLedgerData build(ServerPlayer player) {
      MinecraftServer server = player.serverLevel().getServer();
      UUID playerUuid = player.getUUID();
      refreshRelevantSnapshots(server, playerUuid);
      VillagerLedgerDataBuilder.CompanionLists companionLists = buildCompanionLists(server, playerUuid);
      return new VillagerLedgerDataBuilder.VillagerLedgerData(companionLists.companions(), buildFamily(server, playerUuid), companionLists.hidden());
   }

   private static VillagerLedgerDataBuilder.CompanionLists buildCompanionLists(MinecraftServer server, UUID playerUuid) {
      Map<UUID, VillagerLedgerEntryPayload> companions = new LinkedHashMap<>();
      Map<UUID, VillagerLedgerEntryPayload> hidden = new LinkedHashMap<>();
      VillagerLedgerHiddenSavedData hiddenData = VillagerLedgerHiddenSavedData.get(server);
      FamilyTreeSavedData familyData = FamilyTreeSavedData.get(server);

      for (VillagerLedgerHiddenSavedData.LedgerVillagerSnapshot snapshot : hiddenData.getCompanionSnapshots(playerUuid)) {
         if (snapshot.getVillagerUuid() != null && !hiddenData.isSuppressed(playerUuid, snapshot.getVillagerUuid())) {
            VillagerCommandState commandState = snapshot.getCommandState();
            if (commandState == VillagerCommandState.FOLLOW || commandState == VillagerCommandState.STAY) {
               FamilyTreeVillagerSnapshot familySnapshot = familyData.getVillager(snapshot.getVillagerUuid());
               if (familySnapshot != null) {
                  hiddenData.upsertCompanionSnapshot(playerUuid, snapshot.getVillagerUuid(), familySnapshot, commandState);
               }

               VillagerLedgerEntryPayload entry = createCompanionSnapshotEntry(
                  server,
                  familySnapshot,
                  snapshot,
                  commandState == VillagerCommandState.FOLLOW ? VillagerLedgerEntryPayload.EntryType.FOLLOW : VillagerLedgerEntryPayload.EntryType.STAY
               );
               if (hiddenData.isHidden(playerUuid, snapshot.getVillagerUuid())) {
                  hidden.put(snapshot.getVillagerUuid(), entry);
               } else {
                  companions.put(snapshot.getVillagerUuid(), entry);
               }
            }
         }
      }

      for (ServerLevel level : server.getAllLevels()) {
         for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Villager villager
               && entity instanceof CommandableVillager commandableVillager
               && (villager instanceof MaleVillagerEntity || villager instanceof FemaleVillagerEntity)
               && playerUuid.equals(commandableVillager.getCommandOwnerUuid())) {
               VillagerCommandState commandState = commandableVillager.getCommandState();
               if (commandState == VillagerCommandState.FOLLOW || commandState == VillagerCommandState.STAY) {
                  FamilyTreeTracker.upsertVillager(server, villager);
                  UUID villagerUuid = GraveIdentityResolver.resolveHistoricalUuid(villager);
                  if (villagerUuid != null && !hiddenData.isSuppressed(playerUuid, villagerUuid)) {
                     FamilyTreeVillagerSnapshot snapshot = familyData.getVillager(villagerUuid);
                     if (snapshot != null) {
                        hiddenData.upsertCompanionSnapshot(playerUuid, villagerUuid, villager, commandableVillager);
                        VillagerLedgerEntryPayload entry = createEntry(
                           server,
                           snapshot,
                           villager,
                           commandState == VillagerCommandState.FOLLOW
                              ? VillagerLedgerEntryPayload.EntryType.FOLLOW
                              : VillagerLedgerEntryPayload.EntryType.STAY
                        );
                        if (hiddenData.isHidden(playerUuid, villagerUuid)) {
                           hiddenData.ensureHiddenTracked(playerUuid, villagerUuid);
                           hidden.put(villagerUuid, entry);
                        } else {
                           hiddenData.ensureCompanionTracked(playerUuid, villagerUuid);
                           companions.put(villagerUuid, entry);
                        }
                     }
                  }
               }
            }
         }
      }

      List<VillagerLedgerEntryPayload> companionEntries = sortCompanionEntries(companions, hiddenData.getCompanionOrder(playerUuid));
      List<VillagerLedgerEntryPayload> hiddenEntries = sortCompanionEntries(hidden, hiddenData.getHiddenOrder(playerUuid));
      return new VillagerLedgerDataBuilder.CompanionLists(companionEntries, hiddenEntries);
   }

   private static List<VillagerLedgerEntryPayload> sortCompanionEntries(Map<UUID, VillagerLedgerEntryPayload> entriesByUuid, List<UUID> savedOrder) {
      Map<UUID, Integer> orderIndex = new HashMap<>();

      for (int i = 0; i < savedOrder.size(); i++) {
         orderIndex.putIfAbsent(savedOrder.get(i), i);
      }

      List<VillagerLedgerEntryPayload> entries = new ArrayList<>(entriesByUuid.values());
      entries.sort(
         Comparator.<VillagerLedgerEntryPayload>comparingInt(entry -> orderIndex.getOrDefault(entry.getNode().getUuid(), Integer.MAX_VALUE))
            .thenComparing(entry -> entry.getEntryType() == VillagerLedgerEntryPayload.EntryType.FOLLOW ? 0 : 1)
            .thenComparing(entry -> entry.getNode().getDisplayName(), String.CASE_INSENSITIVE_ORDER)
      );
      return entries;
   }

   private static List<VillagerLedgerEntryPayload> buildFamily(MinecraftServer server, UUID playerUuid) {
      FamilyTreeSavedData data = FamilyTreeSavedData.get(server);
      VillagerLedgerHiddenSavedData hiddenData = VillagerLedgerHiddenSavedData.get(server);
      Map<UUID, VillagerLedgerEntryPayload> entries = new LinkedHashMap<>();
      Set<UUID> children = new TreeSet<>(Comparator.comparing(UUID::toString));

      for (FamilyTreeVillagerSnapshot snapshot : data.getVillagers()) {
         if (playerUuid.equals(snapshot.getSpousePlayerUuid())) {
            entries.put(
               snapshot.getVillagerUuid(),
               createEntry(server, snapshot, findLoadedVillager(server, snapshot.getVillagerUuid()), VillagerLedgerEntryPayload.EntryType.SPOUSE)
            );
            hiddenData.ensureFamilyTracked(playerUuid, snapshot.getVillagerUuid());
         }

         if (playerUuid.equals(snapshot.getParentPlayerUuid())) {
            children.add(snapshot.getVillagerUuid());
            entries.put(
               snapshot.getVillagerUuid(),
               createEntry(server, snapshot, findLoadedVillager(server, snapshot.getVillagerUuid()), VillagerLedgerEntryPayload.EntryType.CHILD)
            );
            hiddenData.ensureFamilyTracked(playerUuid, snapshot.getVillagerUuid());
         }
      }

      Deque<VillagerLedgerDataBuilder.QueuedRelative> queue = new ArrayDeque<>();

      for (UUID childUuid : children) {
         queue.addLast(new VillagerLedgerDataBuilder.QueuedRelative(childUuid, 1));
      }

      Set<UUID> visited = new HashSet<>(children);

      while (!queue.isEmpty()) {
         VillagerLedgerDataBuilder.QueuedRelative current = queue.removeFirst();
         FamilyTreeVillagerSnapshot snapshot = data.getVillager(GraveIdentityResolver.resolveHistoricalUuid(server, current.uuid()));
         if (snapshot != null) {
            for (UUID childUuid : snapshot.getChildVillagerUuids()) {
               childUuid = GraveIdentityResolver.resolveHistoricalUuid(server, childUuid);
               if (childUuid != null && visited.add(childUuid)) {
                  FamilyTreeVillagerSnapshot childSnapshot = data.getVillager(childUuid);
                  if (childSnapshot != null) {
                     VillagerLedgerEntryPayload.EntryType entryType = current.depth() == 1
                        ? VillagerLedgerEntryPayload.EntryType.GRANDCHILD
                        : VillagerLedgerEntryPayload.EntryType.DESCENDANT;
                     entries.put(childUuid, createEntry(server, childSnapshot, findLoadedVillager(server, childUuid), entryType));
                     hiddenData.ensureFamilyTracked(playerUuid, childUuid);
                     queue.addLast(new VillagerLedgerDataBuilder.QueuedRelative(childUuid, current.depth() + 1));
                  }
               }
            }
         }
      }

      List<VillagerLedgerEntryPayload> result = new ArrayList<>(entries.values());
      Map<UUID, Integer> orderIndex = new HashMap<>();
      List<UUID> savedOrder = hiddenData.getFamilyOrder(playerUuid);

      for (int i = 0; i < savedOrder.size(); i++) {
         orderIndex.putIfAbsent(savedOrder.get(i), i);
      }

      result.sort(
         Comparator.<VillagerLedgerEntryPayload>comparingInt(entry -> orderIndex.getOrDefault(entry.getNode().getUuid(), Integer.MAX_VALUE))
            .thenComparingInt(VillagerLedgerDataBuilder::familySortOrder)
            .thenComparing(entry -> entry.getNode().getDisplayName(), String.CASE_INSENSITIVE_ORDER)
      );
      return result;
   }

   private static int familySortOrder(VillagerLedgerEntryPayload entry) {
      return switch (entry.getEntryType()) {
         case SPOUSE -> 0;
         case CHILD -> 1;
         case GRANDCHILD -> 2;
         case DESCENDANT -> 3;
         default -> 4;
      };
   }

   private static VillagerLedgerEntryPayload createEntry(
      MinecraftServer server, FamilyTreeVillagerSnapshot snapshot, Villager villager, VillagerLedgerEntryPayload.EntryType entryType
   ) {
      boolean alive = villager != null || snapshot.isAlive();
      VillagerDeathRecord deathRecord = GraveSavedData.get(server).getRecordByHistoricalEntityUuid(snapshot.getVillagerUuid());
      if (deathRecord != null && deathRecord.getState() == VillagerDeathRecordState.ACTIVE) {
         alive = false;
      }

      boolean hasHealth = villager != null && alive;
      float health = hasHealth ? villager.getHealth() : 0.0F;
      float maxHealth = hasHealth ? villager.getMaxHealth() : 0.0F;
      VillagerLedgerEntryPayload.HomeState homeState = VillagerLedgerEntryPayload.HomeState.UNKNOWN;
      if (villager instanceof CommandableVillager commandableVillager) {
         homeState = commandableVillager.hasHome() ? VillagerLedgerEntryPayload.HomeState.HOME_SET : VillagerLedgerEntryPayload.HomeState.NO_HOME;
      }

      return new VillagerLedgerEntryPayload(
         new FamilyTreeNodePayload(
            "ledger:" + snapshot.getVillagerUuid(),
            FamilyTreeNodePayload.NodeType.VILLAGER,
            snapshot.getVillagerUuid(),
            snapshot.getName(),
            snapshot.getGender(),
            snapshot.getAgeStage().name(),
            alive,
            false,
            snapshot.isBornFromFamilySystem(),
            createPortrait(snapshot)
         ),
         entryType,
         hasHealth,
         health,
         maxHealth,
         homeState
      );
   }

   private static VillagerLedgerEntryPayload createCompanionSnapshotEntry(
      MinecraftServer server,
      FamilyTreeVillagerSnapshot familySnapshot,
      VillagerLedgerHiddenSavedData.LedgerVillagerSnapshot snapshot,
      VillagerLedgerEntryPayload.EntryType entryType
   ) {
      boolean alive = snapshot.isAlive();
      VillagerDeathRecord deathRecord = GraveSavedData.get(server).getRecordByHistoricalEntityUuid(snapshot.getVillagerUuid());
      if (deathRecord != null && deathRecord.getState() == VillagerDeathRecordState.ACTIVE) {
         alive = false;
      }

      String displayName = familySnapshot != null && !familySnapshot.getName().isEmpty() ? familySnapshot.getName() : snapshot.getDisplayName();
      String gender = familySnapshot != null && !familySnapshot.getGender().isEmpty() ? familySnapshot.getGender() : snapshot.getGender();
      String ageStage = familySnapshot != null ? familySnapshot.getAgeStage().name() : "ADULT";
      boolean bornFromFamilySystem = familySnapshot != null && familySnapshot.isBornFromFamilySystem();
      VillagerLedgerEntryPayload.HomeState homeState = snapshot.getHomePos() != null && snapshot.getHomeDimension() != null
         ? VillagerLedgerEntryPayload.HomeState.HOME_SET
         : VillagerLedgerEntryPayload.HomeState.NO_HOME;
      return new VillagerLedgerEntryPayload(
         new FamilyTreeNodePayload(
            "ledger:" + snapshot.getVillagerUuid(),
            FamilyTreeNodePayload.NodeType.VILLAGER,
            snapshot.getVillagerUuid(),
            displayName,
            gender,
            ageStage,
            alive,
            false,
            bornFromFamilySystem,
            createPortrait(familySnapshot, snapshot)
         ),
         entryType,
         snapshot.hasHealth() && alive,
         snapshot.hasHealth() && alive ? snapshot.getHealth() : 0.0F,
         snapshot.hasHealth() && alive ? snapshot.getMaxHealth() : 0.0F,
         homeState
      );
   }

   private static FamilyTreePortraitPayload createPortrait(FamilyTreeVillagerSnapshot snapshot) {
      FamilyTreePortraitPayload.PortraitType portraitType;
      if (snapshot.hasCustomSkin()) {
         portraitType = FamilyTreePortraitPayload.PortraitType.VILLAGER_CUSTOM_PNG;
      } else if (!snapshot.getSkinResourcePath().isEmpty()) {
         portraitType = FamilyTreePortraitPayload.PortraitType.VILLAGER_RESOURCE;
      } else {
         portraitType = FamilyTreePortraitPayload.PortraitType.VILLAGER_DEFAULT;
      }

      return new FamilyTreePortraitPayload(
         portraitType, snapshot.getSkinIndex(), snapshot.getSkinResourcePath(), snapshot.getSavedSkinInput(), !snapshot.isAlive(), new byte[0]
      );
   }

   private static FamilyTreePortraitPayload createPortrait(
      FamilyTreeVillagerSnapshot familySnapshot, VillagerLedgerHiddenSavedData.LedgerVillagerSnapshot snapshot
   ) {
      if (familySnapshot != null) {
         return createPortrait(familySnapshot);
      }

      FamilyTreePortraitPayload.PortraitType portraitType;
      if (snapshot.hasCustomSkin()) {
         portraitType = FamilyTreePortraitPayload.PortraitType.VILLAGER_CUSTOM_PNG;
      } else if (!snapshot.getSkinResourcePath().isEmpty()) {
         portraitType = FamilyTreePortraitPayload.PortraitType.VILLAGER_RESOURCE;
      } else {
         portraitType = FamilyTreePortraitPayload.PortraitType.VILLAGER_DEFAULT;
      }

      return new FamilyTreePortraitPayload(
         portraitType, snapshot.getSkinIndex(), snapshot.getSkinResourcePath(), snapshot.getSavedSkinInput(), !snapshot.isAlive(), new byte[0]
      );
   }

   private static Villager findLoadedVillager(MinecraftServer server, UUID historicalUuid) {
      if (historicalUuid == null) {
         return null;
      }

      for (ServerLevel level : server.getAllLevels()) {
         for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Villager villager && (villager instanceof MaleVillagerEntity || villager instanceof FemaleVillagerEntity)) {
               UUID effectiveUuid = GraveIdentityResolver.resolveHistoricalUuid(villager);
               if (historicalUuid.equals(effectiveUuid)) {
                  return villager;
               }
            }
         }
      }

      return null;
   }

   private static void refreshRelevantSnapshots(MinecraftServer server, UUID playerUuid) {
      for (ServerLevel level : server.getAllLevels()) {
         for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Villager villager
               && (villager instanceof MaleVillagerEntity || villager instanceof FemaleVillagerEntity)
               && villager instanceof FamilyVillager familyVillager
               && (playerUuid.equals(familyVillager.getSpousePlayerUuid()) || playerUuid.equals(familyVillager.getParentPlayerUuid()))) {
               FamilyTreeTracker.upsertVillager(server, villager);
            }
         }
      }
   }

   private record CompanionLists(List<VillagerLedgerEntryPayload> companions, List<VillagerLedgerEntryPayload> hidden) {
   }

   private record QueuedRelative(UUID uuid, int depth) {
   }

   public record VillagerLedgerData(
      List<VillagerLedgerEntryPayload> companions, List<VillagerLedgerEntryPayload> family, List<VillagerLedgerEntryPayload> hidden
   ) {
   }
}
