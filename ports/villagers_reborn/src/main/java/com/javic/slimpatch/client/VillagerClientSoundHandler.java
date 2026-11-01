package com.javic.slimpatch.client;

import com.javic.slimpatch.Config;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;

public final class VillagerClientSoundHandler {
    private VillagerClientSoundHandler() {}

    public static boolean shouldSuppress(SoundInstance sound) {
        if (!Config.CUSTOM_VILLAGER_SOUNDS.get() || sound == null) return false;
        String name = sound.getLocation().getPath();
        if (!name.startsWith("entity.villager.") && !name.startsWith("entity.wandering_trader.")) return false;
        return name.equals("entity.villager.ambient") || name.equals("entity.villager.celebrate")
                || name.equals("entity.wandering_trader.ambient") || name.equals("entity.wandering_trader.trade")
                || name.equals(SoundEvents.VILLAGER_NO.getLocation().getPath())
                || name.equals(SoundEvents.VILLAGER_YES.getLocation().getPath());
    }
}
