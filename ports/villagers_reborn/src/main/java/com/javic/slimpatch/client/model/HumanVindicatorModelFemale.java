package com.javic.slimpatch.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.item.Items;

public class HumanVindicatorModelFemale<T extends Vindicator> extends HumanoidModel<T> {
   public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
      new ResourceLocation("slimpatch", "human_vindicator_female"), "main"
   );

   public HumanVindicatorModelFemale(ModelPart root) {
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

      if (entity.isAggressive() && entity.isHolding(Items.IRON_AXE)) {
         this.rightArm.xRot = (float)Math.toRadians(-95.0);
         this.rightArm.yRot = (float)Math.toRadians(5.0);
         this.leftArm.xRot = (float)Math.toRadians(-25.0);
         this.leftArm.yRot = (float)Math.toRadians(-8.0);
      }
   }
}
