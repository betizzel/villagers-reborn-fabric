package com.javic.slimpatch.client.renderer;

import com.javic.slimpatch.client.model.CustomVillagerModelMale;
import com.javic.slimpatch.entity.MaleVillagerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;

public class MaleVillagerRenderer extends AbstractVillagerHumanoidRenderer<MaleVillagerEntity> {
   public MaleVillagerRenderer(Context context) {
      super(context, new CustomVillagerModelMale(context.bakeLayer(CustomVillagerModelMale.LAYER_LOCATION)));
   }

   protected String getCustomSkinPath(MaleVillagerEntity entity) {
      return entity.getCustomSkinPath();
   }

   protected ResourceLocation getFallbackTextureLocation(MaleVillagerEntity entity) {
      return entity.getSkinTexture();
   }

   protected float getHorseMountedOffset(MaleVillagerEntity entity) {
      return -0.42F;
   }

   protected float getCamelMountedOffset(MaleVillagerEntity entity) {
      return -0.32F;
   }

   protected void applyModelScale(MaleVillagerEntity entity, PoseStack poseStack) {
      float widthScale = entity.getVisualWidth() / 100.0F;
      float heightScale = entity.getVisualHeight() / 100.0F;
      float ageStageScale = this.getAgeStageScale(entity);
      poseStack.scale(1.0F, 1.0F, 0.995F);
      poseStack.scale(widthScale, heightScale, widthScale);
      poseStack.scale(ageStageScale, ageStageScale, ageStageScale);
   }
}
