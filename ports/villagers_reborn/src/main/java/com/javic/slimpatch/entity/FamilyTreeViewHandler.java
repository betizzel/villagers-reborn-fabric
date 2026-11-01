package com.javic.slimpatch.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;

public final class FamilyTreeViewHandler {
   private static final Map<UUID, ViewState> ACTIVE = new HashMap<>();

   private FamilyTreeViewHandler() {
   }

   public static void beginViewing(ServerPlayer player, Villager villager) {
      if (player != null && villager instanceof CommandableVillager commandableVillager) {
         endViewing(player);
         VillagerCommandHandler.TemporaryCommandStateSnapshot snapshot = VillagerCommandHandler.createTemporaryStaySnapshot(commandableVillager);
         boolean temporaryStayApplied = VillagerCommandHandler.beginTemporaryStay(villager, commandableVillager);
         ACTIVE.put(player.getUUID(), new ViewState(villager.getUUID(), snapshot, temporaryStayApplied));
      }
   }

   public static void endViewing(ServerPlayer player) {
      if (player != null) {
         ViewState state = ACTIVE.remove(player.getUUID());
         if (state != null && state.temporaryStayApplied()
               && player.serverLevel().getEntity(state.villagerUuid()) instanceof Villager villager
               && villager instanceof CommandableVillager commandableVillager) {
            VillagerCommandHandler.restoreTemporaryStay(villager, commandableVillager, state.snapshot());
         }
      }
   }

   public static void onPlayerLoggedOut(ServerPlayer player) {
      endViewing(player);
   }

   public static void onPlayerChangedDimension(ServerPlayer player) {
      endViewing(player);
   }

   public static void onServerStopping(MinecraftServer server) {
      ACTIVE.clear();
   }

   private record ViewState(UUID villagerUuid, VillagerCommandHandler.TemporaryCommandStateSnapshot snapshot, boolean temporaryStayApplied) {
   }
}
