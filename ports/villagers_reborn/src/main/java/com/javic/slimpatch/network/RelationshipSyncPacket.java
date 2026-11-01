package com.javic.slimpatch.network;

import com.javic.slimpatch.client.DialogueClientHandler;
import net.minecraft.network.FriendlyByteBuf;

public class RelationshipSyncPacket {
   private final int entityId;
   private final float relationship;
   private final float goldenRelationship;

   public RelationshipSyncPacket(int entityId, float relationship) {
      this(entityId, relationship, 0.0F);
   }

   public RelationshipSyncPacket(int entityId, float relationship, float goldenRelationship) {
      this.entityId = entityId;
      this.relationship = relationship;
      this.goldenRelationship = goldenRelationship;
   }

   public static void encode(RelationshipSyncPacket packet, FriendlyByteBuf buf) {
      buf.writeVarInt(packet.entityId);
      buf.writeFloat(packet.relationship);
      buf.writeFloat(packet.goldenRelationship);
   }

   public static RelationshipSyncPacket decode(FriendlyByteBuf buf) {
      return new RelationshipSyncPacket(buf.readVarInt(), buf.readFloat(), buf.readFloat());
   }

   public static void handle(RelationshipSyncPacket msg) {
DialogueClientHandler.handleRelationshipSync(msg.entityId, msg.relationship, msg.goldenRelationship);
   }
}
