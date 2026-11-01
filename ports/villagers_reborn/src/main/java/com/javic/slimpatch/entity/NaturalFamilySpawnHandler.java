package com.javic.slimpatch.entity;

import com.javic.slimpatch.ModEntities;
import com.javic.slimpatch.config.SlimPatchConfig;
import com.javic.slimpatch.config.VillagerNameConfig;
import com.javic.slimpatch.data.WorldSkinData;
import com.javic.slimpatch.fabric.PersistentEntityData;
import com.javic.slimpatch.familytree.FamilyTreeTracker;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class NaturalFamilySpawnHandler {
    private static final double NATURAL_FAMILY_CHANCE = 0.35;
    private static final double LOCAL_RADIUS = 48.0;
    private static final int LOCAL_VILLAGER_CAP = 28;
    private static final int LOCAL_NATURAL_FAMILY_GROUP_CAP = 3;
    private static final int MIN_CHILDREN = 1;
    private static final int MAX_CHILDREN = 2;
    private static final int MAX_SPAWN_POSITION_ATTEMPTS = 12;
    public static final String SKIP_NATURAL_FAMILY_SPAWN_TAG = "slimpatch_skip_natural_family_spawn";
    private static final String NATURAL_FAMILY_SPAWN_PROCESSED_TAG = "slimpatch_natural_family_spawn_processed";
    private static final long NATURAL_FAMILY_QUEUE_DELAY_TICKS = 2L;
    private static final long NATURAL_FAMILY_QUEUE_RETENTION_TICKS = 200L;
    private static final Map<ResourceKey<Level>, Map<UUID, Long>> QUEUED_NATURAL_FAMILY_SPAWNS = new HashMap<>();

    private NaturalFamilySpawnHandler() {}

    public static void maybeSpawnNaturalFamily(Villager anchor, FamilyVillager family, ServerLevelAccessor level, MobSpawnType reason) {
        if (anchor != null && family != null && level != null && !level.isClientSide()
                && SlimPatchConfig.effectiveEnableNaturalFamilySpawns(level.getLevel().getServer())
                && reason == MobSpawnType.NATURAL) {
            queueNaturalFamilySpawn(anchor);
        }
    }

    public static void queueFreshMigratedVillagerCandidates(ServerLevel level, Entity entity) {
        if (SlimPatchConfig.effectiveEnableNaturalFamilySpawns(level.getServer())
                && entity instanceof AbstractRomanceVillagerEntity villager
                && PersistentEntityData.get(villager).getBoolean("slimpatch_vanilla_migrated")
                && !PersistentEntityData.get(villager).getBoolean("slimpatch_vanilla_replacement_loaded_from_disk")) {
            queueNaturalFamilySpawn(level, villager.getUUID());
        }
    }

    public static void processQueuedNaturalFamilySpawns(ServerLevel level) {
        if (!SlimPatchConfig.effectiveEnableNaturalFamilySpawns(level.getServer())) return;
        Map<UUID, Long> queued = QUEUED_NATURAL_FAMILY_SPAWNS.get(level.dimension());
        if (queued == null || queued.isEmpty()) return;
        long gameTime = level.getGameTime();
        for (Map.Entry<UUID, Long> entry : new ArrayList<>(queued.entrySet())) {
            if (entry.getValue() <= gameTime) {
                Entity entity = level.getEntity(entry.getKey());
                if (entity instanceof Villager anchor && entity instanceof FamilyVillager family
                        && isAnchorStillValid(level, anchor, family)) {
                    trySpawnNaturalFamilyNow(level, anchor, family);
                }
                queued.remove(entry.getKey());
            }
        }
        queued.entrySet().removeIf(entry -> entry.getValue() + NATURAL_FAMILY_QUEUE_RETENTION_TICKS < gameTime);
        if (queued.isEmpty()) QUEUED_NATURAL_FAMILY_SPAWNS.remove(level.dimension());
    }

    private static void queueNaturalFamilySpawn(Villager anchor) {
        if (anchor.level() instanceof ServerLevel level) queueNaturalFamilySpawn(level, anchor.getUUID());
    }

    private static void queueNaturalFamilySpawn(ServerLevel level, UUID anchorUuid) {
        if (level != null && anchorUuid != null) {
            QUEUED_NATURAL_FAMILY_SPAWNS.computeIfAbsent(level.dimension(), key -> new HashMap<>())
                    .putIfAbsent(anchorUuid, level.getGameTime() + NATURAL_FAMILY_QUEUE_DELAY_TICKS);
        }
    }

    private static boolean isAnchorStillValid(ServerLevel level, Villager anchor, FamilyVillager family) {
        return !anchor.isRemoved() && anchor.isAlive() && anchor instanceof AbstractRomanceVillagerEntity
                && family.getAgeStage() == VillagerAgeStage.ADULT && !family.hasSpouse()
                && family.getRelationshipStage() != VillagerRelationshipStage.DATING
                && family.getRelationshipStage() != VillagerRelationshipStage.MARRIED
                && !family.isNaturalFamilyMember() && family.getNaturalFamilyGroupId() == null
                && !PersistentEntityData.get(anchor).getBoolean(SKIP_NATURAL_FAMILY_SPAWN_TAG)
                && !PersistentEntityData.get(anchor).getBoolean(NATURAL_FAMILY_SPAWN_PROCESSED_TAG)
                && !PersistentEntityData.get(anchor).getBoolean("slimpatch_vanilla_replacement_loaded_from_disk")
                && countNearbyCustomVillagers(level, anchor) < LOCAL_VILLAGER_CAP
                && countNearbyNaturalFamilyGroups(level, anchor) < LOCAL_NATURAL_FAMILY_GROUP_CAP;
    }

    private static void trySpawnNaturalFamilyNow(ServerLevel level, Villager anchor, FamilyVillager family) {
        if (anchor.getRandom().nextDouble() >= NATURAL_FAMILY_CHANCE) return;
        PreparedFamily prepared = prepareFamily(level, anchor, anchor.getRandom());
        if (prepared == null || prepared.children().size() < MIN_CHILDREN) return;
        if (countNearbyCustomVillagers(level, anchor) + 1 + prepared.children().size() > LOCAL_VILLAGER_CAP) {
            prepared.spouse().discard();
            prepared.children().forEach(Entity::discard);
            return;
        }
        List<Villager> added = new ArrayList<>();
        if (!level.addFreshEntity(prepared.spouse())) {
            prepared.spouse().discard();
            prepared.children().forEach(Entity::discard);
            return;
        }
        added.add(prepared.spouse());
        for (Villager child : prepared.children()) {
            if (!level.addFreshEntity(child)) {
                child.discard();
                for (Villager existing : added) {
                    existing.discard();
                    FamilyTreeTracker.markVillagerDead(level.getServer(), existing.getUUID());
                }
                prepared.children().stream().filter(v -> v != child && !added.contains(v)).forEach(Entity::discard);
                return;
            }
            added.add(child);
        }
        family.setNaturalFamilyGroupId(prepared.groupId());
        family.setNaturalFamilyMember(true);
        family.setSpouseVillagerUuid(prepared.spouse().getUUID());
        family.setSpouseVillagerName(resolveVillagerName(prepared.spouse()));
        family.setParentPlayerUuid(null);
        family.setParentPlayerName("");
        family.setFamilyOwnerPlayerUuid(null);
        family.setFamilyOwnerPlayerName("");
        PersistentEntityData.get(anchor).putBoolean(NATURAL_FAMILY_SPAWN_PROCESSED_TAG, true);
        FamilyTreeTracker.upsertVillager(level.getServer(), anchor);
        FamilyTreeTracker.upsertVillager(level.getServer(), prepared.spouse());
        for (Villager child : prepared.children()) FamilyTreeTracker.upsertVillager(level.getServer(), child);
    }

    @Nullable
    private static PreparedFamily prepareFamily(ServerLevel level, Villager anchor, RandomSource random) {
        Villager spouse = createSpouse(level, anchor);
        if (!(spouse instanceof FamilyVillager spouseFamily)) return null;
        UUID groupId = UUID.randomUUID();
        applyParentFamilyData(spouseFamily, groupId, anchor.getUUID(), resolveVillagerName(anchor));
        int targetChildren = MIN_CHILDREN + random.nextInt(MAX_CHILDREN - MIN_CHILDREN + 1);
        List<Villager> children = new ArrayList<>();
        for (int i = 0; i < targetChildren; i++) {
            Villager child = createChild(level, anchor, spouse, groupId, random);
            if (child != null) children.add(child);
        }
        if (children.size() < MIN_CHILDREN) {
            spouse.discard();
            children.forEach(Entity::discard);
            return null;
        }
        return new PreparedFamily(groupId, spouse, children);
    }

    @Nullable
    private static Villager createSpouse(ServerLevel level, Villager anchor) {
        Villager spouse = createOppositeGenderVillager(level, anchor);
        if (!(spouse instanceof FamilyVillager family)) return null;
        Vec3 pos = findSpawnPosition(level, spouse, List.of(anchor.position()));
        if (pos == null) return null;
        PersistentEntityData.get(spouse).putBoolean(SKIP_NATURAL_FAMILY_SPAWN_TAG, true);
        family.setNaturalFamilyMember(true);
        spouse.moveTo(pos.x, pos.y, pos.z, anchor.getYRot(), anchor.getXRot());
        spouse.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(pos)), MobSpawnType.NATURAL, null, null);
        applyVillagerType(spouse, anchor.getVillagerData().getType());
        return hasLoadedCollisionChunks(level, spouse.getBoundingBox()) && level.noCollision(spouse) ? spouse : null;
    }

    @Nullable
    private static Villager createChild(ServerLevel level, Villager anchor, Villager spouse, UUID groupId, RandomSource random) {
        String gender = random.nextBoolean() ? "male" : "female";
        Villager child = createVillagerForGender(level, gender);
        if (!(child instanceof FamilyVillager family)) return null;
        Vec3 pos = findSpawnPosition(level, child, List.of(anchor.position(), spouse.position()));
        if (pos == null) return null;
        PersistentEntityData.get(child).putBoolean(SKIP_NATURAL_FAMILY_SPAWN_TAG, true);
        family.setNaturalFamilyMember(true);
        child.moveTo(pos.x, pos.y, pos.z, anchor.getYRot(), anchor.getXRot());
        child.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(pos)), MobSpawnType.NATURAL, null, null);
        applyVillagerType(child, anchor.getVillagerData().getType());
        applyNaturalChildData(level, child, family, anchor, spouse, groupId, gender, random);
        return hasLoadedCollisionChunks(level, child.getBoundingBox()) && level.noCollision(child) ? child : null;
    }

    @Nullable
    private static Villager createOppositeGenderVillager(ServerLevel level, Villager anchor) {
        if (anchor instanceof MaleVillagerEntity) return ModEntities.FEMALE_VILLAGER.create(level);
        return anchor instanceof FemaleVillagerEntity ? ModEntities.MALE_VILLAGER.create(level) : null;
    }

    @Nullable
    private static Villager createVillagerForGender(ServerLevel level, String gender) {
        EntityType<? extends Villager> type = "female".equals(BirthScreenData.normalizeGender(gender))
                ? ModEntities.FEMALE_VILLAGER : ModEntities.MALE_VILLAGER;
        return type.create(level);
    }

    private static void applyParentFamilyData(FamilyVillager parent, UUID groupId, UUID spouseUuid, String spouseName) {
        parent.setNaturalFamilyGroupId(groupId);
        parent.setNaturalFamilyMember(true);
        parent.setSpouseVillagerUuid(spouseUuid);
        parent.setSpouseVillagerName(spouseName);
        parent.setParentPlayerUuid(null);
        parent.setParentPlayerName("");
        parent.setFamilyOwnerPlayerUuid(null);
        parent.setFamilyOwnerPlayerName("");
    }

    private static void applyNaturalChildData(ServerLevel level, Villager child, FamilyVillager family, Villager parent1,
                                               Villager parent2, UUID groupId, String gender, RandomSource random) {
        String normalizedGender = BirthScreenData.normalizeGender(gender);
        String name = "female".equals(normalizedGender) ? VillagerNameConfig.getRandomFemaleName(random) : VillagerNameConfig.getRandomMaleName(random);
        child.setCustomName(Component.literal(name));
        child.setCustomNameVisible(true);
        VillagerAgeStage ageStage = switch (random.nextInt(3)) {
            case 0 -> VillagerAgeStage.TODDLER;
            case 1 -> VillagerAgeStage.CHILD;
            default -> VillagerAgeStage.TEEN;
        };
        family.setAgeStage(ageStage);
        FamilyAgingHandler.setAgeStageWithTimer(family, ageStage, level.getGameTime());
        family.setNaturalFamilyGroupId(groupId);
        family.setNaturalFamilyMember(true);
        family.setParentVillagerUuid(parent1.getUUID());
        family.setParentVillagerName(resolveVillagerName(parent1));
        family.setParentVillager2Uuid(parent2.getUUID());
        family.setParentVillager2Name(resolveVillagerName(parent2));
        family.setParentPlayerUuid(null);
        family.setParentPlayerName("");
        family.setFamilyOwnerPlayerUuid(null);
        family.setFamilyOwnerPlayerName("");
        family.setSpouseVillagerUuid(null);
        family.setSpouseVillagerName("");
        family.setBornFromFamilySystem(false);
        family.setRelationshipStage(VillagerRelationshipStage.FRIENDSHIP);
        VillagerPersonality personality = BirthScreenData.getRandomPersonality(random);
        String skinType = BirthScreenData.normalizeSkinType(WorldSkinData.get(level).getTheme());
        int skinId = BirthScreenData.getRandomSkinId(normalizedGender, skinType, random);
        String skinPath = BirthScreenData.getCuratedSkinResourcePath(normalizedGender, skinType, skinId);
        if (child instanceof MaleVillagerEntity male) {
            male.setPersonality(personality);
            male.setSkinIndex(skinId);
            male.setSavedSkinInput("");
            male.setCustomSkinPath(skinPath == null ? "" : skinPath);
        } else if (child instanceof FemaleVillagerEntity female) {
            female.setPersonality(personality);
            female.setSkinIndex(skinId);
            female.setSavedSkinInput("");
            female.setCustomSkinPath(skinPath == null ? "" : skinPath);
        }
    }

    private static void applyVillagerType(Villager villager, VillagerType type) {
        if (villager != null && type != null) {
            VillagerData data = villager.getVillagerData();
            villager.setVillagerData(data.setType(type));
        }
    }

    private static int countNearbyCustomVillagers(ServerLevel level, Villager anchor) {
        AABB bounds = anchor.getBoundingBox().inflate(LOCAL_RADIUS, 16.0, LOCAL_RADIUS);
        return level.getEntitiesOfClass(MaleVillagerEntity.class, bounds).size() + level.getEntitiesOfClass(FemaleVillagerEntity.class, bounds).size();
    }

    private static int countNearbyNaturalFamilyGroups(ServerLevel level, Villager anchor) {
        AABB bounds = anchor.getBoundingBox().inflate(LOCAL_RADIUS, 16.0, LOCAL_RADIUS);
        Set<UUID> groups = new HashSet<>();
        for (MaleVillagerEntity villager : level.getEntitiesOfClass(MaleVillagerEntity.class, bounds)) {
            if (villager.getNaturalFamilyGroupId() != null) groups.add(villager.getNaturalFamilyGroupId());
        }
        for (FemaleVillagerEntity villager : level.getEntitiesOfClass(FemaleVillagerEntity.class, bounds)) {
            if (villager.getNaturalFamilyGroupId() != null) groups.add(villager.getNaturalFamilyGroupId());
        }
        return groups.size();
    }

    @Nullable
    private static Vec3 findSpawnPosition(ServerLevel level, Entity entity, List<Vec3> anchors) {
        int attempts = 0;
        for (Vec3 anchor : anchors) {
            for (int radius = 1; radius <= 3; radius++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        for (int dy = 0; dy <= 1; dy++) {
                            if (attempts++ >= MAX_SPAWN_POSITION_ATTEMPTS) return null;
                            BlockPos pos = BlockPos.containing(anchor.x + dx, anchor.y + dy, anchor.z + dz);
                            Vec3 spawnPos = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                            AABB box = entity.getBoundingBox().move(spawnPos.x - entity.getX(), spawnPos.y - entity.getY(), spawnPos.z - entity.getZ());
                            if (hasLoadedCollisionChunks(level, box) && level.noCollision(entity, box)) return spawnPos;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean hasLoadedCollisionChunks(ServerLevel level, AABB box) {
        return level.hasChunksAt(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ));
    }

    private static String resolveVillagerName(Villager villager) {
        if (villager == null) return "";
        Component customName = villager.getCustomName();
        return customName != null && !customName.getString().isEmpty() ? customName.getString() : villager.getName().getString();
    }

    private record PreparedFamily(UUID groupId, Villager spouse, List<Villager> children) {}
}
