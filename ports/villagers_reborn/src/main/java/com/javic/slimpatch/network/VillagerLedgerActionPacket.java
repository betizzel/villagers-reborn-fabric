package com.javic.slimpatch.network;

import com.javic.slimpatch.entity.CommandableVillager;
import com.javic.slimpatch.entity.FemaleVillagerEntity;
import com.javic.slimpatch.entity.MaleVillagerEntity;
import com.javic.slimpatch.entity.VillagerCommandHandler;
import com.javic.slimpatch.entity.VillagerCommandState;
import com.javic.slimpatch.familytree.FamilyTreeSavedData;
import com.javic.slimpatch.familytree.FamilyTreeTracker;
import com.javic.slimpatch.familytree.FamilyTreeVillagerSnapshot;
import com.javic.slimpatch.graves.GraveIdentityResolver;
import com.javic.slimpatch.graves.GraveSavedData;
import com.javic.slimpatch.graves.VillagerDeathRecord;
import com.javic.slimpatch.graves.VillagerDeathRecordState;
import com.javic.slimpatch.ledger.VillagerLedgerDataBuilder;
import com.javic.slimpatch.ledger.VillagerLedgerEntryPayload;
import com.javic.slimpatch.ledger.VillagerLedgerHiddenSavedData;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VillagerLedgerActionPacket {
   private static final Logger LOGGER = LoggerFactory.getLogger(VillagerLedgerActionPacket.class);
   private static final Map<Integer, VillagerLedgerActionPacket.PendingLedgerAction> PENDING_ACTIONS = new LinkedHashMap<>();
   private static int nextPendingActionId = 1;
   private final UUID villagerUuid;
   private final VillagerLedgerActionPacket.Action action;
   private final UUID targetVillagerUuid;
   private final VillagerLedgerActionPacket.LedgerTab tab;
   private final boolean placeAfter;

   public static void init() {
      net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.START_SERVER_TICK.register(VillagerLedgerActionPacket::onServerTickPre);
      net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(VillagerLedgerActionPacket::onServerTick);
   }

   public VillagerLedgerActionPacket(UUID villagerUuid, VillagerLedgerActionPacket.Action action) {
      this(villagerUuid, action, null, null, false);
   }

   public VillagerLedgerActionPacket(
      UUID villagerUuid, VillagerLedgerActionPacket.Action action, UUID targetVillagerUuid, VillagerLedgerActionPacket.LedgerTab tab, boolean placeAfter
   ) {
      this.villagerUuid = villagerUuid;
      this.action = action;
      this.targetVillagerUuid = targetVillagerUuid;
      this.tab = tab;
      this.placeAfter = placeAfter;
   }

   public static void encode(VillagerLedgerActionPacket packet, FriendlyByteBuf buf) {
      buf.writeUUID(packet.villagerUuid);
      buf.writeEnum(packet.action);
      buf.writeBoolean(packet.targetVillagerUuid != null);
      if (packet.targetVillagerUuid != null) {
         buf.writeUUID(packet.targetVillagerUuid);
      }

      buf.writeBoolean(packet.tab != null);
      if (packet.tab != null) {
         buf.writeEnum(packet.tab);
      }

      buf.writeBoolean(packet.placeAfter);
   }

   public static VillagerLedgerActionPacket decode(FriendlyByteBuf buf) {
      UUID villagerUuid = buf.readUUID();
      VillagerLedgerActionPacket.Action action = (VillagerLedgerActionPacket.Action)buf.readEnum(VillagerLedgerActionPacket.Action.class);
      UUID targetVillagerUuid = buf.readBoolean() ? buf.readUUID() : null;
      VillagerLedgerActionPacket.LedgerTab tab = buf.readBoolean()
         ? (VillagerLedgerActionPacket.LedgerTab)buf.readEnum(VillagerLedgerActionPacket.LedgerTab.class)
         : null;
      boolean placeAfter = buf.readBoolean();
      return new VillagerLedgerActionPacket(villagerUuid, action, targetVillagerUuid, tab, placeAfter);
   }

   public static void handle(VillagerLedgerActionPacket msg, ServerPlayer player) {

            if (player != null) {
               Villager updatedVillager = switch (msg.action) {
                  case TELEPORT_TO_PLAYER -> {
                     Villager initiallyLoadedVillager = findLoadedVillager(player, msg.villagerUuid);
                     Villager villager = resolveVillagerForControl(player, msg.villagerUuid);
                     if (villager == null) {
                        queueDeferredAction(player, msg.villagerUuid, VillagerLedgerActionPacket.Action.TELEPORT_TO_PLAYER);
                        yield null;
                     } else {
                        CommandableVillager commandableVillager = validateControlAction(player, villager, msg.villagerUuid, true);
                        Villager result = commandableVillager == null ? null : handleTeleportToPlayer(player, villager, commandableVillager);
                        if (result != null) {
                           sendTeleportNotice(player, OpenVillagerLedgerTeleportNoticePacket.NoticeAction.TELEPORT_TO_PLAYER, initiallyLoadedVillager == null);
                        }

                        yield result;
                     }
                  }
                  case SEND_HOME -> {
                     Villager initiallyLoadedVillager = findLoadedVillager(player, msg.villagerUuid);
                     Villager villager = resolveVillagerForControl(player, msg.villagerUuid);
                     if (villager == null) {
                        queueDeferredAction(player, msg.villagerUuid, VillagerLedgerActionPacket.Action.SEND_HOME);
                        yield null;
                     } else {
                        CommandableVillager commandableVillager = validateControlAction(player, villager, msg.villagerUuid, false);
                        Villager result = commandableVillager == null ? null : handleSendHome(player, villager, commandableVillager);
                        if (result != null) {
                           sendTeleportNotice(
                              player,
                              OpenVillagerLedgerTeleportNoticePacket.NoticeAction.SEND_HOME,
                              initiallyLoadedVillager == null && shouldShowSendHomeNotice(player, result)
                           );
                        }

                        yield result;
                     }
                  }
                  case HIDE_FROM_COMPANIONS -> {
                     Villager villager = resolveVillagerForCompanion(player, msg.villagerUuid);
                     CommandableVillager commandableVillager = validateCompanionAction(player, villager);
                     yield commandableVillager == null && !hasCompanionSnapshotAccess(player, msg.villagerUuid)
                        ? null
                        : handleHiddenToggle(player, villager, msg.villagerUuid, true);
                  }
                  case SHOW_IN_COMPANIONS -> {
                     Villager villager = resolveVillagerForCompanion(player, msg.villagerUuid);
                     CommandableVillager commandableVillager = validateCompanionAction(player, villager);
                     yield commandableVillager == null && !hasCompanionSnapshotAccess(player, msg.villagerUuid)
                        ? null
                        : handleHiddenToggle(player, villager, msg.villagerUuid, false);
                  }
                  case LOCATE -> handleLocate(player, msg.villagerUuid);
                  case REMOVE_FROM_LEDGER -> handleRemoveFromLedger(player, msg.villagerUuid);
                  case REORDER -> handleReorder(player, msg.villagerUuid, msg.targetVillagerUuid, msg.tab, msg.placeAfter);
               };
               if (updatedVillager != null) {
                  FamilyTreeTracker.upsertVillager(player.serverLevel().getServer(), updatedVillager);
               }

               refresh(player);
            }
         
   }

   private static CommandableVillager validateCompanionAction(ServerPlayer player, Villager villager) {
      if (villager instanceof CommandableVillager commandableVillager) {
         if (!(villager instanceof MaleVillagerEntity) && !(villager instanceof FemaleVillagerEntity)) {
            player.displayClientMessage(Component.translatable("slimpatch.message.ledger.not_available"), true);
            return null;
         } else if (!player.getUUID().equals(commandableVillager.getCommandOwnerUuid())) {
            player.displayClientMessage(Component.translatable("slimpatch.message.ledger.cannot_control"), true);
            return null;
         } else {
            VillagerCommandState commandState = commandableVillager.getCommandState();
            if (commandState != VillagerCommandState.FOLLOW && commandState != VillagerCommandState.STAY) {
               player.displayClientMessage(Component.translatable("slimpatch.message.ledger.not_available"), true);
               return null;
            } else if (!villager.isAlive()) {
               player.displayClientMessage(Component.translatable("slimpatch.message.ledger.not_available"), true);
               return null;
            } else {
               return commandableVillager;
            }
         }
      } else {
         player.displayClientMessage(Component.translatable("slimpatch.message.ledger.not_available"), true);
         return null;
      }
   }

   private static CommandableVillager validateControlAction(ServerPlayer player, Villager villager, UUID villagerUuid, boolean setFollow) {
      if (!(villager instanceof CommandableVillager commandableVillager)) {
         logUnloadedFailure(setFollow ? "teleport" : "send_home", player, villagerUuid, "resolved villager is null or not commandable");
         return null;
      } else {
         if (!(villager instanceof MaleVillagerEntity) && !(villager instanceof FemaleVillagerEntity)) {
            player.displayClientMessage(Component.translatable("slimpatch.message.ledger.not_available"), true);
            return null;
         }

         if (!villager.isAlive()) {
            player.displayClientMessage(Component.translatable("slimpatch.message.ledger.not_available"), true);
            return null;
         }

         if (player.getUUID().equals(commandableVillager.getCommandOwnerUuid())) {
            VillagerCommandState commandState = commandableVillager.getCommandState();
            if (commandState == VillagerCommandState.FOLLOW || commandState == VillagerCommandState.STAY) {
               return commandableVillager;
            }
         }

         if (!isFamilyEntryForPlayer(player, villagerUuid)) {
            player.displayClientMessage(Component.translatable("slimpatch.message.ledger.not_available"), true);
            return null;
         } else if (!VillagerCommandHandler.canUseProtectedAction(villager, commandableVillager, player)) {
            return null;
         } else {
            return setFollow || commandableVillager.getHomePos() != null && commandableVillager.getHomeDimension() != null
               ? commandableVillager
               : commandableVillager;
         }
      }
   }

   private static Villager handleTeleportToPlayer(ServerPlayer player, Villager villager, CommandableVillager commandableVillager) {
      Villager teleportedVillager = VillagerCommandHandler.teleportNearPlayer(villager, player);
      if (teleportedVillager instanceof CommandableVillager teleportedCommandableVillager) {
         VillagerCommandHandler.applyCommand(teleportedVillager, teleportedCommandableVillager, VillagerCommandState.FOLLOW, player);
         player.displayClientMessage(Component.translatable("slimpatch.message.ledger.teleported_to_player"), true);
         return teleportedVillager;
      } else {
         player.displayClientMessage(Component.translatable("slimpatch.message.ledger.no_safe_position"), true);
         return null;
      }
   }

   private static Villager handleSendHome(ServerPlayer player, Villager villager, CommandableVillager commandableVillager) {
      if (commandableVillager.hasHome() && commandableVillager.getHomePos() != null && commandableVillager.getHomeDimension() != null) {
         ServerLevel targetLevel = player.serverLevel().getServer().getLevel(commandableVillager.getHomeDimension());
         if (targetLevel != null && ensureChunkLoaded(targetLevel, commandableVillager.getHomePos())) {
            Villager teleportedVillager = VillagerCommandHandler.teleportToHome(villager, commandableVillager);
            if (teleportedVillager != null) {
               player.displayClientMessage(Component.translatable("slimpatch.message.ledger.sent_home"), true);
               return teleportedVillager;
            } else {
               player.displayClientMessage(Component.translatable("slimpatch.message.ledger.no_safe_position"), true);
               return null;
            }
         } else {
            player.displayClientMessage(Component.translatable("slimpatch.message.ledger.not_available"), true);
            return null;
         }
      } else {
         player.displayClientMessage(Component.translatable("slimpatch.message.ledger.no_home"), true);
         return null;
      }
   }

   public static void onServerTickPre(MinecraftServer server) {
      if (!PENDING_ACTIONS.isEmpty()) {
         Iterator<Entry<Integer, VillagerLedgerActionPacket.PendingLedgerAction>> iterator = PENDING_ACTIONS.entrySet().iterator();

         while (iterator.hasNext()) {
            Entry<Integer, VillagerLedgerActionPacket.PendingLedgerAction> entry = iterator.next();
            VillagerLedgerActionPacket.PendingLedgerAction pending = entry.getValue();
            if (pending.readyToExecute()) {
               ServerPlayer player = server.getPlayerList().getPlayer(pending.playerUuid());
               if (player == null) {
                  releasePendingTicket(server, pending);
                  iterator.remove();
               } else {
                  Villager villager = findLoadedVillager(player, pending.villagerUuid());
                  CommandableVillager commandableVillager = validateControlAction(
                     player, villager, pending.villagerUuid(), pending.action() == VillagerLedgerActionPacket.Action.TELEPORT_TO_PLAYER
                  );
                  if (commandableVillager == null) {
                     player.displayClientMessage(
                        Component.translatable(
                           pending.action() == VillagerLedgerActionPacket.Action.TELEPORT_TO_PLAYER
                              ? "slimpatch.message.ledger.not_available"
                              : "slimpatch.message.ledger.too_far_home"
                        ),
                        true
                     );
                     LOGGER.info(
                        "Villager Ledger pending action failed before execute: id={}, action={}, player={}, villagerUuid={}",
                        new Object[]{entry.getKey(), pending.action(), player.getGameProfile().getName(), pending.villagerUuid()}
                     );
                     releasePendingTicket(server, pending);
                     iterator.remove();
                     refresh(player);
                  } else {
                     Villager result = pending.action() == VillagerLedgerActionPacket.Action.TELEPORT_TO_PLAYER
                        ? handleTeleportToPlayer(player, villager, commandableVillager)
                        : handleSendHome(player, villager, commandableVillager);
                     if (result != null) {
                        if (pending.action() == VillagerLedgerActionPacket.Action.TELEPORT_TO_PLAYER) {
                           LOGGER.info(
                              "Villager Ledger deferred teleport completed: player={}, level={}, chunk={}, villagerEntityId={}, villagerUuid={}",
                              new Object[]{
                                 player.getGameProfile().getName(),
                                 result.level().dimension().location(),
                                 result.chunkPosition(),
                                 result.getId(),
                                 result.getUUID()
                              }
                           );
                           sendTeleportNotice(player, OpenVillagerLedgerTeleportNoticePacket.NoticeAction.TELEPORT_TO_PLAYER, true);
                           retrackTeleportedVillager(result);
                           recomputeTrackingForPlayer(player, result);
                           LOGGER.info(
                              "Villager Ledger final state: action={}, villagerUuid={}, playerDistance={}",
                              new Object[]{pending.action(), pending.villagerUuid(), Math.sqrt(result.distanceToSqr(player))}
                           );
                        } else {
                           sendTeleportNotice(player, OpenVillagerLedgerTeleportNoticePacket.NoticeAction.SEND_HOME, shouldShowSendHomeNotice(player, result));
                        }

                        FamilyTreeTracker.upsertVillager(player.serverLevel().getServer(), result);
                     }

                     refresh(player);
                     releasePendingTicket(server, pending);
                     iterator.remove();
                  }
               }
            }
         }
      }
   }

   public static void onServerTick(MinecraftServer server) {
      if (!PENDING_ACTIONS.isEmpty()) {
         Iterator<Entry<Integer, VillagerLedgerActionPacket.PendingLedgerAction>> iterator = PENDING_ACTIONS.entrySet().iterator();

         while (iterator.hasNext()) {
            Entry<Integer, VillagerLedgerActionPacket.PendingLedgerAction> entry = iterator.next();
            VillagerLedgerActionPacket.PendingLedgerAction pending = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(pending.playerUuid());
            if (player == null) {
               releasePendingTicket(server, pending);
               iterator.remove();
            } else if (!pending.readyToExecute()) {
               LOGGER.info(
                  "Villager Ledger pending action retry: id={}, action={}, player={}, villagerUuid={}, attemptsRemaining={}",
                  new Object[]{entry.getKey(), pending.action(), player.getGameProfile().getName(), pending.villagerUuid(), pending.attemptsRemaining()}
               );
               Villager villager = tryLoadVillagerFromSnapshot(
                  server,
                  pending.snapshot(),
                  pending.villagerUuid(),
                  player.getUUID(),
                  pending.action() == VillagerLedgerActionPacket.Action.TELEPORT_TO_PLAYER ? "teleport" : "send_home"
               );
               if (villager != null) {
                  LOGGER.info(
                     "Villager Ledger pending action deferred resolved: id={}, action={}, player={}, villagerUuid={}",
                     new Object[]{entry.getKey(), pending.action(), player.getGameProfile().getName(), pending.villagerUuid()}
                  );
                  entry.setValue(pending.ready());
               } else {
                  int nextAttempts = pending.attemptsRemaining() - 1;
                  if (nextAttempts <= 0) {
                     player.displayClientMessage(
                        Component.translatable(
                           pending.action() == VillagerLedgerActionPacket.Action.TELEPORT_TO_PLAYER
                              ? "slimpatch.message.ledger.not_available"
                              : "slimpatch.message.ledger.too_far_home"
                        ),
                        true
                     );
                     LOGGER.info(
                        "Villager Ledger pending action failed after timeout: id={}, action={}, player={}, villagerUuid={}",
                        new Object[]{entry.getKey(), pending.action(), player.getGameProfile().getName(), pending.villagerUuid()}
                     );
                     releasePendingTicket(server, pending);
                     iterator.remove();
                     refresh(player);
                  } else {
                     entry.setValue(pending.withAttemptsRemaining(nextAttempts));
                  }
               }
            }
         }
      }
   }

   private static Villager handleHiddenToggle(ServerPlayer player, Villager villager, UUID villagerUuid, boolean hidden) {
      if (villagerUuid == null && villager != null) {
         villagerUuid = GraveIdentityResolver.resolveHistoricalUuid(villager);
      }

      if (villagerUuid == null) {
         player.displayClientMessage(Component.translatable("slimpatch.message.ledger.not_available"), true);
         return null;
      } else {
         VillagerLedgerHiddenSavedData.get(player.serverLevel().getServer()).setHidden(player.getUUID(), villagerUuid, hidden);
         return villager;
      }
   }

   private static Villager handleRemoveFromLedger(ServerPlayer player, UUID villagerUuid) {
      if (villagerUuid == null) {
         player.displayClientMessage(Component.translatable("slimpatch.message.ledger.remove_failed"), true);
         return null;
      } else if (isFamilyEntryForPlayer(player, villagerUuid)) {
         player.displayClientMessage(Component.translatable("slimpatch.message.ledger.family_cannot_remove"), true);
         return null;
      } else {
         Villager villager = resolveVillagerForCompanion(player, villagerUuid);
         CommandableVillager commandableVillager = villager == null ? null : validateCompanionAction(player, villager);
         VillagerLedgerHiddenSavedData data = VillagerLedgerHiddenSavedData.get(player.serverLevel().getServer());
         if (commandableVillager != null) {
            VillagerCommandHandler.resetCommand(villager, commandableVillager);
            data.removeVillager(player.getUUID(), villagerUuid);
            player.displayClientMessage(Component.translatable("slimpatch.message.ledger.removed"), true);
            return villager;
         } else if (!hasCompanionSnapshotAccess(player, villagerUuid)) {
            player.displayClientMessage(Component.translatable("slimpatch.message.ledger.remove_failed"), true);
            return null;
         } else {
            data.markRemoved(player.getUUID(), villagerUuid);
            player.displayClientMessage(Component.translatable("slimpatch.message.ledger.removed"), true);
            return null;
         }
      }
   }

   private static Villager handleReorder(
      ServerPlayer player, UUID villagerUuid, UUID targetVillagerUuid, VillagerLedgerActionPacket.LedgerTab tab, boolean placeAfter
   ) {
      if (villagerUuid != null && tab != null) {
         VillagerLedgerHiddenSavedData data = VillagerLedgerHiddenSavedData.get(player.serverLevel().getServer());

         return (Villager)(switch (tab) {
            case COMPANIONS -> {
               if (!containsEntry(VillagerLedgerDataBuilder.build(player).companions(), villagerUuid)) {
                  yield null;
               } else {
                  data.ensureCompanionTracked(player.getUUID(), villagerUuid);
                  data.moveCompanion(player.getUUID(), villagerUuid, targetVillagerUuid, placeAfter);
                  yield null;
               }
            }
            case HIDDEN -> {
               if (!containsEntry(VillagerLedgerDataBuilder.build(player).hidden(), villagerUuid)) {
                  yield null;
               } else {
                  data.ensureHiddenTracked(player.getUUID(), villagerUuid);
                  data.moveHidden(player.getUUID(), villagerUuid, targetVillagerUuid, placeAfter);
                  yield null;
               }
            }
            case FAMILY -> {
               if (!containsEntry(VillagerLedgerDataBuilder.build(player).family(), villagerUuid)) {
                  yield null;
               } else {
                  data.ensureFamilyTracked(player.getUUID(), villagerUuid);
                  data.moveFamily(player.getUUID(), villagerUuid, targetVillagerUuid, placeAfter);
                  yield null;
               }
            }
         });
      } else {
         return null;
      }
   }

   private static Villager handleLocate(ServerPlayer player, UUID villagerUuid) {
      Villager villager = resolveVillagerForLocate(player, villagerUuid);
      if (villager != null && canLocateVillager(player, villager, villagerUuid)) {
         int x = villager.blockPosition().getX();
         int y = villager.blockPosition().getY();
         int z = villager.blockPosition().getZ();
         int distance = Mth.floor(player.position().distanceTo(villager.position()) + 0.5);
         MutableComponent coordinates = createCoordinatesComponent(villager.level().dimension().location().toString(), x, y, z);
         player.sendSystemMessage(Component.translatable("slimpatch.message.ledger.located", new Object[]{villager.getDisplayName(), coordinates, distance}));
         return villager;
      }

      VillagerLedgerHiddenSavedData.LedgerVillagerSnapshot snapshot = VillagerLedgerHiddenSavedData.get(player.serverLevel().getServer())
         .getCompanionSnapshot(player.getUUID(), villagerUuid);
      if (snapshot != null && !snapshot.getLastKnownDimension().isEmpty()) {
         int distance = horizontalDistance(player, snapshot.getLastKnownX(), snapshot.getLastKnownY(), snapshot.getLastKnownZ());
         MutableComponent coordinates = createCoordinatesComponent(
            snapshot.getLastKnownDimension(), snapshot.getLastKnownX(), snapshot.getLastKnownY(), snapshot.getLastKnownZ()
         );
         player.sendSystemMessage(
            Component.translatable(
               "slimpatch.message.ledger.last_seen",
               new Object[]{Component.literal(snapshot.getDisplayName().isEmpty() ? "Villager" : snapshot.getDisplayName()), coordinates, distance}
            )
         );
         return null;
      }

      VillagerDeathRecord deathRecord = GraveSavedData.get(player.serverLevel().getServer()).getRecordByHistoricalEntityUuid(villagerUuid);
      if (deathRecord != null) {
         String dimension = deathRecord.isGravePlaced() ? deathRecord.getGraveDimension() : deathRecord.getLastKnownDimension();
         int x = deathRecord.isGravePlaced() ? deathRecord.getGraveX() : deathRecord.getLastKnownX();
         int y = deathRecord.isGravePlaced() ? deathRecord.getGraveY() : deathRecord.getLastKnownY();
         int z = deathRecord.isGravePlaced() ? deathRecord.getGraveZ() : deathRecord.getLastKnownZ();
         if (!dimension.isEmpty()) {
            int distance = horizontalDistance(player, x, y, z);
            MutableComponent coordinates = createCoordinatesComponent(dimension, x, y, z);
            player.sendSystemMessage(
               Component.translatable(
                  deathRecord.isGravePlaced() ? "slimpatch.message.ledger.grave_located" : "slimpatch.message.ledger.last_seen",
                  new Object[]{Component.literal(deathRecord.getDisplayName().isEmpty() ? "Villager" : deathRecord.getDisplayName()), coordinates, distance}
               )
            );
            return null;
         }
      }

      player.sendSystemMessage(Component.translatable("slimpatch.message.ledger.locate_failed"));
      return null;
   }

   private static boolean canLocateVillager(ServerPlayer player, Villager villager, UUID villagerUuid) {
      if (villager instanceof CommandableVillager commandableVillager && player.getUUID().equals(commandableVillager.getCommandOwnerUuid())) {
         VillagerCommandState commandState = commandableVillager.getCommandState();
         if (commandState == VillagerCommandState.FOLLOW || commandState == VillagerCommandState.STAY) {
            return true;
         }
      }

      return isFamilyEntryForPlayer(player, villagerUuid);
   }

   private static boolean isFamilyEntryForPlayer(ServerPlayer player, UUID villagerUuid) {
      if (villagerUuid == null) {
         return false;
      }

      for (VillagerLedgerEntryPayload entry : VillagerLedgerDataBuilder.build(player).family()) {
         if (villagerUuid.equals(entry.getNode().getUuid())) {
            return true;
         }
      }

      return false;
   }

   private static boolean containsEntry(List<VillagerLedgerEntryPayload> entries, UUID villagerUuid) {
      for (VillagerLedgerEntryPayload entry : entries) {
         if (villagerUuid.equals(entry.getNode().getUuid())) {
            return true;
         }
      }

      return false;
   }

   private static boolean hasCompanionSnapshotAccess(ServerPlayer player, UUID villagerUuid) {
      return VillagerLedgerHiddenSavedData.get(player.serverLevel().getServer()).isTrackedCompanion(player.getUUID(), villagerUuid);
   }

   private static Villager resolveVillagerForCompanion(ServerPlayer player, UUID villagerUuid) {
      Villager villager = findLoadedVillager(player, villagerUuid);
      if (villager != null) {
         return villager;
      } else {
         return !hasCompanionSnapshotAccess(player, villagerUuid)
            ? null
            : tryLoadVillagerFromSnapshot(player.serverLevel().getServer(), resolveSnapshot(player, villagerUuid), villagerUuid, player.getUUID(), "companion");
      }
   }

   private static Villager resolveVillagerForControl(ServerPlayer player, UUID villagerUuid) {
      Villager villager = resolveVillagerForCompanion(player, villagerUuid);
      if (villager != null) {
         return villager;
      } else {
         return !isFamilyEntryForPlayer(player, villagerUuid)
            ? null
            : tryLoadVillagerFromSnapshot(player.serverLevel().getServer(), resolveSnapshot(player, villagerUuid), villagerUuid, player.getUUID(), "control");
      }
   }

   private static Villager resolveVillagerForLocate(ServerPlayer player, UUID villagerUuid) {
      Villager villager = findLoadedVillager(player, villagerUuid);
      return villager != null
         ? villager
         : tryLoadVillagerFromSnapshot(player.serverLevel().getServer(), resolveSnapshot(player, villagerUuid), villagerUuid, player.getUUID(), "locate");
   }

   private static Villager tryLoadVillagerFromSnapshot(
      MinecraftServer server, VillagerLedgerHiddenSavedData.LedgerVillagerSnapshot companionSnapshot, UUID villagerUuid, UUID playerUuid, String context
   ) {
      ServerLevel targetLevel = null;
      BlockPos lastKnownPos = null;
      UUID requestedVillagerUuid = villagerUuid;
      if (companionSnapshot != null && !companionSnapshot.getLastKnownDimension().isEmpty()) {
         ResourceLocation dimensionId = ResourceLocation.tryParse(companionSnapshot.getLastKnownDimension());
         if (dimensionId != null) {
            targetLevel = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
            lastKnownPos = new BlockPos(companionSnapshot.getLastKnownX(), companionSnapshot.getLastKnownY(), companionSnapshot.getLastKnownZ());
         }
      }

      LOGGER.info(
         "Villager Ledger {} unloaded resolve start: player={}, villagerUuid={}, snapshotDimension={}, snapshotPos={}",
         new Object[]{
            context,
            playerUuid,
            requestedVillagerUuid,
            companionSnapshot == null ? "null" : companionSnapshot.getLastKnownDimension(),
            companionSnapshot == null
               ? "null"
               : companionSnapshot.getLastKnownX() + "," + companionSnapshot.getLastKnownY() + "," + companionSnapshot.getLastKnownZ()
         }
      );
      if (targetLevel == null) {
         VillagerDeathRecord deathRecord = GraveSavedData.get(server).getRecordByHistoricalEntityUuid(villagerUuid);
         if (deathRecord != null && deathRecord.getState() == VillagerDeathRecordState.RESTORED && deathRecord.getRestoredLivingUuid() != null) {
            villagerUuid = deathRecord.getHistoricalEntityUuid();
         }

         FamilyTreeSavedData familyData = FamilyTreeSavedData.get(server);
         FamilyTreeVillagerSnapshot familySnapshot = familyData.getVillager(villagerUuid);
         if (familySnapshot != null && !familySnapshot.getLastKnownDimension().isEmpty()) {
            ResourceLocation dimensionId = ResourceLocation.tryParse(familySnapshot.getLastKnownDimension());
            if (dimensionId != null) {
               targetLevel = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
               lastKnownPos = new BlockPos(familySnapshot.getLastKnownX(), familySnapshot.getLastKnownY(), familySnapshot.getLastKnownZ());
            }
         }
      }

      if (targetLevel != null && lastKnownPos != null) {
         LOGGER.info(
            "Villager Ledger {} unloaded resolve target: player={}, villagerUuid={}, dimension={}, chunk={}, blockPos={}",
            new Object[]{context, playerUuid, requestedVillagerUuid, targetLevel.dimension().location(), new ChunkPos(lastKnownPos), lastKnownPos}
         );
         if (!(
            targetLevel.getEntity(villagerUuid) instanceof Villager directVillager
               && (directVillager instanceof MaleVillagerEntity || directVillager instanceof FemaleVillagerEntity)
         )) {
            LOGGER.info(
               "Villager Ledger {} unloaded resolve direct lookup miss before chunk load: player={}, villagerUuid={}",
               new Object[]{context, playerUuid, requestedVillagerUuid}
            );
            ChunkPos originChunk = new ChunkPos(lastKnownPos);

            for (int chunkRadius = 0; chunkRadius <= 1; chunkRadius++) {
               for (int chunkX = originChunk.x - chunkRadius; chunkX <= originChunk.x + chunkRadius; chunkX++) {
                  for (int chunkZ = originChunk.z - chunkRadius; chunkZ <= originChunk.z + chunkRadius; chunkZ++) {
                     ChunkAccess chunkAccess = targetLevel.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);
                     LOGGER.info(
                        "Villager Ledger {} unloaded resolve chunk load: player={}, villagerUuid={}, chunk={}, loaded={}",
                        new Object[]{context, playerUuid, requestedVillagerUuid, new ChunkPos(chunkX, chunkZ), chunkAccess != null}
                     );
                     if (targetLevel.getEntity(villagerUuid) instanceof Villager loadedVillager
                        && (loadedVillager instanceof MaleVillagerEntity || loadedVillager instanceof FemaleVillagerEntity)) {
                        LOGGER.info(
                           "Villager Ledger {} unloaded resolve success after chunk load: player={}, villagerUuid={}, entityUuid={}, foundChunk={}",
                           new Object[]{context, playerUuid, requestedVillagerUuid, loadedVillager.getUUID(), new ChunkPos(chunkX, chunkZ)}
                        );
                        syncResolvedSnapshot(server, playerUuid, villagerUuid, loadedVillager);
                        return loadedVillager;
                     }

                     AABB searchBox = new AABB(
                        chunkX << 4, targetLevel.getMinBuildHeight(), chunkZ << 4, (chunkX << 4) + 16, targetLevel.getMaxBuildHeight(), (chunkZ << 4) + 16
                     );
                     List<Villager> villagers = targetLevel.getEntitiesOfClass(
                        Villager.class, searchBox, candidatex -> candidatex instanceof MaleVillagerEntity || candidatex instanceof FemaleVillagerEntity
                     );
                     if (!villagers.isEmpty()) {
                        LOGGER.info(
                           "Villager Ledger {} unloaded resolve nearby search: player={}, villagerUuid={}, chunk={}, candidates={}",
                           new Object[]{context, playerUuid, requestedVillagerUuid, new ChunkPos(chunkX, chunkZ), villagers.size()}
                        );
                     }

                     for (Villager candidate : villagers) {
                        if (villagerUuid.equals(GraveIdentityResolver.resolveHistoricalUuid(candidate))) {
                           LOGGER.info(
                              "Villager Ledger {} unloaded resolve success in nearby search: player={}, villagerUuid={}, entityUuid={}, foundChunk={}",
                              new Object[]{context, playerUuid, requestedVillagerUuid, candidate.getUUID(), new ChunkPos(chunkX, chunkZ)}
                           );
                           syncResolvedSnapshot(server, playerUuid, villagerUuid, candidate);
                           return candidate;
                        }
                     }
                  }
               }
            }

            LOGGER.info(
               "Villager Ledger {} unloaded resolve failed: player={}, villagerUuid={}, reason=entity not found after limited chunk search",
               new Object[]{context, playerUuid, requestedVillagerUuid}
            );
            return null;
         } else {
            LOGGER.info(
               "Villager Ledger {} unloaded resolve success before chunk load: player={}, villagerUuid={}, entityUuid={}",
               new Object[]{context, playerUuid, requestedVillagerUuid, directVillager.getUUID()}
            );
            return directVillager;
         }
      } else {
         LOGGER.info(
            "Villager Ledger {} unloaded resolve failed: player={}, villagerUuid={}, reason=no dimension or position",
            new Object[]{context, playerUuid, requestedVillagerUuid}
         );
         return null;
      }
   }

   private static VillagerLedgerHiddenSavedData.LedgerVillagerSnapshot resolveSnapshot(ServerPlayer player, UUID villagerUuid) {
      VillagerLedgerHiddenSavedData data = VillagerLedgerHiddenSavedData.get(player.serverLevel().getServer());
      VillagerLedgerHiddenSavedData.LedgerVillagerSnapshot snapshot = data.getCompanionSnapshot(player.getUUID(), villagerUuid);
      if (snapshot != null) {
         return snapshot;
      }

      FamilyTreeSavedData familyData = FamilyTreeSavedData.get(player.serverLevel().getServer());
      FamilyTreeVillagerSnapshot familySnapshot = familyData.getVillager(villagerUuid);
      if (familySnapshot == null) {
         return null;
      }

      data.upsertCompanionSnapshot(player.getUUID(), villagerUuid, familySnapshot, VillagerCommandState.NONE);
      return data.getCompanionSnapshot(player.getUUID(), villagerUuid);
   }

   private static void syncResolvedSnapshot(MinecraftServer server, UUID playerUuid, UUID villagerUuid, Villager villager) {
      if (server != null && playerUuid != null && villagerUuid != null && villager != null && villager instanceof CommandableVillager commandableVillager) {
         VillagerLedgerHiddenSavedData.get(server).upsertCompanionSnapshot(playerUuid, villagerUuid, villager, commandableVillager);
      }
   }

   private static void logUnloadedFailure(String action, ServerPlayer player, UUID villagerUuid, String reason) {
      LOGGER.info(
         "Villager Ledger {} failed: player={}({}), villagerUuid={}, reason={}",
         new Object[]{action, player.getGameProfile().getName(), player.getUUID(), villagerUuid, reason}
      );
   }

   private static void queueDeferredAction(ServerPlayer player, UUID villagerUuid, VillagerLedgerActionPacket.Action action) {
      VillagerLedgerHiddenSavedData.LedgerVillagerSnapshot snapshot = resolveSnapshot(player, villagerUuid);
      if (snapshot == null || snapshot.getLastKnownDimension().isEmpty()) {
         player.displayClientMessage(
            Component.translatable(
               action == VillagerLedgerActionPacket.Action.TELEPORT_TO_PLAYER
                  ? "slimpatch.message.ledger.not_available"
                  : "slimpatch.message.ledger.too_far_home"
            ),
            true
         );
         logUnloadedFailure(
            action == VillagerLedgerActionPacket.Action.TELEPORT_TO_PLAYER ? "teleport" : "send_home",
            player,
            villagerUuid,
            "no snapshot or dimension for deferred resolve"
         );
      } else if (action == VillagerLedgerActionPacket.Action.SEND_HOME && snapshot.getHomePos() == null) {
         player.displayClientMessage(Component.translatable("slimpatch.message.ledger.no_home"), true);
      } else {
         ResourceLocation dimensionId = ResourceLocation.tryParse(snapshot.getLastKnownDimension());
         if (dimensionId == null) {
            player.displayClientMessage(
               Component.translatable(
                  action == VillagerLedgerActionPacket.Action.TELEPORT_TO_PLAYER
                     ? "slimpatch.message.ledger.not_available"
                     : "slimpatch.message.ledger.too_far_home"
               ),
               true
            );
            logUnloadedFailure(
               action == VillagerLedgerActionPacket.Action.TELEPORT_TO_PLAYER ? "teleport" : "send_home",
               player,
               villagerUuid,
               "invalid snapshot dimension for deferred resolve"
            );
         } else {
            ServerLevel level = player.serverLevel().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
            if (level == null) {
               player.displayClientMessage(
                  Component.translatable(
                     action == VillagerLedgerActionPacket.Action.TELEPORT_TO_PLAYER
                        ? "slimpatch.message.ledger.not_available"
                        : "slimpatch.message.ledger.too_far_home"
                  ),
                  true
               );
               logUnloadedFailure(
                  action == VillagerLedgerActionPacket.Action.TELEPORT_TO_PLAYER ? "teleport" : "send_home",
                  player,
                  villagerUuid,
                  "snapshot dimension does not exist for deferred resolve"
               );
            } else {
               int pendingId = nextPendingActionId++;
               ChunkPos chunkPos = new ChunkPos(new BlockPos(snapshot.getLastKnownX(), snapshot.getLastKnownY(), snapshot.getLastKnownZ()));
               level.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, chunkPos, 1, pendingId);
               PENDING_ACTIONS.put(
                  pendingId, new VillagerLedgerActionPacket.PendingLedgerAction(player.getUUID(), villagerUuid, action, snapshot, 6, pendingId)
               );
               LOGGER.info(
                  "Villager Ledger pending action queued: id={}, action={}, player={}, villagerUuid={}, dimension={}, chunk={}",
                  new Object[]{pendingId, action, player.getGameProfile().getName(), villagerUuid, snapshot.getLastKnownDimension(), chunkPos}
               );
            }
         }
      }
   }

   private static void releasePendingTicket(MinecraftServer server, VillagerLedgerActionPacket.PendingLedgerAction pending) {
      ResourceLocation dimensionId = ResourceLocation.tryParse(pending.snapshot().getLastKnownDimension());
      if (dimensionId != null) {
         ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
         if (level != null) {
            ChunkPos chunkPos = new ChunkPos(
               new BlockPos(pending.snapshot().getLastKnownX(), pending.snapshot().getLastKnownY(), pending.snapshot().getLastKnownZ())
            );
            level.getChunkSource().removeRegionTicket(TicketType.POST_TELEPORT, chunkPos, 1, pending.ticketId());
         }
      }
   }

   private static void recomputeTrackingForPlayer(ServerPlayer player, Villager villager) {
      if (!player.isRemoved()) {
         player.serverLevel().getChunkSource().move(player);
         LOGGER.info(
            "Villager Ledger tracking recomputation executed: player={}, level={}, chunk={}, villagerEntityId={}, villagerUuid={}",
            new Object[]{
               player.getGameProfile().getName(), villager.level().dimension().location(), villager.chunkPosition(), villager.getId(), villager.getUUID()
            }
         );
      }
   }

   private static void retrackTeleportedVillager(Villager villager) {
      ServerLevel level = (ServerLevel)villager.level();
      level.getChunkSource().removeEntity(villager);
      level.getChunkSource().addEntity(villager);
      LOGGER.info(
         "Villager Ledger tracking refresh executed: level={}, chunk={}, villagerEntityId={}, villagerUuid={}",
         new Object[]{level.dimension().location(), villager.chunkPosition(), villager.getId(), villager.getUUID()}
      );
   }

   private static boolean shouldShowSendHomeNotice(ServerPlayer player, Villager villager) {
      return villager.level() instanceof ServerLevel level
         ? level.getChunkSource().chunkMap.getPlayers(villager.chunkPosition(), false).contains(player)
         : false;
   }

   private static void sendTeleportNotice(ServerPlayer player, OpenVillagerLedgerTeleportNoticePacket.NoticeAction action, boolean risky) {
      ModNetworking.sendToClient(new OpenVillagerLedgerTeleportNoticePacket(action, risky), player);
   }

   private static boolean ensureChunkLoaded(ServerLevel level, BlockPos pos) {
      ChunkPos chunkPos = new ChunkPos(pos);
      return level.getChunkSource().getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, true) != null;
   }

   private static MutableComponent createCoordinatesComponent(String dimension, int x, int y, int z) {
      return Component.literal("[" + x + ", " + y + ", " + z + "]")
         .withStyle(
            style -> style.withColor(ChatFormatting.GREEN)
               .withClickEvent(
                  new ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/execute in " + dimension + " run tp @s " + x + " " + y + " " + z)
               )
               .withHoverEvent(
                  new HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Component.translatable("slimpatch.message.ledger.click_to_teleport"))
               )
         );
   }

   private static int horizontalDistance(ServerPlayer player, int x, int y, int z) {
      return Mth.floor(player.position().distanceTo(new Vec3(x + 0.5, y, z + 0.5)) + 0.5);
   }

   private static Villager findLoadedVillager(ServerPlayer player, UUID villagerUuid) {
      for (ServerLevel level : player.serverLevel().getServer().getAllLevels()) {
         for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Villager villager && (villager instanceof MaleVillagerEntity || villager instanceof FemaleVillagerEntity)) {
               UUID effectiveUuid = GraveIdentityResolver.resolveHistoricalUuid(villager);
               if (villagerUuid.equals(effectiveUuid)) {
                  return villager;
               }
            }
         }
      }

      return null;
   }

   private static void refresh(ServerPlayer player) {
      ModNetworking.sendToClient(new VillagerLedgerDataPacket(VillagerLedgerDataBuilder.build(player), false), player);
   }

   public enum Action {
      TELEPORT_TO_PLAYER,
      SEND_HOME,
      HIDE_FROM_COMPANIONS,
      SHOW_IN_COMPANIONS,
      LOCATE,
      REMOVE_FROM_LEDGER,
      REORDER;
   }

   public enum LedgerTab {
      COMPANIONS,
      FAMILY,
      HIDDEN;
   }

   private record PendingLedgerAction(
      UUID playerUuid,
      UUID villagerUuid,
      VillagerLedgerActionPacket.Action action,
      VillagerLedgerHiddenSavedData.LedgerVillagerSnapshot snapshot,
      int attemptsRemaining,
      int ticketId
   ) {
      private boolean readyToExecute() {
         return this.attemptsRemaining < 0;
      }

      private VillagerLedgerActionPacket.PendingLedgerAction ready() {
         return new VillagerLedgerActionPacket.PendingLedgerAction(this.playerUuid, this.villagerUuid, this.action, this.snapshot, -1, this.ticketId);
      }

      private VillagerLedgerActionPacket.PendingLedgerAction withAttemptsRemaining(int attemptsRemaining) {
         return new VillagerLedgerActionPacket.PendingLedgerAction(
            this.playerUuid, this.villagerUuid, this.action, this.snapshot, attemptsRemaining, this.ticketId
         );
      }
   }
}
