package com.are;

import net.neoforged.neoforge.common.ModConfigSpec;

public class AreConfig {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue INTENSITY = B
            .comment("How often scary things happen. 1.0 = normal, 0.3 = gentle, 3.0 = nightmare.")
            .defineInRange("intensity", 1.0, 0.1, 5.0);

    public static final ModConfigSpec.BooleanValue SYSTEM_INFO = B
            .comment("Show your computer's user name / OS / time inside the welcome book and in creepy messages.",
                    "This only ever appears on YOUR screen. Nothing is sent anywhere. Set false to disable.")
            .define("systemInfoInBook", true);

    public static final ModConfigSpec.BooleanValue SNUFF_TORCHES = B
            .comment("Allow torches near you to be blown out (removes the torch block).")
            .define("extinguishTorches", true);

    public static final ModConfigSpec.BooleanValue OPEN_DOORS = B
            .comment("Allow wooden doors near you to open by themselves.")
            .define("openDoors", true);

    public static final ModConfigSpec.BooleanValue FAKE_CHAT = B
            .comment("Allow fake chat messages (fake leave/join, messages in your own name).")
            .define("fakeChat", true);

    public static final ModConfigSpec SPEC = B.build();
}
