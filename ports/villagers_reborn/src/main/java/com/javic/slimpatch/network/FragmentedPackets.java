package com.javic.slimpatch.network;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Minecraft 1.20.1 limits serverbound custom payloads to 32767 bytes. */
final class FragmentedPackets {
   static final ResourceLocation CHANNEL = new ResourceLocation("slimpatch", "fragment");
   private static final int CHUNK = 24000;
   private static final int MAX_SIZE = 3 * 1024 * 1024;

   private FragmentedPackets() {}

   static void send(ResourceLocation id, FriendlyByteBuf payload, BiConsumer<ResourceLocation, FriendlyByteBuf> sender) {
      int size = payload.readableBytes();
      if (size <= CHUNK) {
         sender.accept(id, payload);
         return;
      }
      if (size > MAX_SIZE) {
         payload.release();
         throw new IllegalArgumentException("Packet too large: " + size);
      }
      try {
         for (int offset = 0; offset < size; offset += CHUNK) {
            int length = Math.min(CHUNK, size - offset);
            FriendlyByteBuf fragment = PacketByteBufs.create();
            fragment.writeResourceLocation(id);
            fragment.writeVarInt(size);
            fragment.writeVarInt(offset);
            fragment.writeBytes(payload, payload.readerIndex() + offset, length);
            sender.accept(CHANNEL, fragment);
         }
      } finally {
         payload.release();
      }
   }

   static final class Receiver {
      private ResourceLocation id;
      private byte[] data;
      private int offset;

      void accept(FriendlyByteBuf fragment, java.util.function.Predicate<ResourceLocation> allowed,
                  BiConsumer<ResourceLocation, FriendlyByteBuf> completed) {
         ResourceLocation packetId = fragment.readResourceLocation();
         int size = fragment.readVarInt();
         int start = fragment.readVarInt();
         if (!allowed.test(packetId) || size <= CHUNK || size > MAX_SIZE || start < 0 || start >= size) {
            throw new DecoderException("Invalid packet fragment header");
         }
         int length = Math.min(CHUNK, size - start);
         if (fragment.readableBytes() != length) throw new DecoderException("Invalid packet fragment length");
         if (start == 0) {
            if (data != null) throw new DecoderException("Overlapping fragmented packet");
            id = packetId;
            data = new byte[size];
            offset = 0;
         }
         if (data == null || !id.equals(packetId) || data.length != size || offset != start) {
            throw new DecoderException("Out-of-order packet fragment");
         }
         fragment.readBytes(data, offset, length);
         offset += length;
         if (offset == size) {
            FriendlyByteBuf payload = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
            data = null;
            id = null;
            offset = 0;
            try {
               completed.accept(packetId, payload);
            } finally {
               payload.release();
            }
         }
      }
   }
}
