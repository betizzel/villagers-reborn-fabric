package com.javic.slimpatch.client;

import com.javic.slimpatch.client.key.ModKeyBindings;
import com.javic.slimpatch.network.ModNetworking;
import com.javic.slimpatch.util.SkinPathHelper;
import java.io.File;
import java.util.UUID;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.resources.ResourceLocation;

public final class SlimPatchClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SkinPathHelper.setClientAccess(new SkinPathHelper.ClientAccess() {
            public ResourceLocation getMountedExternalSkinTexture() { return ClientSkinTextureHandler.getMountedExternalSkinTexture(); }
            public ResourceLocation loadExternalSkinTexture(UUID id, File file) { return ClientSkinTextureHandler.loadExternalSkinTexture(id, file); }
            public String getClientTheme() { return ClientSkinTextureHandler.getClientTheme(); }
            public boolean isResourceMissing(ResourceLocation location) { return ClientSkinTextureHandler.isResourceMissing(location); }
        });
        ModNetworking.registerClient();
        ModKeyBindings.register();
        ClientRenderers.register();
        ClientEvents.register();
    }
}
