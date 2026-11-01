package com.javic.slimpatch.network;

import com.javic.slimpatch.client.ClientGraveScreenHandler;
import com.javic.slimpatch.familytree.FamilyTreePortraitPayload;
import com.mojang.logging.LogUtils;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import org.slf4j.Logger;

public class OpenGraveScreenPacket {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final UUID recordId;
   private final String displayName;
   private final String gender;
   private final String ageStage;
   private final String entityKind;
   private final FamilyTreePortraitPayload portrait;

   public OpenGraveScreenPacket(UUID recordId, String displayName, String gender, String ageStage, String entityKind, FamilyTreePortraitPayload portrait) {
      this.recordId = recordId;
      this.displayName = displayName == null ? "" : displayName;
      this.gender = gender == null ? "" : gender;
      this.ageStage = ageStage == null ? "" : ageStage;
      this.entityKind = entityKind == null ? "" : entityKind;
      this.portrait = portrait == null
         ? new FamilyTreePortraitPayload(FamilyTreePortraitPayload.PortraitType.PLAYER_PLACEHOLDER, 0, "", "", true, new byte[0])
         : portrait;
   }

   public static void encode(OpenGraveScreenPacket packet, FriendlyByteBuf buf) {
      buf.writeUUID(packet.recordId);
      buf.writeUtf(packet.displayName);
      buf.writeUtf(packet.gender);
      buf.writeUtf(packet.ageStage);
      buf.writeUtf(packet.entityKind);
      packet.portrait.encode(buf);
   }

   public static OpenGraveScreenPacket decode(FriendlyByteBuf buf) {
      return new OpenGraveScreenPacket(buf.readUUID(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(), FamilyTreePortraitPayload.decode(buf));
   }

   public static void handle(OpenGraveScreenPacket msg) {
            try {
               ClientGraveScreenHandler.openGraveScreen(msg.recordId, msg.displayName, msg.gender, msg.ageStage, msg.entityKind, msg.portrait);
            } catch (RuntimeException e) {
               LOGGER.error("Failed to open grave screen on client for record {}", msg.recordId, e);
            }
         
   }
}
