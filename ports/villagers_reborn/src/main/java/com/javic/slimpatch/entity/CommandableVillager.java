package com.javic.slimpatch.entity;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public interface CommandableVillager {
   VillagerCombatMode getCombatMode();

   void setCombatMode(VillagerCombatMode var1);

   VillagerFollowMode getFollowMode();

   void setFollowMode(VillagerFollowMode var1);

   boolean isArmorHidden();

   void setArmorHidden(boolean var1);

   boolean isMuted();

   void setMuted(boolean var1);

   VillagerCommandState getCommandState();

   void setCommandState(VillagerCommandState var1);

   UUID getCommandTargetUuid();

   void setCommandTargetUuid(UUID var1);

   UUID getCommandOwnerUuid();

   void setCommandOwnerUuid(UUID var1);

   String getCommandOwnerName();

   void setCommandOwnerName(String var1);

   BlockPos getStayPos();

   void setStayPos(BlockPos var1);

   Vec3 getStayAnchorPos();

   void setStayAnchorPos(Vec3 var1);

   boolean hasHome();

   void setHome(BlockPos var1, ResourceKey<Level> var2);

   void clearHome();

   BlockPos getHomePos();

   ResourceKey<Level> getHomeDimension();

   long getLastCommandTeleportTick();

   void setLastCommandTeleportTick(long var1);

   int getCombatLockTicks();

   void setCombatLockTicks(int var1);

   int getLastDamageTick();

   void setLastDamageTick(int var1);

   int getLastAggressiveScanTick();

   void setLastAggressiveScanTick(int var1);

   int getLastBowAttackTick();

   void setLastBowAttackTick(int var1);

   int getBowChargeStartTick();

   void setBowChargeStartTick(int var1);

   int getLastCrossbowAttackTick();

   void setLastCrossbowAttackTick(int var1);

   int getCrossbowChargeStartTick();

   void setCrossbowChargeStartTick(int var1);

   int getLastShieldBlockTick();

   void setLastShieldBlockTick(int var1);

   int getShieldRaiseUntilTick();

   void setShieldRaiseUntilTick(int var1);

   float getBonusHealth();

   void setBonusHealth(float var1);

   double getBaseMaxHealth();

   void setBaseMaxHealth(double var1);
}
