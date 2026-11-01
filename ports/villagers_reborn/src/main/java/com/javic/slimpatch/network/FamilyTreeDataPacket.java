package com.javic.slimpatch.network;

import com.javic.slimpatch.client.DialogueClientHandler;
import com.javic.slimpatch.familytree.FamilyTreeBuildResult;
import com.javic.slimpatch.familytree.FamilyTreeEdgePayload;
import com.javic.slimpatch.familytree.FamilyTreeNodePayload;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;

public class FamilyTreeDataPacket {
   private final UUID rootVillagerUuid;
   private final List<FamilyTreeNodePayload> nodes;
   private final List<FamilyTreeEdgePayload> edges;
   private final boolean truncated;

   public FamilyTreeDataPacket(UUID rootVillagerUuid, List<FamilyTreeNodePayload> nodes, List<FamilyTreeEdgePayload> edges, boolean truncated) {
      this.rootVillagerUuid = rootVillagerUuid;
      this.nodes = nodes;
      this.edges = edges;
      this.truncated = truncated;
   }

   public FamilyTreeDataPacket(FamilyTreeBuildResult result) {
      this(result.getRootVillagerUuid(), result.getNodes(), result.getEdges(), result.isTruncated());
   }

   public UUID getRootVillagerUuid() {
      return this.rootVillagerUuid;
   }

   public List<FamilyTreeNodePayload> getNodes() {
      return this.nodes;
   }

   public List<FamilyTreeEdgePayload> getEdges() {
      return this.edges;
   }

   public boolean isTruncated() {
      return this.truncated;
   }

   public static void encode(FamilyTreeDataPacket packet, FriendlyByteBuf buf) {
      buf.writeUUID(packet.rootVillagerUuid);
      buf.writeVarInt(packet.nodes.size());

      for (FamilyTreeNodePayload node : packet.nodes) {
         node.encode(buf);
      }

      buf.writeVarInt(packet.edges.size());

      for (FamilyTreeEdgePayload edge : packet.edges) {
         edge.encode(buf);
      }

      buf.writeBoolean(packet.truncated);
   }

   public static FamilyTreeDataPacket decode(FriendlyByteBuf buf) {
      UUID rootVillagerUuid = buf.readUUID();
      int nodeCount = ModNetworking.readCount(buf, com.javic.slimpatch.familytree.FamilyTreeGraphBuilder.MAX_NODES);
      List<FamilyTreeNodePayload> nodes = new ArrayList<>(nodeCount);

      for (int i = 0; i < nodeCount; i++) {
         nodes.add(FamilyTreeNodePayload.decode(buf));
      }

      int edgeCount = ModNetworking.readCount(buf, com.javic.slimpatch.familytree.FamilyTreeGraphBuilder.MAX_EDGES);
      List<FamilyTreeEdgePayload> edges = new ArrayList<>(edgeCount);

      for (int i = 0; i < edgeCount; i++) {
         edges.add(FamilyTreeEdgePayload.decode(buf));
      }

      return new FamilyTreeDataPacket(rootVillagerUuid, nodes, edges, buf.readBoolean());
   }

   public static void handle(FamilyTreeDataPacket msg) {
DialogueClientHandler.openFamilyTreeScreen(msg.getRootVillagerUuid(), msg.getNodes(), msg.getEdges(), msg.isTruncated());
   }
}
