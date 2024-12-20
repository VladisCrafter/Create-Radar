package com.happysg.radar.block.monitor;

import com.happysg.radar.block.radar.bearing.RadarBearingBlockEntity;
import com.happysg.radar.block.radar.bearing.RadarTrack;
import com.happysg.radar.registry.ModRenderTypes;
import com.jozufozu.flywheel.util.Color;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MonitorRenderer extends SmartBlockEntityRenderer<MonitorBlockEntity> {

    public MonitorRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(MonitorBlockEntity blockEntity, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
        super.renderSafe(blockEntity, partialTicks, ms, bufferSource, light, overlay);
        if (!blockEntity.isController())
            return;


        Direction direction = blockEntity.getBlockState().getValue(MonitorBlock.FACING);
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(Axis.YN.rotationDegrees(direction.toYRot()));
        ms.translate(-0.5, -0.5, -0.5);
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(Axis.XP.rotationDegrees(90));
        ms.translate(-0.5, -0.5, -0.5);
        blockEntity.getRadar().ifPresent(radar -> {
            if (!radar.isRunning())
                return;
            renderGrid(radar, blockEntity, ms, bufferSource);
            renderRadarTracks(radar, blockEntity, ms, bufferSource);
            renderBG(blockEntity, ms, bufferSource, MonitorSprite.RADAR_BG_FILLER);
            renderBG(blockEntity, ms, bufferSource, MonitorSprite.RADAR_BG_CIRCLE);
            renderSweep(radar, blockEntity, ms, bufferSource);
        });
    }

    private void renderGrid(RadarBearingBlockEntity radar, MonitorBlockEntity blockEntity, PoseStack ms, MultiBufferSource bufferSource) {
        int size = blockEntity.getSize();
        float range = radar.getRange();
        final int GRID_BLOCK_SIZE = 50;

        float gridSpacing = range / GRID_BLOCK_SIZE;
        VertexConsumer buffer = bufferSource.getBuffer(ModRenderTypes.entityTranslucent(MonitorSprite.GRID_SQUARE.getTexture()));
        Matrix4f m = ms.last().pose();
        Matrix3f n = ms.last().normal();


        Color color = new Color(0, 255, 0);
        float alpha = .5f;
        float deptY = 0.94f;

        float xmin = 1 - size;
        float zmin = 1 - size;
        float xmax = 1;
        float zmax = 1;


        // Adjust UV coordinates based on grid spacing
        float u0 = -0.5f * gridSpacing, v0 = -0.5f * gridSpacing;
        float u1 = 0.5f * gridSpacing, v1 = -0.5f * gridSpacing;
        float u2 = 0.5f * gridSpacing, v2 = 0.5f * gridSpacing;
        float u3 = -0.5f * gridSpacing, v3 = 0.5f * gridSpacing;

        buffer.vertex(m, xmin, deptY, zmin)
                .color(color.getRedAsFloat(), color.getGreenAsFloat(), color.getBlueAsFloat(), alpha)
                .uv(u0, v0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(255)
                .normal(n, 0, 1, 0)
                .endVertex();

        buffer.vertex(m, xmax, deptY, zmin)
                .color(color.getRedAsFloat(), color.getGreenAsFloat(), color.getBlueAsFloat(), alpha)
                .uv(u1, v1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(255)
                .normal(n, 0, 1, 0)
                .endVertex();

        buffer.vertex(m, xmax, deptY, zmax)
                .color(color.getRedAsFloat(), color.getGreenAsFloat(), color.getBlueAsFloat(), alpha)
                .uv(u2, v2)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(255)
                .normal(n, 0, 1, 0)
                .endVertex();

        buffer.vertex(m, xmin, deptY, zmax)
                .color(color.getRedAsFloat(), color.getGreenAsFloat(), color.getBlueAsFloat(), alpha)
                .uv(u3, v3)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(255)
                .normal(n, 0, 1, 0)
                .endVertex();

    }


    private void renderRadarTracks(RadarBearingBlockEntity radar, MonitorBlockEntity blockEntity, PoseStack ms, MultiBufferSource bufferSource) {
        List<RadarTrack> tracks = radar.getEntityPositions();
        AtomicInteger depthCounter = new AtomicInteger(0);
        tracks.forEach(track -> renderTrack(track, blockEntity, radar, ms, bufferSource, depthCounter.getAndIncrement()));

    }

    private void renderTrack(RadarTrack track, MonitorBlockEntity monitor, RadarBearingBlockEntity radar, PoseStack ms, MultiBufferSource bufferSource, int depthMultiplier) {
        VertexConsumer buffer = getBuffer(bufferSource, track.contraption() ? MonitorSprite.CONTRAPTION_HITBOX : MonitorSprite.ENTITY_HITBOX);
        Matrix4f m = ms.last().pose();
        Matrix3f n = ms.last().normal();
        Color color = track.color();
        float alpha = 1f;
        float deptY = 0.95f + (depthMultiplier * 0.0001f);
        float size = monitor.getSize();
        float scale = radar.getRange();
        Direction monitorFacing = monitor.getBlockState().getValue(MonitorBlock.FACING);
        Vec3 relativePos = track.position().subtract(radar.getBlockPos().getCenter());
        relativePos = monitor.adjustRelativeVectorForFacing(relativePos, monitorFacing);
        float xOff = (float) (relativePos.x() / scale) * size / 2.0f;
        float zOff = (float) (relativePos.z() / scale) * size / 2.0f;

        //todo improve this, very hacky
        if (monitorFacing == Direction.NORTH) {
            xOff = -xOff;
            zOff = -zOff;
        }
        if (monitorFacing == Direction.WEST) {
            zOff = -zOff;
        }
        if (monitorFacing == Direction.EAST) {
            xOff = -xOff;
        }

        if (Math.abs(xOff) > .5f || Math.abs(zOff) > .5f)
            return;


        float xmin = 1 - size + (xOff * size);
        float zmin = 1 - size + (zOff * size);
        float xmax = xOff + 1;
        float zmax = zOff + 1;

        float fade = (track.scannedTime() - monitor.getLevel().getGameTime()) / 100f;

        renderVertices(buffer, m, n, color, alpha - Math.abs(fade * .99f), deptY, xmin, zmin, xmax, zmax);

        if (track.entityId().equals(monitor.hoveredEntity))
            renderVertices(getBuffer(bufferSource, MonitorSprite.TARGET_HOVERED), m, n, new Color(255, 255, 0), alpha, deptY, xmin, zmin, xmax, zmax);
        if (track.entityId().equals(monitor.selectedEntity))
            renderVertices(getBuffer(bufferSource, MonitorSprite.TARGET_SELECTED), m, n, new Color(255, 0, 0), alpha, deptY, xmin, zmin, xmax, zmax);
    }

    private VertexConsumer getBuffer(MultiBufferSource bufferSource, MonitorSprite sprite) {
        return bufferSource.getBuffer(ModRenderTypes.polygonOffset(sprite.getTexture()));
    }


    private float getOffset(double coordinate, float scale) {
        return (float) (coordinate / scale) / 2f;
    }


    private void renderVertices(VertexConsumer buffer, Matrix4f m, Matrix3f n, Color color, float alpha, float deptY, float xmin, float zmin, float xmax, float zmax) {
        float u0 = 0, v0 = 0, u1 = 1, v1 = 0, u2 = 1, v2 = 1, u3 = 0, v3 = 1;

        buffer.vertex(m, xmin, deptY, zmin)
                .color(color.getRedAsFloat(), color.getGreenAsFloat(), color.getBlueAsFloat(), alpha)
                .uv(u0, v0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(255)
                .normal(n, 0, 1, 0)
                .endVertex();

        buffer.vertex(m, xmax, deptY, zmin)
                .color(color.getRedAsFloat(), color.getGreenAsFloat(), color.getBlueAsFloat(), alpha)
                .uv(u1, v1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(255)
                .normal(n, 0, 1, 0)
                .endVertex();

        buffer.vertex(m, xmax, deptY, zmax)
                .color(color.getRedAsFloat(), color.getGreenAsFloat(), color.getBlueAsFloat(), alpha)
                .uv(u2, v2)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(255)
                .normal(n, 0, 1, 0)
                .endVertex();

        buffer.vertex(m, xmin, deptY, zmax)
                .color(color.getRedAsFloat(), color.getGreenAsFloat(), color.getBlueAsFloat(), alpha)
                .uv(u3, v3)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(255)
                .normal(n, 0, 1, 0)
                .endVertex();
    }

    private void renderBG(MonitorBlockEntity blockEntity, PoseStack ms, MultiBufferSource bufferSource, MonitorSprite monitorSprite) {
        int size = blockEntity.getSize();
        Matrix4f m = ms.last().pose();
        Matrix3f n = ms.last().normal();
        Color color = new Color(0, 255, 0);
        float alpha = .6f;
        float deptY = 0.94f;

        float minX = 1f - size;
        float minZ = 1f - size;
        float maxX = 1;
        float maxZ = 1;

        renderVertices(getBuffer(bufferSource, monitorSprite), m, n, color, alpha, deptY, minX, minZ, maxX, maxZ);

    }

    public void renderSweep(RadarBearingBlockEntity radar, MonitorBlockEntity controller, PoseStack ms, MultiBufferSource bufferSource) {
        if (!radar.isRunning())
            return;
        VertexConsumer buffer = bufferSource.getBuffer(ModRenderTypes.polygonOffset(MonitorSprite.RADAR_SWEEP.getTexture()));
        Matrix4f m = ms.last().pose();
        Matrix3f n = ms.last().normal();
        Color color = new Color(0, 255, 0);
        float alpha = 0.8f;
        Direction monitorFacing = controller.getBlockState().getValue(MonitorBlock.FACING);
        Direction radarFacing = radar.getReceiverFacing();
        float angleDiff = monitorFacing.toYRot() - radarFacing.toYRot();
        float angle = ((radar.getAngle() + angleDiff) % 360.0f) * (float) Math.PI / 180.0f;
        // System.out.println(angle);
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        //center to change when panning and partial rendering added
        float centerX = 0.5f;
        float centerY = 0.5f;
        float deptY = .945f;

        int size = controller.getSize();


        float u0 = centerX + (0 - centerX) * cos - (0 - centerY) * sin;
        float v0 = centerY + (0 - centerX) * sin + (0 - centerY) * cos;
        float u1 = centerX + (1 - centerX) * cos - (0 - centerY) * sin;
        float v1 = centerY + (1 - centerX) * sin + (0 - centerY) * cos;
        float u2 = centerX + (1 - centerX) * cos - (1 - centerY) * sin;
        float v2 = centerY + (1 - centerX) * sin + (1 - centerY) * cos;
        float u3 = centerX + (0 - centerX) * cos - (1 - centerY) * sin;
        float v3 = centerY + (0 - centerX) * sin + (1 - centerY) * cos;

        buffer
                .vertex(m, 1f - size, deptY, 1f - size)
                .color(color.getRedAsFloat(), color.getGreenAsFloat(), color.getBlueAsFloat(), alpha)
                .uv(u0, v0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(255)
                .normal(n, 0, 0, 0)
                .endVertex();

        buffer
                .vertex(m, 1, deptY, 1f - size)
                .color(color.getRedAsFloat(), color.getGreenAsFloat(), color.getBlueAsFloat(), alpha)
                .uv(u1, v1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(255)
                .normal(n, 0, 0, 0)
                .endVertex();

        buffer
                .vertex(m, 1, deptY, 1f)
                .color(color.getRedAsFloat(), color.getGreenAsFloat(), color.getBlueAsFloat(), alpha)
                .uv(u2, v2)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(255)
                .normal(n, 0, 0, 0)
                .endVertex();

        buffer
                .vertex(m, 1f - size, deptY, 1f)
                .color(color.getRedAsFloat(), color.getGreenAsFloat(), color.getBlueAsFloat(), alpha)
                .uv(u3, v3)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(255)
                .normal(n, 0, 0, 0)
                .endVertex();
    }

}
