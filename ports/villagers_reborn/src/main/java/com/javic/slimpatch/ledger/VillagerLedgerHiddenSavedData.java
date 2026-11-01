package com.javic.slimpatch.ledger;

import com.javic.slimpatch.entity.CommandableVillager;
import com.javic.slimpatch.entity.FemaleVillagerEntity;
import com.javic.slimpatch.entity.MaleVillagerEntity;
import com.javic.slimpatch.entity.VillagerCommandState;
import com.javic.slimpatch.familytree.FamilyTreeVillagerSnapshot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

public class VillagerLedgerHiddenSavedData extends SavedData {
   private static final String DATA_ID = "slimpatch_villager_ledger_hidden";
   private final Map<UUID, Set<UUID>> hiddenVillagerUuidsByPlayer = new LinkedHashMap<>();
   private final Map<UUID, Set<UUID>> suppressedVillagerUuidsByPlayer = new LinkedHashMap<>();
   private final Map<UUID, List<UUID>> companionOrderByPlayer = new LinkedHashMap<>();
   private final Map<UUID, List<UUID>> hiddenOrderByPlayer = new LinkedHashMap<>();
   private final Map<UUID, List<UUID>> familyOrderByPlayer = new LinkedHashMap<>();
   private final Map<UUID, Map<UUID, VillagerLedgerHiddenSavedData.LedgerVillagerSnapshot>> companionSnapshotsByPlayer = new LinkedHashMap<>();

   public static VillagerLedgerHiddenSavedData get(MinecraftServer server) {
      return (VillagerLedgerHiddenSavedData)server.overworld()
         .getDataStorage()
         .computeIfAbsent(VillagerLedgerHiddenSavedData::load, VillagerLedgerHiddenSavedData::new, "slimpatch_villager_ledger_hidden");
   }

   public static VillagerLedgerHiddenSavedData load(CompoundTag tag) {
      VillagerLedgerHiddenSavedData data = new VillagerLedgerHiddenSavedData();
      ListTag players = tag.getList("Players", 10);

      for (int i = 0; i < players.size(); i++) {
         CompoundTag playerTag = players.getCompound(i);
         if (playerTag.hasUUID("PlayerUuid")) {
            UUID playerUuid = playerTag.getUUID("PlayerUuid");
            Set<UUID> hiddenVillagerUuids = new LinkedHashSet<>();
            ListTag hidden = playerTag.getList("HiddenVillagers", 10);

            for (int j = 0; j < hidden.size(); j++) {
               CompoundTag villagerTag = hidden.getCompound(j);
               if (villagerTag.hasUUID("VillagerUuid")) {
                  hiddenVillagerUuids.add(villagerTag.getUUID("VillagerUuid"));
               }
            }

            if (!hiddenVillagerUuids.isEmpty()) {
               data.hiddenVillagerUuidsByPlayer.put(playerUuid, hiddenVillagerUuids);
            }

            Set<UUID> suppressedVillagerUuids = new LinkedHashSet<>();
            ListTag suppressed = playerTag.getList("SuppressedVillagers", 10);

            for (int j = 0; j < suppressed.size(); j++) {
               CompoundTag villagerTag = suppressed.getCompound(j);
               if (villagerTag.hasUUID("VillagerUuid")) {
                  suppressedVillagerUuids.add(villagerTag.getUUID("VillagerUuid"));
               }
            }

            if (!suppressedVillagerUuids.isEmpty()) {
               data.suppressedVillagerUuidsByPlayer.put(playerUuid, suppressedVillagerUuids);
            }

            List<UUID> companionOrder = readUuidList(playerTag.getList("CompanionOrder", 10));
            if (!companionOrder.isEmpty()) {
               data.companionOrderByPlayer.put(playerUuid, companionOrder);
            }

            List<UUID> hiddenOrder = readUuidList(playerTag.getList("HiddenOrder", 10));
            if (!hiddenOrder.isEmpty()) {
               data.hiddenOrderByPlayer.put(playerUuid, hiddenOrder);
            }

            List<UUID> familyOrder = readUuidList(playerTag.getList("FamilyOrder", 10));
            if (!familyOrder.isEmpty()) {
               data.familyOrderByPlayer.put(playerUuid, familyOrder);
            }

            Map<UUID, VillagerLedgerHiddenSavedData.LedgerVillagerSnapshot> snapshots = new LinkedHashMap<>();
            ListTag snapshotTags = playerTag.getList("CompanionSnapshots", 10);

            for (int j = 0; j < snapshotTags.size(); j++) {
               VillagerLedgerHiddenSavedData.LedgerVillagerSnapshot snapshot = VillagerLedgerHiddenSavedData.LedgerVillagerSnapshot.load(
                  snapshotTags.getCompound(j)
               );
               if (snapshot.villagerUuid != null) {
                  snapshots.put(snapshot.villagerUuid, snapshot);
               }
            }

            if (!snapshots.isEmpty()) {
               data.companionSnapshotsByPlayer.put(playerUuid, snapshots);
            }
         }
      }

      return data;
   }

   public CompoundTag save(CompoundTag tag) {
      ListTag players = new ListTag();
      Set<UUID> playerUuids = new LinkedHashSet<>();
      playerUuids.addAll(this.hiddenVillagerUuidsByPlayer.keySet());
      playerUuids.addAll(this.suppressedVillagerUuidsByPlayer.keySet());
      playerUuids.addAll(this.companionOrderByPlayer.keySet());
      playerUuids.addAll(this.hiddenOrderByPlayer.keySet());
      playerUuids.addAll(this.familyOrderByPlayer.keySet());
      playerUuids.addAll(this.companionSnapshotsByPlayer.keySet());

      for (UUID playerUuid : playerUuids) {
         CompoundTag playerTag = new CompoundTag();
         playerTag.putUUID("PlayerUuid", playerUuid);
         ListTag hidden = new ListTag();

         for (UUID villagerUuid : this.hiddenVillagerUuidsByPlayer.getOrDefault(playerUuid, Set.of())) {
            CompoundTag villagerTag = new CompoundTag();
            villagerTag.putUUID("VillagerUuid", villagerUuid);
            hidden.add(villagerTag);
         }

         playerTag.put("HiddenVillagers", hidden);
         ListTag suppressed = new ListTag();

         for (UUID villagerUuid : this.suppressedVillagerUuidsByPlayer.getOrDefault(playerUuid, Set.of())) {
            CompoundTag villagerTag = new CompoundTag();
            villagerTag.putUUID("VillagerUuid", villagerUuid);
            suppressed.add(villagerTag);
         }

         playerTag.put("SuppressedVillagers", suppressed);
         playerTag.put("CompanionOrder", writeUuidList(this.companionOrderByPlayer.get(playerUuid)));
         playerTag.put("HiddenOrder", writeUuidList(this.hiddenOrderByPlayer.get(playerUuid)));
         playerTag.put("FamilyOrder", writeUuidList(this.familyOrderByPlayer.get(playerUuid)));
         ListTag snapshots = new ListTag();

         for (VillagerLedgerHiddenSavedData.LedgerVillagerSnapshot snapshot : this.companionSnapshotsByPlayer.getOrDefault(playerUuid, Map.of()).values()) {
            snapshots.add(snapshot.save());
         }

         playerTag.put("CompanionSnapshots", snapshots);
         players.add(playerTag);
      }

      tag.put("Players", players);
      return tag;
   }

   public Set<UUID> getHiddenVillagerUuids(UUID playerUuid) {
      Set<UUID> hiddenVillagerUuids = this.hiddenVillagerUuidsByPlayer.get(playerUuid);
      return hiddenVillagerUuids == null ? Set.of() : Set.copyOf(hiddenVillagerUuids);
   }

   public boolean isHidden(UUID playerUuid, UUID villagerUuid) {
      if (playerUuid != null && villagerUuid != null) {
         Set<UUID> hiddenVillagerUuids = this.hiddenVillagerUuidsByPlayer.get(playerUuid);
         return hiddenVillagerUuids != null && hiddenVillagerUuids.contains(villagerUuid);
      } else {
         return false;
      }
   }

   public boolean isSuppressed(UUID playerUuid, UUID villagerUuid) {
      if (playerUuid != null && villagerUuid != null) {
         Set<UUID> suppressedVillagerUuids = this.suppressedVillagerUuidsByPlayer.get(playerUuid);
         return suppressedVillagerUuids != null && suppressedVillagerUuids.contains(villagerUuid);
      } else {
         return false;
      }
   }

   public void setHidden(UUID playerUuid, UUID villagerUuid, boolean hidden) {
      if (playerUuid != null && villagerUuid != null) {
         Set<UUID> hiddenVillagerUuids = this.hiddenVillagerUuidsByPlayer.computeIfAbsent(playerUuid, ignored -> new LinkedHashSet<>());
         boolean changed = hidden ? hiddenVillagerUuids.add(villagerUuid) : hiddenVillagerUuids.remove(villagerUuid);
         if (changed) {
            if (hiddenVillagerUuids.isEmpty()) {
               this.hiddenVillagerUuidsByPlayer.remove(playerUuid);
            }

            if (hidden) {
               this.ensureTracked(this.hiddenOrderByPlayer, playerUuid, villagerUuid);
            }

            this.setDirty();
         }
      }
   }

   public void setSuppressed(UUID playerUuid, UUID villagerUuid, boolean suppressed) {
      if (playerUuid != null && villagerUuid != null) {
         Set<UUID> suppressedVillagerUuids = this.suppressedVillagerUuidsByPlayer.computeIfAbsent(playerUuid, ignored -> new LinkedHashSet<>());
         boolean changed = suppressed ? suppressedVillagerUuids.add(villagerUuid) : suppressedVillagerUuids.remove(villagerUuid);
         if (changed) {
            if (suppressedVillagerUuids.isEmpty()) {
               this.suppressedVillagerUuidsByPlayer.remove(playerUuid);
            }

            this.setDirty();
         }
      }
   }

   public VillagerLedgerHiddenSavedData.LedgerVillagerSnapshot getCompanionSnapshot(UUID playerUuid, UUID villagerUuid) {
      Map<UUID, VillagerLedgerHiddenSavedData.LedgerVillagerSnapshot> snapshots = this.companionSnapshotsByPlayer.get(playerUuid);
      return snapshots != null && villagerUuid != null ? snapshots.get(villagerUuid) : null;
   }

   public Iterable<VillagerLedgerHiddenSavedData.LedgerVillagerSnapshot> getCompanionSnapshots(UUID playerUuid) {
      Map<UUID, VillagerLedgerHiddenSavedData.LedgerVillagerSnapshot> snapshots = this.companionSnapshotsByPlayer.get(playerUuid);
      return snapshots == null ? List.of() : List.copyOf(snapshots.values());
   }

   public boolean isTrackedCompanion(UUID playerUuid, UUID villagerUuid) {
      VillagerLedgerHiddenSavedData.LedgerVillagerSnapshot snapshot = this.getCompanionSnapshot(playerUuid, villagerUuid);
      return snapshot != null && !this.isSuppressed(playerUuid, villagerUuid)
         ? snapshot.getCommandState() == VillagerCommandState.FOLLOW || snapshot.getCommandState() == VillagerCommandState.STAY
         : false;
   }

   public void upsertCompanionSnapshot(UUID playerUuid, UUID villagerUuid, Villager villager, CommandableVillager commandableVillager) {
      if (playerUuid != null && villagerUuid != null && villager != null && commandableVillager != null) {
         VillagerLedgerHiddenSavedData.LedgerVillagerSnapshot snapshot = this.getOrCreateSnapshot(playerUuid, villagerUuid);
         snapshot.villagerUuid = villagerUuid;
         snapshot.displayName = villager.getName().getString();
         snapshot.gender = villager instanceof MaleVillagerEntity ? "male" : (villager instanceof FemaleVillagerEntity ? "female" : snapshot.gender);
         snapshot.alive = villager.isAlive();
         snapshot.commandState = commandableVillager.getCommandState();
         snapshot.lastKnownDimension = villager.level().dimension().location().toString();
         snapshot.lastKnownX = villager.blockPosition().getX();
         snapshot.lastKnownY = villager.blockPosition().getY();
         snapshot.lastKnownZ = villager.blockPosition().getZ();
         snapshot.hasHealth = villager.isAlive();
         snapshot.health = villager.getHealth();
         snapshot.maxHealth = villager.getMaxHealth();
         snapshot.homePos = commandableVillager.getHomePos();
         snapshot.homeDimension = commandableVillager.getHomeDimension() == null ? "" : commandableVillager.getHomeDimension().location().toString();
         if (villager instanceof MaleVillagerEntity maleVillager) {
            snapshot.skinIndex = maleVillager.getSkinIndex();
            snapshot.savedSkinInput = maleVillager.getSavedSkinInput();
            snapshot.skinResourcePath = maleVillager.getCustomSkinPath();
         } else if (villager instanceof FemaleVillagerEntity femaleVillager) {
            snapshot.skinIndex = femaleVillager.getSkinIndex();
            snapshot.savedSkinInput = femaleVillager.getSavedSkinInput();
            snapshot.skinResourcePath = femaleVillager.getCustomSkinPath();
         }

         snapshot.hasCustomSkin = !snapshot.savedSkinInput.isEmpty();
         this.setSuppressed(playerUuid, villagerUuid, false);
         this.ensureCompanionTracked(playerUuid, villagerUuid);
         if (this.isHidden(playerUuid, villagerUuid)) {
            this.ensureHiddenTracked(playerUuid, villagerUuid);
         }

         this.setDirty();
      }
   }

   public void upsertCompanionSnapshot(UUID playerUuid, UUID villagerUuid, FamilyTreeVillagerSnapshot familySnapshot, VillagerCommandState commandState) {
      if (playerUuid != null && villagerUuid != null && familySnapshot != null) {
         VillagerLedgerHiddenSavedData.LedgerVillagerSnapshot snapshot = this.getOrCreateSnapshot(playerUuid, villagerUuid);
         snapshot.villagerUuid = villagerUuid;
         snapshot.displayName = familySnapshot.getName();
         snapshot.gender = familySnapshot.getGender();
         snapshot.alive = familySnapshot.isAlive();
         snapshot.commandState = commandState == null ? snapshot.commandState : commandState;
         snapshot.lastKnownDimension = familySnapshot.getLastKnownDimension();
         snapshot.lastKnownX = familySnapshot.getLastKnownX();
         snapshot.lastKnownY = familySnapshot.getLastKnownY();
         snapshot.lastKnownZ = familySnapshot.getLastKnownZ();
         snapshot.skinIndex = familySnapshot.getSkinIndex();
         snapshot.savedSkinInput = familySnapshot.getSavedSkinInput();
         snapshot.skinResourcePath = familySnapshot.getSkinResourcePath();
         snapshot.hasCustomSkin = familySnapshot.hasCustomSkin();
         this.setDirty();
      }
   }

   public void updateSnapshotAlive(UUID playerUuid, UUID villagerUuid, boolean alive) {
      VillagerLedgerHiddenSavedData.LedgerVillagerSnapshot snapshot = this.getCompanionSnapshot(playerUuid, villagerUuid);
      if (snapshot != null) {
         snapshot.alive = alive;
         if (!alive) {
            snapshot.hasHealth = false;
            snapshot.health = 0.0F;
            snapshot.maxHealth = 0.0F;
         }

         this.setDirty();
      }
   }

   public void markRemoved(UUID playerUuid, UUID villagerUuid) {
      VillagerLedgerHiddenSavedData.LedgerVillagerSnapshot snapshot = this.getCompanionSnapshot(playerUuid, villagerUuid);
      if (snapshot != null) {
         snapshot.commandState = VillagerCommandState.NONE;
      }

      this.setHidden(playerUuid, villagerUuid, false);
      this.setSuppressed(playerUuid, villagerUuid, true);
      this.removeFromOrder(this.companionOrderByPlayer, playerUuid, villagerUuid);
      this.removeFromOrder(this.hiddenOrderByPlayer, playerUuid, villagerUuid);
      this.setDirty();
   }

   public List<UUID> getCompanionOrder(UUID playerUuid) {
      return this.getOrder(this.companionOrderByPlayer, playerUuid);
   }

   public List<UUID> getHiddenOrder(UUID playerUuid) {
      return this.getOrder(this.hiddenOrderByPlayer, playerUuid);
   }

   public List<UUID> getFamilyOrder(UUID playerUuid) {
      return this.getOrder(this.familyOrderByPlayer, playerUuid);
   }

   public void ensureCompanionTracked(UUID playerUuid, UUID villagerUuid) {
      this.ensureTracked(this.companionOrderByPlayer, playerUuid, villagerUuid);
   }

   public void ensureHiddenTracked(UUID playerUuid, UUID villagerUuid) {
      this.ensureTracked(this.hiddenOrderByPlayer, playerUuid, villagerUuid);
   }

   public void ensureFamilyTracked(UUID playerUuid, UUID villagerUuid) {
      this.ensureTracked(this.familyOrderByPlayer, playerUuid, villagerUuid);
   }

   public void moveCompanion(UUID playerUuid, UUID villagerUuid, UUID targetUuid, boolean placeAfter) {
      this.moveInOrder(this.companionOrderByPlayer, playerUuid, villagerUuid, targetUuid, placeAfter);
   }

   public void moveHidden(UUID playerUuid, UUID villagerUuid, UUID targetUuid, boolean placeAfter) {
      this.moveInOrder(this.hiddenOrderByPlayer, playerUuid, villagerUuid, targetUuid, placeAfter);
   }

   public void moveFamily(UUID playerUuid, UUID villagerUuid, UUID targetUuid, boolean placeAfter) {
      this.moveInOrder(this.familyOrderByPlayer, playerUuid, villagerUuid, targetUuid, placeAfter);
   }

   public void removeVillager(UUID playerUuid, UUID villagerUuid) {
      if (playerUuid != null && villagerUuid != null) {
         boolean changed = false;
         Set<UUID> hiddenVillagers = this.hiddenVillagerUuidsByPlayer.get(playerUuid);
         if (hiddenVillagers != null && hiddenVillagers.remove(villagerUuid)) {
            changed = true;
            if (hiddenVillagers.isEmpty()) {
               this.hiddenVillagerUuidsByPlayer.remove(playerUuid);
            }
         }

         changed |= this.removeFromOrder(this.companionOrderByPlayer, playerUuid, villagerUuid);
         changed |= this.removeFromOrder(this.hiddenOrderByPlayer, playerUuid, villagerUuid);
         changed |= this.removeFromOrder(this.familyOrderByPlayer, playerUuid, villagerUuid);
         Map<UUID, VillagerLedgerHiddenSavedData.LedgerVillagerSnapshot> snapshots = this.companionSnapshotsByPlayer.get(playerUuid);
         if (snapshots != null && snapshots.remove(villagerUuid) != null) {
            changed = true;
            if (snapshots.isEmpty()) {
               this.companionSnapshotsByPlayer.remove(playerUuid);
            }
         }

         if (changed) {
            this.setDirty();
         }
      }
   }

   private VillagerLedgerHiddenSavedData.LedgerVillagerSnapshot getOrCreateSnapshot(UUID playerUuid, UUID villagerUuid) {
      Map<UUID, VillagerLedgerHiddenSavedData.LedgerVillagerSnapshot> snapshots = this.companionSnapshotsByPlayer
         .computeIfAbsent(playerUuid, ignored -> new LinkedHashMap<>());
      return snapshots.computeIfAbsent(villagerUuid, ignored -> new VillagerLedgerHiddenSavedData.LedgerVillagerSnapshot(villagerUuid));
   }

   private List<UUID> getOrder(Map<UUID, List<UUID>> orders, UUID playerUuid) {
      List<UUID> order = orders.get(playerUuid);
      return order == null ? List.of() : List.copyOf(order);
   }

   private void ensureTracked(Map<UUID, List<UUID>> orders, UUID playerUuid, UUID villagerUuid) {
      if (playerUuid != null && villagerUuid != null) {
         List<UUID> order = orders.computeIfAbsent(playerUuid, ignored -> new ArrayList<>());
         if (!order.contains(villagerUuid)) {
            order.add(villagerUuid);
            this.setDirty();
         }
      }
   }

   private void moveInOrder(Map<UUID, List<UUID>> orders, UUID playerUuid, UUID villagerUuid, UUID targetUuid, boolean placeAfter) {
      if (playerUuid != null && villagerUuid != null) {
         List<UUID> order = orders.computeIfAbsent(playerUuid, ignored -> new ArrayList<>());
         if (!order.remove(villagerUuid)) {
            order.add(villagerUuid);
         }

         if (targetUuid == null) {
            order.add(villagerUuid);
         } else {
            int targetIndex = order.indexOf(targetUuid);
            if (targetIndex < 0) {
               order.add(villagerUuid);
            } else {
               order.add(placeAfter ? targetIndex + 1 : targetIndex, villagerUuid);
            }
         }

         this.setDirty();
      }
   }

   private boolean removeFromOrder(Map<UUID, List<UUID>> orders, UUID playerUuid, UUID villagerUuid) {
      List<UUID> order = orders.get(playerUuid);
      if (order != null && order.remove(villagerUuid)) {
         if (order.isEmpty()) {
            orders.remove(playerUuid);
         }

         return true;
      } else {
         return false;
      }
   }

   private static List<UUID> readUuidList(ListTag listTag) {
      List<UUID> uuids = new ArrayList<>();

      for (int i = 0; i < listTag.size(); i++) {
         CompoundTag entryTag = listTag.getCompound(i);
         if (entryTag.hasUUID("VillagerUuid")) {
            uuids.add(entryTag.getUUID("VillagerUuid"));
         }
      }

      return uuids;
   }

   private static ListTag writeUuidList(List<UUID> uuids) {
      ListTag listTag = new ListTag();
      if (uuids == null) {
         return listTag;
      }

      for (UUID uuid : uuids) {
         CompoundTag entryTag = new CompoundTag();
         entryTag.putUUID("VillagerUuid", uuid);
         listTag.add(entryTag);
      }

      return listTag;
   }

   public static class LedgerVillagerSnapshot {
      private UUID villagerUuid;
      private String displayName = "";
      private String gender = "";
      private boolean alive = true;
      private boolean hasHealth;
      private float health;
      private float maxHealth;
      private VillagerCommandState commandState = VillagerCommandState.NONE;
      private String lastKnownDimension = "";
      private int lastKnownX;
      private int lastKnownY;
      private int lastKnownZ;
      private BlockPos homePos;
      private String homeDimension = "";
      private int skinIndex;
      private String skinResourcePath = "";
      private String savedSkinInput = "";
      private boolean hasCustomSkin;

      private LedgerVillagerSnapshot(UUID villagerUuid) {
         this.villagerUuid = villagerUuid;
      }

      public static VillagerLedgerHiddenSavedData.LedgerVillagerSnapshot load(CompoundTag tag) {
         UUID villagerUuid = tag.hasUUID("VillagerUuid") ? tag.getUUID("VillagerUuid") : null;
         VillagerLedgerHiddenSavedData.LedgerVillagerSnapshot snapshot = new VillagerLedgerHiddenSavedData.LedgerVillagerSnapshot(villagerUuid);
         snapshot.displayName = tag.contains("DisplayName") ? tag.getString("DisplayName") : "";
         snapshot.gender = tag.contains("Gender") ? tag.getString("Gender") : "";
         snapshot.alive = !tag.contains("Alive") || tag.getBoolean("Alive");
         snapshot.hasHealth = tag.getBoolean("HasHealth");
         snapshot.health = tag.contains("Health") ? tag.getFloat("Health") : 0.0F;
         snapshot.maxHealth = tag.contains("MaxHealth") ? tag.getFloat("MaxHealth") : 0.0F;
         if (tag.contains("CommandState")) {
            try {
               snapshot.commandState = VillagerCommandState.valueOf(tag.getString("CommandState"));
            } catch (IllegalArgumentException ignored) {
               snapshot.commandState = VillagerCommandState.NONE;
            }
         }

         snapshot.lastKnownDimension = tag.contains("LastKnownDimension") ? tag.getString("LastKnownDimension") : "";
         snapshot.lastKnownX = tag.contains("LastKnownX") ? tag.getInt("LastKnownX") : 0;
         snapshot.lastKnownY = tag.contains("LastKnownY") ? tag.getInt("LastKnownY") : 0;
         snapshot.lastKnownZ = tag.contains("LastKnownZ") ? tag.getInt("LastKnownZ") : 0;
         snapshot.homePos = tag.contains("HomePos") ? BlockPos.of(tag.getLong("HomePos")) : null;
         snapshot.homeDimension = tag.contains("HomeDimension") ? tag.getString("HomeDimension") : "";
         snapshot.skinIndex = tag.contains("SkinIndex") ? tag.getInt("SkinIndex") : 0;
         snapshot.skinResourcePath = tag.contains("SkinResourcePath") ? tag.getString("SkinResourcePath") : "";
         snapshot.savedSkinInput = tag.contains("SavedSkinInput") ? tag.getString("SavedSkinInput") : "";
         snapshot.hasCustomSkin = tag.getBoolean("HasCustomSkin");
         return snapshot;
      }

      public CompoundTag save() {
         CompoundTag tag = new CompoundTag();
         if (this.villagerUuid != null) {
            tag.putUUID("VillagerUuid", this.villagerUuid);
         }

         if (!this.displayName.isEmpty()) {
            tag.putString("DisplayName", this.displayName);
         }

         if (!this.gender.isEmpty()) {
            tag.putString("Gender", this.gender);
         }

         tag.putBoolean("Alive", this.alive);
         tag.putBoolean("HasHealth", this.hasHealth);
         tag.putFloat("Health", this.health);
         tag.putFloat("MaxHealth", this.maxHealth);
         tag.putString("CommandState", this.commandState.name());
         if (!this.lastKnownDimension.isEmpty()) {
            tag.putString("LastKnownDimension", this.lastKnownDimension);
         }

         tag.putInt("LastKnownX", this.lastKnownX);
         tag.putInt("LastKnownY", this.lastKnownY);
         tag.putInt("LastKnownZ", this.lastKnownZ);
         if (this.homePos != null) {
            tag.putLong("HomePos", this.homePos.asLong());
         }

         if (!this.homeDimension.isEmpty()) {
            tag.putString("HomeDimension", this.homeDimension);
         }

         tag.putInt("SkinIndex", this.skinIndex);
         if (!this.skinResourcePath.isEmpty()) {
            tag.putString("SkinResourcePath", this.skinResourcePath);
         }

         if (!this.savedSkinInput.isEmpty()) {
            tag.putString("SavedSkinInput", this.savedSkinInput);
         }

         tag.putBoolean("HasCustomSkin", this.hasCustomSkin);
         return tag;
      }

      public UUID getVillagerUuid() {
         return this.villagerUuid;
      }

      public String getDisplayName() {
         return this.displayName;
      }

      public String getGender() {
         return this.gender;
      }

      public boolean isAlive() {
         return this.alive;
      }

      public boolean hasHealth() {
         return this.hasHealth;
      }

      public float getHealth() {
         return this.health;
      }

      public float getMaxHealth() {
         return this.maxHealth;
      }

      public VillagerCommandState getCommandState() {
         return this.commandState;
      }

      public String getLastKnownDimension() {
         return this.lastKnownDimension;
      }

      public int getLastKnownX() {
         return this.lastKnownX;
      }

      public int getLastKnownY() {
         return this.lastKnownY;
      }

      public int getLastKnownZ() {
         return this.lastKnownZ;
      }

      public BlockPos getHomePos() {
         return this.homePos;
      }

      public ResourceKey<Level> getHomeDimension() {
         if (this.homeDimension.isEmpty()) {
            return null;
         }

         ResourceLocation id = ResourceLocation.tryParse(this.homeDimension);
         return id == null ? null : ResourceKey.create(Registries.DIMENSION, id);
      }

      public int getSkinIndex() {
         return this.skinIndex;
      }

      public String getSkinResourcePath() {
         return this.skinResourcePath;
      }

      public String getSavedSkinInput() {
         return this.savedSkinInput;
      }

      public boolean hasCustomSkin() {
         return this.hasCustomSkin;
      }
   }
}
