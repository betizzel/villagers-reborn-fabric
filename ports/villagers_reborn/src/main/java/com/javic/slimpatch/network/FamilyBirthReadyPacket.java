package com.javic.slimpatch.network;

import com.javic.slimpatch.client.DialogueClientHandler;
import net.minecraft.network.FriendlyByteBuf;

public class FamilyBirthReadyPacket {
   private final int entityId;
   private final String lineKey;

   public FamilyBirthReadyPacket(int entityId, String lineKey) {
      this.entityId = entityId;
      this.lineKey = lineKey == null ? "" : lineKey;
   }

   public static void encode(FamilyBirthReadyPacket packet, FriendlyByteBuf buf) {
      buf.writeVarInt(packet.entityId);
      buf.writeUtf(packet.lineKey);
   }

   public static FamilyBirthReadyPacket decode(FriendlyByteBuf buf) {
      return new FamilyBirthReadyPacket(buf.readVarInt(), buf.readUtf());
   }

   public static void handle(FamilyBirthReadyPacket msg) {
DialogueClientHandler.showBirthReadyPrompt(msg.entityId, msg.lineKey);
   }
}
