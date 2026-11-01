package com.javic.slimpatch.client.cutscene;

import com.javic.slimpatch.client.gui.FamilyCutsceneOverlayScreen;
import com.javic.slimpatch.sounds.HumanVillagerSounds;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.player.Input;

public final class FamilyCutsceneController {
   private static final int DURATION_TICKS = 200;
   private static final int FADE_IN_TICKS = 45;
   private static final int VISIBLE_SCENE_TICKS = 75;
   private static final int FADE_TO_BLACK_TICKS = 70;
   private static final int VANILLA_MUSIC_RESTART_DELAY_TICKS = 100;
   private static final int VANILLA_MUSIC_STOP_INTERVAL_TICKS = 5;
   private static final int SKIP_HINT_TICKS = 70;
   private static boolean active;
   private static int villagerEntityId = -1;
   private static String villagerName = "";
   private static int remainingTicks;
   private static CameraType previousCameraType;
   private static Entity previousCameraEntity;
   private static float previousYRot;
   private static float previousXRot;
   private static float previousYHeadRot;
   private static float previousYBodyRot;
   private static boolean previousHideGui;
   private static WeddingCutsceneMusicInstance musicInstance;
   private static int vanillaMusicRestartDelay;
   private static int vanillaMusicStopCooldown;
   private static int skipHintTicks;

   private FamilyCutsceneController() {
   }

   public static boolean start(int entityId, String targetVillagerName) {
      Minecraft minecraft = Minecraft.getInstance();
      LocalPlayer player = minecraft.player;
      if (player != null && minecraft.level != null && minecraft.level.getEntity(entityId) != null) {
         if (active) {
            finish(false);
         }

         active = true;
         villagerEntityId = entityId;
         villagerName = targetVillagerName;
         remainingTicks = 200;
         vanillaMusicRestartDelay = 0;
         vanillaMusicStopCooldown = 0;
         skipHintTicks = 0;
         WeddingMusicDuckingHandler.keepDuckedForCutscene();
         previousCameraType = minecraft.options.getCameraType();
         previousCameraEntity = minecraft.getCameraEntity();
         previousYRot = player.getYRot();
         previousXRot = player.getXRot();
         previousYHeadRot = player.yHeadRot;
         previousYBodyRot = player.yBodyRot;
         previousHideGui = minecraft.options.hideGui;
         playFamilyMusic(minecraft);
         minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
         minecraft.options.hideGui = true;
         if (minecraft.getCameraEntity() != player) {
            minecraft.setCameraEntity(player);
         }

         minecraft.setScreen(new FamilyCutsceneOverlayScreen());
         return true;
      } else {
         return false;
      }
   }

   public static boolean isActive() {
      return active;
   }

   public static int getVillagerEntityId() {
      return villagerEntityId;
   }

   public static float getProgress() {
      return active ? 1.0F - remainingTicks / 200.0F : 0.0F;
   }

   public static int getOverlayAlpha() {
      if (!active) {
         return 0;
      } else {
         int elapsedTicks = 200 - remainingTicks;
         if (elapsedTicks < 45) {
            float progress = elapsedTicks / 45.0F;
            float eased = 1.0F - progress * progress * (3.0F - 2.0F * progress);
            return (int)(eased * 255.0F);
         } else if (elapsedTicks < 75) {
            return 0;
         } else {
            int fadeTicks = elapsedTicks - 75;
            if (fadeTicks < 70) {
               float progress = fadeTicks / 70.0F;
               float eased = progress * progress * (3.0F - 2.0F * progress);
               return (int)(eased * 255.0F);
            } else {
               return 255;
            }
         }
      }
   }

   public static boolean shouldShowSkipHint() {
      return active && skipHintTicks > 0;
   }

   public static void handleSkipRequest() {
      if (active) {
         if (skipHintTicks > 0) {
            finish(true);
         } else {
            skipHintTicks = 70;
         }
      }
   }

   public static void onClientTick() {
         tickVanillaMusicRestart();
         if (active) {
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer player = minecraft.player;
            if (player == null || minecraft.level == null) {
               finish(false);
            } else if (!player.isAlive()) {
               finish(true);
            } else {
               Entity villager = minecraft.level.getEntity(villagerEntityId);
               if (villager == null || villager.level() != player.level()) {
                  finish(true);
               } else if (minecraft.screen != null && !(minecraft.screen instanceof FamilyCutsceneOverlayScreen)) {
                  finish(true);
               } else {
                  if (skipHintTicks > 0) {
                     skipHintTicks--;
                  }

                  stopVanillaMusicDuringCutscene(minecraft);
                  orientPlayerTowards(player, villager);
                  orientVillagerTowards(player, villager);
                  remainingTicks--;
                  if (remainingTicks <= 0) {
                     finish(true);
                  }
               }
            }
         }
   }

   public static void onMovementInput(Input input) {
      if (active) {
         input.forwardImpulse = 0.0F;
         input.leftImpulse = 0.0F;
         input.up = false;
         input.down = false;
         input.left = false;
         input.right = false;
         input.jumping = false;
         input.shiftKeyDown = false;
      }
   }

   public static void finish(boolean showMessage) {
      if (active) {
         Minecraft minecraft = Minecraft.getInstance();
         if (minecraft.player != null) {
            minecraft.player.setYRot(previousYRot);
            minecraft.player.setXRot(previousXRot);
            minecraft.player.yHeadRot = previousYHeadRot;
            minecraft.player.yBodyRot = previousYBodyRot;
            if (showMessage) {
               minecraft.player.displayClientMessage(Component.translatable("slimpatch.message.family_started", new Object[]{villagerName}), false);
            }
         }

         if (minecraft.options != null && previousCameraType != null) {
            minecraft.options.setCameraType(previousCameraType);
         }

         if (minecraft.options != null) {
            minecraft.options.hideGui = previousHideGui;
         }

         if (previousCameraEntity != null) {
            minecraft.setCameraEntity(previousCameraEntity);
         }

         if (minecraft.screen instanceof FamilyCutsceneOverlayScreen) {
            minecraft.setScreen(null);
         }

         stopFamilyMusic(minecraft);
         WeddingMusicDuckingHandler.restore();
         scheduleVanillaMusicRestart();
         active = false;
         villagerEntityId = -1;
         villagerName = "";
         remainingTicks = 0;
         skipHintTicks = 0;
         previousCameraType = null;
         previousCameraEntity = null;
      }
   }

   private static void stopVanillaMusicDuringCutscene(Minecraft minecraft) {
      if (vanillaMusicStopCooldown > 0) {
         vanillaMusicStopCooldown--;
      } else {
         minecraft.getMusicManager().stopPlaying();
         vanillaMusicStopCooldown = 5;
      }
   }

   private static void scheduleVanillaMusicRestart() {
      vanillaMusicRestartDelay = 100;
   }

   private static void tickVanillaMusicRestart() {
      if (vanillaMusicRestartDelay > 0) {
         vanillaMusicRestartDelay--;
         if (vanillaMusicRestartDelay <= 0) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null && minecraft.level != null && minecraft.options != null) {
               if (!(minecraft.options.getSoundSourceVolume(SoundSource.MUSIC) <= 0.0F)) {
                  Music music = minecraft.getSituationalMusic();
                  if (music != null) {
                     minecraft.getMusicManager().startPlaying(music);
                  }
               }
            }
         }
      }
   }

   private static void playFamilyMusic(Minecraft minecraft) {
      stopFamilyMusic(minecraft);
      if (minecraft.player != null && minecraft.level != null && minecraft.getSoundManager() != null) {
         musicInstance = new WeddingCutsceneMusicInstance(switch (minecraft.level.random.nextInt(4)) {
            case 1 -> HumanVillagerSounds.familyCutscene2();
            case 2 -> HumanVillagerSounds.familyCutscene3();
            case 3 -> HumanVillagerSounds.familyCutscene4();
            default -> HumanVillagerSounds.familyCutscene1();
         });
         minecraft.getSoundManager().play(musicInstance);
      }
   }

   private static void stopFamilyMusic(Minecraft minecraft) {
      if (musicInstance != null && minecraft.getSoundManager() != null) {
         musicInstance.stopPlayback();
         minecraft.getSoundManager().stop(musicInstance);
         musicInstance = null;
      } else {
         musicInstance = null;
      }
   }

   private static void orientPlayerTowards(LocalPlayer player, Entity villager) {
      double dx = villager.getX() - player.getX();
      double dy = villager.getEyeY() - player.getEyeY();
      double dz = villager.getZ() - player.getZ();
      double horizontal = Math.sqrt(dx * dx + dz * dz);
      float targetYaw = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
      float targetPitch = (float)(-(Math.atan2(dy, horizontal) * 180.0 / Math.PI));
      player.setYRot(Mth.rotLerp(0.25F, player.getYRot(), targetYaw));
      player.setXRot(Mth.lerp(0.25F, player.getXRot(), targetPitch * 0.35F));
      player.yHeadRot = player.getYRot();
      player.yBodyRot = player.getYRot();
   }

   private static void orientVillagerTowards(LocalPlayer player, Entity villager) {
      double dx = player.getX() - villager.getX();
      double dz = player.getZ() - villager.getZ();
      float targetYaw = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
      villager.setYRot(Mth.rotLerp(0.3F, villager.getYRot(), targetYaw));
      if (villager instanceof LivingEntity living) {
         living.yHeadRot = villager.getYRot();
         living.yBodyRot = villager.getYRot();
      }
   }
}
