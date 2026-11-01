package com.javic.slimpatch.network;

import com.javic.slimpatch.client.DialogueClientHandler;
import net.minecraft.network.FriendlyByteBuf;

public class OpenBirthScreenPacket {
   private final int spouseEntityId;
   private final String villagerName;
   private final String childGender;
   private final String initialPersonality;
   private final String skinType;
   private final int initialSkinId;

   public OpenBirthScreenPacket(int spouseEntityId, String villagerName, String childGender, String initialPersonality, String skinType, int initialSkinId) {
      this.spouseEntityId = spouseEntityId;
      this.villagerName = villagerName;
      this.childGender = childGender;
      this.initialPersonality = initialPersonality;
      this.skinType = skinType;
      this.initialSkinId = initialSkinId;
   }

   public static void encode(OpenBirthScreenPacket packet, FriendlyByteBuf buf) {
      buf.writeVarInt(packet.spouseEntityId);
      buf.writeUtf(packet.villagerName);
      buf.writeUtf(packet.childGender);
      buf.writeUtf(packet.initialPersonality);
      buf.writeUtf(packet.skinType);
      buf.writeVarInt(packet.initialSkinId);
   }

   public static OpenBirthScreenPacket decode(FriendlyByteBuf buf) {
      return new OpenBirthScreenPacket(buf.readVarInt(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readVarInt());
   }

   public static void handle(OpenBirthScreenPacket msg) {
DialogueClientHandler.openBirthChildScreen(
               msg.spouseEntityId, msg.villagerName, msg.childGender, msg.initialPersonality, msg.skinType, msg.initialSkinId
            );
   }
}
