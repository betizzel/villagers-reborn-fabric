package com.javic.slimpatch.client.gui;

import com.javic.slimpatch.entity.VillagerPersonality;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class VillagerPersonalityIcons {
   public static final Map<VillagerPersonality, String> NAMES = Map.ofEntries(
      Map.entry(VillagerPersonality.FRIENDLY, "slimpatch.personality.friendly"),
      Map.entry(VillagerPersonality.MEAN, "slimpatch.personality.mean"),
      Map.entry(VillagerPersonality.SHY, "slimpatch.personality.shy"),
      Map.entry(VillagerPersonality.BRAVE, "slimpatch.personality.brave"),
      Map.entry(VillagerPersonality.GRUMPY, "slimpatch.personality.grumpy"),
      Map.entry(VillagerPersonality.GREEDY, "slimpatch.personality.greedy"),
      Map.entry(VillagerPersonality.ROMANTIC, "slimpatch.personality.romantic"),
      Map.entry(VillagerPersonality.WISE, "slimpatch.personality.wise")
   );
   public static final Map<VillagerPersonality, ResourceLocation> ICONS = Map.ofEntries(
      Map.entry(VillagerPersonality.FRIENDLY, new ResourceLocation("slimpatch", "textures/gui/personalities/personality_friendly.png")),
      Map.entry(VillagerPersonality.MEAN, new ResourceLocation("slimpatch", "textures/gui/personalities/personality_mean.png")),
      Map.entry(VillagerPersonality.SHY, new ResourceLocation("slimpatch", "textures/gui/personalities/personality_shy.png")),
      Map.entry(VillagerPersonality.BRAVE, new ResourceLocation("slimpatch", "textures/gui/personalities/personality_brave.png")),
      Map.entry(VillagerPersonality.GRUMPY, new ResourceLocation("slimpatch", "textures/gui/personalities/personality_grumpy.png")),
      Map.entry(VillagerPersonality.GREEDY, new ResourceLocation("slimpatch", "textures/gui/personalities/personality_greedy.png")),
      Map.entry(VillagerPersonality.ROMANTIC, new ResourceLocation("slimpatch", "textures/gui/personalities/personality_romantic.png")),
      Map.entry(VillagerPersonality.WISE, new ResourceLocation("slimpatch", "textures/gui/personalities/personality_wise.png"))
   );

   private VillagerPersonalityIcons() {
   }

   public static String getName(VillagerPersonality personality) {
      return Component.translatable(NAMES.get(personality)).getString();
   }

   public static ResourceLocation getIcon(VillagerPersonality personality) {
      return ICONS.get(personality);
   }
}
