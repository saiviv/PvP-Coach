package com.saivivaahn.pvpcoach;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record BotModePayload() implements CustomPacketPayload {
    public static final Type<BotModePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PvPCoach.MOD_ID, "toggle_bot_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BotModePayload> CODEC = StreamCodec.unit(new BotModePayload());
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
