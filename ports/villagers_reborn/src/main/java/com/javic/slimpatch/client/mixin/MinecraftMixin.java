package com.javic.slimpatch.client.mixin;

import com.javic.slimpatch.client.camera.DialogueThirdPersonCameraHandler;
import com.javic.slimpatch.client.gui.VillagerDialogueScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow public Screen screen;

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void slimpatch$closeDialogue(Screen next, CallbackInfo ci) {
        if (screen instanceof VillagerDialogueScreen && screen != next) DialogueThirdPersonCameraHandler.reset();
    }
}
