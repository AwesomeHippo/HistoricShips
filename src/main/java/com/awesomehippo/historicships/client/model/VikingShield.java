package com.awesomehippo.historicships.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

public final class VikingShield {
    private static final int WEDGES = 14;
    private static final int RIVETS = 10;
    private static final float WEDGE_STEP = (float) (Math.PI * 2.0 / WEDGES);
    private static final float RIVET_STEP = (float) (Math.PI * 2.0 / RIVETS);

    private VikingShield() {}

    public static void add(PartDefinition body, String name, float x, float y, float zFace, int outSign, int faceU, int faceV, int rimU, int rimV, int ironU, int ironV, CubeDeformation d0) {
        PartDefinition sh = body.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.offsetAndRotation(x, y, zFace, 0.0f, (outSign < 0 ? (float) Math.PI : 0.0f), 0.0f));
        float zRim = -0.356f;
        VikingShield.addRoundDisc(sh, "rim", 3.13f, 0.28f, zRim, rimU + 4, rimV + 4, d0);
        float z0 = 0.0f;
        float core = 1.881f;
        sh.addOrReplaceChild("face_core", CubeListBuilder.create().texOffs(faceU + 8, faceV + 8).addBox(-core, -core, z0 - 0.2f, core * 2.0f, core * 2.0f, 0.4f, d0), PartPose.ZERO);
        float wedgeLen = 2.85f - core * 0.48f;
        float wedgeW = ((float) Math.PI * 2f * 2.85f / WEDGES) * 1.1f;
        float wedgeInner = core * 0.42f;
        for (int i = 0; i < WEDGES; ++i) {
            float ang = i * WEDGE_STEP;
            sh.addOrReplaceChild("fw" + i, CubeListBuilder.create().texOffs(faceU + 8, faceV + 8).addBox(-wedgeW * 0.5f, wedgeInner, z0 - 0.192f, wedgeW, wedgeLen, 0.384f, d0), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, ang));
        }
        VikingShield.addRoundRing(sh, "face_band", 2.508f, 2.793f, 0.088f, 0.112f, rimU + 4, rimV + 4, d0);
        float zBoss = 0.23f;
        sh.addOrReplaceChild("boss_plate", CubeListBuilder.create().texOffs(ironU + 4, ironV + 4).addBox(-1.15f, -1.15f, zBoss, 2.3f, 2.3f, 0.18f, d0), PartPose.ZERO);
        sh.addOrReplaceChild("boss_dome", CubeListBuilder.create().texOffs(ironU + 4, ironV + 4).addBox(-0.72f, -0.72f, zBoss + 0.14f, 1.44f, 1.44f, 0.28f, d0), PartPose.ZERO);
        float rivR = 2.976f;
        float zRiv = zRim + 0.098f;
        for (int i = 0; i < RIVETS; ++i) {
            float ang = i * RIVET_STEP;
            float rx = Mth.sin(ang) * rivR;
            float ry = Mth.cos(ang) * rivR;
            sh.addOrReplaceChild("riv" + i, CubeListBuilder.create().texOffs(ironU + 4, ironV + 4).addBox(-0.15f, -0.15f, zRiv, 0.3f, 0.3f, 0.18f, d0), PartPose.offset(rx, ry, 0.0f));
        }
    }

    private static void addRoundDisc(PartDefinition parent, String prefix, float radius, float thick, float zCenter, int u, int v, CubeDeformation d0) {
        float core = radius * 0.66f;
        parent.addOrReplaceChild(prefix + "_c", CubeListBuilder.create().texOffs(u, v).addBox(-core, -core, zCenter - thick * 0.5f, core * 2.0f, core * 2.0f, thick, d0), PartPose.ZERO);
        float wedgeLen = radius - core * 0.48f;
        float wedgeW = ((float) Math.PI * 2f * radius / WEDGES) * 1.1f;
        float wedgeInner = core * 0.42f;
        for (int i = 0; i < WEDGES; ++i) {
            float ang = i * WEDGE_STEP;
            parent.addOrReplaceChild(prefix + "_w" + i, CubeListBuilder.create().texOffs(u, v).addBox(-wedgeW * 0.5f, wedgeInner, zCenter - thick * 0.48f, wedgeW, wedgeLen, thick * 0.96f, d0), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, ang));
        }
    }

    private static void addRoundRing(PartDefinition parent, String prefix, float rIn, float rOut, float thick, float zCenter, int u, int v, CubeDeformation d0) {
        float mid = (rIn + rOut) * 0.5f;
        float rad = (rOut - rIn) * 0.55f;
        float wedgeW = ((float) Math.PI * 2f * mid / WEDGES) * 1.12f;
        for (int i = 0; i < WEDGES; ++i) {
            float ang = i * WEDGE_STEP;
            parent.addOrReplaceChild(prefix + i, CubeListBuilder.create().texOffs(u, v).addBox(-wedgeW * 0.5f, mid - rad, zCenter - thick * 0.5f, wedgeW, rad * 2.0f, thick, d0), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, ang));
        }
    }
}
