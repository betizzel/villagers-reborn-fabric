package com.javic.slimpatch.item;

import com.javic.slimpatch.ledger.VillagerLedgerDataBuilder;
import com.javic.slimpatch.network.ModNetworking;
import com.javic.slimpatch.network.VillagerLedgerDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class VillagerLedgerItem extends Item {
   public VillagerLedgerItem(Properties properties) { super(properties); }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
      ItemStack stack = player.getItemInHand(usedHand);
      if (level.isClientSide) return InteractionResultHolder.success(stack);
      if (player instanceof ServerPlayer serverPlayer) {
         ModNetworking.sendToClient(new VillagerLedgerDataPacket(VillagerLedgerDataBuilder.build(serverPlayer), true), serverPlayer);
         return InteractionResultHolder.consume(stack);
      }
      return InteractionResultHolder.pass(stack);
   }
}
