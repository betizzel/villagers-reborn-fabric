package com.javic.slimpatch.fabric;

import com.javic.slimpatch.client.gui.SlimPatchConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class SlimPatchModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return SlimPatchConfigScreen::new;
    }
}
