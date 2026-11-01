package com.javic.slimpatch.client;

import com.javic.slimpatch.client.gui.grave.GraveScreen;
import com.javic.slimpatch.familytree.FamilyTreePortraitPayload;
import java.util.UUID;
import net.minecraft.client.Minecraft;

public final class ClientGraveScreenHandler {
   private ClientGraveScreenHandler() {
   }

   public static void openGraveScreen(UUID recordId, String displayName, String gender, String ageStage, String entityKind, FamilyTreePortraitPayload portrait) {
      Minecraft.getInstance().setScreen(new GraveScreen(recordId, displayName, gender, ageStage, entityKind, portrait));
   }
}
