package com.javic.slimpatch.events;

import com.javic.slimpatch.ModEntities;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;

public final class ModEntityAttributes {
    private ModEntityAttributes() {}

    public static void registerAttributes() {
        AttributeSupplier trader = DefaultAttributes.getSupplier(EntityType.WANDERING_TRADER);
        if (trader != null) {
            FabricDefaultAttributeRegistry.register(ModEntities.HUMAN_WANDERING_TRADER, trader);
            FabricDefaultAttributeRegistry.register(ModEntities.HUMAN_TRADER_NATURAL, trader);
        }
        AttributeSupplier villager = Villager.createAttributes().add(Attributes.ATTACK_DAMAGE, 3.0).build();
        FabricDefaultAttributeRegistry.register(ModEntities.MALE_VILLAGER, villager);
        FabricDefaultAttributeRegistry.register(ModEntities.FEMALE_VILLAGER, villager);
        FabricDefaultAttributeRegistry.register(ModEntities.HUMAN_PILLAGER, Pillager.createAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.HUMAN_VINDICATOR, Vindicator.createAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.HUMAN_EVOKER, Evoker.createAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.HUMAN_ZOMBIE_VILLAGER, ZombieVillager.createAttributes());
    }
}
