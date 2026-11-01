package com.javic.slimpatch.events;

import com.javic.slimpatch.config.SlimPatchConfig;
import com.javic.slimpatch.fabric.PersistentEntityData;
import com.javic.slimpatch.network.ModNetworking;
import com.javic.slimpatch.network.MultiplayerSkinSettingsSyncPacket;
import com.javic.slimpatch.network.VillagerCustomSkinSyncPacket;
import com.javic.slimpatch.util.MultiplayerSkinStorage;
import java.io.IOException;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;

public final class MultiplayerSkinSyncHandler {
    private MultiplayerSkinSyncHandler() {}

    public static void onPlayerLoggedIn(ServerPlayer player) {
        if (player.getServer() != null && (player.getServer().isDedicatedServer() || player.getServer().isPublished())) {
            ModNetworking.sendToClient(new MultiplayerSkinSettingsSyncPacket(
                    SlimPatchConfig.SERVER.allowMultiplayerCustomSkins.get(),
                    SlimPatchConfig.SERVER.maxCustomSkinSizeKb.get(),
                    SlimPatchConfig.effectiveEnableHavingChildren(player.getServer())), player);
        }
    }

    public static void broadcastSkin(Villager villager, byte[] pngData, String savedSkinInput, boolean clear) {
        if (villager.getServer() != null) {
            for (ServerPlayer player : villager.getServer().getPlayerList().getPlayers()) {
                if (player.level() == villager.level()) {
                    ModNetworking.sendToClient(new VillagerCustomSkinSyncPacket(villager.getId(), villager.getUUID(), savedSkinInput, pngData, clear), player);
                }
            }
        }
    }

    public static void sendExistingSkin(Villager villager, ServerPlayer player) {
        if (PersistentEntityData.get(villager).contains("CustomSkinPath")) {
            try {
                byte[] pngData = MultiplayerSkinStorage.readSkin(villager.getUUID());
                if (pngData == null || pngData.length == 0) return;
                ModNetworking.sendToClient(new VillagerCustomSkinSyncPacket(villager.getId(), villager.getUUID(),
                        PersistentEntityData.get(villager).getString("SavedSkinInput"), pngData, false), player);
            } catch (IOException ignored) {
            }
        }
    }
}
