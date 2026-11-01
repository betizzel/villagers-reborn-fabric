package com.javic.slimpatch.network;

import com.javic.slimpatch.client.DialogueClientHandler;
import net.minecraft.network.FriendlyByteBuf;

public class DialogueResultPacket {
   private final int entityId;
   private final String option;
   private final boolean success;
   private final String line;

   public DialogueResultPacket(int entityId, String option, boolean success) {
      this(entityId, option, success, "");
   }

   public DialogueResultPacket(int entityId, String option, boolean success, String line) {
      this.entityId = entityId;
      this.option = option;
      this.success = success;
      this.line = line == null ? "" : line;
   }

   public static void encode(DialogueResultPacket packet, FriendlyByteBuf buf) {
      buf.writeVarInt(packet.entityId);
      buf.writeUtf(packet.option);
      buf.writeBoolean(packet.success);
      buf.writeUtf(packet.line);
   }

   public static DialogueResultPacket decode(FriendlyByteBuf buf) {
      return new DialogueResultPacket(buf.readVarInt(), buf.readUtf(), buf.readBoolean(), buf.readUtf());
   }

   public static void handle(DialogueResultPacket msg) {
DialogueClientHandler.handleDialogueResult(msg.entityId, msg.option, msg.success, msg.line);
   }
}
