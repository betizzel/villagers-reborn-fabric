package com.javic.slimpatch.fabric;

import io.github.fabricators_of_create.porting_lib.entity.extensions.EntityExtensions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

/** Shares the ForgeData compound with Porting Lib and other entity addons. */
public final class PersistentEntityData {
    private PersistentEntityData() {
    }

    public static CompoundTag get(Entity entity) {
        return ((EntityExtensions) entity).getCustomData();
    }
}
