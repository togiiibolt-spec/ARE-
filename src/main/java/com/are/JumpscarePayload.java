package com.are;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: play a screen effect. */
public record JumpscarePayload(int kind) implements CustomPacketPayload {
    public static final int GLITCH = 0;
    public static final int FACE = 1;
    public static final int BLACKOUT = 2;
    public static final int PULSE = 3;

    public static final CustomPacketPayload.Type<JumpscarePayload> TYPE = new CustomPacketPayload.Type<>(Are.id("jumpscare"));
    public static final StreamCodec<ByteBuf, JumpscarePayload> CODEC =
            ByteBufCodecs.VAR_INT.map(JumpscarePayload::new, JumpscarePayload::kind);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
