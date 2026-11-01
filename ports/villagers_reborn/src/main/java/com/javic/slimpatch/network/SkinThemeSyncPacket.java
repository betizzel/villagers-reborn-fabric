package com.javic.slimpatch.network;

import net.minecraft.network.FriendlyByteBuf;

public class SkinThemeSyncPacket {
   private final String theme;

   public SkinThemeSyncPacket(String theme) {
      this.theme = theme;
   }

   public static void encode(SkinThemeSyncPacket packet, FriendlyByteBuf buf) {
      buf.writeUtf(packet.theme);
   }

   public static SkinThemeSyncPacket decode(FriendlyByteBuf buf) {
      return new SkinThemeSyncPacket(buf.readUtf());
   }

   public static void handle(SkinThemeSyncPacket msg) {
      com.javic.slimpatch.client.ClientSkinTheme.setTheme(msg.theme);
   }
}
