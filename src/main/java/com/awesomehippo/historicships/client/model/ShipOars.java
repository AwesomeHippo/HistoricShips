package com.awesomehippo.historicships.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

public final class ShipOars {
    public static final float REST_PITCH = 0.6f;

    public static void addOar(PartDefinition parent, String name, float x, float y, float zGunwale, boolean port, float length, int texU, int texV, CubeDeformation d0) {
        float blade = Math.min(4.6f, length * 0.34f);
        CubeListBuilder cubes = CubeListBuilder.create().texOffs(texU, texV).addBox(-0.45f, -0.45f, 0.0f, 0.9f, 0.9f, length, d0).texOffs(texU, texV).addBox(-0.32f, -1.75f, length - 0.2f, 0.64f, 3.5f, blade, d0);
        float yBase = port ? (float) Math.PI : 0.0f;
        parent.addOrReplaceChild(name, cubes, PartPose.offsetAndRotation(x, y, zGunwale, REST_PITCH, yBase, 0.0f));
    }

    public static void poseOar(ModelPart oar, boolean port, float phase, float intensity, float hard, float restPitch) {
        intensity = Mth.clamp(intensity, 0.0f, 1.0f);
        hard = Mth.clamp(hard, 0.0f, 1.0f);
        float swingAmp = (0.55f + 0.28f * hard) * intensity;
        float dipAmp = (0.22f + 0.12f * hard) * intensity;
        float swing = Mth.sin(phase) * swingAmp;
        float drive = Mth.cos(phase);
        float pitch = Mth.clamp(restPitch + drive * dipAmp, 0.18f, 1.15f);
        float feather = (0.5f + 0.5f * drive) * (0.10f + 0.05f * hard) * intensity;
        oar.xRot = pitch;
        if (port) {
            oar.yRot = (float) Math.PI + swing;
            oar.zRot = -feather;
        } else {
            oar.yRot = -swing;
            oar.zRot = feather;
        }
    }
}
