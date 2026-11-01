package com.javic.slimpatch.events;

import com.javic.slimpatch.commands.SlimPatchDebugCommand;
import com.javic.slimpatch.commands.WorldSkinTypeCommand;
import com.javic.slimpatch.entity.FamilyCharmHandler;
import com.javic.slimpatch.entity.FamilyTreeViewHandler;
import com.javic.slimpatch.entity.NaturalFamilySpawnHandler;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public final class VillagersEventBootstrap {
    private VillagersEventBootstrap() {}

    public static void init() {
        ModEntityAttributes.registerAttributes();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            SlimPatchDebugCommand.register(dispatcher);
            WorldSkinTypeCommand.register(dispatcher);
        });
        ServerTickEvents.END_WORLD_TICK.register(level -> {
            VanillaReplacementHandler.processQueuedVillagerReplacements(level);
            NaturalFamilySpawnHandler.processQueuedNaturalFamilySpawns(level);
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> FamilyTreeEventHandler.onLivingDeath(entity));
        ServerLivingEntityEvents.MOB_CONVERSION.register((original, outcome, keepEquipment) ->
                FamilyTreeEventHandler.onLivingConversionPost(original, outcome));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            SkinThemeSyncHandler.onPlayerLoggedIn(handler.player);
            MultiplayerSkinSyncHandler.onPlayerLoggedIn(handler.player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            DialogueSessionHandler.onPlayerLoggedOut(handler.player);
            FamilyCharmHandler.onPlayerLoggedOut(handler.player);
            FamilyTreeViewHandler.onPlayerLoggedOut(handler.player);
        });
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            FamilyCharmHandler.onPlayerChangedDimension(player);
            FamilyTreeViewHandler.onPlayerChangedDimension(player);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            FamilyCharmHandler.onServerStopping(server);
            FamilyTreeViewHandler.onServerStopping(server);
        });
    }
}
