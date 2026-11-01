package com.javic.slimpatch.client.renderer;

import com.javic.slimpatch.client.model.FemaleZombieVillagerModel;
import com.javic.slimpatch.client.model.MaleZombieVillagerModel;
import com.javic.slimpatch.entity.HumanZombieVillagerEntity;
import com.javic.slimpatch.util.SkinPathHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class HumanZombieVillagerRenderer extends MobRenderer<HumanZombieVillagerEntity, HumanoidModel<HumanZombieVillagerEntity>> {
   private final HumanoidModel<HumanZombieVillagerEntity> maleModel;
   private final HumanoidModel<HumanZombieVillagerEntity> femaleModel;

   public HumanZombieVillagerRenderer(Context context) {
      super(context, new MaleZombieVillagerModel(context.bakeLayer(MaleZombieVillagerModel.LAYER_LOCATION)), 0.5F);
      this.maleModel = new MaleZombieVillagerModel(context.bakeLayer(MaleZombieVillagerModel.LAYER_LOCATION));
      this.femaleModel = new FemaleZombieVillagerModel(context.bakeLayer(FemaleZombieVillagerModel.LAYER_LOCATION));
      this.addLayer(new ItemInHandLayer(this, context.getItemInHandRenderer()));
   }

   public ResourceLocation getTextureLocation(HumanZombieVillagerEntity entity) {
      String gender = entity.getGender();
      if (gender == null || gender.isEmpty()) {
         gender = entity.getRandom().nextBoolean() ? "male" : "female";
         entity.setGender(gender);
      }

      return SkinPathHelper.getSkinForType("human_zombie_villager", gender, 1, entity.level());
   }

   public void render(HumanZombieVillagerEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      boolean isFemale = entity.getGender().equalsIgnoreCase("female");
      if (isFemale) {
         this.model = this.femaleModel;
         poseStack.scale(0.95F, 0.95F, 0.95F);
      } else {
         this.model = this.maleModel;
         poseStack.scale(1.0F, 1.0F, 0.995F);
      }

      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
   }
}
