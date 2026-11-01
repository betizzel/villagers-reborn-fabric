package com.javic.slimpatch.client.mixin;

import com.javic.slimpatch.client.DialogueHudOverlay;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"))
    private void slimpatch$beforeCrosshair(GuiGraphics graphics, CallbackInfo ci) { DialogueHudOverlay.beforeCrosshair(); }

    @Inject(method = "renderCrosshair", at = @At("RETURN"))
    private void slimpatch$afterCrosshair(GuiGraphics graphics, CallbackInfo ci) { DialogueHudOverlay.afterCrosshair(graphics); }
}
