package com.javic.slimpatch;

import com.javic.slimpatch.block.GraveBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
   public static final Block GRAVE = Registry.register(BuiltInRegistries.BLOCK, new ResourceLocation("slimpatch", "grave"),
      new GraveBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5F, 6.0F).sound(SoundType.STONE).noOcclusion()));

   public static void init() { }
   private ModBlocks() { }
}
