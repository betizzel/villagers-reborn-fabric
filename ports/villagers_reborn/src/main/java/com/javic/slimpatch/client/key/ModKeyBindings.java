package com.javic.slimpatch.client.key;

import com.mojang.blaze3d.platform.InputConstants.Type;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class ModKeyBindings {
    public static KeyMapping OPEN_DIALOGUE;

    private ModKeyBindings() {
    }

    public static Component getOpenDialogueKeyMessage() {
        return OPEN_DIALOGUE != null ? OPEN_DIALOGUE.getTranslatedKeyMessage() : Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_J).getDisplayName();
    }

    public static void register() {
        OPEN_DIALOGUE = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.slimpatch.open_dialogue", Type.KEYSYM, GLFW.GLFW_KEY_J, "key.categories.slimpatch"));
    }
}
