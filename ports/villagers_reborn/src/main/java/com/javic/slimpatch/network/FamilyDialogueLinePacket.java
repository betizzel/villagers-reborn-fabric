package com.javic.slimpatch.network;

import com.javic.slimpatch.client.DialogueClientHandler;
import net.minecraft.network.FriendlyByteBuf;

public class FamilyDialogueLinePacket {
   private final int entityId;
   private final String lineKey;

   public FamilyDialogueLinePacket(int entityId, String lineKey) {
      this.entityId = entityId;
      this.lineKey = lineKey == null ? "" : lineKey;
   }

   public static void encode(FamilyDialogueLinePacket packet, FriendlyByteBuf buf) {
      buf.writeVarInt(packet.entityId);
      buf.writeUtf(packet.lineKey);
   }

   public static FamilyDialogueLinePacket decode(FriendlyByteBuf buf) {
      return new FamilyDialogueLinePacket(buf.readVarInt(), buf.readUtf());
   }

   public static void handle(FamilyDialogueLinePacket msg) {
DialogueClientHandler.handleFamilyDialogueLine(msg.entityId, msg.lineKey);
   }
}
