package com.javic.slimpatch.client;

import com.javic.slimpatch.Config;
import com.javic.slimpatch.ModEntities;
import com.javic.slimpatch.ModMenus;
import com.javic.slimpatch.client.gui.VillagerEquipmentScreen;
import com.javic.slimpatch.client.model.CustomVillagerModelMale;
import com.javic.slimpatch.client.model.FemaleVillagerModel;
import com.javic.slimpatch.client.model.FemaleZombieVillagerModel;
import com.javic.slimpatch.client.model.HumanEvokerModelFemale;
import com.javic.slimpatch.client.model.HumanEvokerModelMale;
import com.javic.slimpatch.client.model.HumanPillagerModelFemale;
import com.javic.slimpatch.client.model.HumanPillagerModelMale;
import com.javic.slimpatch.client.model.HumanVindicatorModelFemale;
import com.javic.slimpatch.client.model.HumanVindicatorModelMale;
import com.javic.slimpatch.client.model.HumanWanderingTraderModelFemale;
import com.javic.slimpatch.client.model.HumanWanderingTraderModelMale;
import com.javic.slimpatch.client.model.MaleZombieVillagerModel;
import com.javic.slimpatch.client.renderer.FemaleVillagerRenderer;
import com.javic.slimpatch.client.renderer.HumanEvokerRenderer;
import com.javic.slimpatch.client.renderer.HumanPillagerRenderer;
import com.javic.slimpatch.client.renderer.HumanVindicatorRenderer;
import com.javic.slimpatch.client.renderer.HumanWanderingTraderRenderer;
import com.javic.slimpatch.client.renderer.HumanWitchRenderer;
import com.javic.slimpatch.client.renderer.HumanZombieVillagerRenderer;
import com.javic.slimpatch.client.renderer.MaleVillagerRenderer;
import com.javic.slimpatch.item.ModItems;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.EvokerRenderer;
import net.minecraft.client.renderer.entity.PillagerRenderer;
import net.minecraft.client.renderer.entity.VindicatorRenderer;
import net.minecraft.client.renderer.entity.WitchRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.ItemLike;

public final class ClientRenderers {
   private ClientRenderers() {
   }

   public static void register() {
      registerRenderers();
      registerLayerDefinitions();
      MenuScreens.register(ModMenus.VILLAGER_EQUIPMENT, VillagerEquipmentScreen::new);
      net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.ITEM.register((stack, layer) -> -1,
         ModItems.MALE_VILLAGER_SPAWN_EGG, ModItems.FEMALE_VILLAGER_SPAWN_EGG);
   }

   private static void registerRenderers() {
      net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(ModEntities.MALE_VILLAGER, MaleVillagerRenderer::new);
      net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(ModEntities.FEMALE_VILLAGER, FemaleVillagerRenderer::new);
      net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(ModEntities.HUMAN_WANDERING_TRADER, HumanWanderingTraderRenderer::new);
      net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(ModEntities.HUMAN_TRADER_NATURAL, HumanWanderingTraderRenderer::new);
      net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(ModEntities.HUMAN_PILLAGER, Config.CUSTOM_ILLAGER_SKINS.get() && Config.CUSTOM_PILLAGER_MODEL.get() ? HumanPillagerRenderer::new : PillagerRenderer::new);
      net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(ModEntities.HUMAN_VINDICATOR, Config.CUSTOM_ILLAGER_SKINS.get() && Config.CUSTOM_VINDICATOR_MODEL.get() ? HumanVindicatorRenderer::new : VindicatorRenderer::new);
      net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(ModEntities.HUMAN_EVOKER, Config.CUSTOM_ILLAGER_SKINS.get() && Config.CUSTOM_EVOKER_MODEL.get() ? HumanEvokerRenderer::new : EvokerRenderer::new);
      net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(EntityType.PILLAGER, Config.CUSTOM_ILLAGER_SKINS.get() && Config.CUSTOM_PILLAGER_MODEL.get() ? HumanPillagerRenderer::new : PillagerRenderer::new);
      net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(EntityType.VINDICATOR,
      Config.CUSTOM_ILLAGER_SKINS.get() && Config.CUSTOM_VINDICATOR_MODEL.get() ? HumanVindicatorRenderer::new : VindicatorRenderer::new);
      net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(EntityType.EVOKER, Config.CUSTOM_ILLAGER_SKINS.get() && Config.CUSTOM_EVOKER_MODEL.get() ? HumanEvokerRenderer::new : EvokerRenderer::new);
      net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(EntityType.WITCH, Config.CUSTOM_WITCH_SKIN.get() ? HumanWitchRenderer::new : WitchRenderer::new);
      net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(ModEntities.HUMAN_ZOMBIE_VILLAGER, HumanZombieVillagerRenderer::new);
   }

   private static void registerLayerDefinitions() {
      net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(CustomVillagerModelMale.LAYER_LOCATION, CustomVillagerModelMale::createBodyLayer);
      net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(FemaleVillagerModel.LAYER_LOCATION, FemaleVillagerModel::createBodyLayer);
      net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(HumanWanderingTraderModelMale.LAYER_LOCATION, HumanWanderingTraderModelMale::createBodyLayer);
      net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(HumanWanderingTraderModelFemale.LAYER_LOCATION, HumanWanderingTraderModelFemale::createBodyLayer);
      net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(HumanPillagerModelMale.LAYER_LOCATION, HumanPillagerModelMale::createBodyLayer);
      net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(HumanPillagerModelFemale.LAYER_LOCATION, HumanPillagerModelFemale::createBodyLayer);
      net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(HumanVindicatorModelMale.LAYER_LOCATION, HumanVindicatorModelMale::createBodyLayer);
      net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(HumanVindicatorModelFemale.LAYER_LOCATION, HumanVindicatorModelFemale::createBodyLayer);
      net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(HumanEvokerModelMale.LAYER_LOCATION, HumanEvokerModelMale::createBodyLayer);
      net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(HumanEvokerModelFemale.LAYER_LOCATION, HumanEvokerModelFemale::createBodyLayer);
      net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(MaleZombieVillagerModel.LAYER_LOCATION, MaleZombieVillagerModel::createBodyLayer);
      net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(FemaleZombieVillagerModel.LAYER_LOCATION, FemaleZombieVillagerModel::createBodyLayer);
   }

}
