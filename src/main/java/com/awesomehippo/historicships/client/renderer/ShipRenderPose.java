package com.awesomehippo.historicships.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.util.Mth;

public final class ShipRenderPose {
    public static final Motion STANDARD = new Motion(0.18F, 0.028F, 0.012F, 0.015F, 0.005F, 0.8F, 0.02F, 0.006F, 0.011F, 0.002F, 0.016F, 0.004F, 0.024F, 0.0015F, 1.1F);
    public static final Motion QUINQUEREME = new Motion(0.12F, 0.026F, 0.010F, 0.014F, 0.004F, 0.7F, 0.018F, 0.005F, 0.010F, 0.0018F, 0.015F, 0.0035F, 0.022F, 0.0012F, 1.0F);

    public static void apply(PoseStack poseStack, float age, float yRot, float scale, boolean localPassenger, Motion m, float sinkProgress, float sinkRollDir) {
        float ride = localPassenger ? 0.0F : 0.85F;
        ride *= 1.0F - Mth.clamp(sinkProgress, 0.0F, 1.0F);
        float bob = (Mth.sin(age * m.bobF1) * m.bobA1 + Mth.sin(age * m.bobF2 + m.bobPhase) * m.bobA2) * ride;
        poseStack.translate(0.0F, m.yBase + bob, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yRot));
        float rock = (Mth.sin(age * m.rockF1) * m.rockA1 + Mth.sin(age * m.rockF2) * m.rockA2) * ride;
        float sway = (Mth.cos(age * m.swayF1) * m.swayA1 + Mth.cos(age * m.swayF2 + m.swayPhase) * m.swayA2) * ride;
        poseStack.mulPose(Axis.XP.rotation(rock));
        poseStack.mulPose(Axis.ZP.rotation(sway));
        if (sinkProgress > 0.0F) {
            float tilt = sinkProgress * sinkProgress;
            poseStack.mulPose(Axis.ZP.rotationDegrees(sinkRollDir * tilt * 24.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(tilt * 8.0F));
        }
        poseStack.scale(-scale, scale, scale);
        poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
    }

    public record Motion(float yBase, float bobF1, float bobA1, float bobF2, float bobA2, float bobPhase, float rockF1, float rockA1, float rockF2, float rockA2, float swayF1, float swayA1, float swayF2, float swayA2, float swayPhase) {}
}
