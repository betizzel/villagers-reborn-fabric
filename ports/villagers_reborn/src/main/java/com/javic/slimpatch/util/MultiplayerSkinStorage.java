package com.javic.slimpatch.util;

import com.javic.slimpatch.config.SlimPatchConfig;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import javax.imageio.ImageIO;
import net.fabricmc.loader.api.FabricLoader;

public final class MultiplayerSkinStorage {
   private static final byte[] PNG_SIGNATURE = new byte[]{-119, 80, 78, 71, 13, 10, 26, 10};

   private MultiplayerSkinStorage() {
   }

   public static boolean isEnabled() {
      return (Boolean)SlimPatchConfig.SERVER.allowMultiplayerCustomSkins.get();
   }

   public static int getMaxSizeKb() {
      return (Integer)SlimPatchConfig.SERVER.maxCustomSkinSizeKb.get();
   }

   public static Path getSkinFile(UUID villagerUuid) {
      return FabricLoader.getInstance().getConfigDir().resolve("slimpatch").resolve("skins").resolve("server").resolve(villagerUuid.toString()).resolve("skin.png");
   }

   public static void saveSkin(UUID villagerUuid, byte[] pngData) throws IOException {
      Path skinFile = getSkinFile(villagerUuid);
      Files.createDirectories(skinFile.getParent());
      Files.write(skinFile, pngData);
   }

   public static byte[] readSkin(UUID villagerUuid) throws IOException {
      Path skinFile = getSkinFile(villagerUuid);
      return Files.exists(skinFile) ? Files.readAllBytes(skinFile) : null;
   }

   public static void deleteSkin(UUID villagerUuid) throws IOException {
      Files.deleteIfExists(getSkinFile(villagerUuid));
   }

   public static String getStoredSkinPath(UUID villagerUuid) {
      return getSkinFile(villagerUuid).toAbsolutePath().toString().replace('\\', '/');
   }

   public static String validatePng(byte[] pngData) {
      if (pngData != null && pngData.length != 0) {
         if (pngData.length > getMaxSizeKb() * 1024) {
            return "Custom skin exceeds the configured size limit.";
         }
         if (pngData.length < PNG_SIGNATURE.length) {
            return "Custom skin is not a valid PNG file.";
         }
         for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (pngData[i] != PNG_SIGNATURE[i]) {
               return "Custom skin is not a valid PNG file.";
            }
         }
         try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(pngData));
            if (image == null) {
               return "Custom skin is not a readable PNG image.";
            }
            return image.getWidth() >= 16 && image.getHeight() >= 16 && image.getWidth() <= 1024 && image.getHeight() <= 1024
               ? null : "Custom skin dimensions are out of range.";
         } catch (IOException e) {
            return "Custom skin is not a readable PNG image.";
         }
      }
      return "Missing skin data.";
   }

   public static String sanitizeSavedInput(String savedSkinInput) {
      if (savedSkinInput != null && !savedSkinInput.isBlank()) {
         String value = savedSkinInput.replace('\\', '/');
         int slash = value.lastIndexOf(47);
         if (slash >= 0) {
            value = value.substring(slash + 1);
         }
         if (value.isBlank()) {
            value = "custom_skin.png";
         }
         return value.length() > 200 ? value.substring(0, 200) : value;
      }
      return "custom_skin.png";
   }
}
