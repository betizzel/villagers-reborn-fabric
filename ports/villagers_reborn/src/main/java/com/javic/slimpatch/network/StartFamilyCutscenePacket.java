package com.javic.slimpatch.network;

import com.javic.slimpatch.Config;
import net.minecraft.network.FriendlyByteBuf;

public class StartFamilyCutscenePacket {
   private final int entityId;
   private final String villagerName;

   public StartFamilyCutscenePacket(int entityId, String villagerName) {
      this.entityId = entityId;
      this.villagerName = villagerName;
   }

   public static void encode(StartFamilyCutscenePacket packet, FriendlyByteBuf buf) {
      buf.writeVarInt(packet.entityId);
      buf.writeUtf(packet.villagerName);
   }

   public static StartFamilyCutscenePacket decode(FriendlyByteBuf buf) {
      return new StartFamilyCutscenePacket(buf.readVarInt(), buf.readUtf());
   }

   public static void handle(StartFamilyCutscenePacket msg) {
      try {
         if (Config.ENABLE_FAMILY_CUTSCENE.get()
             && com.javic.slimpatch.client.cutscene.FamilyCutsceneController.start(msg.entityId, msg.villagerName)) return;
      } catch (Exception ignored) {
      }
      showFamilySuccessFallback(msg.villagerName);
   }

   private static void showFamilySuccessFallback(String villagerName) {
      com.javic.slimpatch.client.cutscene.WeddingMusicDuckingHandler.restore();
      var player = net.minecraft.client.Minecraft.getInstance().player;
      if (player != null) {
         player.displayClientMessage(net.minecraft.network.chat.Component.translatable("slimpatch.message.family_started", villagerName), false);
      }
   }
}
