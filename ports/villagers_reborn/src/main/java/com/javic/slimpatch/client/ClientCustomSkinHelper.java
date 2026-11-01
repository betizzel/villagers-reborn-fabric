package com.javic.slimpatch.client;

import com.javic.slimpatch.config.SlimPatchConfig;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

public final class ClientCustomSkinHelper {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final byte[] PNG_SIGNATURE = new byte[]{-119, 80, 78, 71, 13, 10, 26, 10};

   private ClientCustomSkinHelper() {
   }

   private static boolean isPngData(byte[] data) {
      if (data != null && data.length >= PNG_SIGNATURE.length) {
         for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (data[i] != PNG_SIGNATURE[i]) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static byte[] downloadUrlBytes(String url, int maxBytes) {
      try (InputStream in = URI.create(url).toURL().openStream()) {
         byte[] data = in.readNBytes(maxBytes + 1);
         if (data.length > maxBytes) {
            LOGGER.warn("Custom skin download exceeded max size: url='{}', size={}, maxBytes={}", new Object[]{url, data.length, maxBytes});
            return null;
         } else {
            return data;
         }
      } catch (Exception e) {
         LOGGER.error("Failed to download custom skin from url='{}'", url, e);
         return null;
      }
   }

   private static Path resolveLocalSkinPath(String skinInput, File skinFolder) {
      Path directSkinFile = Path.of(skinInput);
      if (Files.exists(directSkinFile) && Files.isRegularFile(directSkinFile)) {
         return directSkinFile;
      }

      Path villagerSkinFile = new File(skinFolder, skinInput).toPath();
      if (Files.exists(villagerSkinFile) && Files.isRegularFile(villagerSkinFile)) {
         return villagerSkinFile;
      }

      if (skinInput.toLowerCase(Locale.ROOT).endsWith(".png")) {
         return null;
      }

      Path directPngSkinFile = Path.of(skinInput + ".png");
      if (Files.exists(directPngSkinFile) && Files.isRegularFile(directPngSkinFile)) {
         return directPngSkinFile;
      }

      Path villagerPngSkinFile = new File(skinFolder, skinInput + ".png").toPath();
      return Files.exists(villagerPngSkinFile) && Files.isRegularFile(villagerPngSkinFile) ? villagerPngSkinFile : null;
   }

   public static boolean allowLocalCustomSkins() {
      Minecraft mc = Minecraft.getInstance();
      return mc.getSingleplayerServer() != null && !mc.getSingleplayerServer().isPublished();
   }

   public static boolean canUploadMultiplayerCustomSkin() {
      Minecraft mc = Minecraft.getInstance();
      return mc.getSingleplayerServer() != null && mc.getSingleplayerServer().isPublished()
         ? (Boolean)SlimPatchConfig.SERVER.allowMultiplayerCustomSkins.get()
         : ClientMultiplayerSkinSettings.isAllowed();
   }

   public static byte[] readSkinBytes(String skinInput, UUID ownerUuid) {
      return readSkinBytes(skinInput);
   }

   public static byte[] readSkinBytes(String skinInput) {
      try {
         String trimmedSkinInput = skinInput == null ? "" : skinInput.trim();
         if (trimmedSkinInput.isEmpty()) {
            return null;
         }

         int maxBytes = ClientMultiplayerSkinSettings.getMaxCustomSkinSizeKb() * 1024;
         if (!trimmedSkinInput.startsWith("http://") && !trimmedSkinInput.startsWith("https://")) {
            File skinFolder = new File(Minecraft.getInstance().gameDirectory, "config/slimpatch/skins/" + Minecraft.getInstance().player.getUUID());
            Path skinFile = resolveLocalSkinPath(trimmedSkinInput, skinFolder);
            if (skinFile == null) {
               return null;
            }

            if (Files.size(skinFile) > maxBytes) {
               return null;
            }

            byte[] data = Files.readAllBytes(skinFile);
            return isPngData(data) ? data : null;
         } else {
            byte[] data = downloadUrlBytes(trimmedSkinInput, maxBytes);
            return data != null && isPngData(data) ? data : null;
         }
      } catch (Exception e) {
         return null;
      }
   }

   public static String storeLocalSkinInput(UUID ownerUuid, String skinInput) {
      try {
         String trimmedInput = skinInput == null ? "" : skinInput.trim();
         if (trimmedInput.isEmpty()) {
            return "";
         }

         Minecraft minecraft = Minecraft.getInstance();
         File skinFolder = new File(minecraft.gameDirectory, "config/slimpatch/skins/" + ownerUuid);
         if (!skinFolder.exists()) {
            skinFolder.mkdirs();
         }

         if (!trimmedInput.startsWith("http://") && !trimmedInput.startsWith("https://")) {
            Path localSkinPath = resolveLocalSkinPath(trimmedInput, skinFolder);
            if (localSkinPath == null) {
               return "";
            }

            byte[] data = Files.readAllBytes(localSkinPath);
            return !isPngData(data) ? "" : localSkinPath.toAbsolutePath().toString().replace('\\', '/');
         } else {
            File skinFile = new File(skinFolder, ownerUuid + ".png");

            try (InputStream inputStream = URI.create(trimmedInput).toURL().openStream()) {
               Files.copy(inputStream, skinFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            if (skinFile.exists() && skinFile.isFile()) {
               byte[] data = Files.readAllBytes(skinFile.toPath());
               if (!isPngData(data)) {
                  Files.deleteIfExists(skinFile.toPath());
                  LOGGER.warn("Downloaded custom skin was not a valid PNG: url='{}', target='{}'", trimmedInput, skinFile.getAbsolutePath());
                  return "";
               } else {
                  return skinFile.exists() && skinFile.isFile() ? skinFile.getAbsolutePath().replace('\\', '/') : "";
               }
            } else {
               return "";
            }
         }
      } catch (Exception e) {
         LOGGER.error("Failed to store local custom skin: ownerUuid={}, skinInput='{}'", new Object[]{ownerUuid, skinInput, e});
         return "";
      }
   }

   public static String getPendingBirthSkinKey(int spouseEntityId) {
      Minecraft minecraft = Minecraft.getInstance();
      UUID playerUuid = minecraft.player != null ? minecraft.player.getUUID() : new UUID(0L, 0L);
      return "birth_" + playerUuid + "_" + spouseEntityId;
   }

   public static File getPendingBirthSkinFolder(String pendingKey) {
      return FabricLoader.getInstance().getConfigDir().resolve("slimpatch").resolve("skins").resolve(pendingKey).toFile();
   }

   public static void openPendingBirthSkinFolder(String pendingKey) {
      try {
         File folder = getPendingBirthSkinFolder(pendingKey);
         if (!folder.exists()) {
            folder.mkdirs();
         }

         Util.getPlatform().openFile(folder);
      } catch (Exception var2) {
      }
   }

   public static String resolvePendingBirthSkinPath(String pendingKey, String skinInput) {
      try {
         String trimmedSkinInput = skinInput == null ? "" : skinInput.trim();
         if (trimmedSkinInput.isEmpty()) {
            return "";
         }

         File folder = getPendingBirthSkinFolder(pendingKey);
         if (!folder.exists()) {
            folder.mkdirs();
         }

         if (!trimmedSkinInput.startsWith("http://") && !trimmedSkinInput.startsWith("https://")) {
            Path localSkinPath = resolveLocalSkinPath(trimmedSkinInput, folder);
            return localSkinPath != null ? localSkinPath.toAbsolutePath().toString().replace('\\', '/') : "";
         }

         File skinFile = new File(folder, pendingKey + ".png");

         try (InputStream inputStream = URI.create(trimmedSkinInput).toURL().openStream()) {
            Files.copy(inputStream, skinFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
         }

         return skinFile.getAbsolutePath().replace('\\', '/');
      } catch (Exception e) {
         return "";
      }
   }

   public static byte[] readPendingBirthSkinBytes(String pendingKey, String skinInput) {
      try {
         String resolvedPath = resolvePendingBirthSkinPath(pendingKey, skinInput);
         if (resolvedPath.isEmpty()) {
            return null;
         } else {
            Path path = Path.of(resolvedPath);
            int maxBytes = ClientMultiplayerSkinSettings.getMaxCustomSkinSizeKb() * 1024;
            if (Files.exists(path) && Files.isRegularFile(path) && Files.size(path) <= maxBytes) {
               byte[] data = Files.readAllBytes(path);
               return isPngData(data) ? data : null;
            } else {
               return null;
            }
         }
      } catch (Exception e) {
         return null;
      }
   }

   public static void openVillagerSkinFolder(UUID ownerUuid) {
      try {
         File folder = new File(Minecraft.getInstance().gameDirectory, "config/slimpatch/skins/" + ownerUuid);
         if (!folder.exists()) {
            folder.mkdirs();
         }

         Util.getPlatform().openFile(folder);
      } catch (Exception var2) {
      }
   }
}
