package com.javic.slimpatch.network;

import com.javic.slimpatch.Config;
import net.minecraft.network.FriendlyByteBuf;

public class StartWeddingCutscenePacket {
   private final int entityId;
   private final String villagerName;

   public StartWeddingCutscenePacket(int entityId, String villagerName) {
      this.entityId = entityId;
      this.villagerName = villagerName;
   }

   public static void encode(StartWeddingCutscenePacket packet, FriendlyByteBuf buf) {
      buf.writeVarInt(packet.entityId);
      buf.writeUtf(packet.villagerName);
   }

   public static StartWeddingCutscenePacket decode(FriendlyByteBuf buf) {
      return new StartWeddingCutscenePacket(buf.readVarInt(), buf.readUtf());
   }

   public static void handle(StartWeddingCutscenePacket msg) {
      try {
         if (Config.ENABLE_WEDDING_CUTSCENE.get()
             && com.javic.slimpatch.client.cutscene.WeddingCutsceneController.start(msg.entityId, msg.villagerName)) return;
      } catch (Exception ignored) {
      }
      showMarriageSuccessFallback(msg.villagerName);
   }

   private static void showMarriageSuccessFallback(String villagerName) {
      com.javic.slimpatch.client.cutscene.WeddingMusicDuckingHandler.restore();
      var player = net.minecraft.client.Minecraft.getInstance().player;
      if (player != null) {
         player.displayClientMessage(net.minecraft.network.chat.Component.translatable("slimpatch.message.marriage_success", villagerName), true);
      }
   }
}
