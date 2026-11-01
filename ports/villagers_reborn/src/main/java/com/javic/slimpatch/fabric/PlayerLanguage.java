package com.javic.slimpatch.fabric;

import net.minecraft.server.level.ServerPlayer;

public interface PlayerLanguage {
    String getSlimpatchLanguage();

    static String get(ServerPlayer player) {
        return ((PlayerLanguage) player).getSlimpatchLanguage();
    }
}
