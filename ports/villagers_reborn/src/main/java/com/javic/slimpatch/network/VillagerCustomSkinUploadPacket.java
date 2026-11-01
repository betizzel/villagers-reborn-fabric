package com.javic.slimpatch.network;

import com.javic.slimpatch.entity.CommandableVillager;
import com.javic.slimpatch.entity.FemaleVillagerEntity;
import com.javic.slimpatch.entity.MaleVillagerEntity;
import com.javic.slimpatch.entity.VillagerCommandHandler;
import com.javic.slimpatch.events.MultiplayerSkinSyncHandler;
import com.javic.slimpatch.familytree.FamilyTreeTracker;
import com.javic.slimpatch.util.MultiplayerSkinStorage;
import java.io.IOException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;

public class VillagerCustomSkinUploadPacket {
   private static final double MAX_EDIT_DISTANCE_SQR = 64.0;
   private final int entityId;
   private final String savedSkinInput;
   private final byte[] pngData;
   private final boolean clear;

   public VillagerCustomSkinUploadPacket(int entityId, String savedSkinInput, byte[] pngData, boolean clear) {
      this.entityId = entityId;
      this.savedSkinInput = savedSkinInput;
      this.pngData = pngData;
      this.clear = clear;
   }

   public static void encode(VillagerCustomSkinUploadPacket packet, FriendlyByteBuf buf) {
      buf.writeVarInt(packet.entityId);
      buf.writeUtf(packet.savedSkinInput == null ? "" : packet.savedSkinInput);
      buf.writeBoolean(packet.clear);
      buf.writeByteArray(packet.pngData == null ? new byte[0] : packet.pngData);
   }

   public static VillagerCustomSkinUploadPacket decode(FriendlyByteBuf buf) {
      int entityId = buf.readVarInt();
      String savedSkinInput = buf.readUtf();
      boolean clear = buf.readBoolean();
      byte[] pngData = buf.readByteArray();
      return new VillagerCustomSkinUploadPacket(entityId, savedSkinInput, pngData, clear);
   }

   public static void handle(VillagerCustomSkinUploadPacket msg, ServerPlayer player) {

         if (player != null) {
            if (!MultiplayerSkinStorage.isEnabled()) {
               player.displayClientMessage(Component.translatable("slimpatch.message.custom_skin_disabled"), true);
            } else {
               ServerLevel level = player.serverLevel();
               Entity entity = level.getEntity(msg.entityId);
               if (entity instanceof Villager villager && entity instanceof CommandableVillager commandableVillager) {
                  if (!(player.distanceToSqr(villager) > 64.0)) {
                     if (VillagerCommandHandler.canUseProtectedAction(villager, commandableVillager, player)) {
                        try {
                           if (msg.clear) {
                              MultiplayerSkinStorage.deleteSkin(villager.getUUID());
                              applySkinMetadata(villager, "", "");
                              FamilyTreeTracker.onVillagerSkinChanged(level.getServer(), villager);
                              MultiplayerSkinSyncHandler.broadcastSkin(villager, null, "", true);
                              return;
                           }

                           String validationError = MultiplayerSkinStorage.validatePng(msg.pngData);
                           if (validationError != null) {
                              player.displayClientMessage(Component.literal(validationError), true);
                              return;
                           }

                           MultiplayerSkinStorage.saveSkin(villager.getUUID(), msg.pngData);
                           String savedInput = MultiplayerSkinStorage.sanitizeSavedInput(msg.savedSkinInput);
                           applySkinMetadata(villager, savedInput, MultiplayerSkinStorage.getStoredSkinPath(villager.getUUID()));
                           FamilyTreeTracker.onVillagerSkinChanged(level.getServer(), villager);
                           MultiplayerSkinSyncHandler.broadcastSkin(villager, msg.pngData, savedInput, false);
                        } catch (IOException e) {
                           player.displayClientMessage(Component.translatable("slimpatch.message.custom_skin_store_failed"), true);
                        }
                     }
                  }
               }
            }
         }
      
   }

   private static void applySkinMetadata(Villager villager, String savedInput, String customSkinPath) {
      if (villager instanceof MaleVillagerEntity maleVillager) {
         maleVillager.setSavedSkinInput(savedInput);
         maleVillager.setCustomSkinPath(customSkinPath);
      } else if (villager instanceof FemaleVillagerEntity femaleVillager) {
         femaleVillager.setSavedSkinInput(savedInput);
         femaleVillager.setCustomSkinPath(customSkinPath);
      } else {
         if (savedInput.isEmpty()) {
            com.javic.slimpatch.fabric.PersistentEntityData.get(villager).remove("SavedSkinInput");
         } else {
            com.javic.slimpatch.fabric.PersistentEntityData.get(villager).putString("SavedSkinInput", savedInput);
         }

         if (customSkinPath.isEmpty()) {
            com.javic.slimpatch.fabric.PersistentEntityData.get(villager).remove("CustomSkinPath");
         } else {
            com.javic.slimpatch.fabric.PersistentEntityData.get(villager).putString("CustomSkinPath", customSkinPath);
         }
      }
   }
}
