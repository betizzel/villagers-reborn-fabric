package com.javic.slimpatch.events;

import com.javic.slimpatch.Config;
import com.javic.slimpatch.config.NewWorldSkinTypeMode;
import com.javic.slimpatch.data.WorldSkinData;
import com.javic.slimpatch.network.ModNetworking;
import com.javic.slimpatch.network.SkinThemeSyncPacket;
import net.minecraft.server.level.ServerPlayer;

public final class SkinThemeSyncHandler {
    private SkinThemeSyncHandler() {}

    public static void onPlayerLoggedIn(ServerPlayer player) {
        if (player.getServer() != null && (player.getServer().isDedicatedServer() || player.getServer().isPublished())) {
            WorldSkinData data = WorldSkinData.get(player.getServer());
            if (!data.isInitialized()) {
                data.setTheme(((NewWorldSkinTypeMode)Config.NEW_WORLD_SKIN_TYPE.get()).resolveAutomaticTheme());
                data.setGuiShown(true);
            }
            ModNetworking.sendToClient(new SkinThemeSyncPacket(data.getTheme()), player);
        }
    }
}
