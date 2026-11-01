package com.javic.slimpatch.network;

import com.javic.slimpatch.client.DialogueClientHandler;
import net.minecraft.network.FriendlyByteBuf;

public class OpenDivorceConfirmationScreenPacket {
   private final int entityId;
   private final String villagerName;

   public OpenDivorceConfirmationScreenPacket(int entityId, String villagerName) {
      this.entityId = entityId;
      this.villagerName = villagerName;
   }

   public static void encode(OpenDivorceConfirmationScreenPacket packet, FriendlyByteBuf buf) {
      buf.writeVarInt(packet.entityId);
      buf.writeUtf(packet.villagerName);
   }

   public static OpenDivorceConfirmationScreenPacket decode(FriendlyByteBuf buf) {
      return new OpenDivorceConfirmationScreenPacket(buf.readVarInt(), buf.readUtf());
   }

   public static void handle(OpenDivorceConfirmationScreenPacket msg) {
DialogueClientHandler.openDivorceConfirmationScreen(msg.entityId, msg.villagerName);
   }
}
