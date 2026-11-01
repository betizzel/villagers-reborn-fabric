package com.javic.slimpatch.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Evoker;

public class HumanEvokerModelFemale<T extends Evoker> extends HumanoidModel<T> {
   public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
      new ResourceLocation("slimpatch", "human_evoker_female"), "main"
   );

   public HumanEvokerModelFemale(ModelPart root) {
      super(root);
   }

   public static LayerDefinition createBodyLayer() {
      return FemaleVillagerModel.createBodyLayer();
   }

   public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
      super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
      if (this.body.getChild("belly") != null) {
         this.body.getChild("belly").visible = false;
      }

      if (entity.isCastingSpell()) {
         this.rightArm.xRot = (float)Math.toRadians(-125.0);
         this.rightArm.yRot = (float)Math.toRadians(18.0);
         this.leftArm.xRot = (float)Math.toRadians(-125.0);
         this.leftArm.yRot = (float)Math.toRadians(-18.0);
      }
   }
}
