package com.javic.slimpatch.network;

import java.util.function.Consumer;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;

final class ClientNetworking {
   private static final java.util.Map<net.minecraft.resources.ResourceLocation, Consumer<FriendlyByteBuf>> CLIENT_HANDLERS = new java.util.HashMap<>();
   private static final java.util.Map<net.minecraft.client.multiplayer.ClientPacketListener, FragmentedPackets.Receiver> FRAGMENTS = new java.util.concurrent.ConcurrentHashMap<>();
   private ClientNetworking() {}

   private static <T> void register(Class<T> type, Function<FriendlyByteBuf, T> decode, Consumer<T> handle) {
      Consumer<FriendlyByteBuf> receive = buf -> {
         T packet = decode.apply(buf);
         if (buf.isReadable()) throw new io.netty.handler.codec.DecoderException("Trailing packet data");
         net.minecraft.client.Minecraft.getInstance().execute(() -> handle.accept(packet));
      };
      CLIENT_HANDLERS.put(ModNetworking.id(type), receive);
      ClientPlayNetworking.registerGlobalReceiver(ModNetworking.id(type), (client, handler, buf, responseSender) -> receive.accept(buf));
   }

   static void register() {
      ClientPlayNetworking.registerGlobalReceiver(FragmentedPackets.CHANNEL, (client, handler, buf, responseSender) ->
         FRAGMENTS.computeIfAbsent(handler, ignored -> new FragmentedPackets.Receiver())
            .accept(buf, CLIENT_HANDLERS::containsKey, (id, payload) -> CLIENT_HANDLERS.get(id).accept(payload)));
      net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> FRAGMENTS.remove(handler));
      register(RelationshipSyncPacket.class, RelationshipSyncPacket::decode, RelationshipSyncPacket::handle);
      register(OpenMarriageProposalScreenPacket.class, OpenMarriageProposalScreenPacket::decode, OpenMarriageProposalScreenPacket::handle);
      register(OpenDivorceConfirmationScreenPacket.class, OpenDivorceConfirmationScreenPacket::decode, OpenDivorceConfirmationScreenPacket::handle);
      register(StartWeddingCutscenePacket.class, StartWeddingCutscenePacket::decode, StartWeddingCutscenePacket::handle);
      register(OpenGraveScreenPacket.class, OpenGraveScreenPacket::decode, OpenGraveScreenPacket::handle);
      register(VillagerLedgerDataPacket.class, VillagerLedgerDataPacket::decode, VillagerLedgerDataPacket::handle);
      register(OpenVillagerLedgerTeleportNoticePacket.class, OpenVillagerLedgerTeleportNoticePacket::decode, OpenVillagerLedgerTeleportNoticePacket::handle);
      register(StartFamilyCutscenePacket.class, StartFamilyCutscenePacket::decode, StartFamilyCutscenePacket::handle);
      register(DialogueResultPacket.class, DialogueResultPacket::decode, DialogueResultPacket::handle);
      register(FamilyDialogueLinePacket.class, FamilyDialogueLinePacket::decode, FamilyDialogueLinePacket::handle);
      register(FamilyBirthReadyPacket.class, FamilyBirthReadyPacket::decode, FamilyBirthReadyPacket::handle);
      register(OpenBirthScreenPacket.class, OpenBirthScreenPacket::decode, OpenBirthScreenPacket::handle);
      register(BirthConfirmResultPacket.class, BirthConfirmResultPacket::decode, BirthConfirmResultPacket::handle);
      register(VillagerCooldownsPacket.class, VillagerCooldownsPacket::decode, VillagerCooldownsPacket::handle);
      register(SpouseCookingLinePacket.class, SpouseCookingLinePacket::decode, SpouseCookingLinePacket::handle);
      register(SkinThemeSyncPacket.class, SkinThemeSyncPacket::decode, SkinThemeSyncPacket::handle);
      register(MultiplayerSkinSettingsSyncPacket.class, MultiplayerSkinSettingsSyncPacket::decode, MultiplayerSkinSettingsSyncPacket::handle);
      register(VillagerCustomSkinSyncPacket.class, VillagerCustomSkinSyncPacket::decode, VillagerCustomSkinSyncPacket::handle);
      register(FamilyTreeDataPacket.class, FamilyTreeDataPacket::decode, FamilyTreeDataPacket::handle);
   }

   static void send(Object packet) {
      FriendlyByteBuf buf = PacketByteBufs.create();
      if (packet instanceof DialogueStatePacket value) DialogueStatePacket.encode(value, buf);
      else if (packet instanceof RelationshipPacket value) RelationshipPacket.encode(value, buf);
      else if (packet instanceof GiftPacket value) GiftPacket.encode(value, buf);
      else if (packet instanceof VillagerCommandPacket value) VillagerCommandPacket.encode(value, buf);
      else if (packet instanceof ConfirmMarriageProposalPacket value) ConfirmMarriageProposalPacket.encode(value, buf);
      else if (packet instanceof ConfirmDivorcePacket value) ConfirmDivorcePacket.encode(value, buf);
      else if (packet instanceof VillagerNamePacket value) VillagerNamePacket.encode(value, buf);
      else if (packet instanceof VillagerEditDataPacket value) VillagerEditDataPacket.encode(value, buf);
      else if (packet instanceof VillagerCustomSkinUploadPacket value) VillagerCustomSkinUploadPacket.encode(value, buf);
      else if (packet instanceof VillagerEquipmentPacket value) VillagerEquipmentPacket.encode(value, buf);
      else if (packet instanceof VillagerCombatModePacket value) VillagerCombatModePacket.encode(value, buf);
      else if (packet instanceof VillagerFollowModePacket value) VillagerFollowModePacket.encode(value, buf);
      else if (packet instanceof VillagerArmorVisibilityPacket value) VillagerArmorVisibilityPacket.encode(value, buf);
      else if (packet instanceof VillagerMutePacket value) VillagerMutePacket.encode(value, buf);
      else if (packet instanceof StartFamilyPacket value) StartFamilyPacket.encode(value, buf);
      else if (packet instanceof FamilyStatusPacket value) FamilyStatusPacket.encode(value, buf);
      else if (packet instanceof RequestBirthScreenPacket value) RequestBirthScreenPacket.encode(value, buf);
      else if (packet instanceof ConfirmBirthPacket value) ConfirmBirthPacket.encode(value, buf);
      else if (packet instanceof RequestFamilyTreePacket value) RequestFamilyTreePacket.encode(value, buf);
      else if (packet instanceof CloseFamilyTreePacket value) CloseFamilyTreePacket.encode(value, buf);
      else if (packet instanceof VillagerLedgerActionPacket value) VillagerLedgerActionPacket.encode(value, buf);
      else throw new IllegalArgumentException("Unknown server packet " + packet.getClass());
      FragmentedPackets.send(ModNetworking.id(packet.getClass()), buf, ClientPlayNetworking::send);
   }
}
