package com.saivivaahn.pvpcoach;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client request to create a practice bot on a server which has PvP Coach installed. */
public record BotSpawnPayload(boolean weak) implements CustomPacketPayload {
    public static final Type<BotSpawnPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PvPCoach.MOD_ID, "spawn_bot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BotSpawnPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, BotSpawnPayload::weak, BotSpawnPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
