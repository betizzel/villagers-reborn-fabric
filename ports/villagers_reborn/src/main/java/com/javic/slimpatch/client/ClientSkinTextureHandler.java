package com.javic.slimpatch.client;

import com.javic.slimpatch.entity.FemaleVillagerEntity;
import com.javic.slimpatch.entity.MaleVillagerEntity;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;

public final class ClientSkinTextureHandler {
   private ClientSkinTextureHandler() {
   }

   public static void applySyncedSkin(int entityId, UUID villagerUuid, String savedSkinInput, byte[] pngData, boolean clear) throws IOException {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level != null) {
         Entity entity = minecraft.level.getEntity(entityId);
         if (entity != null) {
            String cachedPath = "";
            if (clear) {
               ClientMultiplayerSkinCache.clearSkin(villagerUuid);
            } else {
               cachedPath = ClientMultiplayerSkinCache.storeSkin(villagerUuid, pngData);
            }

            if (entity instanceof MaleVillagerEntity maleVillager) {
               maleVillager.setSavedSkinInput(savedSkinInput);
               maleVillager.setCustomSkinPath(cachedPath);
            } else if (entity instanceof FemaleVillagerEntity femaleVillager) {
               femaleVillager.setSavedSkinInput(savedSkinInput);
               femaleVillager.setCustomSkinPath(cachedPath);
            } else if (entity instanceof Villager villager) {
               if (savedSkinInput != null && !savedSkinInput.isEmpty()) {
                  com.javic.slimpatch.fabric.PersistentEntityData.get(villager).putString("SavedSkinInput", savedSkinInput);
               } else {
                  com.javic.slimpatch.fabric.PersistentEntityData.get(villager).remove("SavedSkinInput");
               }

               if (cachedPath.isEmpty()) {
                  com.javic.slimpatch.fabric.PersistentEntityData.get(villager).remove("CustomSkinPath");
               } else {
                  com.javic.slimpatch.fabric.PersistentEntityData.get(villager).putString("CustomSkinPath", cachedPath);
               }
            }
         }
      }
   }

   public static ResourceLocation loadExternalSkinTexture(UUID entityId, File external) {
      try {
         ResourceLocation location = new ResourceLocation("slimpatch_dyn", "skins/" + entityId + "/" + external.getName());
         Minecraft minecraft = Minecraft.getInstance();
         AbstractTexture texture = minecraft.getTextureManager().getTexture(location, null);
         if (texture == null) {
            try (FileInputStream inputStream = new FileInputStream(external)) {
               minecraft.getTextureManager().register(location, new DynamicTexture(NativeImage.read(inputStream)));
            }
         }

         return location;
      } catch (Exception e) {
         return null;
      }
   }

   public static ResourceLocation getMountedExternalSkinTexture() {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player == null) {
         return null;
      }

      Entity entity = minecraft.player.getVehicle();
      if (entity == null) {
         return null;
      }

      File file = new File(minecraft.gameDirectory, "config/slimpatch/skins/" + entity.getUUID() + "/skin.png");
      return file.exists() && file.isFile() ? loadExternalSkinTexture(entity.getUUID(), file) : null;
   }

   public static boolean isResourceMissing(ResourceLocation location) {
      try {
         return Minecraft.getInstance().getResourceManager().getResource(location).isEmpty();
      } catch (Exception e) {
         return false;
      }
   }

   public static String getClientTheme() {
      return ClientSkinTheme.getTheme();
   }
}
