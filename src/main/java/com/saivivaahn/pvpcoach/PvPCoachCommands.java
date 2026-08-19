package com.saivivaahn.pvpcoach;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class PvPCoachCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pvpcoach")
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(PvPCoachAdvice::help, false);
                    return 1;
                })
                .then(Commands.literal("help").executes(ctx -> {
                    ctx.getSource().sendSuccess(PvPCoachAdvice::help, false);
                    return 1;
                }))
                .then(Commands.literal("commands").executes(ctx -> {
                    ctx.getSource().sendSuccess(PvPCoachAdvice::commands, false);
                    return 1;
                }))
                .then(Commands.literal("tip").executes(ctx -> {
                    ctx.getSource().sendSuccess(PvPCoachAdvice::randomTip, false);
                    return 1;
                }))
                .then(Commands.literal("howto")
                        .then(Commands.argument("topic", StringArgumentType.word()).executes(ctx -> {
                            String topic = StringArgumentType.getString(ctx, "topic");
                            ctx.getSource().sendSuccess(() -> PvPCoachAdvice.howTo(topic), false);
                            return 1;
                        }))
                )
                .then(Commands.literal("ask")
                        .then(Commands.argument("question", StringArgumentType.greedyString()).executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String question = StringArgumentType.getString(ctx, "question");
                            ctx.getSource().sendSuccess(() -> CoachConversation.ask(player.getUUID(), question), false);
                            return 1;
                        }))
                )
                .then(Commands.literal("forget").executes(ctx -> {
                    CoachConversation.clear(ctx.getSource().getPlayerOrException().getUUID());
                    ctx.getSource().sendSuccess(() -> Component.literal("\u00A7aCoach conversation cleared."), false);
                    return 1;
                }))
                .then(Commands.literal("bot")
                        .then(Commands.literal("spawn").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            PvPBot.spawn(player);
                            ctx.getSource().sendSuccess(() -> Component.literal("\u00A7aPvP bot spawned. Mode: \u00A7e" + PvPBot.getMode() + "\u00A7a, difficulty: \u00A7e" + PvPBot.getBotLevel()), false);
                            return 1;
                        }))
                        .then(Commands.literal("remove").executes(ctx -> {
                            PvPBot.remove();
                            ctx.getSource().sendSuccess(() -> Component.literal("§aBot removed!"), false);
                            return 1;
                        }))
                        .then(Commands.literal("heal").executes(ctx -> {
                            PvPBot.heal();
                            ctx.getSource().sendSuccess(() -> Component.literal("§aBot healed!"), false);
                            return 1;
                        }))
                        .then(Commands.literal("level")
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 5)).executes(ctx -> {
                                    int lvl = IntegerArgumentType.getInteger(ctx, "level");
                                    PvPBot.setBotLevel(lvl);
                                    ctx.getSource().sendSuccess(() -> Component.literal("§aBot level set to §e" + lvl + "/5"), false);
                                    return 1;
                                }))
                        )
                        .then(Commands.literal("aggressive").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            PvPBot.setMode(PvPBot.Mode.AGGRESSIVE, player);
                            ctx.getSource().sendSuccess(() -> Component.literal("\u00A7aBot is aggressive and will fight you."), false);
                            return 1;
                        }))
                        .then(Commands.literal("passive").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            PvPBot.setMode(PvPBot.Mode.PASSIVE, player);
                            ctx.getSource().sendSuccess(() -> Component.literal("\u00A7aBot is passive and will not attack."), false);
                            return 1;
                        }))
                        .then(Commands.literal("item")
                                .then(Commands.argument("slot", StringArgumentType.word())
                                        .then(Commands.argument("item", StringArgumentType.word()).executes(ctx -> {
                                            String slot = StringArgumentType.getString(ctx, "slot");
                                            String item = StringArgumentType.getString(ctx, "item");
                                            if (!PvPBot.setItem(slot, item)) {
                                                ctx.getSource().sendFailure(Component.literal("\u00A7cUse a spawned bot, a valid slot, and a supported item. Slots: mainhand, offhand, head, chest, legs, feet."));
                                                return 0;
                                            }
                                            ctx.getSource().sendSuccess(() -> Component.literal("\u00A7aSet " + slot + " to " + item + "."), false);
                                            return 1;
                                        }))
                                )
                        )
                )
                .then(Commands.literal("hud")
                        .then(Commands.literal("position")
                                .then(Commands.argument("x", IntegerArgumentType.integer(0, 1000))
                                        .then(Commands.argument("y", IntegerArgumentType.integer(0, 1000)).executes(ctx -> {
                                            CoachSettings.hudX = IntegerArgumentType.getInteger(ctx, "x");
                                            CoachSettings.hudY = IntegerArgumentType.getInteger(ctx, "y");
                                            ctx.getSource().sendSuccess(() -> Component.literal("§aHUD position updated!"), false);
                                            return 1;
                                        }))
                                )
                        )
                        .then(Commands.literal("toggle")
                                .then(Commands.literal("fps").executes(ctx -> {
                                    CoachSettings.showFps = !CoachSettings.showFps;
                                    ctx.getSource().sendSuccess(() -> Component.literal("§aFPS toggle set to: " + CoachSettings.showFps), false);
                                    return 1;
                                }))
                                .then(Commands.literal("accuracy").executes(ctx -> {
                                    CoachSettings.showAccuracy = !CoachSettings.showAccuracy;
                                    ctx.getSource().sendSuccess(() -> Component.literal("§aAccuracy toggle set to: " + CoachSettings.showAccuracy), false);
                                    return 1;
                                }))
                        )
                )
        );
    }
}
