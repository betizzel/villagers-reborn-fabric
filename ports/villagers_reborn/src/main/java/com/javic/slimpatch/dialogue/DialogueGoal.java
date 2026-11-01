package com.javic.slimpatch.dialogue;

import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;

public class DialogueGoal extends Goal {
   private final Villager villager;

   public DialogueGoal(Villager villager) {
      this.villager = villager;
      this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
   }

   public boolean canUse() { return DialogueManager.isInDialogue(this.villager); }
   public boolean canContinueToUse() { return DialogueManager.isInDialogue(this.villager); }
   public void start() { this.villager.getNavigation().stop(); }

   public void tick() {
      UUID playerId = DialogueManager.getDialoguePlayer(this.villager);
      Player player = playerId == null ? null : this.villager.level().getPlayerByUUID(playerId);
      if (player != null) {
         this.villager.getNavigation().stop();
         this.villager.getLookControl().setLookAt(player, 30.0F, 30.0F);
      }
   }

   public void stop() { this.villager.getNavigation().stop(); }
}
