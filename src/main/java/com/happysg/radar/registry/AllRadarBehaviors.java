package com.happysg.radar.registry;

import com.happysg.radar.CreateRadar;
import com.happysg.radar.block.monitor.MonitorLinkBehavior;
import com.happysg.radar.block.radar.bearing.RadarBearingLinkBehavior;
import com.happysg.radar.block.radar.link.RadarLinkBehavior;
import com.happysg.radar.block.radar.link.RadarSource;
import com.happysg.radar.block.radar.link.RadarTarget;
import com.simibubi.create.foundation.utility.AttachedRegistry;
import com.simibubi.create.foundation.utility.RegisteredObjects;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;

public class AllRadarBehaviors {
    public static final Map<ResourceLocation, RadarLinkBehavior> GATHERER_BEHAVIOURS = new HashMap<>();

    private static final AttachedRegistry<Block, List<RadarSource>> SOURCES_BY_BLOCK = new AttachedRegistry<>(ForgeRegistries.BLOCKS);
    private static final AttachedRegistry<BlockEntityType<?>, List<RadarSource>> SOURCES_BY_BLOCK_ENTITY = new AttachedRegistry<>(ForgeRegistries.BLOCK_ENTITY_TYPES);

    private static final AttachedRegistry<Block, RadarTarget> TARGETS_BY_BLOCK = new AttachedRegistry<>(ForgeRegistries.BLOCKS);
    private static final AttachedRegistry<BlockEntityType<?>, RadarTarget> TARGETS_BY_BLOCK_ENTITY = new AttachedRegistry<>(ForgeRegistries.BLOCK_ENTITY_TYPES);


    public static RadarLinkBehavior register(ResourceLocation id, RadarLinkBehavior behaviour) {
        behaviour.id = id;
        GATHERER_BEHAVIOURS.put(id, behaviour);
        return behaviour;
    }

    public static void assignBlock(RadarLinkBehavior behaviour, ResourceLocation block) {
        if (behaviour instanceof RadarSource source) {
            List<RadarSource> sources = SOURCES_BY_BLOCK.get(block);
            if (sources == null) {
                sources = new ArrayList<>();
                SOURCES_BY_BLOCK.register(block, sources);
            }
            sources.add(source);
        }
        if (behaviour instanceof RadarTarget target) {
            TARGETS_BY_BLOCK.register(block, target);
        }
    }

    public static void assignBlockEntity(RadarLinkBehavior behaviour, ResourceLocation beType) {
        if (behaviour instanceof RadarSource source) {
            List<RadarSource> sources = SOURCES_BY_BLOCK_ENTITY.get(beType);
            if (sources == null) {
                sources = new ArrayList<>();
                SOURCES_BY_BLOCK_ENTITY.register(beType, sources);
            }
            sources.add(source);
        }
        if (behaviour instanceof RadarTarget target) {
            TARGETS_BY_BLOCK_ENTITY.register(beType, target);
        }
    }

    public static void assignBlock(RadarLinkBehavior behaviour, Block block) {
        if (behaviour instanceof RadarSource source) {
            List<RadarSource> sources = SOURCES_BY_BLOCK.get(block);
            if (sources == null) {
                sources = new ArrayList<>();
                SOURCES_BY_BLOCK.register(block, sources);
            }
            sources.add(source);
        }
        if (behaviour instanceof RadarTarget target) {
            TARGETS_BY_BLOCK.register(block, target);
        }
    }

    public static void assignBlockEntity(RadarLinkBehavior behaviour, BlockEntityType<?> beType) {
        if (behaviour instanceof RadarSource source) {
            List<RadarSource> sources = SOURCES_BY_BLOCK_ENTITY.get(beType);
            if (sources == null) {
                sources = new ArrayList<>();
                SOURCES_BY_BLOCK_ENTITY.register(beType, sources);
            }
            sources.add(source);
        }
        if (behaviour instanceof RadarTarget target) {
            TARGETS_BY_BLOCK_ENTITY.register(beType, target);
        }
    }

    public static <B extends Block> NonNullConsumer<? super B> assignDataBehaviour(RadarLinkBehavior behaviour,
                                                                                   String... suffix) {
        return b -> {
            ResourceLocation registryName = RegisteredObjects.getKeyOrThrow(b);
            String idSuffix = behaviour instanceof RadarSource ? "_source" : "_target";
            if (suffix.length > 0)
                idSuffix += "_" + suffix[0];
            assignBlock(register(new ResourceLocation(registryName.getNamespace(), registryName.getPath() + idSuffix),
                    behaviour), registryName);
        };
    }

    public static <B extends BlockEntityType<?>> NonNullConsumer<? super B> assignDataBehaviourBE(
            RadarLinkBehavior behaviour, String... suffix) {
        return b -> {
            ResourceLocation registryName = RegisteredObjects.getKeyOrThrow(b);
            String idSuffix = behaviour instanceof RadarSource ? "_source" : "_target";
            if (suffix.length > 0)
                idSuffix += "_" + suffix[0];
            assignBlockEntity(
                    register(new ResourceLocation(registryName.getNamespace(), registryName.getPath() + idSuffix),
                            behaviour),
                    registryName);
        };
    }

    //

    @Nullable
    public static RadarSource getSource(ResourceLocation resourceLocation) {
        RadarLinkBehavior available = GATHERER_BEHAVIOURS.getOrDefault(resourceLocation, null);
        if (available instanceof RadarSource source)
            return source;
        return null;
    }

    @Nullable
    public static RadarTarget getTarget(ResourceLocation resourceLocation) {
        RadarLinkBehavior available = GATHERER_BEHAVIOURS.getOrDefault(resourceLocation, null);
        if (available instanceof RadarTarget target)
            return target;
        return null;
    }

    //

    public static List<RadarSource> sourcesOf(Block block) {
        List<RadarSource> sources = SOURCES_BY_BLOCK.get(block);
        if (sources == null) {
            return Collections.emptyList();
        }
        return sources;
    }

    public static List<RadarSource> sourcesOf(BlockState state) {
        return sourcesOf(state.getBlock());
    }

    public static List<RadarSource> sourcesOf(BlockEntityType<?> blockEntityType) {
        List<RadarSource> sources = SOURCES_BY_BLOCK_ENTITY.get(blockEntityType);
        if (sources == null) {
            return Collections.emptyList();
        }
        return sources;
    }

    public static List<RadarSource> sourcesOf(BlockEntity blockEntity) {
        return sourcesOf(blockEntity.getType());
    }

    @Nullable
    public static RadarTarget targetOf(Block block) {
        return TARGETS_BY_BLOCK.get(block);
    }

    @Nullable
    public static RadarTarget targetOf(BlockState state) {
        return targetOf(state.getBlock());
    }

    @Nullable
    public static RadarTarget targetOf(BlockEntityType<?> blockEntityType) {
        return TARGETS_BY_BLOCK_ENTITY.get(blockEntityType);
    }

    @Nullable
    public static RadarTarget targetOf(BlockEntity blockEntity) {
        return targetOf(blockEntity.getType());
    }

    public static List<RadarSource> sourcesOf(LevelAccessor level, BlockPos pos) {
        BlockState blockState = level.getBlockState(pos);
        BlockEntity blockEntity = level.getBlockEntity(pos);

        List<RadarSource> sourcesOfBlock = sourcesOf(blockState);
        List<RadarSource> sourcesOfBlockEntity = blockEntity == null ? Collections.emptyList() : sourcesOf(blockEntity);

        if (sourcesOfBlockEntity.isEmpty())
            return sourcesOfBlock;
        return sourcesOfBlockEntity;
    }

    @Nullable
    public static RadarTarget targetOf(LevelAccessor level, BlockPos pos) {
        BlockState blockState = level.getBlockState(pos);
        BlockEntity blockEntity = level.getBlockEntity(pos);

        RadarTarget targetOfBlock = targetOf(blockState);
        RadarTarget targetOfBlockEntity = blockEntity == null ? null : targetOf(blockEntity);

        // Commonly added by mods, but with a non-vanilla blockentitytype
        if (targetOfBlockEntity == null && blockEntity instanceof SignBlockEntity)
            targetOfBlockEntity = targetOf(BlockEntityType.SIGN);

        if (targetOfBlockEntity == null)
            return targetOfBlock;
        return targetOfBlockEntity;
    }

    public static void registerDefaults() {
        assignBlockEntity(register(CreateRadar.asResource("monitor"), new MonitorLinkBehavior()), ModBlockEntityTypes.MONITOR.get());
        assignBlockEntity(register(CreateRadar.asResource("radar"), new RadarBearingLinkBehavior()), ModBlockEntityTypes.RADAR_BEARING.get());
    }

}