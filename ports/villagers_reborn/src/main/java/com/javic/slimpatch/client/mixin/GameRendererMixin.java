package com.javic.slimpatch.client.mixin;

import com.javic.slimpatch.client.camera.DialogueThirdPersonCameraHandler;
import com.javic.slimpatch.client.camera.FamilyCutsceneCameraHandler;
import com.javic.slimpatch.client.camera.WeddingCutsceneCameraHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void slimpatch$renderStart(float partialTick, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        DialogueThirdPersonCameraHandler.renderStart();
        FamilyCutsceneCameraHandler.renderStart();
        WeddingCutsceneCameraHandler.renderStart();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void slimpatch$renderEnd(float partialTick, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        WeddingCutsceneCameraHandler.renderEnd();
        FamilyCutsceneCameraHandler.renderEnd();
        DialogueThirdPersonCameraHandler.renderEnd();
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getXRot()F", ordinal = 0))
    private void slimpatch$cameraRoll(float partialTick, long nanoTime, PoseStack poseStack, CallbackInfo ci) {
        float roll = WeddingCutsceneCameraHandler.roll();
        if (roll == 0.0F) roll = FamilyCutsceneCameraHandler.roll();
        if (roll != 0.0F) poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
    }
}
