package com.awesomehippo.historicships.client;

import com.awesomehippo.historicships.entity.StoredShipEntity;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

public final class ShipLook {
    private static final float FOV_STEP = 5.0F;
    private static final float FOV_MIN = -70.0F;
    private static final float FOV_MAX = 6.0F;
    private static final float FOV_FLOOR = 5.0F;
    private static final float SMOOTH = 0.15F;

    private static boolean on;
    private static CameraType savedCam;
    private static float savedYaw;
    private static float savedPitch;
    private static float camYaw;
    private static float camPitch;
    private static float lookFov;
    private static float smoothedFov;

    private ShipLook() {}

    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        boolean onShip = player != null && mc.level != null && player.getVehicle() instanceof StoredShipEntity;
        if (!onShip) {
            if (on) {
                stop(mc, player);
            }
            HistoricShipsKeys.LOOK_AROUND.consumeClick();
            return;
        }
        if (mc.screen != null) {
            HistoricShipsKeys.LOOK_AROUND.consumeClick();
            return;
        }
        if (HistoricShipsKeys.LOOK_AROUND.consumeClick()) {
            if (on) {
                stop(mc, player);
            } else {
                start(mc, player);
            }
        }
        if (on && mc.options.getCameraType().isFirstPerson()) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
    }

    public static void scroll(InputEvent.MouseScrollingEvent event) {
        if (!on || event.getScrollDeltaY() == 0.0) {
            return;
        }
        lookFov = Mth.clamp(lookFov - (float) event.getScrollDeltaY() * FOV_STEP, FOV_MIN, FOV_MAX);
        event.setCanceled(true);
    }

    public static void cameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!on) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !(player.getVehicle() instanceof StoredShipEntity)) {
            return;
        }
        camYaw += Mth.wrapDegrees(player.getYRot() - savedYaw);
        camPitch = Mth.clamp(camPitch + (player.getXRot() - savedPitch), -90.0F, 90.0F);
        freeze(player);
        event.setYaw(camYaw);
        event.setPitch(camPitch);
    }

    public static void cameraFov(ViewportEvent.ComputeFov event) {
        if (!on) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        float dt = Math.max(mc.getDeltaTracker().getRealtimeDeltaTicks(), 0.0F);
        float alpha = 1.0F - (float) Math.pow(1.0F - SMOOTH, dt);
        alpha = Mth.clamp(alpha, 0.0F, 1.0F);
        smoothedFov += (lookFov - smoothedFov) * alpha;
        if (Math.abs(smoothedFov - lookFov) < 0.05F) {
            smoothedFov = lookFov;
        }
        if (smoothedFov == 0.0F) {
            return;
        }
        float base = (float) event.getFOV();
        event.setFOV(Mth.clamp(base + smoothedFov, FOV_FLOOR, base + FOV_MAX));
    }

    public static void cameraDistance(CalculateDetachedCameraDistanceEvent event) {
        if (!on) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !(player.getVehicle() instanceof StoredShipEntity ship)) {
            return;
        }
        AABB box = ship.getBoundingBox();
        float along = (float) Math.max(box.getXsize(), box.getZsize());
        float dist = Mth.clamp(along * 0.72F + 6.0F, 10.0F, 32.0F);
        if (dist > event.getDistance()) {
            event.setDistance(dist);
        }
    }

    private static void start(Minecraft mc, LocalPlayer player) {
        on = true;
        savedCam = mc.options.getCameraType();
        savedYaw = player.getYRot();
        savedPitch = player.getXRot();
        camYaw = savedYaw;
        camPitch = savedPitch;
        lookFov = 0.0F;
        smoothedFov = 0.0F;
        freeze(player);
        if (savedCam.isFirstPerson()) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
    }

    private static void stop(Minecraft mc, LocalPlayer player) {
        if (player != null) {
            freeze(player);
        }
        mc.options.setCameraType(savedCam);
        lookFov = 0.0F;
        smoothedFov = 0.0F;
        on = false;
    }

    private static void freeze(LocalPlayer player) {
        player.setYRot(savedYaw);
        player.yRotO = savedYaw;
        player.setYHeadRot(savedYaw);
        player.yHeadRotO = savedYaw;
        player.yBodyRot = savedYaw;
        player.yBodyRotO = savedYaw;
        player.setXRot(savedPitch);
        player.xRotO = savedPitch;
    }
}
