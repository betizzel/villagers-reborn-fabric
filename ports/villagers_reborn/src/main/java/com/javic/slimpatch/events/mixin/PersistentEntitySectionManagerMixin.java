package com.javic.slimpatch.events.mixin;

import com.javic.slimpatch.events.VanillaReplacementHandler;
import com.javic.slimpatch.events.FamilyTreeEventHandler;
import com.javic.slimpatch.entity.NaturalFamilySpawnHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PersistentEntitySectionManager.class)
public abstract class PersistentEntitySectionManagerMixin {
    @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
    private void slimpatch$entityJoin(EntityAccess access, boolean loadedFromDisk, CallbackInfoReturnable<Boolean> cir) {
        if (access instanceof Entity entity && entity.level() instanceof ServerLevel level) {
            if (VanillaReplacementHandler.replaceVanillaEntities(entity, level, loadedFromDisk)) {
                cir.setReturnValue(false);
                return;
            }
            NaturalFamilySpawnHandler.queueFreshMigratedVillagerCandidates(level, entity);
            FamilyTreeEventHandler.onEntityJoinLevel(entity, level);
        }
    }
}
