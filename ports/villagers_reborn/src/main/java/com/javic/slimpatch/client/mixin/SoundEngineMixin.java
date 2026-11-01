package com.javic.slimpatch.client.mixin;

import com.javic.slimpatch.client.HumanIllagerClientSoundHandler;
import com.javic.slimpatch.client.VillagerClientSoundHandler;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void slimpatch$filterSound(SoundInstance sound, CallbackInfo ci) {
        if (VillagerClientSoundHandler.shouldSuppress(sound) || HumanIllagerClientSoundHandler.shouldSuppress(sound)) ci.cancel();
    }
}
