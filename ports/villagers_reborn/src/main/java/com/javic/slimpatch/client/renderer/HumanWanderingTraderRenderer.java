package com.javic.slimpatch.client.renderer;

import com.javic.slimpatch.Config;
import com.javic.slimpatch.client.model.HumanWanderingTraderModelFemale;
import com.javic.slimpatch.client.model.HumanWanderingTraderModelMale;
import com.javic.slimpatch.entity.HumanWanderingTraderEntity;
import com.javic.slimpatch.util.SkinPathHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;

public class HumanWanderingTraderRenderer extends MobRenderer<HumanWanderingTraderEntity, HumanoidModel<HumanWanderingTraderEntity>> {
   private final HumanoidModel<HumanWanderingTraderEntity> maleModel = (HumanoidModel<HumanWanderingTraderEntity>)this.getModel();
   private final HumanoidModel<HumanWanderingTraderEntity> femaleModel;

   public HumanWanderingTraderRenderer(Context context) {
      super(context, new HumanWanderingTraderModelMale(context.bakeLayer(HumanWanderingTraderModelMale.LAYER_LOCATION)), 0.5F);
      this.femaleModel = new HumanWanderingTraderModelFemale(context.bakeLayer(HumanWanderingTraderModelFemale.LAYER_LOCATION));
   }

   public ResourceLocation getTextureLocation(HumanWanderingTraderEntity entity) {
      int skinIndex = entity.getSkinIndex();
      if (skinIndex <= 0) {
         skinIndex = 1;
      }

      String genderPath = entity.isFemale() ? "female" : "male";
      return SkinPathHelper.from(String.format("textures/entity/human_trader/%s/skin_%02d.png", genderPath, skinIndex));
   }

   public void render(HumanWanderingTraderEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      this.model = entity.isFemale() ? this.femaleModel : this.maleModel;
      poseStack.pushPose();
      if (entity.isFemale()) {
         poseStack.scale(0.94F, 0.94F, 0.94F);
      } else {
         poseStack.scale(0.98F, 0.98F, 0.98F);
      }

      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
      poseStack.popPose();
   }

   protected boolean shouldShowName(HumanWanderingTraderEntity entity) {
      return (Boolean)Config.VILLAGER_NAME_TAG.get() && super.shouldShowName(entity);
   }
}
