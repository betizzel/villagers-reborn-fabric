package com.javic.slimpatch.network;

import com.javic.slimpatch.data.WorldSkinData;
import com.javic.slimpatch.entity.AbstractRomanceVillagerEntity;
import com.javic.slimpatch.entity.BirthScreenData;
import com.javic.slimpatch.entity.FamilyBirthHandler;
import com.javic.slimpatch.entity.FamilyVillager;
import com.javic.slimpatch.entity.VillagerPersonality;
import com.javic.slimpatch.entity.VillagerRelationshipStage;
import com.javic.slimpatch.util.MultiplayerSkinStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class ConfirmBirthPacket {
   private static final double MAX_BIRTH_DISTANCE_SQR = 64.0;
   private final int spouseEntityId;
   private final String childName;
   private final int selectedSkinId;
   private final String selectedPersonality;
   private final String childGender;
   private final boolean customSkin;
   private final String customSkinInput;
   private final byte[] customSkinPngData;

   public ConfirmBirthPacket(
      int spouseEntityId,
      String childName,
      int selectedSkinId,
      String selectedPersonality,
      String childGender,
      boolean customSkin,
      String customSkinInput,
      byte[] customSkinPngData
   ) {
      this.spouseEntityId = spouseEntityId;
      this.childName = childName;
      this.selectedSkinId = selectedSkinId;
      this.selectedPersonality = selectedPersonality;
      this.childGender = childGender;
      this.customSkin = customSkin;
      this.customSkinInput = customSkinInput;
      this.customSkinPngData = customSkinPngData;
   }

   public static void encode(ConfirmBirthPacket packet, FriendlyByteBuf buf) {
      buf.writeVarInt(packet.spouseEntityId);
      buf.writeUtf(packet.childName);
      buf.writeVarInt(packet.selectedSkinId);
      buf.writeUtf(packet.selectedPersonality);
      buf.writeUtf(packet.childGender);
      buf.writeBoolean(packet.customSkin);
      buf.writeUtf(packet.customSkinInput == null ? "" : packet.customSkinInput);
      buf.writeByteArray(packet.customSkinPngData == null ? new byte[0] : packet.customSkinPngData);
   }

   public static ConfirmBirthPacket decode(FriendlyByteBuf buf) {
      return new ConfirmBirthPacket(
         buf.readVarInt(), buf.readUtf(), buf.readVarInt(), buf.readUtf(), buf.readUtf(), buf.readBoolean(), buf.readUtf(), buf.readByteArray()
      );
   }

   public static void handle(ConfirmBirthPacket msg, ServerPlayer player) {

            if (player != null) {
               Entity entity = player.serverLevel().getEntity(msg.spouseEntityId);
               if (entity instanceof AbstractRomanceVillagerEntity villager && entity instanceof FamilyVillager familyVillager && villager.isAlive()) {
                  if (!(player.distanceToSqr(villager) > 64.0)) {
                     if (familyVillager.getRelationshipStage() == VillagerRelationshipStage.MARRIED) {
                        if (player.getUUID().equals(familyVillager.getSpousePlayerUuid())) {
                           if (familyVillager.isExpectingChild() && player.level().getGameTime() >= familyVillager.getExpectingEndsAt()) {
                              String childName = BirthScreenData.sanitizeChildName(msg.childName);
                              if (!BirthScreenData.isValidChildName(childName)) {
                                 ModNetworking.sendToClient(new BirthConfirmResultPacket(false, "slimpatch.message.birth_invalid_name", ""), player);
                              } else {
                                 VillagerPersonality selectedPersonality;
                                 try {
                                    selectedPersonality = VillagerPersonality.valueOf(msg.selectedPersonality);
                                 } catch (IllegalArgumentException e) {
                                    selectedPersonality = null;
                                 }

                                 if (!BirthScreenData.isValidPersonality(selectedPersonality)) {
                                    ModNetworking.sendToClient(new BirthConfirmResultPacket(false, "slimpatch.message.birth_invalid_personality", ""), player);
                                 } else {
                                    String childGender = BirthScreenData.normalizeGender(msg.childGender);
                                    CompoundTag pendingGenders = com.javic.slimpatch.fabric.PersistentEntityData.get(villager).getCompound("SlimPatchBirthScreenPendingGenders");
                                    String storedGender = pendingGenders.getString(player.getUUID().toString());
                                    if (storedGender.isEmpty()) {
                                       ModNetworking.sendToClient(new BirthConfirmResultPacket(false, "slimpatch.message.birth_invalid_skin", ""), player);
                                    } else {
                                       String expectedGender = BirthScreenData.normalizeGender(storedGender);
                                       if (!childGender.equals(expectedGender)) {
                                          ModNetworking.sendToClient(new BirthConfirmResultPacket(false, "slimpatch.message.birth_invalid_skin", ""), player);
                                       } else {
                                          String skinType = BirthScreenData.normalizeSkinType(WorldSkinData.get(player.serverLevel()).getTheme());
                                          if (msg.customSkin) {
                                             if ((player.getServer() == null || player.getServer().isDedicatedServer() || player.getServer().isPublished())
                                                && !MultiplayerSkinStorage.isEnabled()) {
                                                ModNetworking.sendToClient(
                                                   new BirthConfirmResultPacket(false, "slimpatch.message.birth_invalid_skin", ""), player
                                                );
                                                return;
                                             }

                                             if (MultiplayerSkinStorage.validatePng(msg.customSkinPngData) != null) {
                                                ModNetworking.sendToClient(
                                                   new BirthConfirmResultPacket(false, "slimpatch.message.birth_invalid_skin", ""), player
                                                );
                                                return;
                                             }

                                             MultiplayerSkinStorage.sanitizeSavedInput(msg.customSkinInput);
                                          } else if (!BirthScreenData.isValidSkin(childGender, skinType, msg.selectedSkinId)) {
                                             ModNetworking.sendToClient(new BirthConfirmResultPacket(false, "slimpatch.message.birth_invalid_skin", ""), player);
                                             return;
                                          }

                                          String failureMessage = FamilyBirthHandler.spawnChild(
                                             player,
                                             villager,
                                             familyVillager,
                                             childName,
                                             childGender,
                                             selectedPersonality,
                                             msg.selectedSkinId,
                                             msg.customSkin,
                                             msg.customSkinInput,
                                             msg.customSkinPngData
                                          );
                                          if (failureMessage != null) {
                                             ModNetworking.sendToClient(new BirthConfirmResultPacket(false, failureMessage, ""), player);
                                          } else {
                                             ModNetworking.sendToClient(
                                                new BirthConfirmResultPacket(true, "slimpatch.message.birth_success", childName), player
                                             );
                                          }
                                       }
                                    }
                                 }
                              }
                           } else {
                              ModNetworking.sendToClient(new BirthConfirmResultPacket(false, "slimpatch.message.birth_not_ready", ""), player);
                           }
                        }
                     }
                  }
               }
            }
         
   }
}
