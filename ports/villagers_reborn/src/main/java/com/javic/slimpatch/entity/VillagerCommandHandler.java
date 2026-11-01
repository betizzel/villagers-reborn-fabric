package com.javic.slimpatch.entity;

import com.javic.slimpatch.graves.GraveIdentityResolver;
import com.javic.slimpatch.ledger.VillagerLedgerHiddenSavedData;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VillagerCommandHandler {
   private static final Logger LOGGER = LoggerFactory.getLogger(VillagerCommandHandler.class);
   private static final String COMMAND_TAG = "SlimPatchCommand";
   private static final String TARGET_UUID_TAG = "SlimPatchCommandTarget";
   private static final String STAY_POS_TAG = "SlimPatchStayPos";
   private static final String HOME_POS_TAG = "SlimPatchHomePos";
   private static final String HOME_DIMENSION_TAG = "SlimPatchHomeDimension";
   private static final String OWNER_UUID_TAG = "SlimPatchCommandOwnerUuid";
   private static final String OWNER_NAME_TAG = "SlimPatchCommandOwnerName";
   private static final double STAY_MAX_DRIFT_SQR = 0.25;
   private static final double FOLLOW_TELEPORT_DISTANCE_SQR = 2304.0;
   private static final long FOLLOW_TELEPORT_COOLDOWN_TICKS = 100L;
   private static final double HOME_RADIUS = 32.0;
   private static final double HOME_RADIUS_SQR = 1024.0;
   private static final double HOME_RETURN_SPEED = 0.6;
   private static final int HOME_REPATH_TICKS = 20;

   private VillagerCommandHandler() {
   }

   public static void applyCommand(Villager villager, CommandableVillager commandableVillager, VillagerCommandState commandState, ServerPlayer player) {
      assignOwner(commandableVillager, player);
      commandableVillager.setCommandState(commandState);
      if (commandState == VillagerCommandState.FOLLOW) {
         commandableVillager.setCommandTargetUuid(player.getUUID());
         commandableVillager.setStayPos(null);
         commandableVillager.setStayAnchorPos(null);
      } else {
         if (commandState != VillagerCommandState.STAY) {
            resetCommand(villager, commandableVillager);
            return;
         }

         commandableVillager.setCommandTargetUuid(null);
         commandableVillager.setStayPos(villager.blockPosition());
         commandableVillager.setStayAnchorPos(villager.position());
      }

      commandableVillager.setLastCommandTeleportTick(0L);
      updateNavigationForCommand(villager, commandableVillager.getCommandState());
      villager.getNavigation().stop();
      syncLedgerSnapshot(villager, commandableVillager, player.getUUID(), false);
   }

   public static void resetCommand(Villager villager, CommandableVillager commandableVillager) {
      UUID ownerUuid = commandableVillager.getCommandOwnerUuid();
      UUID villagerUuid = resolveLedgerVillagerUuid(villager);
      commandableVillager.setCommandState(VillagerCommandState.NONE);
      commandableVillager.setCommandTargetUuid(null);
      commandableVillager.setStayPos(null);
      commandableVillager.setStayAnchorPos(null);
      commandableVillager.setLastCommandTeleportTick(0L);
      updateNavigationForCommand(villager, VillagerCommandState.NONE);
      villager.getNavigation().stop();
      releaseOwnerIfPossible(villager, commandableVillager);
      removeLedgerSnapshot(villager, ownerUuid, villagerUuid);
   }

   public static void setHome(Villager villager, CommandableVillager commandableVillager, ServerPlayer player) {
      assignOwner(commandableVillager, player);
      commandableVillager.setHome(villager.blockPosition(), villager.level().dimension());
      syncLedgerSnapshot(villager, commandableVillager, player.getUUID(), false);
   }

   public static void clearHome(Villager villager, CommandableVillager commandableVillager) {
      UUID ownerUuid = commandableVillager.getCommandOwnerUuid();
      commandableVillager.clearHome();
      releaseOwnerIfPossible(villager, commandableVillager);
      syncLedgerSnapshot(villager, commandableVillager, ownerUuid, false);
   }

   public static void stopFollowing(Villager villager, CommandableVillager commandableVillager) {
      if (commandableVillager.getCommandState() == VillagerCommandState.FOLLOW) {
         UUID ownerUuid = commandableVillager.getCommandOwnerUuid();
         UUID villagerUuid = resolveLedgerVillagerUuid(villager);
         commandableVillager.setCommandState(VillagerCommandState.NONE);
         commandableVillager.setCommandTargetUuid(null);
         commandableVillager.setLastCommandTeleportTick(0L);
         updateNavigationForCommand(villager, VillagerCommandState.NONE);
         villager.getNavigation().stop();
         releaseOwnerIfPossible(villager, commandableVillager);
         removeLedgerSnapshot(villager, ownerUuid, villagerUuid);
      }
   }

   public static void moveFreely(Villager villager, CommandableVillager commandableVillager) {
      if (commandableVillager.getCommandState() == VillagerCommandState.STAY) {
         UUID ownerUuid = commandableVillager.getCommandOwnerUuid();
         UUID villagerUuid = resolveLedgerVillagerUuid(villager);
         commandableVillager.setCommandState(VillagerCommandState.NONE);
         commandableVillager.setStayPos(null);
         commandableVillager.setStayAnchorPos(null);
         commandableVillager.setLastCommandTeleportTick(0L);
         updateNavigationForCommand(villager, VillagerCommandState.NONE);
         villager.getNavigation().stop();
         releaseOwnerIfPossible(villager, commandableVillager);
         removeLedgerSnapshot(villager, ownerUuid, villagerUuid);
      }
   }

   public static boolean canUseProtectedAction(Villager villager, CommandableVillager commandableVillager, ServerPlayer player) {
      if (villager instanceof FamilyVillager familyVillager
         && familyVillager.getRelationshipStage() == VillagerRelationshipStage.MARRIED
         && familyVillager.getSpousePlayerUuid() != null) {
         if (familyVillager.getSpousePlayerUuid().equals(player.getUUID())) {
            if (!familyVillager.getSpousePlayerName().equals(player.getGameProfile().getName())) {
               familyVillager.setSpousePlayerName(player.getGameProfile().getName());
            }

            return true;
         } else {
            String spouseName = familyVillager.getSpousePlayerName();
            if (spouseName != null && !spouseName.isEmpty()) {
               player.displayClientMessage(Component.translatable("slimpatch.message.villager_married_to", new Object[]{spouseName}), true);
            } else {
               player.displayClientMessage(Component.translatable("slimpatch.message.villager_married"), true);
            }

            return false;
         }
      } else {
         if (!usesMultiplayerOwnership(villager)) {
            return true;
         }

         UUID ownerUuid = commandableVillager.getCommandOwnerUuid();
         if (ownerUuid == null) {
            assignOwner(commandableVillager, player);
            return true;
         }

         if (ownerUuid.equals(player.getUUID())) {
            commandableVillager.setCommandOwnerName(player.getGameProfile().getName());
            return true;
         }

         String ownerName = commandableVillager.getCommandOwnerName();
         if (ownerName == null || ownerName.isEmpty()) {
            ownerName = "another player";
         }

         player.displayClientMessage(Component.translatable("slimpatch.message.villager_controlled", new Object[]{ownerName}), true);
         return false;
      }
   }

   public static void tick(Villager villager, CommandableVillager commandableVillager) {
      VillagerCommandState commandState = commandableVillager.getCommandState();
      if (commandState == VillagerCommandState.NONE) {
         updateNavigationForCommand(villager, VillagerCommandState.NONE);
      } else {
         updateNavigationForCommand(villager, commandState);
         villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
         villager.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
         if (villager.isSleeping()) {
            villager.stopSleeping();
         }

         if (commandState == VillagerCommandState.STAY) {
            villager.getNavigation().stop();
            Vec3 stayAnchorPos = commandableVillager.getStayAnchorPos();
            if (stayAnchorPos == null) {
               BlockPos stayPos = commandableVillager.getStayPos();
               if (stayPos != null) {
                  commandableVillager.setStayAnchorPos(resolveAnchorPos(villager, stayPos));
                  stayAnchorPos = commandableVillager.getStayAnchorPos();
               }
            }

            if (stayAnchorPos == null) {
               commandableVillager.setStayPos(villager.blockPosition());
               commandableVillager.setStayAnchorPos(villager.position());
            } else {
               if (villager.position().distanceToSqr(stayAnchorPos) > 0.25) {
                  villager.teleportTo(stayAnchorPos.x, stayAnchorPos.y, stayAnchorPos.z);
               }

               villager.setDeltaMovement(Vec3.ZERO);
            }
         } else if (commandState == VillagerCommandState.FOLLOW) {
            if (commandableVillager.getCommandTargetUuid() != null) {
               if (villager.level().getPlayerByUUID(commandableVillager.getCommandTargetUuid()) instanceof ServerPlayer player) {
                  if (player.isAlive() && player.level() == villager.level()) {
                     if (!(villager.distanceToSqr(player) < 2304.0)) {
                        long gameTime = villager.level().getGameTime();
                        if (gameTime - commandableVillager.getLastCommandTeleportTick() >= 100L) {
                           if (tryTeleportNearPlayer(villager, player)) {
                              commandableVillager.setLastCommandTeleportTick(gameTime);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static void holdForDialogue(Villager villager, Vec3 holdPos) {
      villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      villager.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
      villager.getNavigation().stop();
      if (villager.isSleeping()) {
         villager.stopSleeping();
      }

      if (holdPos != null && villager.position().distanceToSqr(holdPos) > 0.25) {
         villager.teleportTo(holdPos.x, holdPos.y, holdPos.z);
      }

      villager.setDeltaMovement(Vec3.ZERO);
   }

   public static VillagerCommandHandler.TemporaryCommandStateSnapshot createTemporaryStaySnapshot(CommandableVillager commandableVillager) {
      return new VillagerCommandHandler.TemporaryCommandStateSnapshot(
         commandableVillager.getCommandState(),
         commandableVillager.getCommandTargetUuid(),
         commandableVillager.getStayPos(),
         commandableVillager.getStayAnchorPos(),
         commandableVillager.getLastCommandTeleportTick()
      );
   }

   public static boolean beginTemporaryStay(Villager villager, CommandableVillager commandableVillager) {
      if (commandableVillager.getCommandState() == VillagerCommandState.STAY) {
         return false;
      }

      commandableVillager.setCommandState(VillagerCommandState.STAY);
      commandableVillager.setCommandTargetUuid(null);
      commandableVillager.setStayPos(villager.blockPosition());
      commandableVillager.setStayAnchorPos(villager.position());
      commandableVillager.setLastCommandTeleportTick(0L);
      updateNavigationForCommand(villager, VillagerCommandState.STAY);
      holdForDialogue(villager, commandableVillager.getStayAnchorPos());
      return true;
   }

   public static void restoreTemporaryStay(
      Villager villager, CommandableVillager commandableVillager, VillagerCommandHandler.TemporaryCommandStateSnapshot snapshot
   ) {
      if (snapshot != null) {
         commandableVillager.setCommandState(snapshot.commandState());
         commandableVillager.setCommandTargetUuid(snapshot.commandTargetUuid());
         commandableVillager.setStayPos(snapshot.stayPos());
         commandableVillager.setStayAnchorPos(snapshot.stayAnchorPos());
         commandableVillager.setLastCommandTeleportTick(snapshot.lastCommandTeleportTick());
         updateNavigationForCommand(villager, snapshot.commandState());
         villager.getNavigation().stop();
      }
   }

   public static void suppressBedBehaviorForHomeAtNight(Villager villager, CommandableVillager commandableVillager) {
      if (commandableVillager.hasHome()) {
         if (commandableVillager.getHomePos() != null && commandableVillager.getHomeDimension() != null) {
            if (villager.level().dimension() == commandableVillager.getHomeDimension()) {
               if (isNightOrDusk(villager)) {
                  villager.getBrain().eraseMemory(MemoryModuleType.HOME);
                  if (villager.isSleeping()) {
                     villager.stopSleeping();
                  }
               }
            }
         }
      }
   }

   public static void enforceHomeRadius(Villager villager, CommandableVillager commandableVillager) {
      if (!villager.level().isClientSide && villager.isAlive()) {
         if (commandableVillager.getCommandState() == VillagerCommandState.NONE) {
            if (commandableVillager.hasHome()) {
               BlockPos homePos = commandableVillager.getHomePos();
               ResourceKey<Level> homeDimension = commandableVillager.getHomeDimension();
               if (homePos != null && homeDimension != null && villager.level().dimension() == homeDimension) {
                  Vec3 homeCenter = Vec3.atBottomCenterOf(homePos);
                  Optional<GlobalPos> jobSite = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
                  boolean hasOutOfRadiusJobSite = jobSite.<Boolean>map(
                        pos -> pos.dimension() != homeDimension || !pos.pos().closerToCenterThan(homeCenter, 32.5)
                     )
                     .orElse(false);
                  if (hasOutOfRadiusJobSite && canClearJobSite(villager)) {
                     villager.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
                     villager.getNavigation().stop();
                     jobSite = Optional.empty();
                     hasOutOfRadiusJobSite = false;
                  }

                  Optional<WalkTarget> walkTarget = villager.getBrain().getMemory(MemoryModuleType.WALK_TARGET);
                  boolean hasOutOfRadiusWalkTarget = walkTarget.<Boolean>map(target -> target.getTarget().currentPosition().distanceToSqr(homeCenter) > 1024.0)
                     .orElse(false);
                  BlockPos navigationTargetPos = villager.getNavigation().getTargetPos();
                  boolean hasActiveNavigation = villager.getNavigation().isInProgress() && navigationTargetPos != null;
                  boolean hasHomewardNavigationTarget = hasActiveNavigation && navigationTargetPos.closerToCenterThan(homeCenter, 32.5);
                  boolean hasOutOfRadiusNavigationTarget = hasActiveNavigation && !hasHomewardNavigationTarget;
                  if (villager.position().distanceToSqr(homeCenter) <= 1024.0) {
                     if (hasOutOfRadiusWalkTarget) {
                        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                     }

                     if (hasOutOfRadiusNavigationTarget) {
                        villager.getNavigation().stop();
                     }
                  } else {
                     if (hasOutOfRadiusWalkTarget) {
                        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                     }

                     if (hasOutOfRadiusNavigationTarget) {
                        villager.getNavigation().stop();
                     }

                     boolean shouldRepath = hasOutOfRadiusWalkTarget
                        || hasOutOfRadiusNavigationTarget
                        || !hasHomewardNavigationTarget
                        || villager.getNavigation().isDone()
                        || villager.tickCount % 20 == 0 && !hasActiveNavigation;
                     if (shouldRepath) {
                        villager.getNavigation().moveTo(homePos.getX() + 0.5, homePos.getY(), homePos.getZ() + 0.5, 0.6);
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean canClearJobSite(Villager villager) {
      VillagerData villagerData = villager.getVillagerData();
      VillagerProfession profession = villagerData.getProfession();
      return profession == VillagerProfession.NONE || profession == VillagerProfession.NITWIT || villager.getVillagerXp() > 0 || villagerData.getLevel() > 1;
   }

   public static boolean shouldIgnoreDamage(Villager villager, CommandableVillager commandableVillager, DamageSource source) {
      if (commandableVillager.getCommandState() != VillagerCommandState.FOLLOW) {
         return false;
      }

      if (source.is(DamageTypes.FALL) || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.FLY_INTO_WALL) || source.is(DamageTypes.CRAMMING)) {
         return true;
      }

      if (commandableVillager.getCommandTargetUuid() == null) {
         return false;
      }

      Entity attacker = source.getEntity();
      if (attacker != null && commandableVillager.getCommandTargetUuid().equals(attacker.getUUID())) {
         return true;
      }

      Entity directAttacker = source.getDirectEntity();
      return directAttacker != null && commandableVillager.getCommandTargetUuid().equals(directAttacker.getUUID());
   }

   public static Villager teleportNearPlayer(Villager villager, ServerPlayer player) {
      BlockPos origin = player.blockPosition();
      ServerLevel targetLevel = player.serverLevel();
      Vec3 safePos = findSafeTeleportPos(targetLevel, villager, origin);
      if (safePos == null) {
         return null;
      }

      ChunkPos targetChunk = new ChunkPos(BlockPos.containing(safePos));
      targetLevel.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, targetChunk, 1, villager.getId());
      Villager result = teleportVillager(villager, targetLevel, safePos);
      if (result == null) {
         return null;
      }

      LOGGER.info(
         "Villager Ledger teleport destination chosen: villagerUuid={}, level={}, pos={},{},{}",
         new Object[]{result.getUUID(), targetLevel.dimension().location(), safePos.x, safePos.y, safePos.z}
      );
      LOGGER.info(
         "Villager Ledger teleport result: villagerUuid={}, level={}, pos={},{},{}, targetChunkLoaded={}, distanceToPlayer={}",
         new Object[]{
            result.getUUID(),
            result.level().dimension().location(),
            result.getX(),
            result.getY(),
            result.getZ(),
            targetLevel.hasChunkAt(BlockPos.containing(result.position())),
            Math.sqrt(result.distanceToSqr(player))
         }
      );
      return result;
   }

   public static Villager teleportToHome(Villager villager, CommandableVillager commandableVillager) {
      if (!commandableVillager.hasHome() || commandableVillager.getHomePos() == null || commandableVillager.getHomeDimension() == null) {
         return null;
      }

      if (villager.getServer() == null) {
         return null;
      }

      ServerLevel targetLevel = villager.getServer().getLevel(commandableVillager.getHomeDimension());
      if (targetLevel != null && targetLevel.hasChunkAt(commandableVillager.getHomePos())) {
         Vec3 safePos = findSafeTeleportPos(targetLevel, villager, commandableVillager.getHomePos());
         if (safePos == null) {
            return null;
         }

         Villager result = teleportVillager(villager, targetLevel, safePos);
         if (result == null) {
            return null;
         }

         if (result instanceof CommandableVillager teleportedCommandableVillager) {
            teleportedCommandableVillager.setCommandState(VillagerCommandState.STAY);
            teleportedCommandableVillager.setCommandTargetUuid(null);
            teleportedCommandableVillager.setStayPos(commandableVillager.getHomePos());
            teleportedCommandableVillager.setStayAnchorPos(safePos);
            teleportedCommandableVillager.setLastCommandTeleportTick(0L);
         }

         updateNavigationForCommand(result, VillagerCommandState.STAY);
         return result;
      } else {
         return null;
      }
   }

   private static boolean tryTeleportNearPlayer(Villager villager, ServerPlayer player) {
      Vec3 safePos = findSafeTeleportPos(player.serverLevel(), villager, player.blockPosition());
      return safePos == null ? false : teleportVillager(villager, player.serverLevel(), safePos) != null;
   }

   private static Villager teleportVillager(Villager villager, ServerLevel targetLevel, Vec3 safePos) {
      if (villager.level() == targetLevel) {
         villager.teleportTo(targetLevel, safePos.x, safePos.y, safePos.z, Set.of(), villager.getYRot(), villager.getXRot());
         villager.getNavigation().stop();
         villager.setDeltaMovement(Vec3.ZERO);
         return villager;
      } else {
         boolean teleported = villager.teleportTo(
            targetLevel, safePos.x, safePos.y, safePos.z, Set.of(RelativeMovement.X_ROT, RelativeMovement.Y_ROT), villager.getYRot(), villager.getXRot()
         );
         if (!teleported) {
            return null;
         } else if (targetLevel.getEntity(villager.getUUID()) instanceof Villager teleportedVillager) {
            teleportedVillager.getNavigation().stop();
            teleportedVillager.setDeltaMovement(Vec3.ZERO);
            return teleportedVillager;
         } else {
            return null;
         }
      }
   }

   private static Vec3 findSafeTeleportPos(ServerLevel level, Villager villager, BlockPos origin) {
      for (int radius = 0; radius <= 2; radius++) {
         for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
               for (int y = 0; y <= 1; y++) {
                  double targetX = origin.getX() + x + 0.5;
                  double targetY = origin.getY() + y;
                  double targetZ = origin.getZ() + z + 0.5;
                  BlockPos targetPos = BlockPos.containing(targetX, targetY, targetZ);
                  if (level.getFluidState(targetPos).isEmpty()
                     && level.getFluidState(targetPos.above()).isEmpty()
                     && level.noCollision(
                        villager, villager.getBoundingBox().move(targetX - villager.getX(), targetY - villager.getY(), targetZ - villager.getZ())
                     )) {
                     return new Vec3(targetX, targetY, targetZ);
                  }
               }
            }
         }
      }

      return null;
   }

   public static void load(CompoundTag tag, CommandableVillager commandableVillager) {
      if (tag.contains("SlimPatchCommand")) {
         try {
            commandableVillager.setCommandState(VillagerCommandState.valueOf(tag.getString("SlimPatchCommand")));
         } catch (IllegalArgumentException e) {
            commandableVillager.setCommandState(VillagerCommandState.NONE);
         }
      } else {
         commandableVillager.setCommandState(VillagerCommandState.NONE);
      }

      commandableVillager.setCommandTargetUuid(tag.hasUUID("SlimPatchCommandTarget") ? tag.getUUID("SlimPatchCommandTarget") : null);
      commandableVillager.setCommandOwnerUuid(tag.hasUUID("SlimPatchCommandOwnerUuid") ? tag.getUUID("SlimPatchCommandOwnerUuid") : null);
      commandableVillager.setCommandOwnerName(tag.contains("SlimPatchCommandOwnerName") ? tag.getString("SlimPatchCommandOwnerName") : "");
      commandableVillager.setStayPos(tag.contains("SlimPatchStayPos") ? BlockPos.of(tag.getLong("SlimPatchStayPos")) : null);
      commandableVillager.setStayAnchorPos(null);
      if (tag.contains("SlimPatchHomePos") && tag.contains("SlimPatchHomeDimension")) {
         ResourceLocation homeDimensionId = ResourceLocation.tryParse(tag.getString("SlimPatchHomeDimension"));
         if (homeDimensionId != null) {
            commandableVillager.setHome(BlockPos.of(tag.getLong("SlimPatchHomePos")), ResourceKey.create(Registries.DIMENSION, homeDimensionId));
         } else {
            commandableVillager.clearHome();
         }
      } else {
         commandableVillager.clearHome();
      }

      commandableVillager.setLastCommandTeleportTick(0L);
   }

   public static void updateNavigationForCommand(Villager villager, VillagerCommandState commandState) {
      if (villager.getNavigation() instanceof GroundPathNavigation navigation) {
         navigation.setCanOpenDoors(true);
         navigation.setCanPassDoors(true);
      }
   }

   private static boolean isNightOrDusk(Villager villager) {
      long timeOfDay = villager.level().getDayTime() % 24000L;
      return timeOfDay >= 12000L && timeOfDay < 23000L;
   }

   private static Vec3 resolveAnchorPos(Villager villager, BlockPos stayPos) {
      double x = stayPos.getX() + 0.5;
      double z = stayPos.getZ() + 0.5;
      double y = villager.getY();
      if (villager.onGround()) {
         y = Math.max(y, villager.level().getBlockFloorHeight(stayPos) + stayPos.getY());
      }

      return new Vec3(x, y, z);
   }

   public static void save(CompoundTag tag, CommandableVillager commandableVillager) {
      tag.putString("SlimPatchCommand", commandableVillager.getCommandState().name());
      if (commandableVillager.getCommandTargetUuid() != null) {
         tag.putUUID("SlimPatchCommandTarget", commandableVillager.getCommandTargetUuid());
      } else {
         tag.remove("SlimPatchCommandTarget");
      }

      if (commandableVillager.getCommandOwnerUuid() != null) {
         tag.putUUID("SlimPatchCommandOwnerUuid", commandableVillager.getCommandOwnerUuid());
      } else {
         tag.remove("SlimPatchCommandOwnerUuid");
      }

      if (commandableVillager.getCommandOwnerName() != null && !commandableVillager.getCommandOwnerName().isEmpty()) {
         tag.putString("SlimPatchCommandOwnerName", commandableVillager.getCommandOwnerName());
      } else {
         tag.remove("SlimPatchCommandOwnerName");
      }

      if (commandableVillager.getStayPos() != null) {
         tag.putLong("SlimPatchStayPos", commandableVillager.getStayPos().asLong());
      } else {
         tag.remove("SlimPatchStayPos");
      }

      if (commandableVillager.hasHome() && commandableVillager.getHomePos() != null && commandableVillager.getHomeDimension() != null) {
         tag.putLong("SlimPatchHomePos", commandableVillager.getHomePos().asLong());
         tag.putString("SlimPatchHomeDimension", commandableVillager.getHomeDimension().location().toString());
      } else {
         tag.remove("SlimPatchHomePos");
         tag.remove("SlimPatchHomeDimension");
      }
   }

   private static void assignOwner(CommandableVillager commandableVillager, ServerPlayer player) {
      commandableVillager.setCommandOwnerUuid(player.getUUID());
      commandableVillager.setCommandOwnerName(player.getGameProfile().getName());
   }

   private static void releaseOwnerIfPossible(Villager villager, CommandableVillager commandableVillager) {
      if (usesMultiplayerOwnership(villager)) {
         if (commandableVillager.getCommandState() == VillagerCommandState.NONE && !commandableVillager.hasHome()) {
            commandableVillager.setCommandOwnerUuid(null);
            commandableVillager.setCommandOwnerName("");
         }
      }
   }

   private static boolean usesMultiplayerOwnership(Villager villager) {
      return villager.getServer() != null && (villager.getServer().isDedicatedServer() || villager.getServer().isPublished());
   }

   private static void syncLedgerSnapshot(Villager villager, CommandableVillager commandableVillager, UUID playerUuid, boolean suppressed) {
      if (villager != null && commandableVillager != null && playerUuid != null && villager.getServer() != null) {
         UUID villagerUuid = resolveLedgerVillagerUuid(villager);
         if (villagerUuid != null) {
            VillagerLedgerHiddenSavedData data = VillagerLedgerHiddenSavedData.get(villager.getServer());
            data.upsertCompanionSnapshot(playerUuid, villagerUuid, villager, commandableVillager);
            data.setSuppressed(playerUuid, villagerUuid, suppressed);
         }
      }
   }

   private static void removeLedgerSnapshot(Villager villager, UUID playerUuid, UUID villagerUuid) {
      if (villager != null && playerUuid != null && villagerUuid != null && villager.getServer() != null) {
         VillagerLedgerHiddenSavedData.get(villager.getServer()).removeVillager(playerUuid, villagerUuid);
      }
   }

   private static UUID resolveLedgerVillagerUuid(Villager villager) {
      return villager == null ? null : GraveIdentityResolver.resolveHistoricalUuid(villager);
   }

   public record TemporaryCommandStateSnapshot(
      VillagerCommandState commandState, UUID commandTargetUuid, BlockPos stayPos, Vec3 stayAnchorPos, long lastCommandTeleportTick
   ) {
   }
}
