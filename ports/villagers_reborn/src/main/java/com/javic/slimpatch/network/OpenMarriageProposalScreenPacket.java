package com.javic.slimpatch.network;

import com.javic.slimpatch.client.DialogueClientHandler;
import net.minecraft.network.FriendlyByteBuf;

public class OpenMarriageProposalScreenPacket {
   private final int entityId;
   private final String villagerName;

   public OpenMarriageProposalScreenPacket(int entityId, String villagerName) {
      this.entityId = entityId;
      this.villagerName = villagerName;
   }

   public static void encode(OpenMarriageProposalScreenPacket packet, FriendlyByteBuf buf) {
      buf.writeVarInt(packet.entityId);
      buf.writeUtf(packet.villagerName);
   }

   public static OpenMarriageProposalScreenPacket decode(FriendlyByteBuf buf) {
      return new OpenMarriageProposalScreenPacket(buf.readVarInt(), buf.readUtf());
   }

   public static void handle(OpenMarriageProposalScreenPacket msg) {
DialogueClientHandler.openMarriageProposalScreen(msg.entityId, msg.villagerName);
   }
}
