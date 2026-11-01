package com.javic.slimpatch.mixin;

import com.javic.slimpatch.fabric.PlayerLanguage;
import net.minecraft.network.protocol.game.ServerboundClientInformationPacket;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerLanguageMixin implements PlayerLanguage {
    @Unique private String slimpatch$language = "en_us";

    @Override
    public String getSlimpatchLanguage() {
        return slimpatch$language;
    }

    @Inject(method = "updateOptions", at = @At("HEAD"))
    private void slimpatch$updateLanguage(ServerboundClientInformationPacket packet, CallbackInfo ci) {
        slimpatch$language = packet.language();
    }
}
