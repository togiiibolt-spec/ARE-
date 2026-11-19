package com.are;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Are.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> WHISPER = reg("whisper");
    public static final DeferredHolder<SoundEvent, SoundEvent> STATIC = reg("static");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEARTBEAT = reg("heartbeat");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCREAM = reg("scream");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRONE = reg("drone");
    public static final DeferredHolder<SoundEvent, SoundEvent> KNOCK = reg("knock");
    public static final DeferredHolder<SoundEvent, SoundEvent> STEP = reg("step");

    private static DeferredHolder<SoundEvent, SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Are.id(name)));
    }
}
