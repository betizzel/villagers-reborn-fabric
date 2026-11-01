package com.javic.slimpatch;

import com.javic.slimpatch.blockentity.GraveBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
   public static final BlockEntityType<GraveBlockEntity> GRAVE = Registry.register(
      BuiltInRegistries.BLOCK_ENTITY_TYPE, new ResourceLocation("slimpatch", "grave"),
      BlockEntityType.Builder.of(GraveBlockEntity::new, ModBlocks.GRAVE).build(null));

   public static void init() { }
   private ModBlockEntities() { }
}
