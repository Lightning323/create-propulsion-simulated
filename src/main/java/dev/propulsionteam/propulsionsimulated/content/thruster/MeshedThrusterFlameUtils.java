package dev.propulsionteam.propulsionsimulated.content.thruster;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.content.thruster.thruster.ThrusterBlock;
import dev.propulsionteam.propulsionsimulated.content.thruster.thruster.ThrusterBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import org.joml.Vector3i;

import java.util.Optional;

public class MeshedThrusterFlameUtils {

    private static final ResourceLocation THRUSTER_FLAME_SHADER = CreatePropulsion.loc("thruster_flame");
    private static final float FLAME_SIZE = 2f;
    private static final float BLOCK_PIXEL = 1f / 16f;
    private static final float FLAME_PIXEL = BLOCK_PIXEL / FLAME_SIZE;
    protected static final float DISPLAY_THRESHOLD = 0.03f;

    private static void debug_drawRenderBoundingBox(ThrusterBlockEntity be, PoseStack ms, MultiBufferSource buffer) {
        //Debug render box
        AABB box = be.getRenderBoundingBox();
        BlockPos pos2 = be.getBlockPos();
        AABB localBox = box.move(-pos2.getX(), -pos2.getY(), -pos2.getZ());
        LevelRenderer.renderLineBox(ms, buffer.getBuffer(RenderType.lines()), localBox, 1, 0, 0, 1);
    }

    public static void renderMultiblockFlame(ThrusterBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, boolean soulFlame, int w) {
        final var state = be.getBlockState();
        final var facing = state.getValue(ThrusterBlock.FACING);
        Vector3i offset = new Vector3i(0, 0, 0);
        switch (facing) {
            case UP -> offset.set(-w + 1, 0, 0);//This is down
            case SOUTH -> offset.set(0, 0, 0);//this is north
            case NORTH -> offset.set(0 , w-1, -w + 1);//this is south
            case WEST -> offset.set(-w + 1, 1, 0);//this is east
            case EAST -> offset.set(0, 0, 0);//this is west
            default -> {//this is up
                offset.set(0, w - 1, 0);
            }
        }

        for (int x = 0; x < w; x++) {
            for (int z = 0; z < w; z++) {
                MeshedThrusterFlameUtils.renderMeshFlame(be, partialTicks, ms, buffer, true, x + offset.x, offset.y, z + offset.z);
            }
        }
    }

    public static void renderMeshFlame(ThrusterBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, boolean soulFlame, float offsetX, float offsetY, float offsetZ) {
        debug_drawRenderBoundingBox(be, ms, buffer);
        //Get the interpolated power
        float power = Mth.clamp(be.interpolatedPower.getValue(partialTicks), 0f, 1f);
//        System.out.println("Interpolated power: " + power);
        if (power < DISPLAY_THRESHOLD) return;

        final var state = be.getBlockState();
        final var pos = be.getBlockPos();
        final var facing = state.getValue(ThrusterBlock.FACING);
        var flameOffset = snapToBlockPixel(-1.5f + ((24 - 4 * Mth.clamp(power, 0.5f, 1f)) / 16f));
        var lengthMultiplier = snapToFlamePixel((power * 4f + 1f + (0.5f - power * power * power)));
        var widthMultiplier = snapToFlamePixel((power * 1.5f + 1));

        ms.pushPose();
        ms.translate(0.5f, 0.5f, 0.5f);
        ms.translate(facing.getStepX() * flameOffset, facing.getStepY() * flameOffset, facing.getStepZ() * flameOffset);
        rotateTowardsFacing(ms, facing);
        ms.translate(offsetX, offsetY, offsetZ);

        ms.mulPose(Axis.YP.rotation(getBillboardAngle(be, pos, facing, flameOffset, partialTicks)));


        final ShaderProgram shader = VeilRenderSystem.setShader(THRUSTER_FLAME_SHADER);
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(THRUSTER_FLAME_SHADER);

        if (shader == null) {
            System.out.println("Failed to set shader " + resource.isPresent());
            ms.popPose();
            return;
        }
        float shaderTime = (be.getLevel().getGameTime() + partialTicks) * 0.08f;
        shader.getUniformSafe("FlameRenderTime").setFloat(shaderTime);
        shader.getUniformSafe("Intensity").setFloat(Mth.clamp(power * 2f - .35f, 0.25f, 1.5f));
        shader.getUniformSafe("Palette").setFloat(soulFlame ? 1 : 0); //Soul Fire modifier
        shader.getUniformSafe("LengthMultiplier").setFloat(Math.max(lengthMultiplier, FLAME_PIXEL));
        shader.getUniformSafe("WidthMultiplier").setFloat(Math.max(widthMultiplier, FLAME_PIXEL));

        renderFlame(ms, FLAME_SIZE, lengthMultiplier, widthMultiplier);
        ms.popPose();
    }

    /**
     * Extends the render box in the direction of the thruster's meshed flame
     * @param be
     * @param box
     * @param widthInflation the amount of extra thickness of the new bounding box, to account for vector thrusters
     * @return
     */
    public static AABB inflateRenderBoundingBox(
            ThrusterBlockEntity be,
            AABB box,
            int widthInflation
    ) {
        if (be.isMeshedPlume() && be.interpolatedPower.getValue() < DISPLAY_THRESHOLD)
            return box;

        float partialTicks = Minecraft.getInstance()
                .getTimer()
                .getGameTimeDeltaPartialTick(false);

        float power = Mth.clamp(
                be.interpolatedPower.getValue(partialTicks),
                0f,
                1f
        );

        Direction facing = be.getBlockState().getValue(ThrusterBlock.FACING);

        double length = power * 8.0;
        double width = power * widthInflation;

        return switch (facing) {
            case UP, DOWN -> new AABB(
                    box.minX - width,
                    facing == Direction.UP ? box.minY - length : box.minY,
                    box.minZ - width,

                    box.maxX + width,
                    facing == Direction.UP ? box.maxY : box.maxY + length,
                    box.maxZ + width
            );
            case NORTH, SOUTH -> new AABB(
                    box.minX - width,
                    box.minY - width,
                    facing == Direction.SOUTH ? box.minZ - length : box.minZ,

                    box.maxX + width,
                    box.maxY + width,
                    facing == Direction.SOUTH ? box.maxZ : box.maxZ + length
            );
            case EAST, WEST -> new AABB(
                    facing == Direction.EAST ? box.minX - length : box.minX,
                    box.minY - width,
                    box.minZ - width,

                    facing == Direction.EAST ? box.maxX : box.maxX + length,
                    box.maxY + width,
                    box.maxZ + width
            );
        };
    }


    private static void rotateTowardsFacing(PoseStack poseStack, Direction facing) {
        switch (facing) {
            case UP -> poseStack.mulPose(Axis.ZP.rotation((float) Math.PI));
            case SOUTH -> poseStack.mulPose(Axis.XN.rotation((float) (Math.PI / 2f)));
            case NORTH -> poseStack.mulPose(Axis.XP.rotation((float) (Math.PI / 2f)));
            case WEST -> poseStack.mulPose(Axis.ZN.rotation((float) (Math.PI / 2f)));
            case EAST -> poseStack.mulPose(Axis.ZP.rotation((float) (Math.PI / 2f)));
            default -> {
            }
        }
    }

    private static float getBillboardAngle(ThrusterBlockEntity be, BlockPos pos, Direction facing, float offset, float partialTicks) {
        final var origin = pos.getCenter().add(facing.getStepX() * offset, facing.getStepY() * offset, facing.getStepZ() * offset);
        final var toCamera = getCameraPos(be, partialTicks).subtract(origin);
        final var local = toLocalFacingSpace(toCamera, facing);
        return (float) Math.atan2(local.x, local.z);
    }

    private static Vec3 toLocalFacingSpace(Vec3 vec, Direction facing) {
        return switch (facing) {
            case UP -> new Vec3(-vec.x, -vec.y, vec.z);
            case SOUTH -> new Vec3(vec.x, -vec.z, vec.y);
            case NORTH -> new Vec3(vec.x, vec.z, -vec.y);
            case WEST -> new Vec3(-vec.y, vec.x, vec.z);
            case EAST -> new Vec3(vec.y, -vec.x, vec.z);
            default -> vec;
        };
    }

    private static Vec3 getCameraPos(ThrusterBlockEntity be, float partialTicks) {
        final var mc = Minecraft.getInstance();
        var cam = mc.gameRenderer.getMainCamera().getPosition();

        if (be.getLevel() instanceof PonderLevel && mc.getCameraEntity() instanceof Entity camE)
            cam = camE.getPosition(partialTicks);
        if (Sable.HELPER.getContaining(be) instanceof SubLevel subLevel)
            cam = subLevel.logicalPose().transformPositionInverse(cam);

        return cam;
    }

    private static float snapToBlockPixel(float value) {
        return Math.round(value / BLOCK_PIXEL) * BLOCK_PIXEL;
    }

    private static float snapToFlamePixel(float value) {
        return Math.max(0f, Math.round(value / FLAME_PIXEL) * FLAME_PIXEL);
    }

    @SuppressWarnings("SameParameterValue")
    private static void renderFlame(final PoseStack poseStack, final float size, final float lengthMultiplier, final float widthMultiplier) {
        final var builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        final var halfSize = (size / 2f) * widthMultiplier;

        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();

        final var pose = poseStack.last().pose();
        builder.addVertex(pose, -halfSize, 0f, 0f).setUv(0f, 1f);
        builder.addVertex(pose, halfSize, 0f, 0f).setUv(1f, 1f);
        builder.addVertex(pose, halfSize, size * lengthMultiplier, 0f).setUv(1f, 0f);
        builder.addVertex(pose, -halfSize, size * lengthMultiplier, 0f).setUv(0f, 0f);

        BufferUploader.drawWithShader(builder.buildOrThrow());

        RenderSystem.disableDepthTest();
        RenderSystem.enableCull();
    }
}
