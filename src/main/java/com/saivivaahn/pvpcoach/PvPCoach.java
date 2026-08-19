package com.saivivaahn.pvpcoach;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

public class PvPCoach implements ModInitializer {
    public static final String MOD_ID = "pvpcoach";

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(BotSpawnPayload.TYPE, BotSpawnPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(BotModePayload.TYPE, BotModePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(BotSpawnPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    if (payload.weak()) PvPBot.spawnWeak(context.player());
                    else PvPBot.spawn(context.player());
                })
        );
        ServerPlayNetworking.registerGlobalReceiver(BotModePayload.TYPE, (payload, context) ->
                context.server().execute(() -> PvPBot.toggleMode(context.player()))
        );
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            PvPCoachCommands.register(dispatcher);
        });
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClientSide && player instanceof ServerPlayer serverPlayer
                    && PvPBot.interact(serverPlayer, hand, entity)) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
    }
}
