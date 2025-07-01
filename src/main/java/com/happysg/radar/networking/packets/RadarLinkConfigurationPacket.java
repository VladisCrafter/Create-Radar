package com.happysg.radar.networking.packets;

import com.happysg.radar.block.datalink.DataLinkBlockEntity;
import com.happysg.radar.block.datalink.DataPeripheral;
import com.happysg.radar.registry.AllDataBehaviors;
import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class RadarLinkConfigurationPacket extends BlockEntityConfigurationPacket<DataLinkBlockEntity> {

    private CompoundTag configData;

    public RadarLinkConfigurationPacket(BlockPos pos, CompoundTag configData) {
        super(pos);
        this.configData = configData;
    }

    public RadarLinkConfigurationPacket(FriendlyByteBuf buffer) {
        super(buffer);
    }

    @Override
    protected void writeSettings(FriendlyByteBuf buffer) {
        buffer.writeNbt(configData);
    }

    @Override
    protected void readSettings(FriendlyByteBuf buffer) {
        configData = buffer.readNbt();
    }

    @Override
    protected void applySettings(DataLinkBlockEntity be) {

        if (!configData.contains("Id")) {
            be.notifyUpdate();
            return;
        }

        ResourceLocation id = new ResourceLocation(configData.getString("Id"));
        DataPeripheral source = AllDataBehaviors.getSource(id);
        if (source == null) {
            be.notifyUpdate();
            return;
        }

        if (be.activeSource == null || be.activeSource != source) {
            be.activeSource = source;
            be.setSourceConfig(configData.copy());
        } else {
            be.getSourceConfig()
                    .merge(configData);
        }

        be.updateGatheredData();
        be.notifyUpdate();
    }

}