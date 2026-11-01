package com.javic.slimpatch.network;

import com.javic.slimpatch.client.DialogueClientHandler;
import com.javic.slimpatch.ledger.VillagerLedgerDataBuilder;
import com.javic.slimpatch.ledger.VillagerLedgerEntryPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;

public class VillagerLedgerDataPacket {
   private final List<VillagerLedgerEntryPayload> companions;
   private final List<VillagerLedgerEntryPayload> family;
   private final List<VillagerLedgerEntryPayload> hidden;
   private final boolean openScreen;

   public VillagerLedgerDataPacket(
      List<VillagerLedgerEntryPayload> companions, List<VillagerLedgerEntryPayload> family, List<VillagerLedgerEntryPayload> hidden
   ) {
      this(companions, family, hidden, false);
   }

   public VillagerLedgerDataPacket(
      List<VillagerLedgerEntryPayload> companions, List<VillagerLedgerEntryPayload> family, List<VillagerLedgerEntryPayload> hidden, boolean openScreen
   ) {
      this.companions = companions;
      this.family = family;
      this.hidden = hidden;
      this.openScreen = openScreen;
   }

   public VillagerLedgerDataPacket(VillagerLedgerDataBuilder.VillagerLedgerData data) {
      this(data, false);
   }

   public VillagerLedgerDataPacket(VillagerLedgerDataBuilder.VillagerLedgerData data, boolean openScreen) {
      this(data.companions(), data.family(), data.hidden(), openScreen);
   }

   public List<VillagerLedgerEntryPayload> getCompanions() {
      return this.companions;
   }

   public List<VillagerLedgerEntryPayload> getFamily() {
      return this.family;
   }

   public List<VillagerLedgerEntryPayload> getHidden() {
      return this.hidden;
   }

   public boolean shouldOpenScreen() {
      return this.openScreen;
   }

   public static void encode(VillagerLedgerDataPacket packet, FriendlyByteBuf buf) {
      buf.writeBoolean(packet.openScreen);
      buf.writeVarInt(packet.companions.size());

      for (VillagerLedgerEntryPayload entry : packet.companions) {
         entry.encode(buf);
      }

      buf.writeVarInt(packet.family.size());

      for (VillagerLedgerEntryPayload entry : packet.family) {
         entry.encode(buf);
      }

      buf.writeVarInt(packet.hidden.size());

      for (VillagerLedgerEntryPayload entry : packet.hidden) {
         entry.encode(buf);
      }
   }

   public static VillagerLedgerDataPacket decode(FriendlyByteBuf buf) {
      boolean openScreen = buf.readBoolean();
      int companionCount = ModNetworking.readCount(buf, Integer.MAX_VALUE);
      List<VillagerLedgerEntryPayload> companions = new ArrayList<>(companionCount);

      for (int i = 0; i < companionCount; i++) {
         companions.add(VillagerLedgerEntryPayload.decode(buf));
      }

      int familyCount = ModNetworking.readCount(buf, Integer.MAX_VALUE);
      List<VillagerLedgerEntryPayload> family = new ArrayList<>(familyCount);

      for (int i = 0; i < familyCount; i++) {
         family.add(VillagerLedgerEntryPayload.decode(buf));
      }

      int hiddenCount = ModNetworking.readCount(buf, Integer.MAX_VALUE);
      List<VillagerLedgerEntryPayload> hidden = new ArrayList<>(hiddenCount);

      for (int i = 0; i < hiddenCount; i++) {
         hidden.add(VillagerLedgerEntryPayload.decode(buf));
      }

      return new VillagerLedgerDataPacket(companions, family, hidden, openScreen);
   }

   public static void handle(VillagerLedgerDataPacket msg) {
DialogueClientHandler.openVillagerLedgerScreen(msg.getCompanions(), msg.getFamily(), msg.getHidden(), msg.shouldOpenScreen());
   }
}
