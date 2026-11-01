package com.javic.slimpatch.client;

import com.javic.slimpatch.client.cutscene.FamilyCutsceneController;
import com.javic.slimpatch.client.cutscene.WeddingCutsceneController;
import com.javic.slimpatch.client.cutscene.WeddingMusicDuckingHandler;
import com.javic.slimpatch.client.gui.SkinSelectionHandler;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public final class ClientEvents {
    private ClientEvents() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            DialogueClientHandler.onKeyInput();
            SkinSelectionHandler.onClientTick();
            FamilyCutsceneController.onClientTick();
            WeddingCutsceneController.onClientTick();
            WeddingMusicDuckingHandler.onClientTick();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SkinSelectionHandler.onLogout());
    }
}
