package com.javic.slimpatch.client;

import com.javic.slimpatch.Config;
import net.minecraft.client.resources.sounds.SoundInstance;

public final class HumanIllagerClientSoundHandler {
    private HumanIllagerClientSoundHandler() {
    }

    public static boolean shouldSuppress(SoundInstance sound) {
        if (!Config.CUSTOM_ILLAGER_SOUNDS.get() || sound == null) return false;
        String name = sound.getLocation().getPath();
        return (name.startsWith("entity.pillager.") || name.startsWith("entity.vindicator.") || name.startsWith("entity.evoker."))
                && (name.contains("ambient") || name.contains("hurt") || name.contains("death"));
    }
}
