package com.javic.slimpatch.client.renderer;

import com.javic.slimpatch.client.model.HumanVindicatorModelFemale;
import com.javic.slimpatch.client.model.HumanVindicatorModelMale;
import com.javic.slimpatch.entity.HumanVindicatorEntity;
import com.javic.slimpatch.util.SkinPathHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Vindicator;

public class HumanVindicatorRenderer extends MobRenderer<Vindicator, HumanoidModel<Vindicator>> {
   private final HumanoidModel<Vindicator> maleModel;
   private final HumanoidModel<Vindicator> femaleModel;

   public HumanVindicatorRenderer(Context context) {
      super(context, new HumanVindicatorModelMale(context.bakeLayer(HumanVindicatorModelMale.LAYER_LOCATION)), 0.5F);
      this.maleModel = new HumanVindicatorModelMale<>(context.bakeLayer(HumanVindicatorModelMale.LAYER_LOCATION));
      this.femaleModel = new HumanVindicatorModelFemale<>(context.bakeLayer(HumanVindicatorModelFemale.LAYER_LOCATION));
      this.addLayer(new ItemInHandLayer(this, context.getItemInHandRenderer()));
   }

   public ResourceLocation getTextureLocation(Vindicator entity) {
      return entity instanceof HumanVindicatorEntity humanVindicator
         ? humanVindicator.getSkinTexture()
         : SkinPathHelper.getSkinForType("human_vindicator", resolveGender(entity), resolveSkinIndex(entity, 8), entity.level());
   }

   public void render(Vindicator entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      boolean female = isFemale(entity);
      this.model = female ? this.femaleModel : this.maleModel;
      poseStack.pushPose();
      poseStack.scale(female ? 0.95F : 1.0F, female ? 0.95F : 1.0F, female ? 0.95F : 1.0F);
      super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
      poseStack.popPose();
   }

   private static boolean isFemale(Vindicator entity) {
      return entity instanceof HumanVindicatorEntity humanVindicator ? humanVindicator.isFemale() : "female".equalsIgnoreCase(resolveGender(entity));
   }

   private static String resolveGender(Vindicator entity) {
      CompoundTag data = com.javic.slimpatch.fabric.PersistentEntityData.get(entity);
      if (!data.contains("slimpatch_gender")) {
         data.putString("slimpatch_gender", (entity.getUUID().getLeastSignificantBits() & 1L) == 0L ? "female" : "male");
      }

      return data.getString("slimpatch_gender");
   }

   private static int resolveSkinIndex(Vindicator entity, int maxSkins) {
      CompoundTag data = com.javic.slimpatch.fabric.PersistentEntityData.get(entity);
      if (!data.contains("slimpatch_skin")) {
         int skinIndex = Math.floorMod(entity.getUUID().hashCode(), maxSkins) + 1;
         data.putInt("slimpatch_skin", skinIndex);
      }

      int skinIndex = data.getInt("slimpatch_skin");
      return skinIndex >= 1 && skinIndex <= maxSkins ? skinIndex : 1;
   }
}
