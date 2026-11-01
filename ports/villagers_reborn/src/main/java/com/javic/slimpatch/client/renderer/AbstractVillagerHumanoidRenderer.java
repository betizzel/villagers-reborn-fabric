package com.javic.slimpatch.client.renderer;

import com.javic.slimpatch.Config;
import com.javic.slimpatch.entity.CommandableVillager;
import com.javic.slimpatch.entity.FamilyVillager;
import com.javic.slimpatch.entity.VillagerAgeStage;
import com.javic.slimpatch.util.SkinPathHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import java.io.File;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.horse.Horse;

abstract class AbstractVillagerHumanoidRenderer<T extends Mob & CommandableVillager> extends MobRenderer<T, HumanoidModel<T>> {
   protected static final float TODDLER_MODEL_SCALE = 0.6F;
   protected static final float CHILD_MODEL_SCALE = 0.75F;
   protected static final float TEEN_MODEL_SCALE = 0.9F;

   protected AbstractVillagerHumanoidRenderer(Context context, HumanoidModel<T> model) {
      super(context, model, 0.5F);
      this.addLayer(new ItemInHandLayer(this, context.getItemInHandRenderer()));
      this.addLayer(
         new HumanoidArmorLayer<T, HumanoidModel<T>, HumanoidModel<T>>(
            this,
            new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
            new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
            context.getModelManager()
         ) {
            public void render(
               PoseStack poseStack,
               MultiBufferSource buffer,
               int packedLight,
               T livingEntity,
               float limbSwing,
               float limbSwingAmount,
               float partialTicks,
               float ageInTicks,
               float netHeadYaw,
               float headPitch
            ) {
               if (!livingEntity.isArmorHidden()) {
                  super.render(poseStack, buffer, packedLight, livingEntity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch);
               }
            }
         }
      );
   }

   public ResourceLocation getTextureLocation(T entity) {
      String path = this.getCustomSkinPath(entity);
      if (path != null && !path.isEmpty()) {
         try {
            File file = new File(path);
            if (file.exists()) {
               ResourceLocation location = SkinPathHelper.loadExternalSkinTexture(entity.getUUID(), file);
               if (location != null) {
                  return location;
               }
            }
         } catch (Exception var5) {
         }
      }

      return this.getFallbackTextureLocation(entity);
   }

   public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      poseStack.pushPose();
      if (entity.getVehicle() instanceof Horse) {
         poseStack.translate(0.0F, this.getHorseMountedOffset(entity), 0.0F);
      } else if (entity.getVehicle() instanceof Camel) {
         poseStack.translate(0.0F, this.getCamelMountedOffset(entity), 0.0F);
      }

      this.applyModelScale(entity, poseStack);
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
      poseStack.popPose();
   }

   protected boolean shouldShowName(T entity) {
      return (Boolean)Config.VILLAGER_NAME_TAG.get() && super.shouldShowName(entity);
   }

   protected abstract String getCustomSkinPath(T var1);

   protected abstract ResourceLocation getFallbackTextureLocation(T var1);

   protected abstract float getHorseMountedOffset(T var1);

   protected abstract float getCamelMountedOffset(T var1);

   protected abstract void applyModelScale(T var1, PoseStack var2);

   protected VillagerAgeStage getAgeStage(T entity) {
      return entity instanceof FamilyVillager familyVillager ? familyVillager.getAgeStage() : VillagerAgeStage.ADULT;
   }

   protected float getAgeStageScale(T entity) {
      return switch (this.getAgeStage(entity)) {
         case TODDLER -> 0.6F;
         case CHILD -> 0.75F;
         case TEEN -> 0.9F;
         case ADULT -> 1.0F;
      };
   }
}
