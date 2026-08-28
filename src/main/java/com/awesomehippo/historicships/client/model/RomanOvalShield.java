package com.awesomehippo.historicships.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.PartDefinition;

public final class RomanOvalShield {
    private RomanOvalShield() {}

    public static void add(PartDefinition body, String name, float x, float y, float zFace, int outSign, int faceU, int faceV, int rimU, int rimV, CubeDeformation d0) {
        PartDefinition sh = body.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.offsetAndRotation(x, y, zFace, 0.0F, outSign < 0 ? (float) Math.PI : 0.0F, 0.0F));
        int fu = faceU + 4;
        int fv = faceV + 4;
        int ru = rimU + 4;
        int rv = rimV + 4;
        slab(sh, "f0", fu, fv, 0.62F, 0.82F, 0.88F, 0.05F, 0.20F, d0);
        slab(sh, "f1", fu, fv, 1.00F, -0.62F, 1.56F, 0.05F, 0.22F, d0);
        slab(sh, "f2", fu, fv, 0.62F, -1.58F, 1.10F, 0.05F, 0.20F, d0);
        slab(sh, "r0", ru, rv, 0.74F, 0.88F, 0.86F, -0.05F, 0.12F, d0);
        slab(sh, "r1", ru, rv, 1.12F, -0.68F, 1.64F, -0.05F, 0.12F, d0);
        slab(sh, "r2", ru, rv, 0.74F, -1.64F, 1.10F, -0.05F, 0.12F, d0);
        slab(sh, "spine", ru, rv, 0.13F, -1.28F, 2.66F, 0.16F, 0.16F, d0);
        slab(sh, "umbo0", ru, rv, 0.40F, -0.35F, 0.80F, 0.20F, 0.16F, d0);
        slab(sh, "umbo1", ru, rv, 0.24F, -0.19F, 0.48F, 0.28F, 0.18F, d0);
    }

    private static void slab(PartDefinition sh, String name, int u, int v, float halfW, float y, float h, float z, float thick, CubeDeformation d0) {
        sh.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).addBox(-halfW, y, z, halfW * 2.0F, h, thick, d0), PartPose.ZERO);
    }
}
