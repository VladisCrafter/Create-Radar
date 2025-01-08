package com.happysg.radar.registry;

import com.happysg.radar.CreateRadar;

import static com.happysg.radar.CreateRadar.REGISTRATE;

public class ModLang {

    public static void register() {
        CreateRadar.getLogger().info("Registering Lang!");
        REGISTRATE.addRawLang(CreateRadar.MODID + ".ponder.radar_contraption.header", "Creating a Radar!");
        REGISTRATE.addRawLang(CreateRadar.MODID + ".ponder.radar_contraption.text_1", "Place Radar Bearing");
        REGISTRATE.addRawLang(CreateRadar.MODID + ".ponder.radar_contraption.text_2", "Place Radar Receiver");
        REGISTRATE.addRawLang(CreateRadar.MODID + ".ponder.radar_contraption.text_3", "Add Radar Dishes");
        REGISTRATE.addRawLang(CreateRadar.MODID + ".ponder.radar_contraption.text_4", "Radar Dishes can be used interchangeably with Radar Plates");
        REGISTRATE.addRawLang(CreateRadar.MODID + ".ponder.radar_contraption.text_5", "Additional dishes/plates extend range");
        REGISTRATE.addRawLang(CreateRadar.MODID + ".ponder.radar_contraption.text_6", "Power Radar Bearing");

        REGISTRATE.addRawLang(CreateRadar.MODID + ".ponder.tag.radar_components", "Radars");
        REGISTRATE.addRawLang(CreateRadar.MODID + ".ponder.tag.radar_components.description", "Components which allow the creation of Radar Tracking System");

        REGISTRATE.addRawLang(CreateRadar.MODID + ".ponder.radar_linking.header", "Linking a Radar to a Monitor!");
        REGISTRATE.addRawLang(CreateRadar.MODID + ".ponder.radar_linking.text_1", "Place Monitor");
        REGISTRATE.addRawLang(CreateRadar.MODID + ".ponder.radar_linking.text_2", "Build Radar Contraption");
        REGISTRATE.addRawLang(CreateRadar.MODID + ".ponder.radar_linking.text_3", "Link using Display Link");
        REGISTRATE.addRawLang(CreateRadar.MODID + ".ponder.radar_linking.text_4", "Right click Display Link to activate");

        REGISTRATE.addRawLang(CreateRadar.MODID + ".display_source.radar", "Radar");
        REGISTRATE.addRawLang(CreateRadar.MODID + ".display_source.yaw_controller", "Auto Yaw Controller");
        REGISTRATE.addRawLang(CreateRadar.MODID + ".display_source.pitch_controller", "Auto Pitch Controller");

        REGISTRATE.addRawLang(CreateRadar.MODID + ".filter.show", "Show");

        REGISTRATE.addRawLang(CreateRadar.MODID + ".filter.all_entities", "All Entities");
        REGISTRATE.addRawLang(CreateRadar.MODID + ".filter.no_mobs", "No Mobs");
        REGISTRATE.addRawLang(CreateRadar.MODID + ".filter.only_mobs", "Only Mobs");
        REGISTRATE.addRawLang(CreateRadar.MODID + ".filter.players_only", "Players Only");
        REGISTRATE.addRawLang(CreateRadar.MODID + ".filter.projectiles_only", "Projectiles Only");
        REGISTRATE.addRawLang(CreateRadar.MODID + ".filter.vs2_only", "VS2 Only");
        REGISTRATE.addRawLang(CreateRadar.MODID + ".filter.mob_bosses_only", "Mob Bosses Only");


        REGISTRATE.addRawLang(CreateRadar.MODID + ".ponder.controller_linking.header", "Linking a Controller to a Monitor!");
        REGISTRATE.addRawLang(CreateRadar.MODID + ".ponder.controller_linking.text_1", "Place Yaw Controller under turret mount");
        REGISTRATE.addRawLang(CreateRadar.MODID + ".ponder.controller_linking.text_2", "Link using Display Links");
        REGISTRATE.addRawLang(CreateRadar.MODID + ".ponder.controller_linking.text_3", "Repeat for pitch Controller");
        REGISTRATE.addRawLang(CreateRadar.MODID + ".ponder.controller_linking.text_4", "Select Target on monitor for turret to aim/fire at");

        REGISTRATE.addRawLang(CreateRadar.MODID + ".pitch.powder_charge", "Powder Charge");
        REGISTRATE.addRawLang(CreateRadar.MODID + ".guided_fuze.no_monitor", "No Monitor Linked");
        REGISTRATE.addRawLang(CreateRadar.MODID + ".radar.no_receiver", "No Receiver Found");
    }
}
