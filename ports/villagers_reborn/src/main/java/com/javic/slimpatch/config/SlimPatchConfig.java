package com.javic.slimpatch.config;

import com.javic.slimpatch.Config;
import com.javic.slimpatch.config.ConfigStore.Value;
import net.minecraft.server.MinecraftServer;

public final class SlimPatchConfig {
    public static final ConfigStore SERVER_SPEC = new ConfigStore("villagersreborn-server.toml");
    public static final ServerConfig SERVER = new ServerConfig();

    static { SERVER_SPEC.save(); }

    private SlimPatchConfig() {}

    public static boolean useServerOverrides(MinecraftServer server) {
        return server != null && (server.isDedicatedServer() || server.isPublished());
    }

    public static boolean effectiveEnableHavingChildren(MinecraftServer server) {
        return useServerOverrides(server) ? SERVER.enableHavingChildren.get() : Config.ENABLE_HAVING_CHILDREN.get();
    }

    public static boolean effectiveEnableNaturalFamilySpawns(MinecraftServer server) {
        return useServerOverrides(server) ? SERVER.enableNaturalFamilySpawns.get() : Config.ENABLE_NATURAL_FAMILY_SPAWNS.get();
    }

    public static final class ServerConfig {
        public final Value<String> skinType = SERVER_SPEC.string("server.skinType", "modern");
        public final Value<Boolean> allowMultiplayerCustomSkins = SERVER_SPEC.bool("server.allowMultiplayerCustomSkins", true);
        public final Value<Integer> maxCustomSkinSizeKb = SERVER_SPEC.integer("server.maxCustomSkinSizeKb", 256, 16, 2048);
        public final Value<Boolean> enableHavingChildren = SERVER_SPEC.bool("server.enableHavingChildren", true);
        public final Value<Boolean> enableNaturalFamilySpawns = SERVER_SPEC.bool("server.enableNaturalFamilySpawns", true);
    }
}
