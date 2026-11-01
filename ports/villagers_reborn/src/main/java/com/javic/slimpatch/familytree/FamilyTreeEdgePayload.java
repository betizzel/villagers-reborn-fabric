package com.javic.slimpatch.familytree;

import net.minecraft.network.FriendlyByteBuf;

public class FamilyTreeEdgePayload {
   private final String fromNodeId;
   private final String toNodeId;
   private final FamilyTreeEdgePayload.EdgeType edgeType;

   public FamilyTreeEdgePayload(String fromNodeId, String toNodeId, FamilyTreeEdgePayload.EdgeType edgeType) {
      this.fromNodeId = fromNodeId == null ? "" : fromNodeId;
      this.toNodeId = toNodeId == null ? "" : toNodeId;
      this.edgeType = edgeType == null ? FamilyTreeEdgePayload.EdgeType.PARENT_CHILD : edgeType;
   }

   public String getFromNodeId() { return this.fromNodeId; }
   public String getToNodeId() { return this.toNodeId; }
   public FamilyTreeEdgePayload.EdgeType getEdgeType() { return this.edgeType; }

   public void encode(FriendlyByteBuf buf) {
      buf.writeUtf(this.fromNodeId);
      buf.writeUtf(this.toNodeId);
      buf.writeEnum(this.edgeType);
   }

   public static FamilyTreeEdgePayload decode(FriendlyByteBuf buf) {
      return new FamilyTreeEdgePayload(buf.readUtf(), buf.readUtf(), buf.readEnum(FamilyTreeEdgePayload.EdgeType.class));
   }

   public enum EdgeType { PARENT_CHILD, SPOUSE, FORMER_SPOUSE }
}
