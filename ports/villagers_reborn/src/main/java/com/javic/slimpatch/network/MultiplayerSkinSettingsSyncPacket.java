package com.javic.slimpatch.network;

import net.minecraft.network.FriendlyByteBuf;

public class MultiplayerSkinSettingsSyncPacket {
   private final boolean allowMultiplayerCustomSkins;
   private final int maxCustomSkinSizeKb;
   private final boolean enableHavingChildren;

   public MultiplayerSkinSettingsSyncPacket(boolean allowMultiplayerCustomSkins, int maxCustomSkinSizeKb, boolean enableHavingChildren) {
      this.allowMultiplayerCustomSkins = allowMultiplayerCustomSkins;
      this.maxCustomSkinSizeKb = maxCustomSkinSizeKb;
      this.enableHavingChildren = enableHavingChildren;
   }

   public static void encode(MultiplayerSkinSettingsSyncPacket packet, FriendlyByteBuf buf) {
      buf.writeBoolean(packet.allowMultiplayerCustomSkins);
      buf.writeVarInt(packet.maxCustomSkinSizeKb);
      buf.writeBoolean(packet.enableHavingChildren);
   }

   public static MultiplayerSkinSettingsSyncPacket decode(FriendlyByteBuf buf) {
      return new MultiplayerSkinSettingsSyncPacket(buf.readBoolean(), buf.readVarInt(), buf.readBoolean());
   }

   public static void handle(MultiplayerSkinSettingsSyncPacket msg) {
      com.javic.slimpatch.client.ClientMultiplayerSkinSettings.set(msg.allowMultiplayerCustomSkins, msg.maxCustomSkinSizeKb, msg.enableHavingChildren);
   }
}
