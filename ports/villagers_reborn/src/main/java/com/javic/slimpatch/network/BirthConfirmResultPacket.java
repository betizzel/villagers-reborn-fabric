package com.javic.slimpatch.network;

import com.javic.slimpatch.client.DialogueClientHandler;
import net.minecraft.network.FriendlyByteBuf;

public class BirthConfirmResultPacket {
   private final boolean success;
   private final String translationKey;
   private final String translationArgument;

   public BirthConfirmResultPacket(boolean success, String translationKey, String translationArgument) {
      this.success = success;
      this.translationKey = translationKey;
      this.translationArgument = translationArgument == null ? "" : translationArgument;
   }

   public static void encode(BirthConfirmResultPacket packet, FriendlyByteBuf buf) {
      buf.writeBoolean(packet.success);
      buf.writeUtf(packet.translationKey);
      buf.writeUtf(packet.translationArgument);
   }

   public static BirthConfirmResultPacket decode(FriendlyByteBuf buf) {
      return new BirthConfirmResultPacket(buf.readBoolean(), buf.readUtf(), buf.readUtf());
   }

   public static void handle(BirthConfirmResultPacket msg) {
DialogueClientHandler.handleBirthConfirmResult(msg.success, msg.translationKey, msg.translationArgument);
   }
}
