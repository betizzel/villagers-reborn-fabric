package com.javic.slimpatch.network;

import com.javic.slimpatch.dialogue.DialogueManager;
import com.javic.slimpatch.entity.FemaleVillagerEntity;
import com.javic.slimpatch.entity.MaleVillagerEntity;
import com.javic.slimpatch.entity.VillagerCooldownData;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;

public final class ServerCooldownTracker {
   private static int tickCounter;

   private ServerCooldownTracker() {
   }

   public static void onServerTick(MinecraftServer server) {
      {
         if (server != null) {
            tickCounter++;
            if (tickCounter % 20 == 0) {
               for (Entry<UUID, UUID> entry : DialogueManager.getActiveDialogues().entrySet()) {
                  Villager villager = findVillagerByUUID(server, entry.getKey());
                  ServerPlayer player = server.getPlayerList().getPlayer(entry.getValue());
                  if (villager != null && player != null) {
                     if (villager instanceof MaleVillagerEntity male) {
                        VillagerCooldownData.syncDialogueCooldownsToPlayer(male, player, male.getOptionCooldowns());
                     } else if (villager instanceof FemaleVillagerEntity female) {
                        VillagerCooldownData.syncDialogueCooldownsToPlayer(female, player, female.getOptionCooldowns());
                     }
                  }
               }
            }
         }
      }
   }

   private static Villager findVillagerByUUID(MinecraftServer server, UUID uuid) {
      for (ServerLevel level : server.getAllLevels()) {
         if (level.getEntity(uuid) instanceof Villager villager) {
            return villager;
         }
      }

      return null;
   }
}
