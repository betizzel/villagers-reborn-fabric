package com.javic.slimpatch.network;

import com.javic.slimpatch.data.WorldSkinData;
import com.javic.slimpatch.entity.AbstractRomanceVillagerEntity;
import com.javic.slimpatch.entity.BabyGenderCharmHandler;
import com.javic.slimpatch.entity.BirthScreenData;
import com.javic.slimpatch.entity.FamilyVillager;
import com.javic.slimpatch.entity.VillagerRelationshipStage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class RequestBirthScreenPacket {
   private static final double MAX_BIRTH_DISTANCE_SQR = 64.0;
   public static final String BIRTH_SCREEN_PENDING_GENDERS_TAG = "SlimPatchBirthScreenPendingGenders";
   private final int entityId;

   public RequestBirthScreenPacket(int entityId) {
      this.entityId = entityId;
   }

   public static void encode(RequestBirthScreenPacket packet, FriendlyByteBuf buf) {
      buf.writeVarInt(packet.entityId);
   }

   public static RequestBirthScreenPacket decode(FriendlyByteBuf buf) {
      return new RequestBirthScreenPacket(buf.readVarInt());
   }

   public static void handle(RequestBirthScreenPacket msg, ServerPlayer player) {

            if (player != null) {
               Entity entity = player.serverLevel().getEntity(msg.entityId);
               if (entity instanceof AbstractRomanceVillagerEntity villager && entity instanceof FamilyVillager familyVillager && villager.isAlive()) {
                  if (!(player.distanceToSqr(villager) > 64.0)) {
                     if (familyVillager.getRelationshipStage() == VillagerRelationshipStage.MARRIED) {
                        if (player.getUUID().equals(familyVillager.getSpousePlayerUuid())) {
                           if (familyVillager.isExpectingChild() && player.level().getGameTime() >= familyVillager.getExpectingEndsAt()) {
                              ServerLevel serverLevel = player.serverLevel();
                              long genderRoll = serverLevel.random.nextLong()
                                 ^ player.getUUID().getMostSignificantBits()
                                 ^ player.getUUID().getLeastSignificantBits()
                                 ^ villager.getUUID().getMostSignificantBits()
                                 ^ villager.getUUID().getLeastSignificantBits()
                                 ^ serverLevel.getGameTime();
                              String childGender = BabyGenderCharmHandler.resolveChildGender(familyVillager, (genderRoll & 1L) == 0L ? "male" : "female");
                              String skinType = BirthScreenData.normalizeSkinType(WorldSkinData.get(serverLevel).getTheme());
                              int initialSkinId = BirthScreenData.getRandomSkinId(childGender, skinType, serverLevel.random);
                              String initialPersonality = BirthScreenData.getRandomPersonality(serverLevel.random).name();
                              CompoundTag pendingGenders = com.javic.slimpatch.fabric.PersistentEntityData.get(villager).getCompound("SlimPatchBirthScreenPendingGenders");
                              pendingGenders.putString(player.getUUID().toString(), childGender);
                              com.javic.slimpatch.fabric.PersistentEntityData.get(villager).put("SlimPatchBirthScreenPendingGenders", pendingGenders);
                              ModNetworking.sendToClient(
                                 new OpenBirthScreenPacket(
                                    villager.getId(), villager.getName().getString(), childGender, initialPersonality, skinType, initialSkinId
                                 ),
                                 player
                              );
                           } else {
                              player.displayClientMessage(Component.translatable("slimpatch.message.birth_not_ready"), true);
                           }
                        }
                     }
                  }
               }
            }
         
   }
}
