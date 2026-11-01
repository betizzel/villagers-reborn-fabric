package com.javic.slimpatch.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.item.Items;

public class HumanPillagerModelFemale<T extends Pillager> extends HumanoidModel<T> {
   public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
      new ResourceLocation("slimpatch", "human_pillager_female"), "main"
   );

   public HumanPillagerModelFemale(ModelPart root) {
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

      boolean hasCrossbowRight = entity.getMainHandItem().is(Items.CROSSBOW);
      boolean hasCrossbowLeft = entity.getOffhandItem().is(Items.CROSSBOW);
      boolean isCharging = entity.isChargingCrossbow();
      boolean hasTarget = entity.getTarget() != null;
      if (isCharging) {
         this.rightArm.xRot = (float)Math.toRadians(-60.0);
         this.rightArm.yRot = 0.0F;
         this.rightArm.zRot = 0.0F;
         this.leftArm.xRot = (float)Math.toRadians(-30.0);
         this.leftArm.yRot = (float)Math.toRadians(-12.0);
         this.leftArm.zRot = (float)Math.toRadians(8.0);
      } else if (hasCrossbowRight && hasTarget) {
         this.rightArm.xRot = (float)Math.toRadians(-85.0);
         this.rightArm.yRot = 0.0F;
         this.leftArm.xRot = (float)Math.toRadians(-28.0);
         this.leftArm.yRot = (float)Math.toRadians(12.0);
      } else if (hasCrossbowLeft && hasTarget) {
         this.leftArm.xRot = (float)Math.toRadians(-85.0);
         this.leftArm.yRot = 0.0F;
         this.rightArm.xRot = (float)Math.toRadians(-28.0);
         this.rightArm.yRot = (float)Math.toRadians(-12.0);
      } else {
         this.rightArm.xRot = (float)Math.toRadians(-7.0);
         this.rightArm.yRot = 0.0F;
         this.leftArm.xRot = (float)Math.toRadians(-7.0);
         this.leftArm.yRot = 0.0F;
      }
   }
}
