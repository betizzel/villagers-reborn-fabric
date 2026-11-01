package com.javic.slimpatch.network;

import java.util.function.BiConsumer;
import java.util.function.Function;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class ModNetworking {
   private static final java.util.Map<ResourceLocation, BiConsumer<ServerPlayer, FriendlyByteBuf>> SERVER_HANDLERS = new java.util.HashMap<>();
   private static final java.util.Map<ServerPlayer, FragmentedPackets.Receiver> FRAGMENTS = new java.util.concurrent.ConcurrentHashMap<>();
   private ModNetworking() {}

   static ResourceLocation id(Class<?> type) {
      return new ResourceLocation("slimpatch", type.getSimpleName().toLowerCase(java.util.Locale.ROOT));
   }

   static int readCount(FriendlyByteBuf buf, int max) {
      int count = buf.readVarInt();
      if (count < 0 || count > max || count > buf.readableBytes()) {
         throw new io.netty.handler.codec.DecoderException("Invalid packet entry count: " + count);
      }
      return count;
   }

   private static <T> void registerServer(Class<T> type, Function<FriendlyByteBuf, T> decode, BiConsumer<T, ServerPlayer> handle) {
      BiConsumer<ServerPlayer, FriendlyByteBuf> receive = (player, buf) -> {
         T packet = decode.apply(buf);
         if (buf.isReadable()) throw new io.netty.handler.codec.DecoderException("Trailing packet data");
         player.getServer().execute(() -> handle.accept(packet, player));
      };
      SERVER_HANDLERS.put(id(type), receive);
      ServerPlayNetworking.registerGlobalReceiver(id(type), (server, player, handler, buf, responseSender) -> receive.accept(player, buf));
   }

   public static void register() {
      ServerPlayNetworking.registerGlobalReceiver(FragmentedPackets.CHANNEL, (server, player, handler, buf, responseSender) ->
         FRAGMENTS.computeIfAbsent(player, ignored -> new FragmentedPackets.Receiver())
            .accept(buf, SERVER_HANDLERS::containsKey, (id, payload) -> SERVER_HANDLERS.get(id).accept(player, payload)));
      net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> FRAGMENTS.remove(handler.player));
      registerServer(DialogueStatePacket.class, DialogueStatePacket::decode, DialogueStatePacket::handle);
      registerServer(RelationshipPacket.class, RelationshipPacket::decode, RelationshipPacket::handle);
      registerServer(GiftPacket.class, GiftPacket::decode, GiftPacket::handle);
      registerServer(VillagerCommandPacket.class, VillagerCommandPacket::decode, VillagerCommandPacket::handle);
      registerServer(ConfirmMarriageProposalPacket.class, ConfirmMarriageProposalPacket::decode, ConfirmMarriageProposalPacket::handle);
      registerServer(ConfirmDivorcePacket.class, ConfirmDivorcePacket::decode, ConfirmDivorcePacket::handle);
      registerServer(VillagerNamePacket.class, VillagerNamePacket::decode, VillagerNamePacket::handle);
      registerServer(VillagerEditDataPacket.class, VillagerEditDataPacket::decode, VillagerEditDataPacket::handle);
      registerServer(VillagerCustomSkinUploadPacket.class, VillagerCustomSkinUploadPacket::decode, VillagerCustomSkinUploadPacket::handle);
      registerServer(VillagerEquipmentPacket.class, VillagerEquipmentPacket::decode, VillagerEquipmentPacket::handle);
      registerServer(VillagerCombatModePacket.class, VillagerCombatModePacket::decode, VillagerCombatModePacket::handle);
      registerServer(VillagerFollowModePacket.class, VillagerFollowModePacket::decode, VillagerFollowModePacket::handle);
      registerServer(VillagerArmorVisibilityPacket.class, VillagerArmorVisibilityPacket::decode, VillagerArmorVisibilityPacket::handle);
      registerServer(VillagerMutePacket.class, VillagerMutePacket::decode, VillagerMutePacket::handle);
      registerServer(StartFamilyPacket.class, StartFamilyPacket::decode, StartFamilyPacket::handle);
      registerServer(FamilyStatusPacket.class, FamilyStatusPacket::decode, FamilyStatusPacket::handle);
      registerServer(RequestBirthScreenPacket.class, RequestBirthScreenPacket::decode, RequestBirthScreenPacket::handle);
      registerServer(ConfirmBirthPacket.class, ConfirmBirthPacket::decode, ConfirmBirthPacket::handle);
      registerServer(RequestFamilyTreePacket.class, RequestFamilyTreePacket::decode, RequestFamilyTreePacket::handle);
      registerServer(CloseFamilyTreePacket.class, CloseFamilyTreePacket::decode, CloseFamilyTreePacket::handle);
      registerServer(VillagerLedgerActionPacket.class, VillagerLedgerActionPacket::decode, VillagerLedgerActionPacket::handle);
      VillagerLedgerActionPacket.init();
      net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(ServerCooldownTracker::onServerTick);
   }

   public static void registerClient() { ClientNetworking.register(); }

   public static void sendToClient(Object packet, ServerPlayer player) {
      FriendlyByteBuf buf = PacketByteBufs.create();
      if (packet instanceof RelationshipSyncPacket value) RelationshipSyncPacket.encode(value, buf);
      else if (packet instanceof OpenMarriageProposalScreenPacket value) OpenMarriageProposalScreenPacket.encode(value, buf);
      else if (packet instanceof OpenDivorceConfirmationScreenPacket value) OpenDivorceConfirmationScreenPacket.encode(value, buf);
      else if (packet instanceof StartWeddingCutscenePacket value) StartWeddingCutscenePacket.encode(value, buf);
      else if (packet instanceof OpenGraveScreenPacket value) OpenGraveScreenPacket.encode(value, buf);
      else if (packet instanceof VillagerLedgerDataPacket value) VillagerLedgerDataPacket.encode(value, buf);
      else if (packet instanceof OpenVillagerLedgerTeleportNoticePacket value) OpenVillagerLedgerTeleportNoticePacket.encode(value, buf);
      else if (packet instanceof StartFamilyCutscenePacket value) StartFamilyCutscenePacket.encode(value, buf);
      else if (packet instanceof DialogueResultPacket value) DialogueResultPacket.encode(value, buf);
      else if (packet instanceof FamilyDialogueLinePacket value) FamilyDialogueLinePacket.encode(value, buf);
      else if (packet instanceof FamilyBirthReadyPacket value) FamilyBirthReadyPacket.encode(value, buf);
      else if (packet instanceof OpenBirthScreenPacket value) OpenBirthScreenPacket.encode(value, buf);
      else if (packet instanceof BirthConfirmResultPacket value) BirthConfirmResultPacket.encode(value, buf);
      else if (packet instanceof VillagerCooldownsPacket value) VillagerCooldownsPacket.encode(value, buf);
      else if (packet instanceof SpouseCookingLinePacket value) SpouseCookingLinePacket.encode(value, buf);
      else if (packet instanceof SkinThemeSyncPacket value) SkinThemeSyncPacket.encode(value, buf);
      else if (packet instanceof MultiplayerSkinSettingsSyncPacket value) MultiplayerSkinSettingsSyncPacket.encode(value, buf);
      else if (packet instanceof VillagerCustomSkinSyncPacket value) VillagerCustomSkinSyncPacket.encode(value, buf);
      else if (packet instanceof FamilyTreeDataPacket value) FamilyTreeDataPacket.encode(value, buf);
      else throw new IllegalArgumentException("Unknown client packet " + packet.getClass());
      FragmentedPackets.send(id(packet.getClass()), buf, (channel, payload) -> ServerPlayNetworking.send(player, channel, payload));
   }

   public static void sendToServer(Object packet) { ClientNetworking.send(packet); }
}
