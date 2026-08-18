package com.awesomehippo.historicships.client.model;

import com.awesomehippo.historicships.client.renderer.OarShipRenderState;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class DrakkarModel extends EntityModel<OarShipRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(Identifier.fromNamespaceAndPath("historicships", "drakkar"), "main");

    public static final int TEX = 256;

    private static final int HULL = 0;
    private static final int HULL_V = 8;
    private static final int MAST = 136;
    private static final int MAST_V = 8;

    private static final int SAIL_W = 0;
    private static final int SAIL_W_V = 96;
    private static final int SAIL_R = 64;
    private static final int SAIL_R_V = 96;
    private static final int OAR = 184;
    private static final int OAR_V = 204;

    private static final int DECK = 4;
    private static final int DECK_V = 180;
    private static final int GOLD = 208;
    private static final int GOLD_V = 180;

    private static final int BLACK = 248;
    private static final int BLACK_V = 2;
    private static final int SHIELD_YEL = 128;
    private static final int SHIELD_YEL_V = 96;
    private static final int SHIELD_BLK = 192;
    private static final int SHIELD_BLK_V = 96;
    private static final int RIM = 224;
    private static final int RIM_V = 96;
    private static final int PENNANT = 184;
    private static final int PENNANT_V = 180;
    private static final int IRON = 228;
    private static final int IRON_V = 204;

    private static final int OAR_PAIRS = 8;

    private static final int SHIELD_COUNT = 7;
    private static final float SHIELD_PITCH = 6.0F;
    private static final float SHIELD_X0 = -18.0F;

    private static final float SHIELD_Y = 8.95F;

    private static final float SHIELD_Z = 7.15F;
    private static final float OAR_REST_PITCH = ShipOars.REST_PITCH;

    private static final float SAIL_MAX_BELLY = 7.8F;
    private static final float LINE = 0.32F;

    private final ModelPart[] oarsPort;
    private final ModelPart[] oarsStbd;
    private final ModelPart sailRoot;

    private final ModelPart[][] sailCells;
    private final ModelPart pennant;

    public DrakkarModel(ModelPart root) {
        super(root);
        ModelPart body = root.getChild("body");
        this.oarsPort = new ModelPart[OAR_PAIRS];
        this.oarsStbd = new ModelPart[OAR_PAIRS];
        for (int i = 0; i < OAR_PAIRS; i++) {
            this.oarsPort[i] = body.getChild("oar_p" + i);
            this.oarsStbd[i] = body.getChild("oar_s" + i);
        }
        ModelPart mast = body.getChild("mast");
        this.sailRoot = mast.getChild("sail_fixed");

        this.sailCells = SquareSail.resolveCells(this.sailRoot);
        this.pennant = mast.getChild("pennant");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation d0 = new CubeDeformation(0.0F);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);

        final float fillTop = 4.6F;
        final float floorY = 4.55F;
        final float wallTop = 7.2F;
        for (int i = 0; i < 2; i++) {
            float x0 = -20.0F + i * 20.0F;
            body.addOrReplaceChild("hull_fill" + i, CubeListBuilder.create().texOffs(HULL, HULL_V).addBox(x0, 0.0F, -6.2F, 20.0F, fillTop, 12.4F, d0), PartPose.ZERO);
        }

        for (int i = 0; i < 4; i++) {
            float x0 = -20.15F + i * 10.0F;
            body.addOrReplaceChild("floor_" + i, CubeListBuilder.create().texOffs(DECK, DECK_V).addBox(x0, floorY, -5.2F, 10.3F, 0.55F, 10.4F, d0), PartPose.ZERO);
        }

        for (int i = 0; i < 2; i++) {
            float x0 = -20.0F + i * 20.0F;
            float h = wallTop - floorY;
            body.addOrReplaceChild("wall_p" + i, CubeListBuilder.create().texOffs(HULL, HULL_V).addBox(x0, floorY, -6.2F, 20.0F, h, 1.15F, d0), PartPose.ZERO);
            body.addOrReplaceChild("wall_s" + i, CubeListBuilder.create().texOffs(HULL, HULL_V).addBox(x0, floorY, 5.05F, 20.0F, h, 1.15F, d0), PartPose.ZERO);
        }
        body.addOrReplaceChild("clinker_p", CubeListBuilder.create().texOffs(HULL, HULL_V).addBox(-18.0F, 2.0F, -6.55F, 36.0F, 1.2F, 0.5F, d0), PartPose.ZERO);
        body.addOrReplaceChild("clinker_s", CubeListBuilder.create().texOffs(HULL, HULL_V).addBox(-18.0F, 2.0F, 6.05F, 36.0F, 1.2F, 0.5F, d0), PartPose.ZERO);
        body.addOrReplaceChild("clinker2_p", CubeListBuilder.create().texOffs(HULL, HULL_V).addBox(-18.0F, 4.2F, -6.55F, 36.0F, 1.0F, 0.5F, d0), PartPose.ZERO);
        body.addOrReplaceChild("clinker2_s", CubeListBuilder.create().texOffs(HULL, HULL_V).addBox(-18.0F, 4.2F, 6.05F, 36.0F, 1.0F, 0.5F, d0), PartPose.ZERO);

        final float wallH = wallTop - floorY;

        body.addOrReplaceChild("hull_bow", CubeListBuilder.create().texOffs(HULL, HULL_V).addBox(19.5F, 0.4F, -5.2F, 11.0F, fillTop - 0.2F, 10.4F, d0), PartPose.ZERO);
        body.addOrReplaceChild("floor_bow", CubeListBuilder.create().texOffs(DECK, DECK_V).addBox(19.5F, floorY, -5.2F, 11.0F, 0.55F, 10.4F, d0), PartPose.ZERO);
        body.addOrReplaceChild("wall_bow_p", CubeListBuilder.create().texOffs(HULL, HULL_V).addBox(19.5F, floorY, -6.2F, 11.0F, wallH, 1.15F, d0), PartPose.ZERO);
        body.addOrReplaceChild("wall_bow_s", CubeListBuilder.create().texOffs(HULL, HULL_V).addBox(19.5F, floorY, 5.05F, 11.0F, wallH, 1.15F, d0), PartPose.ZERO);

        body.addOrReplaceChild("bow_bulkhead", CubeListBuilder.create().texOffs(HULL, HULL_V).addBox(29.0F, floorY, -5.2F, 2.8F, wallH, 10.4F, d0), PartPose.ZERO);

        body.addOrReplaceChild("stem_base", CubeListBuilder.create().texOffs(HULL, HULL_V).addBox(30.5F, 0.4F, -2.8F, 4.5F, wallTop - 0.4F, 5.6F, d0), PartPose.ZERO);

        body.addOrReplaceChild("sheer_bow", CubeListBuilder.create().texOffs(HULL, HULL_V).addBox(30.0F, 5.0F, -2.4F, 7.0F, 4.8F, 4.8F, d0), PartPose.ZERO);

        body.addOrReplaceChild("hull_stern", CubeListBuilder.create().texOffs(HULL, HULL_V).addBox(-30.5F, 0.4F, -5.2F, 11.0F, fillTop - 0.2F, 10.4F, d0), PartPose.ZERO);
        body.addOrReplaceChild("floor_stern", CubeListBuilder.create().texOffs(DECK, DECK_V).addBox(-30.5F, floorY, -5.2F, 11.0F, 0.55F, 10.4F, d0), PartPose.ZERO);
        body.addOrReplaceChild("wall_stern_p", CubeListBuilder.create().texOffs(HULL, HULL_V).addBox(-30.5F, floorY, -6.2F, 11.0F, wallH, 1.15F, d0), PartPose.ZERO);
        body.addOrReplaceChild("wall_stern_s", CubeListBuilder.create().texOffs(HULL, HULL_V).addBox(-30.5F, floorY, 5.05F, 11.0F, wallH, 1.15F, d0), PartPose.ZERO);

        body.addOrReplaceChild("stern_bulkhead", CubeListBuilder.create().texOffs(HULL, HULL_V).addBox(-31.8F, floorY, -5.2F, 2.8F, wallH, 10.4F, d0), PartPose.ZERO);

        body.addOrReplaceChild("stern_post_base", CubeListBuilder.create().texOffs(HULL, HULL_V).addBox(-35.0F, 0.4F, -2.8F, 4.5F, wallTop - 0.4F, 5.6F, d0), PartPose.ZERO);

        for (int i = 0; i < 2; i++) {
            float x0 = -24.0F + i * 24.0F;
            body.addOrReplaceChild("keel" + i, CubeListBuilder.create().texOffs(HULL, HULL_V).addBox(x0, -3.2F, -3.2F, 24.0F, 3.2F, 6.4F, d0), PartPose.ZERO);
        }
        body.addOrReplaceChild("keel_bow", CubeListBuilder.create().texOffs(HULL, HULL_V).addBox(24.0F, -2.2F, -2.2F, 10.0F, 2.6F, 4.4F, d0), PartPose.ZERO);
        body.addOrReplaceChild("keel_stern", CubeListBuilder.create().texOffs(HULL, HULL_V).addBox(-34.0F, -2.2F, -2.2F, 10.0F, 2.6F, 4.4F, d0), PartPose.ZERO);

        PartDefinition stem = body.addOrReplaceChild("stem_root", CubeListBuilder.create().texOffs(HULL, HULL_V).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, d0), PartPose.offsetAndRotation(35.5F, 6.8F, 0.0F, 0.0F, 0.0F, 0.20F));

        PartDefinition n1 = stem.addOrReplaceChild("neck_1", CubeListBuilder.create().texOffs(HULL, HULL_V).addBox(-1.85F, 0.0F, -1.85F, 3.7F, 5.5F, 3.7F, d0), PartPose.offsetAndRotation(0.0F, 5.6F, 0.0F, 0.0F, 0.0F, 0.52F));

        PartDefinition n2 = n1.addOrReplaceChild("neck_2", CubeListBuilder.create().texOffs(HULL, HULL_V).addBox(-1.7F, 0.0F, -1.7F, 3.4F, 5.0F, 3.4F, d0), PartPose.offsetAndRotation(0.0F, 5.2F, 0.0F, 0.0F, 0.0F, -0.80F));

        PartDefinition head = n2.addOrReplaceChild("dragon_head", CubeListBuilder.create(), PartPose.offsetAndRotation(0.15F, 4.6F, 0.0F, 0.0F, 0.0F, -0.25F));

        head.addOrReplaceChild("skull", CubeListBuilder.create().texOffs(HULL, HULL_V).addBox(-1.5F, -1.8F, -2.2F, 5.0F, 4.2F, 4.4F, d0), PartPose.ZERO);
        head.addOrReplaceChild("snout", CubeListBuilder.create().texOffs(HULL, HULL_V).addBox(3.0F, -1.2F, -1.5F, 4.0F, 2.2F, 3.0F, d0), PartPose.ZERO);

        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(HULL, HULL_V).addBox(2.2F, 1.55F, -1.25F, 4.3F, 1.15F, 2.5F, d0), PartPose.ZERO);
        head.addOrReplaceChild("crest", CubeListBuilder.create().texOffs(HULL, HULL_V).addBox(-1.5F, 2.0F, -1.2F, 3.0F, 2.8F, 2.4F, d0), PartPose.ZERO);
        head.addOrReplaceChild("eye_p", CubeListBuilder.create().texOffs(GOLD, GOLD_V).addBox(1.2F, 0.0F, -2.45F, 1.4F, 1.3F, 0.45F, d0), PartPose.ZERO);
        head.addOrReplaceChild("eye_s", CubeListBuilder.create().texOffs(GOLD, GOLD_V).addBox(1.2F, 0.0F, 2.0F, 1.4F, 1.3F, 0.45F, d0), PartPose.ZERO);

        body.addOrReplaceChild("stern_post", CubeListBuilder.create().texOffs(HULL, HULL_V).addBox(-36.2F, 4.0F, -1.5F, 3.0F, 10.0F, 3.0F, d0), PartPose.ZERO);
        body.addOrReplaceChild("stern_curl", CubeListBuilder.create().texOffs(HULL, HULL_V).addBox(-39.5F, 12.5F, -1.6F, 5.0F, 3.8F, 3.2F, d0), PartPose.ZERO);
        body.addOrReplaceChild("stern_curl_tip", CubeListBuilder.create().texOffs(HULL, HULL_V).addBox(-41.2F, 9.5F, -1.2F, 2.5F, 3.5F, 2.4F, d0), PartPose.ZERO);

        for (int i = 0; i < 4; i++) {
            float x0 = -28.0F + i * 14.0F;
            body.addOrReplaceChild("rail_p" + i, CubeListBuilder.create().texOffs(HULL, HULL_V).addBox(x0, 7.2F, -6.5F, 14.0F, 1.35F, 0.9F, d0), PartPose.ZERO);
            body.addOrReplaceChild("rail_s" + i, CubeListBuilder.create().texOffs(HULL, HULL_V).addBox(x0, 7.2F, 5.6F, 14.0F, 1.35F, 0.9F, d0), PartPose.ZERO);
        }

        for (int i = 0; i < SHIELD_COUNT; i++) {
            float x = SHIELD_X0 + i * SHIELD_PITCH;
            boolean yel = (i % 2 == 0);
            int faceU = yel ? SHIELD_YEL : SHIELD_BLK;
            int faceV = yel ? SHIELD_YEL_V : SHIELD_BLK_V;
            VikingShield.add(body, "sp" + i, x, SHIELD_Y, -SHIELD_Z, -1, faceU, faceV, RIM, RIM_V, IRON, IRON_V, d0);
            VikingShield.add(body, "ss" + i, x, SHIELD_Y, SHIELD_Z, +1, faceU, faceV, RIM, RIM_V, IRON, IRON_V, d0);
        }

        final float mastBaseY = 5.1F;
        final float mastH = 35.6F;
        final float mastTopY = mastBaseY + mastH;
        final float yardY = 32.0F;

        PartDefinition mast = body.addOrReplaceChild("mast", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(-1.25F, 0.0F, -1.25F, 2.5F, mastH, 2.5F, d0), PartPose.offset(0.0F, mastBaseY, 0.0F));
        mast.addOrReplaceChild("mast_top", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(-1.0F, mastH - 0.5F, -1.0F, 2.0F, 2.0F, 2.0F, d0), PartPose.ZERO);

        PartDefinition sail = SquareSail.add(mast, "sail_fixed", yardY, 1.28F, 22.0F, 13.5F, 12.0F, 0.95F, SAIL_W, SAIL_W_V, SAIL_R, SAIL_R_V, MAST, MAST_V, d0);

        sail.addOrReplaceChild("yard", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(-0.8F, -0.8F, -15.5F, 1.6F, 1.6F, 31.0F, d0), PartPose.ZERO);

        mast.addOrReplaceChild("pennant", CubeListBuilder.create().texOffs(PENNANT, PENNANT_V).addBox(-0.25F, -1.6F, 0.0F, 0.5F, 1.6F, 6.5F, d0).texOffs(SAIL_W, SAIL_W_V).addBox(-0.2F, -1.5F, 3.0F, 0.4F, 1.4F, 2.0F, d0), PartPose.offset(0.0F, mastH + 1.2F, 0.0F));

        final float truckY = mastTopY + 0.6F;

        addLineBetween(body, "stay_bow", 32.6F, 23.0F, 0.0F, 0.9F, truckY, 0.0F);

        addLineBetween(body, "stay_stern", -37.0F, 15.95F, 0.0F, -0.9F, truckY, 0.0F);

        for (int i = 0; i < OAR_PAIRS; i++) {
            float x = -14.0F + i * 4.0F;
            ShipOars.addOar(body, "oar_p" + i, x, 5.45F, -6.05F, true, 13.5F, OAR, OAR_V, d0);
            ShipOars.addOar(body, "oar_s" + i, x, 5.45F, 6.05F, false, 13.5F, OAR, OAR_V, d0);
        }

        return LayerDefinition.create(mesh, TEX, TEX);
    }

    private static void addLineBetween(PartDefinition body, String name, float x0, float y0, float z0, float x1, float y1, float z1) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float dz = z1 - z0;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.05F) {
            return;
        }
        final float pad = 0.40F;
        float inv = 1.0F / len;
        float ux = dx * inv;
        float uy = dy * inv;
        float uz = dz * inv;
        x0 -= ux * pad;
        y0 -= uy * pad;
        z0 -= uz * pad;
        x1 += ux * pad;
        y1 += uy * pad;
        z1 += uz * pad;
        dx = x1 - x0;
        dy = y1 - y0;
        dz = z1 - z0;
        len = Mth.sqrt(dx * dx + dy * dy + dz * dz);

        float xRot = (float) Math.acos(Mth.clamp(dy / len, -1.0F, 1.0F));
        float yRot = (float) Math.atan2(dx, dz);
        float h = LINE * 0.5F;
        body.addOrReplaceChild(name, CubeListBuilder.create().texOffs(BLACK, BLACK_V).addBox(-h, 0.0F, -h, LINE, len, LINE, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(x0, y0, z0, xRot, yRot, 0.0F));
    }

    @Override
    public void setupAnim(OarShipRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        float phase = state.rowPhase;
        float intensity = state.rowIntensity;
        float hard = state.hardAmount;
        float fill = state.sailFill;

        for (int i = 0; i < OAR_PAIRS; i++) {
            ShipOars.poseOar(this.oarsPort[i], true, phase, intensity, hard, OAR_REST_PITCH);
            ShipOars.poseOar(this.oarsStbd[i], false, phase, intensity, hard, OAR_REST_PITCH);
        }

        SquareSail.animate(this.sailRoot, this.sailCells, age, fill, SAIL_MAX_BELLY, 0.3F, 0.55F, 0.90F, 0.06F);

        this.pennant.xRot = Mth.sin(age * 0.08F) * 0.10F;
        this.pennant.yRot = Mth.sin(age * 0.05F) * 0.12F;
    }
}
