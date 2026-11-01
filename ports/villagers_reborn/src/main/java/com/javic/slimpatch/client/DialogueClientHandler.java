package com.javic.slimpatch.client;

import com.javic.slimpatch.client.gui.BirthChildScreen;
import com.javic.slimpatch.client.gui.DivorceConfirmationScreen;
import com.javic.slimpatch.client.gui.MarriageProposalScreen;
import com.javic.slimpatch.client.gui.VillagerDialogueScreen;
import com.javic.slimpatch.client.gui.familytree.FamilyTreeScreen;
import com.javic.slimpatch.client.gui.ledger.VillagerLedgerScreen;
import com.javic.slimpatch.client.gui.ledger.VillagerLedgerTeleportNoticeScreen;
import com.javic.slimpatch.client.key.ModKeyBindings;
import com.javic.slimpatch.entity.FemaleVillagerEntity;
import com.javic.slimpatch.entity.MaleVillagerEntity;
import com.javic.slimpatch.entity.VillagerRelationshipData;
import com.javic.slimpatch.familytree.FamilyTreeEdgePayload;
import com.javic.slimpatch.familytree.FamilyTreeNodePayload;
import com.javic.slimpatch.ledger.VillagerLedgerEntryPayload;
import com.javic.slimpatch.network.DialogueStatePacket;
import com.javic.slimpatch.network.ModNetworking;
import com.javic.slimpatch.network.OpenVillagerLedgerTeleportNoticePacket;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult.Type;

public final class DialogueClientHandler {
   private static final double MAX_DIALOGUE_DISTANCE_SQR = 16.0;

   private DialogueClientHandler() {
   }

   public static void onKeyInput() {
      if (ModKeyBindings.OPEN_DIALOGUE != null && ModKeyBindings.OPEN_DIALOGUE.consumeClick()) {
         Minecraft minecraft = Minecraft.getInstance();
         if (minecraft.screen == null) {
            LocalPlayer player = minecraft.player;
            if (player != null && minecraft.hitResult != null && minecraft.hitResult.getType() == Type.ENTITY) {
               if (((EntityHitResult)minecraft.hitResult).getEntity() instanceof Villager villager) {
                  if (villager instanceof MaleVillagerEntity || villager instanceof FemaleVillagerEntity) {
                     if (villager.isAlive() && !(player.distanceToSqr(villager) > 16.0)) {
                        ModNetworking.sendToServer(new DialogueStatePacket(villager.getId(), true));
                        minecraft.setScreen(new VillagerDialogueScreen(villager));
                     }
                  }
               }
            }
         }
      }
   }

   public static void handleDialogueResult(int entityId, String option, boolean success, String line) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.screen instanceof VillagerDialogueScreen screen) {
         screen.showDialogueResult(entityId, option, success, line);
      }
   }

   public static void handleRelationshipSync(int entityId, float relationship, float goldenRelationship) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level != null) {
         if (minecraft.level.getEntity(entityId) instanceof Villager villager) {
            VillagerRelationshipData.setClientRelationship(villager, relationship);
            VillagerRelationshipData.setClientGoldenRelationship(villager, goldenRelationship);
         }
      }
   }

   public static void handleFamilyDialogueLine(int entityId, String lineKey) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.screen instanceof VillagerDialogueScreen screen) {
         screen.showFamilyLine(entityId, lineKey);
      }
   }

   public static void showBirthReadyPrompt(int entityId, String lineKey) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.screen instanceof VillagerDialogueScreen screen) {
         screen.showBirthReadyPrompt(entityId, lineKey);
      }
   }

   public static void openBirthChildScreen(
      int spouseEntityId, String villagerName, String childGender, String initialPersonality, String skinType, int initialSkinId
   ) {
      Minecraft minecraft = Minecraft.getInstance();
      minecraft.setScreen(new BirthChildScreen(spouseEntityId, villagerName, childGender, initialPersonality, skinType, initialSkinId));
   }

   public static void handleBirthConfirmResult(boolean success, String translationKey, String translationArgument) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.screen instanceof BirthChildScreen birthChildScreen) {
         birthChildScreen.handleConfirmResult(success, translationKey, translationArgument);
      } else {
         if (minecraft.player != null) {
            minecraft.player
               .displayClientMessage(
                  translationArgument != null && !translationArgument.isEmpty()
                     ? Component.translatable(translationKey, new Object[]{translationArgument})
                     : Component.translatable(translationKey),
                  true
               );
         }
      }
   }

   public static void openFamilyTreeScreen(UUID rootVillagerUuid, List<FamilyTreeNodePayload> nodes, List<FamilyTreeEdgePayload> edges, boolean truncated) {
      Minecraft minecraft = Minecraft.getInstance();
      minecraft.setScreen(new FamilyTreeScreen(rootVillagerUuid, nodes, edges, truncated));
   }

   public static void openVillagerLedgerScreen(
      List<VillagerLedgerEntryPayload> companions, List<VillagerLedgerEntryPayload> family, List<VillagerLedgerEntryPayload> hidden, boolean openScreen
   ) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.screen instanceof VillagerLedgerScreen screen) {
         screen.handleDataRefresh(companions, family, hidden);
      } else if (openScreen) {
         minecraft.setScreen(new VillagerLedgerScreen(companions, family, hidden));
      }
   }

   public static void openVillagerLedgerTeleportNoticeScreen(OpenVillagerLedgerTeleportNoticePacket.NoticeAction action, boolean risky) {
      Minecraft minecraft = Minecraft.getInstance();
      VillagerLedgerTeleportNoticeScreen.refreshSession(minecraft.level);
      if (VillagerLedgerTeleportNoticeScreen.shouldShow(action, risky)) {
         minecraft.setScreen(new VillagerLedgerTeleportNoticeScreen(minecraft.screen));
      }
   }

   public static void openMarriageProposalScreen(int entityId, String villagerName) {
      Minecraft minecraft = Minecraft.getInstance();
      minecraft.setScreen(new MarriageProposalScreen(entityId, villagerName));
   }

   public static void openDivorceConfirmationScreen(int entityId, String villagerName) {
      Minecraft minecraft = Minecraft.getInstance();
      minecraft.setScreen(new DivorceConfirmationScreen(entityId, villagerName));
   }

   public static void showMarriageSuccessFallback(String villagerName) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player != null) {
         minecraft.player.displayClientMessage(Component.translatable("slimpatch.message.marriage_success", new Object[]{villagerName}), true);
      }
   }
}
