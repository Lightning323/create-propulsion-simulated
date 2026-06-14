package dev.propulsionteam.propulsionsimulated.content.thruster;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.propulsionteam.propulsionsimulated.CreatePropulsion;
import dev.propulsionteam.propulsionsimulated.content.thruster.thruster.ThrusterBlock;
import dev.propulsionteam.propulsionsimulated.content.thruster.thruster.ThrusterBlockEntity;
import dev.propulsionteam.propulsionsimulated.content.thruster.vector_thruster.VectorThrusterBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import org.joml.*;

import java.lang.Math;

public class MeshedThrusterFlameUtils {

    private static final ResourceLocation THRUSTER_FLAME_SHADER = CreatePropulsion.loc("thruster_flame");
    private static final float FLAME_SIZE = 2f;
    private static final float BLOCK_PIXEL = 1f / 16f;
    private static final float FLAME_PIXEL = BLOCK_PIXEL / FLAME_SIZE;
    protected static final float DISPLAY_THRESHOLD = 0.03f;
    public static final float RENDER_BOX_FLAME_LENGTH = 6.0f;


    public static void renderMultiblockFlame(ThrusterBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int w) {
        final var state = be.getBlockState();
        final var facing = state.getValue(ThrusterBlock.FACING);
        Vector3i offset = new Vector3i(0, 0, 0);
        switch (facing) {
            case UP -> offset.set(-w + 1, 0, 0);//This is down
            case SOUTH -> offset.set(0, 0, 0);//this is north
            case NORTH -> offset.set(0, w - 1, -w + 1);//this is south
            case WEST -> offset.set(-w + 1, 1, 0);//this is east
            case EAST -> offset.set(0, 0, 0);//this is west
            default -> {//this is up
                offset.set(0, w - 1, 0);
            }
        }

        for (int x = 0; x < w; x++) {
            for (int z = 0; z < w; z++) {
                MeshedThrusterFlameUtils.renderMeshFlame(be, partialTicks, ms, buffer, be.isBluePlume(), x + offset.x, offset.y, z + offset.z, false);

            }
        }
    }

    public static void renderMeshFlame(ThrusterBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer) {
        renderMeshFlame(be, partialTicks, ms, buffer, be.isBluePlume(), 0, 0, 0, false);
    }

    public static void renderMeshFlame(ThrusterBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                                       boolean soulFlame, int offsetX, int offsetY, int offsetZ, boolean offsetPoseStack) {

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

        if (offsetPoseStack) {//TODO: Fix this when I learn how to properly rotate the billboard
            //V2 isnt really that bad, but It seemed like there was a bit of a tradeoff with how the billboard behaved
            ms.mulPose(Axis.YP.rotation(getBillboardAngleV2(be, pos, ms, partialTicks)));
        } else {
            ms.mulPose(Axis.YP.rotation(getBillboardAngleV1(be, pos, facing, flameOffset, partialTicks)));
        }

        final ShaderProgram shader = VeilRenderSystem.setShader(THRUSTER_FLAME_SHADER);
        if (shader != null) {
            float r = random01(
                    (pos.getX() + offsetX) * 73856093
                            ^ (pos.getY() + offsetY) * 19349663
                            ^ (pos.getZ() + offsetZ) * 83492791);
            float shaderTime = r + (be.getLevel().getGameTime() + partialTicks) * 0.1f;
            shader.getUniformSafe("FlameRenderTime").setFloat(shaderTime);
            shader.getUniformSafe("Intensity").setFloat(Mth.clamp(power * 2f - .35f, 0.25f, 1.5f));
            shader.getUniformSafe("Palette").setFloat(soulFlame ? 1 : 0); //Soul Fire modifier
            shader.getUniformSafe("LengthMultiplier").setFloat(Math.max(lengthMultiplier, FLAME_PIXEL));
            shader.getUniformSafe("WidthMultiplier").setFloat(Math.max(widthMultiplier, FLAME_PIXEL));
            renderFlame(ms, FLAME_SIZE, lengthMultiplier, widthMultiplier);
        }
        ms.popPose();
    }

    public static float random01(long seed) {
        long z = seed + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z ^= z >>> 31;
        return (float) ((z >>> 40) * (1.0 / (1L << 24)));
    }

    public static AABB inflateRenderBoundingBox(ThrusterBlockEntity be, AABB box) {
        final var state = be.getBlockState();
        Vec3 center = box.getCenter();
        float power = Mth.clamp(be.interpolatedPower.getValue(), 0f, 1f);
        double length = power * RENDER_BOX_FLAME_LENGTH;

        Vec3 flameEnd = switch (state.getValue(ThrusterBlock.FACING)) {
            case DOWN -> new Vec3(center.x, box.maxY + length, center.z);
            case UP -> new Vec3(center.x, box.minY - length, center.z);
            case SOUTH -> new Vec3(center.x, center.y, box.minZ - length);
            case NORTH -> new Vec3(center.x, center.y, box.maxZ + length);
            case WEST -> new Vec3(box.maxX + length, center.y, center.z);
            case EAST -> new Vec3(box.minX - length, center.y, center.z);
        };
        return fitPoint(box, flameEnd.x, flameEnd.y, flameEnd.z);
    }

    public static AABB inflateVectorRenderBoundingBox(VectorThrusterBlockEntity be, AABB box) {
        Vec3 center = box.getCenter();
        Direction dir = be.getBlockState().getValue(ThrusterBlock.FACING);
        float rotX = be.getInterpolatedVectorX(1);
        float rotY = be.getInterpolatedVectorY(1);
        float power = Mth.clamp(be.interpolatedPower.getValue(), 0f, 1f);
        double length = power * RENDER_BOX_FLAME_LENGTH;

        Quaternionf rotation = new Quaternionf()
                .rotateY((float) Math.toRadians(45));

        Vector3f direction = new Vector3f(0, rotY, rotX);
//        if (dir == Direction.UP) {
//            direction.set(rotY, 0, rotX);
//        } else if (dir == Direction.DOWN) {
//            direction.set(0, 0, rotY);
//        }
        rotation.transform(direction);
        Vector3d end = new Vector3d(center.x, center.y, center.z).fma(length, direction); // start + direction * length

        System.out.println("rotx=" + rotX + " roty=" + rotY + " dir=" + direction + " flameEnd=" + end + " center=" + center);
        return fitPoint(box, end.x, end.y, end.z);
    }


    public static AABB fitPoint(AABB box, double x, double y, double z) {
        return new AABB(
                Math.min(box.minX, x),
                Math.min(box.minY, y),
                Math.min(box.minZ, z),

                Math.max(box.maxX, x),
                Math.max(box.maxY, y),
                Math.max(box.maxZ, z)
        );
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

    private static float getBillboardAngleV1(ThrusterBlockEntity be, BlockPos pos, Direction facing, float offset, float partialTicks) {
        final var origin = pos.getCenter().add(facing.getStepX() * offset, facing.getStepY() * offset, facing.getStepZ() * offset);
        final var toCamera = getCameraPos(be, partialTicks).subtract(origin);
        final var local = toLocalFacingSpace(toCamera, facing);
        return (float) Math.atan2(local.x, local.z);
    }

    private static float getBillboardAngleV2(//TODO: Make this one unified method when I figure out how to do this right
                                             ThrusterBlockEntity be,
                                             BlockPos pos,
                                             PoseStack ms,
                                             float partialTicks
    ) {
        // Actual billboard pivot in world space
        Vector4f pivot = new Vector4f(0, 0, 0, 1);
        ms.last().pose().transform(pivot);

        Vec3 origin = new Vec3(
                pos.getX() + pivot.x,
                pos.getY() + pivot.y,
                pos.getZ() + pivot.z
        );

        // Camera direction in world space
        Vec3 toCamera = getCameraPos(be, partialTicks).subtract(origin);

        // Convert into billboard-local space
        Matrix3f invRot = new Matrix3f(ms.last().normal()).invert();

        Vector3f local = toCamera.toVector3f();
        invRot.transform(local);

        // Rotation around local Y
        return (float) Math.atan2(local.x(), local.z());
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
