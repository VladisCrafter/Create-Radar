package com.happysg.radar.registry;

import com.happysg.radar.CreateRadar;
import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags;
import com.tterrag.registrate.util.entry.RegistryEntry;

import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
public class ModPonderTags {
    public static final ResourceLocation RADAR_COMPONENT = CreateRadar.asResource("radar_components");



    public static void register(PonderTagRegistrationHelper<ResourceLocation> helper) {
        // Add items to tags here
        PonderTagRegistrationHelper<RegistryEntry<?>> entryHelper = helper.withKeyFunction(RegistryEntry::getId);
        helper.registerTag(RADAR_COMPONENT)
                .addToIndex()
                .item(ModBlocks.RADAR_PLATE_BLOCK)
                .title("Radar Components")
                .description("Components which allow the creation of Radar Contraptions")
                .register();
        entryHelper.addToTag(RADAR_COMPONENT)
                .add(ModBlocks.RADAR_BEARING_BLOCK)
                .add(ModBlocks.RADAR_DISH_BLOCK)
                .add(ModBlocks.RADAR_PLATE_BLOCK)
                .add(ModBlocks.RADAR_RECEIVER_BLOCK)
                .add(ModBlocks.MONITOR);

        entryHelper.addToTag(AllCreatePonderTags.MOVEMENT_ANCHOR)
                .add(ModBlocks.RADAR_BEARING_BLOCK);

        entryHelper.addToTag(AllCreatePonderTags.DISPLAY_SOURCES)
                .add(ModBlocks.RADAR_BEARING_BLOCK);

        entryHelper.addToTag(AllCreatePonderTags.DISPLAY_TARGETS)
                .add(ModBlocks.MONITOR);
    }

}
