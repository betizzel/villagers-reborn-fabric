package com.javic.slimpatch.familytree;

import com.javic.slimpatch.graves.GraveIdentityResolver;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

public final class FamilyTreeGraphBuilder {
   public static final int MAX_NODES = 2048;
   public static final int MAX_DEPTH = 64;
   public static final int MAX_EDGES = 4096;

   private FamilyTreeGraphBuilder() {
   }

   public static FamilyTreeBuildResult build(MinecraftServer server, UUID rootVillagerUuid) {
      if (server != null && rootVillagerUuid != null) {
         rootVillagerUuid = GraveIdentityResolver.resolveHistoricalUuid(server, rootVillagerUuid);
         FamilyTreeSavedData data = FamilyTreeSavedData.get(server);
         FamilyTreeVillagerSnapshot root = data.getVillager(rootVillagerUuid);
         if (root == null) {
            return new FamilyTreeBuildResult(rootVillagerUuid, List.of(), List.of(), false);
         }

         FamilyTreeGraphBuilder.BuildState state = new FamilyTreeGraphBuilder.BuildState();
         addVillagerNode(state, root, false);
         addRootContexts(server, data, root, state);
         addAncestorLine(server, data, root, 0, true, state);
         addDescendantLine(server, data, root, 0, state);
         return new FamilyTreeBuildResult(rootVillagerUuid, new ArrayList<>(state.nodes.values()), state.edges, state.truncated);
      } else {
         return new FamilyTreeBuildResult(rootVillagerUuid, List.of(), List.of(), false);
      }
   }

   private static void addRootContexts(
      MinecraftServer server, FamilyTreeSavedData data, FamilyTreeVillagerSnapshot snapshot, FamilyTreeGraphBuilder.BuildState state
   ) {
      addCurrentSpouseContext(server, data, snapshot, state);
      addFormerSpousePlayerContext(server, data, snapshot, state);
      addParentPlayerContext(snapshot, data, state);
   }

   private static void addAncestorLine(
      MinecraftServer server,
      FamilyTreeSavedData data,
      FamilyTreeVillagerSnapshot snapshot,
      int depth,
      boolean includeRootSiblings,
      FamilyTreeGraphBuilder.BuildState state
   ) {
      if (!state.truncated && snapshot != null && depth < 64) {
         String nodeId = villagerNodeId(snapshot.getVillagerUuid());
         if (state.expandedAncestorNodeIds.add(nodeId)) {
            addVillagerNode(state, snapshot, false);
            addParentPlayerContext(snapshot, data, state);
            UUID parentVillagerUuid = GraveIdentityResolver.resolveHistoricalUuid(server, snapshot.getParentVillagerUuid());
            UUID parentVillager2Uuid = GraveIdentityResolver.resolveHistoricalUuid(server, snapshot.getParentVillager2Uuid());
            FamilyTreeVillagerSnapshot parentSnapshot = addDirectParent(server, data, snapshot, parentVillagerUuid, snapshot.getParentVillagerName(), state);
            FamilyTreeVillagerSnapshot parent2Snapshot = addDirectParent(server, data, snapshot, parentVillager2Uuid, snapshot.getParentVillager2Name(), state);
            if (includeRootSiblings) {
               addDirectRootSiblings(server, data, snapshot, parentVillagerUuid, parentVillager2Uuid, state);
            }

            if (parentSnapshot != null) {
               addAncestorLine(server, data, parentSnapshot, depth + 1, false, state);
            }

            if (parent2Snapshot != null) {
               addAncestorLine(server, data, parent2Snapshot, depth + 1, false, state);
            }
         }
      }
   }

   private static void addDescendantLine(
      MinecraftServer server, FamilyTreeSavedData data, FamilyTreeVillagerSnapshot snapshot, int depth, FamilyTreeGraphBuilder.BuildState state
   ) {
      if (!state.truncated && snapshot != null && depth < 64) {
         String nodeId = villagerNodeId(snapshot.getVillagerUuid());
         if (state.expandedDescendantNodeIds.add(nodeId)) {
            addVillagerNode(state, snapshot, false);
            addCurrentSpouseContext(server, data, snapshot, state);
            addFormerSpousePlayerContext(server, data, snapshot, state);

            for (UUID childUuid : snapshot.getChildVillagerUuids()) {
               childUuid = GraveIdentityResolver.resolveHistoricalUuid(server, childUuid);
               if (childUuid != null && !childUuid.equals(snapshot.getVillagerUuid())) {
                  FamilyTreeVillagerSnapshot childSnapshot = data.getVillager(childUuid);
                  if (childSnapshot != null) {
                     addVillagerNode(state, childSnapshot, false);
                  } else {
                     addVillagerPlaceholder(state, childUuid, "");
                  }

                  addEdge(state, nodeId, villagerNodeId(childUuid), FamilyTreeEdgePayload.EdgeType.PARENT_CHILD);
                  if (!state.truncated && childSnapshot != null) {
                     addDirectCoParentContexts(server, data, childSnapshot, snapshot.getVillagerUuid(), state);
                     addDescendantLine(server, data, childSnapshot, depth + 1, state);
                  }
               }
            }
         }
      }
   }

   private static FamilyTreeVillagerSnapshot addDirectParent(
      MinecraftServer server,
      FamilyTreeSavedData data,
      FamilyTreeVillagerSnapshot childSnapshot,
      UUID parentVillagerUuid,
      String parentName,
      FamilyTreeGraphBuilder.BuildState state
   ) {
      if (!state.truncated && parentVillagerUuid != null && !parentVillagerUuid.equals(childSnapshot.getVillagerUuid())) {
         FamilyTreeVillagerSnapshot parentSnapshot = data.getVillager(parentVillagerUuid);
         if (parentSnapshot != null) {
            addVillagerNode(state, parentSnapshot, false);
         } else {
            addVillagerPlaceholder(state, parentVillagerUuid, parentName);
         }

         addEdge(state, villagerNodeId(parentVillagerUuid), villagerNodeId(childSnapshot.getVillagerUuid()), FamilyTreeEdgePayload.EdgeType.PARENT_CHILD);
         return parentSnapshot;
      } else {
         return null;
      }
   }

   private static void addDirectRootSiblings(
      MinecraftServer server,
      FamilyTreeSavedData data,
      FamilyTreeVillagerSnapshot rootSnapshot,
      UUID parentVillagerUuid,
      UUID parentVillager2Uuid,
      FamilyTreeGraphBuilder.BuildState state
   ) {
      addSiblingsFromParent(server, data, rootSnapshot, parentVillagerUuid, parentVillager2Uuid, state);
      if (parentVillager2Uuid != null && !parentVillager2Uuid.equals(parentVillagerUuid)) {
         addSiblingsFromParent(server, data, rootSnapshot, parentVillager2Uuid, parentVillagerUuid, state);
      }
   }

   private static void addSiblingsFromParent(
      MinecraftServer server,
      FamilyTreeSavedData data,
      FamilyTreeVillagerSnapshot rootSnapshot,
      UUID primaryParentUuid,
      UUID secondaryParentUuid,
      FamilyTreeGraphBuilder.BuildState state
   ) {
      if (!state.truncated && primaryParentUuid != null) {
         FamilyTreeVillagerSnapshot parentSnapshot = data.getVillager(primaryParentUuid);
         if (parentSnapshot != null) {
            for (UUID siblingUuid : parentSnapshot.getChildVillagerUuids()) {
               siblingUuid = GraveIdentityResolver.resolveHistoricalUuid(server, siblingUuid);
               if (siblingUuid != null && !siblingUuid.equals(rootSnapshot.getVillagerUuid())) {
                  FamilyTreeVillagerSnapshot siblingSnapshot = data.getVillager(siblingUuid);
                  if (siblingSnapshot != null) {
                     addVillagerNode(state, siblingSnapshot, false);
                  } else {
                     addVillagerPlaceholder(state, siblingUuid, "");
                  }

                  addEdge(state, villagerNodeId(primaryParentUuid), villagerNodeId(siblingUuid), FamilyTreeEdgePayload.EdgeType.PARENT_CHILD);
                  if (secondaryParentUuid != null && siblingSnapshot != null) {
                     UUID siblingParent1 = GraveIdentityResolver.resolveHistoricalUuid(server, siblingSnapshot.getParentVillagerUuid());
                     UUID siblingParent2 = GraveIdentityResolver.resolveHistoricalUuid(server, siblingSnapshot.getParentVillager2Uuid());
                     if (secondaryParentUuid.equals(siblingParent1) || secondaryParentUuid.equals(siblingParent2)) {
                        addEdge(state, villagerNodeId(secondaryParentUuid), villagerNodeId(siblingUuid), FamilyTreeEdgePayload.EdgeType.PARENT_CHILD);
                     }
                  }

                  if (state.truncated) {
                     return;
                  }
               }
            }
         }
      }
   }

   private static void addCurrentSpouseContext(
      MinecraftServer server, FamilyTreeSavedData data, FamilyTreeVillagerSnapshot snapshot, FamilyTreeGraphBuilder.BuildState state
   ) {
      addPlayerRelation(
         state,
         snapshot.getSpousePlayerUuid(),
         snapshot.getSpousePlayerName(),
         villagerNodeId(snapshot.getVillagerUuid()),
         FamilyTreeEdgePayload.EdgeType.SPOUSE,
         data
      );
      if (!state.truncated) {
         UUID spouseVillagerUuid = GraveIdentityResolver.resolveHistoricalUuid(server, snapshot.getSpouseVillagerUuid());
         if (spouseVillagerUuid != null) {
            FamilyTreeVillagerSnapshot spouseSnapshot = data.getVillager(spouseVillagerUuid);
            if (spouseSnapshot != null) {
               addVillagerNode(state, spouseSnapshot, false);
            } else {
               addVillagerPlaceholder(state, spouseVillagerUuid, snapshot.getSpouseVillagerName());
            }

            addEdge(state, villagerNodeId(snapshot.getVillagerUuid()), villagerNodeId(spouseVillagerUuid), FamilyTreeEdgePayload.EdgeType.SPOUSE);
         }
      }
   }

   private static void addFormerSpousePlayerContext(
      MinecraftServer server, FamilyTreeSavedData data, FamilyTreeVillagerSnapshot snapshot, FamilyTreeGraphBuilder.BuildState state
   ) {
      if (hasChildrenWithFormerSpouse(server, snapshot, data)) {
         addPlayerRelation(
            state,
            snapshot.getFormerSpousePlayerUuid(),
            snapshot.getFormerSpousePlayerName(),
            villagerNodeId(snapshot.getVillagerUuid()),
            FamilyTreeEdgePayload.EdgeType.FORMER_SPOUSE,
            data
         );
      }
   }

   private static void addParentPlayerContext(FamilyTreeVillagerSnapshot snapshot, FamilyTreeSavedData data, FamilyTreeGraphBuilder.BuildState state) {
      addPlayerRelation(
         state,
         snapshot.getParentPlayerUuid(),
         snapshot.getParentPlayerName(),
         villagerNodeId(snapshot.getVillagerUuid()),
         FamilyTreeEdgePayload.EdgeType.PARENT_CHILD,
         data
      );
   }

   private static void addDirectCoParentContexts(
      MinecraftServer server,
      FamilyTreeSavedData data,
      FamilyTreeVillagerSnapshot childSnapshot,
      UUID directParentUuid,
      FamilyTreeGraphBuilder.BuildState state
   ) {
      addParentPlayerContext(childSnapshot, data, state);
      UUID parentVillagerUuid = GraveIdentityResolver.resolveHistoricalUuid(server, childSnapshot.getParentVillagerUuid());
      if (parentVillagerUuid != null && !parentVillagerUuid.equals(directParentUuid)) {
         FamilyTreeVillagerSnapshot parentSnapshot = data.getVillager(parentVillagerUuid);
         if (parentSnapshot != null) {
            addVillagerNode(state, parentSnapshot, false);
         } else {
            addVillagerPlaceholder(state, parentVillagerUuid, childSnapshot.getParentVillagerName());
         }

         addEdge(state, villagerNodeId(parentVillagerUuid), villagerNodeId(childSnapshot.getVillagerUuid()), FamilyTreeEdgePayload.EdgeType.PARENT_CHILD);
      }

      UUID parentVillager2Uuid = GraveIdentityResolver.resolveHistoricalUuid(server, childSnapshot.getParentVillager2Uuid());
      if (parentVillager2Uuid != null && !parentVillager2Uuid.equals(directParentUuid)) {
         FamilyTreeVillagerSnapshot parentSnapshot = data.getVillager(parentVillager2Uuid);
         if (parentSnapshot != null) {
            addVillagerNode(state, parentSnapshot, false);
         } else {
            addVillagerPlaceholder(state, parentVillager2Uuid, childSnapshot.getParentVillager2Name());
         }

         addEdge(state, villagerNodeId(parentVillager2Uuid), villagerNodeId(childSnapshot.getVillagerUuid()), FamilyTreeEdgePayload.EdgeType.PARENT_CHILD);
      }
   }

   private static void addVillagerNode(FamilyTreeGraphBuilder.BuildState state, FamilyTreeVillagerSnapshot snapshot, boolean placeholder) {
      if (!state.truncated && snapshot != null) {
         String nodeId = villagerNodeId(snapshot.getVillagerUuid());
         if (!state.nodes.containsKey(nodeId) && state.nodes.size() >= 2048) {
            state.truncated = true;
         } else {
            state.nodes
               .putIfAbsent(
                  nodeId,
                  new FamilyTreeNodePayload(
                     nodeId,
                     FamilyTreeNodePayload.NodeType.VILLAGER,
                     snapshot.getVillagerUuid(),
                     snapshot.getName(),
                     snapshot.getGender(),
                     snapshot.getAgeStage().name(),
                     snapshot.isAlive(),
                     placeholder,
                     snapshot.isBornFromFamilySystem(),
                     createVillagerPortrait(snapshot)
                  )
               );
         }
      }
   }

   private static void addVillagerPlaceholder(FamilyTreeGraphBuilder.BuildState state, UUID villagerUuid, String displayName) {
      if (!state.truncated && villagerUuid != null) {
         String nodeId = villagerNodeId(villagerUuid);
         if (!state.nodes.containsKey(nodeId) && state.nodes.size() >= 2048) {
            state.truncated = true;
         } else {
            state.nodes
               .putIfAbsent(
                  nodeId,
                  new FamilyTreeNodePayload(
                     nodeId,
                     FamilyTreeNodePayload.NodeType.VILLAGER,
                     villagerUuid,
                     displayName == null ? "" : displayName,
                     "",
                     "ADULT",
                     true,
                     true,
                     false,
                     new FamilyTreePortraitPayload(FamilyTreePortraitPayload.PortraitType.VILLAGER_DEFAULT, 0, "", "", false, new byte[0])
                  )
               );
         }
      }
   }

   private static void addPlayerRelation(
      FamilyTreeGraphBuilder.BuildState state,
      UUID playerUuid,
      String playerName,
      String villagerNodeId,
      FamilyTreeEdgePayload.EdgeType edgeType,
      FamilyTreeSavedData data
   ) {
      if (!state.truncated && playerUuid != null) {
         String playerNodeId = playerNodeId(playerUuid);
         if (!state.nodes.containsKey(playerNodeId) && state.nodes.size() >= 2048) {
            state.truncated = true;
         } else {
            FamilyTreePlayerSnapshot playerSnapshot = data.getPlayer(playerUuid);
            String displayName = playerSnapshot != null && !playerSnapshot.getName().isEmpty()
               ? playerSnapshot.getName()
               : (playerName == null ? "" : playerName);
            state.nodes
               .putIfAbsent(
                  playerNodeId,
                  new FamilyTreeNodePayload(
                     playerNodeId,
                     FamilyTreeNodePayload.NodeType.PLAYER,
                     playerUuid,
                     displayName,
                     "",
                     "",
                     true,
                     playerSnapshot == null,
                     false,
                     new FamilyTreePortraitPayload(FamilyTreePortraitPayload.PortraitType.PLAYER_PLACEHOLDER, 0, "", "", false, new byte[0])
                  )
               );
            addEdge(state, villagerNodeId, playerNodeId, edgeType);
         }
      }
   }

   private static boolean hasChildrenWithFormerSpouse(MinecraftServer server, FamilyTreeVillagerSnapshot snapshot, FamilyTreeSavedData data) {
      if (snapshot.getFormerSpousePlayerUuid() == null) {
         return false;
      }

      for (UUID childUuid : snapshot.getChildVillagerUuids()) {
         childUuid = GraveIdentityResolver.resolveHistoricalUuid(server, childUuid);
         if (childUuid != null) {
            FamilyTreeVillagerSnapshot childSnapshot = data.getVillager(childUuid);
            if (childSnapshot != null
               && snapshot.getFormerSpousePlayerUuid().equals(childSnapshot.getParentPlayerUuid())
               && (
                  snapshot.getVillagerUuid().equals(childSnapshot.getParentVillagerUuid())
                     || snapshot.getVillagerUuid().equals(childSnapshot.getParentVillager2Uuid())
               )) {
               return true;
            }
         }
      }

      return false;
   }

   private static void addEdge(FamilyTreeGraphBuilder.BuildState state, String fromNodeId, String toNodeId, FamilyTreeEdgePayload.EdgeType edgeType) {
      if (!state.truncated && fromNodeId != null && toNodeId != null && !fromNodeId.equals(toNodeId)) {
         String a = fromNodeId.compareTo(toNodeId) <= 0 ? fromNodeId : toNodeId;
         String b = fromNodeId.compareTo(toNodeId) <= 0 ? toNodeId : fromNodeId;
         String edgeKey = edgeType.name() + "|" + a + "|" + b;
         if (state.edgeKeys.add(edgeKey)) {
            if (state.edges.size() >= 4096) {
               state.truncated = true;
            } else {
               state.edges.add(new FamilyTreeEdgePayload(fromNodeId, toNodeId, edgeType));
            }
         }
      }
   }

   private static FamilyTreePortraitPayload createVillagerPortrait(FamilyTreeVillagerSnapshot snapshot) {
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

   private static String villagerNodeId(UUID uuid) {
      return "villager:" + uuid;
   }

   private static String playerNodeId(UUID uuid) {
      return "player:" + uuid;
   }

   private static final class BuildState {
      private final Map<String, FamilyTreeNodePayload> nodes = new LinkedHashMap<>();
      private final List<FamilyTreeEdgePayload> edges = new ArrayList<>();
      private final Set<String> edgeKeys = new LinkedHashSet<>();
      private final Set<String> expandedAncestorNodeIds = new LinkedHashSet<>();
      private final Set<String> expandedDescendantNodeIds = new LinkedHashSet<>();
      private boolean truncated;
   }
}
