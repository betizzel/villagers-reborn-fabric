package com.javic.slimpatch.network;

import com.javic.slimpatch.entity.CommandableVillager;
import com.javic.slimpatch.entity.FemaleVillagerEntity;
import com.javic.slimpatch.entity.MaleVillagerEntity;
import com.javic.slimpatch.entity.VillagerCommandHandler;
import com.javic.slimpatch.entity.VillagerCommandState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;

public class VillagerCommandPacket {
   private static final double MAX_COMMAND_DISTANCE_SQR = 64.0;
   private final int entityId;
   private final VillagerCommandPacket.Action action;
   private final VillagerCommandState commandState;

   public VillagerCommandPacket(int entityId, VillagerCommandState commandState) {
      this(entityId, VillagerCommandPacket.Action.COMMAND, commandState);
   }

   public VillagerCommandPacket(int entityId, VillagerCommandPacket.Action action) {
      this(entityId, action, VillagerCommandState.NONE);
   }

   private VillagerCommandPacket(int entityId, VillagerCommandPacket.Action action, VillagerCommandState commandState) {
      this.entityId = entityId;
      this.action = action;
      this.commandState = commandState;
   }

   public static void encode(VillagerCommandPacket packet, FriendlyByteBuf buf) {
      buf.writeVarInt(packet.entityId);
      buf.writeEnum(packet.action);
      buf.writeEnum(packet.commandState);
   }

   public static VillagerCommandPacket decode(FriendlyByteBuf buf) {
      return new VillagerCommandPacket(
         buf.readVarInt(),
         (VillagerCommandPacket.Action)buf.readEnum(VillagerCommandPacket.Action.class),
         (VillagerCommandState)buf.readEnum(VillagerCommandState.class)
      );
   }

   public static void handle(VillagerCommandPacket msg, ServerPlayer player) {

            if (player != null) {
               Entity entity = player.serverLevel().getEntity(msg.entityId);
               if (entity instanceof Villager villager
                  && entity instanceof CommandableVillager commandableVillager
                  && (entity instanceof MaleVillagerEntity || entity instanceof FemaleVillagerEntity)
                  && entity.isAlive()) {
                  if (!(player.distanceToSqr(villager) > 64.0)) {
                     if (VillagerCommandHandler.canUseProtectedAction(villager, commandableVillager, player)) {
                        switch (msg.action) {
                           case SET_HOME:
                              VillagerCommandHandler.setHome(villager, commandableVillager, player);
                              player.displayClientMessage(Component.translatable("slimpatch.message.home_set"), true);
                              break;
                           case CLEAR_HOME:
                              VillagerCommandHandler.clearHome(villager, commandableVillager);
                              player.displayClientMessage(Component.translatable("slimpatch.message.home_cleared"), true);
                              break;
                           case STOP_FOLLOWING:
                              VillagerCommandHandler.stopFollowing(villager, commandableVillager);
                              break;
                           case MOVE_FREELY:
                              VillagerCommandHandler.moveFreely(villager, commandableVillager);
                              break;
                           case COMMAND:
                              if (msg.commandState == VillagerCommandState.NONE) {
                                 VillagerCommandHandler.resetCommand(villager, commandableVillager);
                              } else {
                                 VillagerCommandHandler.applyCommand(villager, commandableVillager, msg.commandState, player);
                              }
                        }
                     }
                  }
               }
            }
         
   }

   public enum Action {
      COMMAND,
      SET_HOME,
      CLEAR_HOME,
      STOP_FOLLOWING,
      MOVE_FREELY;
   }
}
