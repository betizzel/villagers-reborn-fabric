package com.javic.slimpatch.network;

import com.javic.slimpatch.client.gui.VillagerDialogueScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

public class SpouseCookingLinePacket {
   private final int entityId;
   private final String lineKey;

   public SpouseCookingLinePacket(int entityId, String lineKey) {
      this.entityId = entityId;
      this.lineKey = lineKey;
   }

   public static void encode(SpouseCookingLinePacket packet, FriendlyByteBuf buf) {
      buf.writeVarInt(packet.entityId);
      buf.writeUtf(packet.lineKey);
   }

   public static SpouseCookingLinePacket decode(FriendlyByteBuf buf) {
      return new SpouseCookingLinePacket(buf.readVarInt(), buf.readUtf());
   }

   public static void handle(SpouseCookingLinePacket msg) {
applyClientLine(msg.entityId, msg.lineKey);
   }

   private static void applyClientLine(int entityId, String lineKey) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.screen instanceof VillagerDialogueScreen screen) {
         screen.showSpouseCookingLine(entityId, lineKey);
      }
   }
}
