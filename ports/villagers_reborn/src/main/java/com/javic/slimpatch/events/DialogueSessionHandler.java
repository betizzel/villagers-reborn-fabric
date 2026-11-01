package com.javic.slimpatch.events;

import com.javic.slimpatch.dialogue.DialogueManager;
import net.minecraft.server.level.ServerPlayer;

public final class DialogueSessionHandler {
    private DialogueSessionHandler() {}

    public static void onPlayerLoggedOut(ServerPlayer player) {
        DialogueManager.endDialoguesForPlayer(player.getUUID());
    }
}
