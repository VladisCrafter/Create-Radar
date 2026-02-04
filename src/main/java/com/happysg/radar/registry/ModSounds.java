package com.happysg.radar.registry;

import com.happysg.radar.CreateRadar;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, CreateRadar.MODID);
    private static RegistryObject<SoundEvent> register(String id) {
        // i use variable range events so minecraft handles distance falloff normally
        return SOUND_EVENTS.register(id, () ->
                SoundEvent.createVariableRangeEvent(new ResourceLocation(CreateRadar.MODID, id)));
    }
    public static final RegistryObject<SoundEvent> RWR_LOCK =
            register("rwr.lock");

    public static final RegistryObject<SoundEvent> RWR_IN_RANGE =
            register("rwr.in_range");

    public static void register(IEventBus modBus) {
        SOUND_EVENTS.register(modBus);
    }
}

