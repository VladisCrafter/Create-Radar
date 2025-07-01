package com.happysg.radar.compat.computercraft;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.block.monitor.MonitorBlockEntity;
import com.happysg.radar.block.radar.track.RadarTrack;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.GenericPeripheral;

import java.util.*;

public class MonitorPeripheral implements GenericPeripheral {

    @LuaFunction(mainThread = true)
    public static String getSelectedTrackId(MonitorBlockEntity monitorEntity){
        MonitorBlockEntity controller = monitorEntity.getController();
        return controller.getSelectedEntity();
    }
    @LuaFunction(mainThread = true)
    public static List<Map<? super String, Object>> getTracks(MonitorBlockEntity monitorEntity){
        List<Map<? super String, Object>> tracks = new ArrayList<>();
        MonitorBlockEntity controller = monitorEntity.getController();

        for (RadarTrack track : controller.getTracks()) {
            HashMap<? super String, Object> map = new HashMap<>();
            map.put("position", RadarBearingPeripheral.getMapFromVector(track.position()));
            map.put("velocity", RadarBearingPeripheral.getMapFromVector(track.velocity()));
            map.put("category", track.trackCategory().toString());
            map.put("id", track.id());
            map.put("scannedTime", track.scannedTime());
            map.put("entityType", track.entityType());
            tracks.add(map);
        }
        return tracks;
    }

    @LuaFunction(mainThread = true)
    public static Map<? super String, Object> getSelectedTrack(MonitorBlockEntity monitorEntity) {
        MonitorBlockEntity controller = monitorEntity.getController();
        RadarTrack selectedTrack = null;
        for (RadarTrack track : controller.getTracks()) {
            if (Objects.equals(track.id(), controller.getSelectedEntity())) {
                selectedTrack = track;
                break;
            }
        }
        if (selectedTrack == null) {
            return null;
        }
        HashMap<? super String, Object> map = new HashMap<>();
        map.put("position", RadarBearingPeripheral.getMapFromVector(selectedTrack.position()));
        map.put("velocity", RadarBearingPeripheral.getMapFromVector(selectedTrack.velocity()));
        map.put("category", selectedTrack.trackCategory().toString());
        map.put("id", selectedTrack.id());
        map.put("scannedTime", selectedTrack.scannedTime());
        map.put("entityType", selectedTrack.entityType());
        return map;
    }

    public static List<String> optStringList(Map<? super String, ?> map, String key) {
        if (!map.containsKey(key)) {
            return new ArrayList<>();
        }
        List<String> out = new ArrayList<>();
        Map<?, ?> list = (Map<?, ?>) map.get(key);
        for (Object k: list.values()) {
            if (k instanceof String) {
                out.add((String) k);
            }
        }
        return out;
    }

    @Override
    public String id() {
        return CreateRadar.asResource("monitor").toString();
    }
}
