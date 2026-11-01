package com.javic.slimpatch.events;

import com.javic.slimpatch.Config;
import com.javic.slimpatch.ModEntities;
import com.javic.slimpatch.config.VillagerNameConfig;
import com.javic.slimpatch.entity.AbstractRomanceVillagerEntity;
import com.javic.slimpatch.entity.FemaleVillagerEntity;
import com.javic.slimpatch.entity.HumanEvokerEntity;
import com.javic.slimpatch.entity.HumanPillagerEntity;
import com.javic.slimpatch.entity.HumanVindicatorEntity;
import com.javic.slimpatch.entity.HumanWanderingTraderEntity;
import com.javic.slimpatch.entity.MaleVillagerEntity;
import com.javic.slimpatch.entity.VillagerPersonality;
import com.javic.slimpatch.fabric.PersistentEntityData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.horse.TraderLlama;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.level.Level;

public final class VanillaReplacementHandler {
    private static final String PENDING = "slimpatch_pending_replacement";
    private static final String LOADED = "slimpatch_pending_replacement_loaded_from_disk";
    private static final Map<UUID, ReplacedWanderingTraderEntry> REPLACED_WANDERING_TRADERS = new HashMap<>();
    private static final Map<ResourceKey<Level>, Set<UUID>> QUEUED_VILLAGER_REPLACEMENTS = new HashMap<>();
    private VanillaReplacementHandler() {}

    public static void processQueuedVillagerReplacements(ServerLevel level) {
        Set<UUID> queued = QUEUED_VILLAGER_REPLACEMENTS.get(level.dimension());
        if (queued == null || queued.isEmpty()) return;
        for (UUID id : new ArrayList<>(queued)) {
            Entity entity = level.getEntity(id);
            if (entity == null) continue;
            if (!(entity instanceof Villager villager)) {
                queued.remove(id);
            } else if (!villager.isAlive() || villager.isRemoved()) {
                queued.remove(id);
            } else if (!shouldQueueVillager(villager)) {
                clearPendingReplacement(villager);
                queued.remove(id);
            } else if (replaceLoadedVillager(level, villager, isLoadedFromDisk(villager))) {
                queued.remove(id);
            }
        }
        if (queued.isEmpty()) QUEUED_VILLAGER_REPLACEMENTS.remove(level.dimension());
    }

    /** Called at the same pre-insertion point as Forge's EntityJoinLevelEvent. */
    public static boolean replaceVanillaEntities(Entity entity, ServerLevel level, boolean loadedFromDisk) {
        if (!Config.REPLACE_VANILLA_ENTITIES.get()) return false;
        long gameTime = level.getGameTime();
        REPLACED_WANDERING_TRADERS.entrySet().removeIf(entry -> entry.getValue().expiresAt() < gameTime);
        if (entity instanceof MaleVillagerEntity || entity instanceof FemaleVillagerEntity
                || entity instanceof HumanWanderingTraderEntity || entity instanceof HumanPillagerEntity
                || entity instanceof HumanVindicatorEntity || entity instanceof HumanEvokerEntity) return false;
        if (entity instanceof Villager villager) {
            queueVillagerReplacement(villager, loadedFromDisk);
        } else if (loadedFromDisk) {
            if (entity instanceof TraderLlama llama) reattachTraderLlamaIfNeeded(llama);
        } else if (entity instanceof WanderingTrader trader) {
            return replaceWanderingTrader(level, trader);
        } else if (entity instanceof TraderLlama llama) {
            reattachTraderLlamaIfNeeded(llama);
        } else if (entity instanceof Pillager pillager) {
            return replacePillager(level, pillager);
        } else if (entity instanceof Evoker evoker) {
            return replaceEvoker(level, evoker);
        } else if (entity instanceof Vindicator vindicator) {
            return replaceVindicator(level, vindicator);
        }
        return false;
    }

    private static boolean replaceWanderingTrader(ServerLevel level, WanderingTrader vanilla) {
        CompoundTag data = PersistentEntityData.get(vanilla);
        if (data.getBoolean("slimpatch_replaced") || data.getBoolean("slimpatch_forced")) return false;
        HumanWanderingTraderEntity replacement = ModEntities.HUMAN_WANDERING_TRADER.create(level);
        if (replacement == null) return false;
        replacement.finalizeSpawn(level, level.getCurrentDifficultyAt(vanilla.blockPosition()), MobSpawnType.NATURAL, null, null);
        copyVanillaWanderingTraderData(vanilla, replacement);
        if (!vanilla.hasCustomName()) {
            replacement.setCustomName(Component.literal(replacement.isFemale() ? "Wanderer" : "Traveler"));
            replacement.setCustomNameVisible(true);
        }
        PersistentEntityData.get(replacement).putBoolean("slimpatch_replaced", true);
        if (!level.addFreshEntity(replacement)) return false;
        REPLACED_WANDERING_TRADERS.put(vanilla.getUUID(), new ReplacedWanderingTraderEntry(replacement.getUUID(), level.getGameTime() + 200L));
        reattachTraderLlamas(vanilla, replacement);
        vanilla.discard();
        return true;
    }

    private static boolean replacePillager(ServerLevel level, Pillager vanilla) {
        if (vanilla.getCurrentRaid() != null || isMarked(vanilla)) return false;
        HumanPillagerEntity replacement = ModEntities.HUMAN_PILLAGER.create(level);
        if (replacement == null) return false;
        copyCommonEntityData(vanilla, replacement);
        PersistentEntityData.get(replacement).putBoolean("slimpatch_replaced", true);
        replacement.finalizeSpawn(level, level.getCurrentDifficultyAt(vanilla.blockPosition()), MobSpawnType.NATURAL, null, null);
        if (!level.addFreshEntity(replacement)) return false;
        vanilla.discard();
        return true;
    }

    private static boolean replaceEvoker(ServerLevel level, Evoker vanilla) {
        if (vanilla.getCurrentRaid() != null || isMarked(vanilla)) return false;
        HumanEvokerEntity replacement = ModEntities.HUMAN_EVOKER.create(level);
        if (replacement == null) return false;
        copyCommonEntityData(vanilla, replacement);
        PersistentEntityData.get(replacement).putBoolean("slimpatch_replaced", true);
        replacement.finalizeSpawn(level, level.getCurrentDifficultyAt(vanilla.blockPosition()), MobSpawnType.NATURAL, null, null);
        if (!level.addFreshEntity(replacement)) return false;
        vanilla.discard();
        return true;
    }

    private static boolean replaceVindicator(ServerLevel level, Vindicator vanilla) {
        if (vanilla.getCurrentRaid() != null || isMarked(vanilla)) return false;
        HumanVindicatorEntity replacement = ModEntities.HUMAN_VINDICATOR.create(level);
        if (replacement == null) return false;
        copyCommonEntityData(vanilla, replacement);
        PersistentEntityData.get(replacement).putBoolean("slimpatch_replaced", true);
        replacement.finalizeSpawn(level, level.getCurrentDifficultyAt(vanilla.blockPosition()), MobSpawnType.NATURAL, null, null);
        if (!level.addFreshEntity(replacement)) return false;
        vanilla.discard();
        return true;
    }

    private static boolean isMarked(Entity entity) {
        CompoundTag data = PersistentEntityData.get(entity);
        return data.getBoolean("slimpatch_replaced") || data.getBoolean("slimpatch_forced");
    }

    private static void copyVanillaVillagerData(Villager vanilla, Villager replacement, boolean loadedFromDisk) {
        replacement.moveTo(vanilla.getX(), vanilla.getY(), vanilla.getZ(), vanilla.getYRot(), vanilla.getXRot());
        replacement.setYBodyRot(vanilla.yBodyRot);
        replacement.setYHeadRot(vanilla.getYHeadRot());
        replacement.setDeltaMovement(vanilla.getDeltaMovement());
        replacement.setVillagerData(vanilla.getVillagerData());
        replacement.setNoAi(vanilla.isNoAi());
        replacement.setSilent(vanilla.isSilent());
        replacement.setInvulnerable(vanilla.isInvulnerable());
        if (vanilla.isPersistenceRequired()) replacement.setPersistenceRequired();
        replacement.setHealth(Math.min(vanilla.getHealth(), replacement.getMaxHealth()));
        replacement.setAge(vanilla.getAge());
        if (vanilla.hasCustomName()) replacement.setCustomName(vanilla.getCustomName());
        replacement.setCustomNameVisible(vanilla.isCustomNameVisible());
        CompoundTag copied = PersistentEntityData.get(vanilla).copy();
        copied.remove("slimpatch_replaced");
        copied.remove(PENDING);
        copied.remove(LOADED);
        CompoundTag data = PersistentEntityData.get(replacement);
        data.merge(copied);
        data.putBoolean("slimpatch_replaced", true);
        data.putBoolean("slimpatch_vanilla_migrated", true);
        data.putBoolean("slimpatch_vanilla_replacement_loaded_from_disk", loadedFromDisk);
    }

    private static void queueVillagerReplacement(Villager villager, boolean loadedFromDisk) {
        if (shouldQueueVillager(villager) && villager.level() instanceof ServerLevel level) {
            CompoundTag data = PersistentEntityData.get(villager);
            data.putBoolean(PENDING, true);
            data.putBoolean(LOADED, loadedFromDisk);
            QUEUED_VILLAGER_REPLACEMENTS.computeIfAbsent(level.dimension(), key -> new HashSet<>()).add(villager.getUUID());
        }
    }

    private static boolean replaceLoadedVillager(ServerLevel level, Villager vanilla, boolean loadedFromDisk) {
        if (isMarked(vanilla)) {
            clearPendingReplacement(vanilla);
            return true;
        }
        Villager replacement = level.getRandom().nextBoolean() ? ModEntities.MALE_VILLAGER.create(level) : ModEntities.FEMALE_VILLAGER.create(level);
        if (replacement == null) return false;
        copyVanillaVillagerData(vanilla, replacement, loadedFromDisk);
        initializeLoadedVillagerReplacement(replacement);
        if (!level.addFreshEntity(replacement)) return false;
        clearPendingReplacement(vanilla);
        PersistentEntityData.get(vanilla).putBoolean("slimpatch_replaced", true);
        vanilla.discard();
        return true;
    }

    private static boolean shouldQueueVillager(Villager villager) {
        return !(villager instanceof MaleVillagerEntity) && !(villager instanceof FemaleVillagerEntity) && !isMarked(villager);
    }

    private static boolean isLoadedFromDisk(Villager villager) {
        CompoundTag data = PersistentEntityData.get(villager);
        return data.contains(LOADED) && data.getBoolean(LOADED);
    }

    private static void clearPendingReplacement(Villager villager) {
        PersistentEntityData.get(villager).remove(PENDING);
        PersistentEntityData.get(villager).remove(LOADED);
    }

    private static void initializeLoadedVillagerReplacement(Villager villager) {
        if (!(villager instanceof AbstractRomanceVillagerEntity replacement)) return;
        CompoundTag data = PersistentEntityData.get(replacement);
        if (replacement instanceof FemaleVillagerEntity) {
            data.putString("slimpatch_gender", "female");
            if (!replacement.hasCustomName()) {
                replacement.setCustomName(Component.literal(VillagerNameConfig.getRandomFemaleName(replacement.getRandom())));
                replacement.setCustomNameVisible(true);
            }
        } else {
            data.putString("slimpatch_gender", "male");
            if (!replacement.hasCustomName()) {
                replacement.setCustomName(Component.literal(VillagerNameConfig.getRandomMaleName(replacement.getRandom())));
                replacement.setCustomNameVisible(true);
            }
        }
        if (!data.contains("slimpatch_skin")) replacement.setSkinIndex(replacement.getRandom().nextInt(150) + 1);
        if (data.contains("slimpatch_personality")) {
            try {
                replacement.setPersonality(VillagerPersonality.valueOf(data.getString("slimpatch_personality")));
            } catch (IllegalArgumentException e) {
                replacement.setPersonality(VillagerPersonality.getRandom(replacement.getRandom()));
            }
        } else {
            replacement.setPersonality(VillagerPersonality.getRandom(replacement.getRandom()));
        }
        if (!data.contains("slimpatch_relationship")) replacement.setRelationship(0.5F);
        data.putBoolean("slimpatch_forced", true);
        data.putBoolean("slimpatch_named", replacement.hasCustomName());
        data.putBoolean("slimpatch_initialized", true);
    }

    private static void copyVanillaWanderingTraderData(WanderingTrader vanilla, HumanWanderingTraderEntity replacement) {
        CompoundTag saved = vanilla.saveWithoutId(new CompoundTag());
        saved.remove("UUID");
        replacement.load(saved);
        replacement.setYBodyRot(vanilla.yBodyRot);
        replacement.setYHeadRot(vanilla.getYHeadRot());
        PersistentEntityData.get(replacement).putBoolean("slimpatch_vanilla_migrated", true);
    }

    private static void copyCommonEntityData(Entity vanilla, Entity replacement) {
        replacement.moveTo(vanilla.getX(), vanilla.getY(), vanilla.getZ(), vanilla.getYRot(), vanilla.getXRot());
        replacement.setYBodyRot(vanilla.getYRot());
        replacement.setYHeadRot(vanilla.getYHeadRot());
        replacement.setDeltaMovement(vanilla.getDeltaMovement());
        replacement.setSilent(vanilla.isSilent());
        replacement.setInvulnerable(vanilla.isInvulnerable());
        if (vanilla instanceof Mob oldMob && replacement instanceof Mob newMob) {
            newMob.setNoAi(oldMob.isNoAi());
            if (oldMob.isPersistenceRequired()) newMob.setPersistenceRequired();
            newMob.setHealth(Math.min(oldMob.getHealth(), newMob.getMaxHealth()));
        }
        if (vanilla.hasCustomName()) replacement.setCustomName(vanilla.getCustomName());
        replacement.setCustomNameVisible(vanilla.isCustomNameVisible());
        CompoundTag copied = PersistentEntityData.get(vanilla).copy();
        copied.remove("slimpatch_replaced");
        PersistentEntityData.get(replacement).merge(copied);
        PersistentEntityData.get(replacement).putBoolean("slimpatch_vanilla_migrated", true);
    }

    private static void reattachTraderLlamas(WanderingTrader vanilla, HumanWanderingTraderEntity replacement) {
        if (replacement.level() instanceof ServerLevel level) {
            for (TraderLlama llama : level.getEntitiesOfClass(TraderLlama.class, vanilla.getBoundingBox().inflate(16.0))) {
                if (llama.getLeashHolder() == vanilla) llama.setLeashedTo(replacement, true);
            }
        }
    }

    private static void reattachTraderLlamaIfNeeded(TraderLlama llama) {
        if (llama.level() instanceof ServerLevel level && llama.getLeashHolder() instanceof WanderingTrader trader) {
            ReplacedWanderingTraderEntry entry = REPLACED_WANDERING_TRADERS.get(trader.getUUID());
            if (entry != null) {
                if (level.getEntity(entry.replacementUuid()) instanceof HumanWanderingTraderEntity replacement) llama.setLeashedTo(replacement, true);
                REPLACED_WANDERING_TRADERS.remove(trader.getUUID());
            }
        }
    }

    private record ReplacedWanderingTraderEntry(UUID replacementUuid, long expiresAt) {}
}
