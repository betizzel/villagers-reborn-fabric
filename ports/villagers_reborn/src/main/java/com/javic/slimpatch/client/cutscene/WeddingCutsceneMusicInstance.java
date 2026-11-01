package com.javic.slimpatch.client.cutscene;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public class WeddingCutsceneMusicInstance extends AbstractTickableSoundInstance {
    public WeddingCutsceneMusicInstance(SoundEvent soundEvent) {
        super(soundEvent, SoundSource.RECORDS, RandomSource.create());
        volume = 1.0F;
        pitch = 1.0F;
        looping = false;
        delay = 0;
        attenuation = Attenuation.NONE;
        relative = true;
        x = 0.0;
        y = 0.0;
        z = 0.0;
    }

    @Override
    public void tick() {}

    public void stopPlayback() { stop(); }
}
