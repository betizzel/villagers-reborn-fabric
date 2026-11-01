package com.javic.slimpatch;

import com.javic.slimpatch.config.SlimPatchConfig;
import com.javic.slimpatch.events.VillagersEventBootstrap;
import com.javic.slimpatch.item.ModCreativeTabs;
import com.javic.slimpatch.item.ModItems;
import com.javic.slimpatch.network.ModNetworking;
import com.javic.slimpatch.sounds.HumanIllagerSounds;
import com.javic.slimpatch.sounds.HumanVillagerSounds;
import com.javic.slimpatch.sounds.HumanZombieVillagerSounds;
import net.fabricmc.api.ModInitializer;

public final class SlimPatch implements ModInitializer {
    public static final String MODID = "slimpatch";

    @Override
    public void onInitialize() {
        Config.SPEC.save();
        SlimPatchConfig.SERVER_SPEC.save();
        ModEntities.init();
        ModBlocks.init();
        ModBlockEntities.init();
        ModMenus.init();
        ModItems.init();
        ModCreativeTabs.init();
        HumanVillagerSounds.init();
        HumanIllagerSounds.init();
        HumanZombieVillagerSounds.init();
        ModNetworking.register();
        VillagersEventBootstrap.init();
    }
}
