package com.saivivaahn.pvpcoach.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.saivivaahn.pvpcoach.CoachConversation;
import com.saivivaahn.pvpcoach.PvPCoachAdvice;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Local command fallback: coaching does not require a PvP Coach server installation. */
public final class ClientCoachCommands {
    private ClientCoachCommands() { }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("pvpcoach")
                        .executes(context -> feedback(context.getSource(), PvPCoachAdvice.help()))
                        .then(ClientCommandManager.literal("help")
                                .executes(context -> feedback(context.getSource(), PvPCoachAdvice.help())))
                        .then(ClientCommandManager.literal("commands")
                                .executes(context -> feedback(context.getSource(), PvPCoachAdvice.commands())))
                        .then(ClientCommandManager.literal("tip")
                                .executes(context -> feedback(context.getSource(), PvPCoachAdvice.randomTip())))
                        .then(ClientCommandManager.literal("howto")
                                .then(ClientCommandManager.argument("topic", StringArgumentType.word())
                                        .executes(context -> feedback(context.getSource(), PvPCoachAdvice.howTo(StringArgumentType.getString(context, "topic"))))))
                        .then(ClientCommandManager.literal("ask")
                                .then(ClientCommandManager.argument("question", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            Minecraft client = Minecraft.getInstance();
                                            if (client.player == null) return 0;
                                            return feedback(context.getSource(), CoachConversation.ask(client.player.getUUID(), StringArgumentType.getString(context, "question")));
                                        })))
                        .then(ClientCommandManager.literal("forget")
                                .executes(context -> {
                                    Minecraft client = Minecraft.getInstance();
                                    if (client.player != null) CoachConversation.clear(client.player.getUUID());
                                    return feedback(context.getSource(), Component.literal("§aCoach conversation cleared."));
                                }))));
    }

    private static int feedback(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source, Component message) {
        source.sendFeedback(message);
        return 1;
    }
}
