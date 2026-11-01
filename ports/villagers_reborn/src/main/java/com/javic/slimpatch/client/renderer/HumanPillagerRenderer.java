package com.javic.slimpatch.client.renderer;

import com.javic.slimpatch.client.model.HumanPillagerModelFemale;
import com.javic.slimpatch.client.model.HumanPillagerModelMale;
import com.javic.slimpatch.entity.HumanPillagerEntity;
import com.javic.slimpatch.util.SkinPathHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Pillager;

public class HumanPillagerRenderer extends MobRenderer<Pillager, HumanoidModel<Pillager>> {
   private final HumanoidModel<Pillager> maleModel;
   private final HumanoidModel<Pillager> femaleModel;

   public HumanPillagerRenderer(Context context) {
      super(context, new HumanPillagerModelMale(context.bakeLayer(HumanPillagerModelMale.LAYER_LOCATION)), 0.5F);
      this.maleModel = new HumanPillagerModelMale<>(context.bakeLayer(HumanPillagerModelMale.LAYER_LOCATION));
      this.femaleModel = new HumanPillagerModelFemale<>(context.bakeLayer(HumanPillagerModelFemale.LAYER_LOCATION));
      this.addLayer(new ItemInHandLayer(this, context.getItemInHandRenderer()));
   }

   public ResourceLocation getTextureLocation(Pillager entity) {
      return entity instanceof HumanPillagerEntity humanPillager
         ? humanPillager.getSkinTexture()
         : SkinPathHelper.getSkinForType("human_pillager", resolveGender(entity), resolveSkinIndex(entity, 8), entity.level());
   }

   public void render(Pillager entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      boolean female = isFemale(entity);
      this.model = female ? this.femaleModel : this.maleModel;
      poseStack.pushPose();
      if (female) {
         poseStack.scale(0.95F, 0.95F, 0.95F);
      } else {
         poseStack.scale(1.0F, 1.0F, 0.995F);
      }

      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
      poseStack.popPose();
   }

   private static boolean isFemale(Pillager entity) {
      return entity instanceof HumanPillagerEntity humanPillager ? humanPillager.isFemale() : "female".equalsIgnoreCase(resolveGender(entity));
   }

   private static String resolveGender(Pillager entity) {
      CompoundTag data = com.javic.slimpatch.fabric.PersistentEntityData.get(entity);
      if (!data.contains("slimpatch_gender")) {
         data.putString("slimpatch_gender", (entity.getUUID().getLeastSignificantBits() & 1L) == 0L ? "female" : "male");
      }

      return data.getString("slimpatch_gender");
   }

   private static int resolveSkinIndex(Pillager entity, int maxSkins) {
      CompoundTag data = com.javic.slimpatch.fabric.PersistentEntityData.get(entity);
      if (!data.contains("slimpatch_skin")) {
         int skinIndex = Math.floorMod(entity.getUUID().hashCode(), maxSkins) + 1;
         data.putInt("slimpatch_skin", skinIndex);
      }

      int skinIndex = data.getInt("slimpatch_skin");
      return skinIndex >= 1 && skinIndex <= maxSkins ? skinIndex : 1;
   }
}
