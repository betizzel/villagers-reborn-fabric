package com.javic.slimpatch.commands;

import com.javic.slimpatch.data.WorldSkinData;
import com.javic.slimpatch.network.ModNetworking;
import com.javic.slimpatch.network.SkinThemeSyncPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class WorldSkinTypeCommand {
    private WorldSkinTypeCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("villagersreborn")
                .then(Commands.literal("skin_type").requires(source -> source.hasPermission(2))
                        .then(Commands.literal("modern").executes(context -> setSkinType(context, "modern")))
                        .then(Commands.literal("fantasy").executes(context -> setSkinType(context, "fantasy")))));
    }

    private static int setSkinType(CommandContext<CommandSourceStack> context, String theme) {
        MinecraftServer server = context.getSource().getServer();
        WorldSkinData data = WorldSkinData.get(server);
        if (theme.equals(data.getTheme())) {
            context.getSource().sendSuccess(() -> Component.translatable("modern".equals(theme)
                    ? "slimpatch.command.world_skin_type.already_modern" : "slimpatch.command.world_skin_type.already_fantasy"), false);
            return 1;
        }
        data.setTheme(theme);
        data.setGuiShown(true);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ModNetworking.sendToClient(new SkinThemeSyncPacket(data.getTheme()), player);
        }
        context.getSource().sendSuccess(() -> Component.translatable("modern".equals(theme)
                ? "slimpatch.command.world_skin_type.changed_modern" : "slimpatch.command.world_skin_type.changed_fantasy"), false);
        return 1;
    }
}
