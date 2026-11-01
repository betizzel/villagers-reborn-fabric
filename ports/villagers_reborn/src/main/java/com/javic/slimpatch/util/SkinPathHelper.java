package com.javic.slimpatch.util;

import com.javic.slimpatch.data.WorldSkinData;
import java.io.File;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class SkinPathHelper {
   public interface ClientAccess {
      ResourceLocation getMountedExternalSkinTexture();
      ResourceLocation loadExternalSkinTexture(UUID entityId, File external);
      String getClientTheme();
      boolean isResourceMissing(ResourceLocation location);
   }

   private static ClientAccess clientAccess;

   public static void setClientAccess(ClientAccess access) {
      clientAccess = access;
   }

   public static ResourceLocation getSkin(String gender, int skinId, Level level) {
      return getSkinForType("custom_villager", gender, skinId, level);
   }

   public static ResourceLocation getSkinForType(String type, String gender, int skinId, Level level) {
      String theme = null;
      boolean useTheme = "custom_villager".equals(type);
      if (useTheme) {
         if (level instanceof ServerLevel serverLevel) {
            theme = WorldSkinData.get(serverLevel).getTheme();
         } else {
            theme = getClientTheme();
         }
         if (theme == null || theme.isEmpty()) {
            theme = "modern";
         }
      }

      try {
         if (level != null && level.isClientSide && clientAccess != null) {
            ResourceLocation mountedSkin = clientAccess.getMountedExternalSkinTexture();
            if (mountedSkin != null) {
               return mountedSkin;
            }
         }
      } catch (Exception ignored) {
      }

      StringBuilder path = new StringBuilder("slimpatch:textures/entity/").append(type).append("/");
      if (useTheme && "fantasy".equalsIgnoreCase(theme)) {
         path.append("fantasy/");
      }
      path.append(gender.toLowerCase()).append("/skin_");
      if (!useTheme && !type.equals("human_trader") && !type.equals("human_witch")) {
         path.append(String.format("%02d", skinId));
      } else {
         path.append(skinId);
      }
      path.append(".png");
      ResourceLocation loc = ResourceLocation.tryParse(path.toString());
      if (loc == null) {
         loc = ResourceLocation.tryParse("slimpatch:textures/entity/" + type + "/" + gender + "/skin_1.png");
      }
      try {
         if (clientAccess != null && clientAccess.isResourceMissing(loc)) {
            int fallback = (int)(Math.random() * 70.0) + 1;
            loc = ResourceLocation.tryParse("slimpatch:textures/entity/custom_villager/modern/" + gender + "/skin_" + fallback + ".png");
         }
      } catch (Exception ignored) {
         int fallback = (int)(Math.random() * 70.0) + 1;
         loc = ResourceLocation.tryParse("slimpatch:textures/entity/custom_villager/modern/" + gender + "/skin_" + fallback + ".png");
      }
      return loc;
   }

   public static ResourceLocation from(String path) {
      return new ResourceLocation("slimpatch", path);
   }

   public static ResourceLocation loadExternalSkinTexture(UUID entityId, File external) {
      if (clientAccess == null) {
         return null;
      }
      try {
         return clientAccess.loadExternalSkinTexture(entityId, external);
      } catch (Exception e) {
         return null;
      }
   }

   private static String getClientTheme() {
      try {
         return clientAccess == null ? null : clientAccess.getClientTheme();
      } catch (Exception e) {
         return null;
      }
   }
}
