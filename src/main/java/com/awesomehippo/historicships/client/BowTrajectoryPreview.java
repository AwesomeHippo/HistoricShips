package com.awesomehippo.historicships.client;

import com.awesomehippo.historicships.entity.NapoleonShipEntity;
import com.awesomehippo.historicships.entity.QuinqueremeEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class BowTrajectoryPreview {
    private static final double CANNON_GRAVITY = 0.022;
    private static final double STONE_GRAVITY = 0.036;
    private static final double INERTIA = 0.99;
    private static final int MAX_TICKS = 170;
    private static final int COLOR = 0xD0FFFFFF;
    private static final float WIDTH = 5.0F;

    private BowTrajectoryPreview() {}

    public static void render(SubmitCustomGeometryEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) {
            return;
        }
        if (!HistoricShipsKeys.FIRE_FRONT.isDown()) {
            return;
        }
        List<Vec3> points = trace(mc.player);
        if (points == null || points.size() < 2) {
            return;
        }
        Vec3 cam = event.getLevelRenderState().cameraRenderState.pos;
        event.getSubmitNodeCollector().submitCustomGeometry(event.getPoseStack(), RenderTypes.lines(), (pose, buffer) -> {
            for (int i = 1; i < points.size(); i++) {
                line(buffer, pose, cam, points.get(i - 1), points.get(i));
            }
        });
    }

    private static @Nullable List<Vec3> trace(LocalPlayer player) {
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof NapoleonShipEntity ship && ship.isConductor(player)) {
            Vec3 aim = ship.gunAimDirection(player, NapoleonShipEntity.BOW_AIM_MAX_YAW, NapoleonShipEntity.BOW_AIM_MAX_UP, -NapoleonShipEntity.BOW_AIM_MAX_DOWN);
            Vec3 shipVel = ship.getDeltaMovement();
            double speed = NapoleonShipEntity.BOW_SHELL_SPEED;
            Vec3 vel = new Vec3(aim.x * speed + shipVel.x * 0.85, aim.y * speed + shipVel.y * 0.15, aim.z * speed + shipVel.z * 0.85);
            return simulate(player.level(), ship.gunPos(), vel, CANNON_GRAVITY);
        }
        if (vehicle instanceof QuinqueremeEntity ship && ship.isConductor(player)) {
            Vec3 aim = ship.gunAimDirection(player, QuinqueremeEntity.TOWER_AIM_MAX_YAW, QuinqueremeEntity.TOWER_AIM_MAX_UP, QuinqueremeEntity.TOWER_AIM_MIN_UP);
            Vec3 shipVel = ship.getDeltaMovement();
            double speed = QuinqueremeEntity.STONE_SPEED;
            Vec3 vel = new Vec3(aim.x * speed + shipVel.x * 0.45, aim.y * speed + shipVel.y * 0.1, aim.z * speed + shipVel.z * 0.45);
            return simulate(player.level(), ship.gunPos(), vel, STONE_GRAVITY);
        }
        return null;
    }

    private static List<Vec3> simulate(Level level, Vec3 pos, Vec3 vel, double gravity) {
        List<Vec3> points = new ArrayList<>();
        points.add(pos);
        for (int t = 0; t < MAX_TICKS; t++) {
            vel = new Vec3(vel.x, vel.y - gravity, vel.z).scale(INERTIA);
            Vec3 next = pos.add(vel);
            BlockHitResult hit = level.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
            Vec3 end = hit.getType() == HitResult.Type.MISS ? next : hit.getLocation();
            points.add(end);
            pos = end;
            if (hit.getType() != HitResult.Type.MISS) {
                break;
            }
            if (level.getFluidState(BlockPos.containing(pos)).is(FluidTags.WATER)) {
                break;
            }
        }
        return points;
    }

    private static void line(VertexConsumer buffer, PoseStack.Pose pose, Vec3 cam, Vec3 a, Vec3 b) {
        float dx = (float) (b.x - a.x);
        float dy = (float) (b.y - a.y);
        float dz = (float) (b.z - a.z);
        buffer.addVertex(pose, (float) (a.x - cam.x), (float) (a.y - cam.y), (float) (a.z - cam.z)).setColor(COLOR).setNormal(pose, dx, dy, dz).setLineWidth(WIDTH);
        buffer.addVertex(pose, (float) (b.x - cam.x), (float) (b.y - cam.y), (float) (b.z - cam.z)).setColor(COLOR).setNormal(pose, dx, dy, dz).setLineWidth(WIDTH);
    }
}
