package com.javic.slimpatch.client.mixin;

import com.javic.slimpatch.client.cutscene.FamilyCutsceneController;
import com.javic.slimpatch.client.cutscene.WeddingCutsceneController;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Shadow public Input input;

    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/Input;tick(ZF)V", shift = At.Shift.AFTER))
    private void slimpatch$freezeInput(CallbackInfo ci) {
        FamilyCutsceneController.onMovementInput(input);
        WeddingCutsceneController.onMovementInput(input);
    }
}
