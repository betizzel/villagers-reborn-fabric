package com.javic.slimpatch.network;

import com.javic.slimpatch.entity.FamilyTreeViewHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class CloseFamilyTreePacket {
   public static void encode(CloseFamilyTreePacket packet, FriendlyByteBuf buf) {
   }

   public static CloseFamilyTreePacket decode(FriendlyByteBuf buf) {
      return new CloseFamilyTreePacket();
   }

   public static void handle(CloseFamilyTreePacket msg, ServerPlayer player) {

         if (player != null) {
            FamilyTreeViewHandler.endViewing(player);
         }
      
   }
}
