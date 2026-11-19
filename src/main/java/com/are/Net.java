package com.are;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class Net {
    private Net() {}

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(JumpscarePayload.TYPE, JumpscarePayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> ClientFx.handle(payload)));
    }

    public static void send(ServerPlayer player, int kind) {
        PacketDistributor.sendToPlayer(player, new JumpscarePayload(kind));
    }
}
