package com.javic.slimpatch.client.renderer;

import com.javic.slimpatch.client.model.FemaleVillagerModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Witch;

public class HumanWitchRenderer extends MobRenderer<Witch, HumanoidModel<Witch>> {
   public HumanWitchRenderer(Context context) {
      super(context, new FemaleVillagerModel(context.bakeLayer(FemaleVillagerModel.LAYER_LOCATION)), 0.5F);
   }

   public ResourceLocation getTextureLocation(Witch entity) {
      CompoundTag tag = com.javic.slimpatch.fabric.PersistentEntityData.get(entity);
      if (!tag.contains("hw_skin")) {
         int skinIndex = entity.getRandom().nextInt(10) + 1;
         tag.putInt("hw_skin", skinIndex);
      }

      int index = tag.getInt("hw_skin");
      return new ResourceLocation("slimpatch", "textures/entity/human_witch/skin_" + String.format("%02d", index) + ".png");
   }

   public void render(Witch entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      poseStack.pushPose();
      poseStack.scale(0.95F, 0.95F, 0.95F);
      poseStack.scale(0.93F, 1.0F, 0.93F);
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
      poseStack.popPose();
   }
}
