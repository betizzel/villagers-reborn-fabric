package com.javic.slimpatch.network;

import com.javic.slimpatch.client.DialogueClientHandler;
import net.minecraft.network.FriendlyByteBuf;

public class OpenVillagerLedgerTeleportNoticePacket {
   private final OpenVillagerLedgerTeleportNoticePacket.NoticeAction action;
   private final boolean risky;

   public OpenVillagerLedgerTeleportNoticePacket(OpenVillagerLedgerTeleportNoticePacket.NoticeAction action, boolean risky) {
      this.action = action;
      this.risky = risky;
   }

   public static void encode(OpenVillagerLedgerTeleportNoticePacket packet, FriendlyByteBuf buf) {
      buf.writeEnum(packet.action);
      buf.writeBoolean(packet.risky);
   }

   public static OpenVillagerLedgerTeleportNoticePacket decode(FriendlyByteBuf buf) {
      return new OpenVillagerLedgerTeleportNoticePacket(
         (OpenVillagerLedgerTeleportNoticePacket.NoticeAction)buf.readEnum(OpenVillagerLedgerTeleportNoticePacket.NoticeAction.class), buf.readBoolean()
      );
   }

   public static void handle(OpenVillagerLedgerTeleportNoticePacket msg) {
DialogueClientHandler.openVillagerLedgerTeleportNoticeScreen(msg.action, msg.risky);
   }

   public enum NoticeAction {
      TELEPORT_TO_PLAYER,
      SEND_HOME;
   }
}
