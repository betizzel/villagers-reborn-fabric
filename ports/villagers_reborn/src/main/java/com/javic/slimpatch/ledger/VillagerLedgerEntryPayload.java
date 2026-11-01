package com.javic.slimpatch.ledger;

import com.javic.slimpatch.familytree.FamilyTreeNodePayload;
import net.minecraft.network.FriendlyByteBuf;

public class VillagerLedgerEntryPayload {
   private final FamilyTreeNodePayload node;
   private final VillagerLedgerEntryPayload.EntryType entryType;
   private final boolean hasHealth;
   private final float health;
   private final float maxHealth;
   private final VillagerLedgerEntryPayload.HomeState homeState;

   public VillagerLedgerEntryPayload(
      FamilyTreeNodePayload node,
      VillagerLedgerEntryPayload.EntryType entryType,
      boolean hasHealth,
      float health,
      float maxHealth,
      VillagerLedgerEntryPayload.HomeState homeState
   ) {
      this.node = node;
      this.entryType = entryType == null ? VillagerLedgerEntryPayload.EntryType.FAMILY : entryType;
      this.hasHealth = hasHealth;
      this.health = health;
      this.maxHealth = maxHealth;
      this.homeState = homeState == null ? VillagerLedgerEntryPayload.HomeState.UNKNOWN : homeState;
   }

   public FamilyTreeNodePayload getNode() {
      return this.node;
   }

   public VillagerLedgerEntryPayload.EntryType getEntryType() {
      return this.entryType;
   }

   public boolean hasHealth() {
      return this.hasHealth;
   }

   public float getHealth() {
      return this.health;
   }

   public float getMaxHealth() {
      return this.maxHealth;
   }

   public VillagerLedgerEntryPayload.HomeState getHomeState() {
      return this.homeState;
   }

   public void encode(FriendlyByteBuf buf) {
      this.node.encode(buf);
      buf.writeEnum(this.entryType);
      buf.writeBoolean(this.hasHealth);
      buf.writeFloat(this.health);
      buf.writeFloat(this.maxHealth);
      buf.writeEnum(this.homeState);
   }

   public static VillagerLedgerEntryPayload decode(FriendlyByteBuf buf) {
      return new VillagerLedgerEntryPayload(
         FamilyTreeNodePayload.decode(buf),
         (VillagerLedgerEntryPayload.EntryType)buf.readEnum(VillagerLedgerEntryPayload.EntryType.class),
         buf.readBoolean(),
         buf.readFloat(),
         buf.readFloat(),
         (VillagerLedgerEntryPayload.HomeState)buf.readEnum(VillagerLedgerEntryPayload.HomeState.class)
      );
   }

   public enum EntryType {
      FOLLOW,
      STAY,
      SPOUSE,
      CHILD,
      GRANDCHILD,
      DESCENDANT,
      FAMILY;
   }

   public enum HomeState {
      UNKNOWN,
      NO_HOME,
      HOME_SET;
   }
}
