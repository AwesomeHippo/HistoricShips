package com.awesomehippo.historicships.client.model;

import com.awesomehippo.historicships.client.renderer.NapoleonShipRenderState;

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

public class NapoleonShipModel extends EntityModel<NapoleonShipRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(Identifier.fromNamespaceAndPath("historicships", "napoleon_ship"), "main");

    public static final int TEX = 256;

    private static final int WOOD = 4;
    private static final int WOOD_V = 4;
    private static final int DECK = 132;
    private static final int DECK_V = 4;
    private static final int SAIL = 4;
    private static final int SAIL_V = 132;
    private static final int SAIL_ATLAS_W = 120;
    private static final int SAIL_ATLAS_H = 80;
    private static final int MAST = 132;
    private static final int MAST_V = 104;
    private static final int WHITE = 8;
    private static final int WHITE_V = 220;
    private static final int BLACK = 136;
    private static final int BLACK_V = 184;
    private static final int METAL = 184;
    private static final int METAL_V = 184;
    private static final int BLUE = 136;
    private static final int BLUE_V = 220;
    private static final int RED = 200;
    private static final int RED_V = 220;

    private static final int RIG = 250;
    private static final int RIG_V = 8;
    private static final float HELM_Y_WORLD = 13.55F;

    private static final int SAIL_COLS = SquareSail.COLS;
    private static final int SAIL_ROWS = SquareSail.ROWS;

    private static final int SAIL_COUNT = 6;

    private final ModelPart[] sailRoots;

    private final ModelPart[] sailCloths;

    private final ModelPart[] sailFurled;

    private final ModelPart[] sailFlyingGear;

    private final ModelPart[][][] sailCells;

    private final float[] sailMaxBelly;

    private static final int FLAG_SEGS = 6;
    private final ModelPart flagMainRoot;
    private final ModelPart[] flagMainSegs;
    private final ModelPart flagBowRoot;
    private final ModelPart[] flagBowSegs;

    private final ModelPart helm;
    private final ModelPart helmWheel;

    private static final float HELM_X = 40.5F;
    private static final float HELM_Y_COCKPIT = 12.6F;
    private static final float HELM_SCALE_COCKPIT = 0.54F;

    private static final float[] SAIL_MAX_BELLY = {8.0F, 5.0F, 9.5F, 5.8F, 7.0F, 4.2F};

    public NapoleonShipModel(ModelPart root) {
        super(root);
        ModelPart body = root.getChild("body");
        ModelPart mastFore = body.getChild("mast_fore");
        ModelPart mastMain = body.getChild("mast_main");
        ModelPart mastMiz = body.getChild("mast_mizzen");
        this.sailRoots = new ModelPart[] {
                    mastFore.getChild("sail_fore"),
                    mastFore.getChild("sail_fore_top"),
                    mastMain.getChild("sail_main"),
                    mastMain.getChild("sail_main_top"),
                    mastMiz.getChild("sail_mizzen"),
                    mastMiz.getChild("sail_miz_top"),
                };
        this.sailCells = new ModelPart[SAIL_COUNT][SAIL_COLS][SAIL_ROWS];
        this.sailCloths = new ModelPart[SAIL_COUNT];
        this.sailFurled = new ModelPart[SAIL_COUNT];
        this.sailFlyingGear = new ModelPart[SAIL_COUNT];
        this.sailMaxBelly = SAIL_MAX_BELLY;
        for (int s = 0; s < SAIL_COUNT; s++) {
            ModelPart sail = this.sailRoots[s];
            ModelPart cloth = sail.getChild("cloth");
            this.sailCloths[s] = cloth;
            this.sailFurled[s] = sail.getChild("furled");
            this.sailFlyingGear[s] = sail.getChild("flying_gear");
            for (int c = 0; c < SAIL_COLS; c++) {
                for (int r = 0; r < SAIL_ROWS; r++) {
                    this.sailCells[s][c][r] = cloth.getChild(cellName(c, r));
                }
            }
        }
        this.flagMainRoot = mastMain.getChild("flag_main");
        this.flagBowRoot = body.getChild("flag_bow");
        this.flagMainSegs = resolveFlagSegs(this.flagMainRoot);
        this.flagBowSegs = resolveFlagSegs(this.flagBowRoot);
        this.helm = body.getChild("helm");
        this.helmWheel = this.helm.getChild("wheel");
    }

    private static ModelPart[] resolveFlagSegs(ModelPart flagRoot) {
        ModelPart[] segs = new ModelPart[FLAG_SEGS];
        for (int i = 0; i < FLAG_SEGS; i++) {
            segs[i] = flagRoot.getChild("seg" + i);
        }
        return segs;
    }

    private static String cellName(int col, int row) {
        return "c" + col + "_r" + row;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation d0 = new CubeDeformation(0.0F);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);

        final float MID_Z = 11.0F;
        final float NARROW_Z = 10.0F;
        final float PROW_Z = 7.15F;
        final float MID_X0 = -19.0F;
        final float MID_X1 = 19.0F;
        final float BOW_X0 = 19.0F;
        final float BOW_X1 = 41.5F;
        final float AFT_X0 = -39.5F;
        final float AFT_X1 = -19.0F;
        final float PROW_X0 = 41.5F;
        final float PROW_X1 = 56.0F;
        final float STERN_X0 = -52.0F;
        final float DECK_Y = 13.15F;
        final float DECK_H = 0.90F;

        final float HULL_TOP = DECK_Y + 0.28F;
        final float RAIL_Y = DECK_Y + DECK_H;
        final float RAIL_H = 1.95F;
        final float RAIL_TOP = RAIL_Y + RAIL_H;
        final float RAIL_T = 0.95F;

        addHullSeg(body, "hull_m0", MID_X0, -6.5F, 0.0F, HULL_TOP, MID_Z, d0);
        addHullSeg(body, "hull_m1", -6.5F, 6.5F, 0.0F, HULL_TOP, MID_Z, d0);
        addHullSeg(body, "hull_m2", 6.5F, MID_X1, 0.0F, HULL_TOP, MID_Z, d0);
        addHullSeg(body, "hull_b0", BOW_X0, 30.5F, 0.0F, HULL_TOP, NARROW_Z, d0);
        addHullSeg(body, "hull_b1", 30.5F, BOW_X1, 0.0F, HULL_TOP, NARROW_Z, d0);

        addHullSeg(body, "prow", PROW_X0, PROW_X1, 0.0F, HULL_TOP, PROW_Z, d0);
        body.addOrReplaceChild("prow_stem", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(PROW_X1, 0.0F, -PROW_Z, 1.6F, HULL_TOP, PROW_Z * 2.0F, d0), PartPose.ZERO);
        body.addOrReplaceChild("prow_stem_mid", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(PROW_X1 + 1.6F, 0.25F, -(PROW_Z - 0.9F), 1.5F, HULL_TOP - 0.4F, (PROW_Z - 0.9F) * 2.0F, d0), PartPose.ZERO);
        body.addOrReplaceChild("prow_nose", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(PROW_X1 + 3.1F, 0.55F, -(PROW_Z - 2.1F), 1.4F, HULL_TOP - 0.9F, (PROW_Z - 2.1F) * 2.0F, d0), PartPose.ZERO);
        addHullSeg(body, "hull_a0", AFT_X0, -28.5F, 0.0F, HULL_TOP, NARROW_Z, d0);
        addHullSeg(body, "hull_a1", -28.5F, AFT_X1, 0.0F, HULL_TOP, NARROW_Z, d0);

        body.addOrReplaceChild("lock_mb_p", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(MID_X1 - 0.85F, 0.0F, -MID_Z - 0.12F, 1.7F, HULL_TOP, MID_Z - NARROW_Z + 0.22F, d0), PartPose.ZERO);
        body.addOrReplaceChild("lock_mb_s", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(MID_X1 - 0.85F, 0.0F, NARROW_Z - 0.10F, 1.7F, HULL_TOP, MID_Z - NARROW_Z + 0.22F, d0), PartPose.ZERO);
        body.addOrReplaceChild("lock_ma_p", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(AFT_X1 - 0.85F, 0.0F, -MID_Z - 0.12F, 1.7F, HULL_TOP, MID_Z - NARROW_Z + 0.22F, d0), PartPose.ZERO);
        body.addOrReplaceChild("lock_ma_s", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(AFT_X1 - 0.85F, 0.0F, NARROW_Z - 0.10F, 1.7F, HULL_TOP, MID_Z - NARROW_Z + 0.22F, d0), PartPose.ZERO);
        body.addOrReplaceChild("lock_bp_p", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(PROW_X0 - 0.85F, 0.0F, -NARROW_Z - 0.12F, 1.7F, HULL_TOP, NARROW_Z - PROW_Z + 0.28F, d0), PartPose.ZERO);
        body.addOrReplaceChild("lock_bp_s", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(PROW_X0 - 0.85F, 0.0F, PROW_Z - 0.16F, 1.7F, HULL_TOP, NARROW_Z - PROW_Z + 0.28F, d0), PartPose.ZERO);

        final float scarfH = HULL_TOP - 0.18F;
        addHullSeg(body, "scarf_m_a", -7.3F, -5.7F, 0.05F, scarfH - 0.05F, MID_Z - 0.05F, d0);
        addHullSeg(body, "scarf_m_b", 5.7F, 7.3F, 0.05F, scarfH - 0.05F, MID_Z - 0.05F, d0);
        addHullSeg(body, "scarf_mb", MID_X1 - 0.9F, MID_X1 + 0.9F, 0.05F, scarfH - 0.05F, NARROW_Z - 0.05F, d0);
        addHullSeg(body, "scarf_ma", AFT_X1 - 0.9F, AFT_X1 + 0.9F, 0.05F, scarfH - 0.05F, NARROW_Z - 0.05F, d0);
        addHullSeg(body, "scarf_b", 29.7F, 31.3F, 0.05F, scarfH - 0.05F, NARROW_Z - 0.05F, d0);
        addHullSeg(body, "scarf_bp", PROW_X0 - 0.9F, PROW_X0 + 0.9F, 0.05F, scarfH - 0.05F, PROW_Z - 0.05F, d0);
        addHullSeg(body, "scarf_a", -29.3F, -27.7F, 0.05F, scarfH - 0.05F, NARROW_Z - 0.05F, d0);
        addHullSeg(body, "scarf_as", AFT_X0 - 0.3F, AFT_X0 + 0.9F, 0.05F, scarfH - 0.05F, NARROW_Z - 0.05F, d0);

        addHullSeg(body, "keel_m0", -18.5F, 0.4F, -5.0F, 5.0F, 7.5F, d0);
        addHullSeg(body, "keel_m1", -0.4F, 18.5F, -5.0F, 5.0F, 7.5F, d0);
        addHullSeg(body, "keel_b", 17.5F, 42.5F, -4.0F, 4.0F, 6.5F, d0);
        addHullSeg(body, "keel_a", -38.5F, -17.5F, -4.0F, 4.0F, 6.5F, d0);

        final float deckMidZ = MID_Z - 0.28F;
        final float deckNarZ = NARROW_Z - 0.24F;
        final float deckProwZ = PROW_Z - 0.18F;
        addDeckSeg(body, "deck_m0", MID_X0, 0.0F, DECK_Y, DECK_H, deckMidZ, d0);
        addDeckSeg(body, "deck_m1", 0.0F, MID_X1, DECK_Y, DECK_H, deckMidZ, d0);
        addDeckSeg(body, "deck_b0", BOW_X0, 30.5F, DECK_Y, DECK_H, deckNarZ, d0);
        addDeckSeg(body, "deck_b1", 30.5F, BOW_X1, DECK_Y, DECK_H, deckNarZ, d0);
        addDeckSeg(body, "deck_prow", PROW_X0, PROW_X1, DECK_Y, DECK_H, deckProwZ, d0);

        final float underY = DECK_Y - 0.22F;
        final float underH = 0.45F;
        addHullSeg(body, "under_m", MID_X0 - 0.35F, MID_X1 + 0.95F, underY, underH, deckMidZ - 0.05F, d0);
        addHullSeg(body, "under_b", BOW_X0 - 0.95F, BOW_X1 + 0.95F, underY, underH, deckNarZ - 0.05F, d0);
        addHullSeg(body, "under_a", AFT_X0 - 0.5F, AFT_X1 + 0.95F, underY, underH, deckNarZ - 0.05F, d0);
        addHullSeg(body, "under_p", PROW_X0 - 0.95F, PROW_X1 + 0.4F, underY, underH, deckProwZ - 0.05F, d0);
        addHullSeg(body, "under_stern", STERN_X0 - 0.2F, AFT_X0 + 0.6F, underY, underH, deckNarZ - 0.15F, d0);
        body.addOrReplaceChild("deck_stem", CubeListBuilder.create().texOffs(DECK, DECK_V).addBox(PROW_X1, DECK_Y, -(PROW_Z - 0.95F), 1.6F, DECK_H, (PROW_Z - 0.95F) * 2.0F, d0), PartPose.ZERO);
        body.addOrReplaceChild("deck_stem_mid", CubeListBuilder.create().texOffs(DECK, DECK_V).addBox(PROW_X1 + 1.6F, DECK_Y, -(PROW_Z - 1.55F), 1.5F, DECK_H, (PROW_Z - 1.55F) * 2.0F, d0), PartPose.ZERO);
        body.addOrReplaceChild("deck_nose", CubeListBuilder.create().texOffs(DECK, DECK_V).addBox(PROW_X1 + 3.1F, DECK_Y, -(PROW_Z - 2.15F), 1.35F, DECK_H, (PROW_Z - 2.15F) * 2.0F, d0), PartPose.ZERO);

        addDeckSeg(body, "deck_a", AFT_X0, AFT_X1, DECK_Y, DECK_H, deckNarZ, d0);

        final float midRailOuter = MID_Z + 0.40F;
        final float narRailOuter = NARROW_Z + 0.40F;
        final float prowRailOuter = PROW_Z + 0.40F;
        body.addOrReplaceChild("rail_m_p", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(MID_X0, RAIL_Y, -midRailOuter, MID_X1 - MID_X0, RAIL_H, RAIL_T, d0), PartPose.ZERO);
        body.addOrReplaceChild("rail_m_s", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(MID_X0, RAIL_Y, midRailOuter - RAIL_T, MID_X1 - MID_X0, RAIL_H, RAIL_T, d0), PartPose.ZERO);
        body.addOrReplaceChild("rail_b_p", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(BOW_X0, RAIL_Y, -narRailOuter, BOW_X1 - BOW_X0, RAIL_H, RAIL_T, d0), PartPose.ZERO);
        body.addOrReplaceChild("rail_b_s", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(BOW_X0, RAIL_Y, narRailOuter - RAIL_T, BOW_X1 - BOW_X0, RAIL_H, RAIL_T, d0), PartPose.ZERO);
        body.addOrReplaceChild("rail_a_p", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(AFT_X0, RAIL_Y, -narRailOuter, AFT_X1 - AFT_X0, RAIL_H, RAIL_T, d0), PartPose.ZERO);
        body.addOrReplaceChild("rail_a_s", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(AFT_X0, RAIL_Y, narRailOuter - RAIL_T, AFT_X1 - AFT_X0, RAIL_H, RAIL_T, d0), PartPose.ZERO);

        body.addOrReplaceChild("rail_pr_p", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(PROW_X0, RAIL_Y, -prowRailOuter, PROW_X1 - PROW_X0 + 0.4F, RAIL_H, 0.85F, d0), PartPose.ZERO);
        body.addOrReplaceChild("rail_pr_s", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(PROW_X0, RAIL_Y, prowRailOuter - 0.85F, PROW_X1 - PROW_X0 + 0.4F, RAIL_H, 0.85F, d0), PartPose.ZERO);

        final float headX = PROW_X1 + 0.05F;
        final float deckTopY = DECK_Y + DECK_H;
        final float headH = RAIL_TOP - deckTopY;
        body.addOrReplaceChild("fore_head_bulk", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(headX, deckTopY, -(PROW_Z - 0.08F), 1.20F, headH, (PROW_Z - 0.08F) * 2.0F, d0), PartPose.ZERO);
        body.addOrReplaceChild("fore_stem_cap", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(headX + 0.05F, RAIL_TOP - 0.40F, -1.6F, 1.10F, 0.50F, 3.2F, d0), PartPose.ZERO);
        body.addOrReplaceChild("rail_head", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(headX + 0.10F, RAIL_Y + 0.08F, -prowRailOuter + 0.08F, 0.90F, RAIL_H - 0.14F, prowRailOuter * 2.0F - 0.16F, d0), PartPose.ZERO);

        body.addOrReplaceChild("rail_scarf_mb_p", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(MID_X1 - 0.5F, RAIL_Y + 0.05F, -midRailOuter - 0.08F, 1.0F, RAIL_H - 0.05F, midRailOuter - narRailOuter + 0.20F, d0), PartPose.ZERO);
        body.addOrReplaceChild("rail_scarf_mb_s", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(MID_X1 - 0.5F, RAIL_Y + 0.05F, narRailOuter - 0.12F, 1.0F, RAIL_H - 0.05F, midRailOuter - narRailOuter + 0.20F, d0), PartPose.ZERO);
        body.addOrReplaceChild("rail_scarf_ma_p", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(AFT_X1 - 0.5F, RAIL_Y + 0.05F, -midRailOuter - 0.08F, 1.0F, RAIL_H - 0.05F, midRailOuter - narRailOuter + 0.20F, d0), PartPose.ZERO);
        body.addOrReplaceChild("rail_scarf_ma_s", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(AFT_X1 - 0.5F, RAIL_Y + 0.05F, narRailOuter - 0.12F, 1.0F, RAIL_H - 0.05F, midRailOuter - narRailOuter + 0.20F, d0), PartPose.ZERO);
        body.addOrReplaceChild("rail_scarf_bp_p", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(PROW_X0 - 0.2F, RAIL_Y + 0.05F, -narRailOuter - 0.08F, 1.0F, RAIL_H - 0.05F, narRailOuter - prowRailOuter + 0.20F, d0), PartPose.ZERO);
        body.addOrReplaceChild("rail_scarf_bp_s", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(PROW_X0 - 0.2F, RAIL_Y + 0.05F, prowRailOuter - 0.12F, 1.0F, RAIL_H - 0.05F, narRailOuter - prowRailOuter + 0.20F, d0), PartPose.ZERO);

        final float WALE_T = 0.28F;
        final float WALE_H = 2.45F;

        final float[] gunXs = {-34.0F, -26.0F, -12.0F, -4.0F, 4.0F, 12.0F, 24.0F, 32.0F};
        final float[] gunYs = {4.5F, 8.6F};

        for (int row = 0; row < gunYs.length; row++) {
            float yBot = gunYs[row] - WALE_H * 0.5F;

            body.addOrReplaceChild("wale_p_aft_r" + row, CubeListBuilder.create().texOffs(WHITE, WHITE_V).addBox(STERN_X0, yBot, -NARROW_Z - WALE_T, AFT_X1 - STERN_X0 + 0.4F, WALE_H, WALE_T, d0), PartPose.ZERO);
            body.addOrReplaceChild("wale_s_aft_r" + row, CubeListBuilder.create().texOffs(WHITE, WHITE_V).addBox(STERN_X0, yBot, NARROW_Z, AFT_X1 - STERN_X0 + 0.4F, WALE_H, WALE_T, d0), PartPose.ZERO);

            body.addOrReplaceChild("wale_p_mid_r" + row, CubeListBuilder.create().texOffs(WHITE, WHITE_V).addBox(MID_X0 - 0.15F, yBot, -MID_Z - WALE_T, MID_X1 - MID_X0 + 0.3F, WALE_H, WALE_T, d0), PartPose.ZERO);
            body.addOrReplaceChild("wale_s_mid_r" + row, CubeListBuilder.create().texOffs(WHITE, WHITE_V).addBox(MID_X0 - 0.15F, yBot, MID_Z, MID_X1 - MID_X0 + 0.3F, WALE_H, WALE_T, d0), PartPose.ZERO);

            body.addOrReplaceChild("wale_p_bow_r" + row, CubeListBuilder.create().texOffs(WHITE, WHITE_V).addBox(BOW_X0 - 0.2F, yBot, -NARROW_Z - WALE_T, BOW_X1 - BOW_X0 - 1.5F, WALE_H, WALE_T, d0), PartPose.ZERO);
            body.addOrReplaceChild("wale_s_bow_r" + row, CubeListBuilder.create().texOffs(WHITE, WHITE_V).addBox(BOW_X0 - 0.2F, yBot, NARROW_Z, BOW_X1 - BOW_X0 - 1.5F, WALE_H, WALE_T, d0), PartPose.ZERO);

            body.addOrReplaceChild("wale_step_aft_p_r" + row, CubeListBuilder.create().texOffs(WHITE, WHITE_V).addBox(AFT_X1 - 0.35F, yBot, -MID_Z - WALE_T, 0.7F, WALE_H, MID_Z - NARROW_Z + WALE_T, d0), PartPose.ZERO);
            body.addOrReplaceChild("wale_step_aft_s_r" + row, CubeListBuilder.create().texOffs(WHITE, WHITE_V).addBox(AFT_X1 - 0.35F, yBot, NARROW_Z, 0.7F, WALE_H, MID_Z - NARROW_Z + WALE_T, d0), PartPose.ZERO);
            body.addOrReplaceChild("wale_step_bow_p_r" + row, CubeListBuilder.create().texOffs(WHITE, WHITE_V).addBox(MID_X1 - 0.35F, yBot, -MID_Z - WALE_T, 0.7F, WALE_H, MID_Z - NARROW_Z + WALE_T, d0), PartPose.ZERO);
            body.addOrReplaceChild("wale_step_bow_s_r" + row, CubeListBuilder.create().texOffs(WHITE, WHITE_V).addBox(MID_X1 - 0.35F, yBot, NARROW_Z, 0.7F, WALE_H, MID_Z - NARROW_Z + WALE_T, d0), PartPose.ZERO);
        }

        final float portW = 1.55F;
        final float portH = 1.45F;
        final float portT = 0.20F;
        final float barrelOut = 2.15F;
        final float barrelIn = 0.7F;
        final float barrelT = 0.72F;
        final float muzzleT = 0.95F;
        final float muzzleL = 0.45F;

        for (int row = 0; row < gunYs.length; row++) {
            float y = gunYs[row];
            for (int i = 0; i < gunXs.length; i++) {
                float x = gunXs[i];
                final boolean mid = x > AFT_X1 + 0.5F && x < MID_X1 - 0.5F;
                final float faceP = mid ? -MID_Z : -NARROW_Z;
                final float faceS = mid ? MID_Z : NARROW_Z;
                final float outerP = faceP - WALE_T;
                final float outerS = faceS + WALE_T;

                body.addOrReplaceChild("gunport_p_r" + row + "_" + i, CubeListBuilder.create().texOffs(BLACK, BLACK_V).addBox(x - portW * 0.5F, y - portH * 0.5F, outerP - portT, portW, portH, portT + 0.05F, d0), PartPose.ZERO);
                body.addOrReplaceChild("gunport_s_r" + row + "_" + i, CubeListBuilder.create().texOffs(BLACK, BLACK_V).addBox(x - portW * 0.5F, y - portH * 0.5F, outerS - 0.05F, portW, portH, portT + 0.05F, d0), PartPose.ZERO);

                PartDefinition gunP = body.addOrReplaceChild("gun_p_r" + row + "_" + i, CubeListBuilder.create(), PartPose.offset(x, y, faceP));
                gunP.addOrReplaceChild("barrel", CubeListBuilder.create().texOffs(METAL, METAL_V).addBox(-barrelT * 0.5F, -barrelT * 0.5F, -barrelOut, barrelT, barrelT, barrelOut + barrelIn, d0), PartPose.ZERO);
                gunP.addOrReplaceChild("muzzle", CubeListBuilder.create().texOffs(METAL, METAL_V).addBox(-muzzleT * 0.5F, -muzzleT * 0.5F, -(barrelOut + muzzleL * 0.15F), muzzleT, muzzleT, muzzleL, d0), PartPose.ZERO);

                PartDefinition gunS = body.addOrReplaceChild("gun_s_r" + row + "_" + i, CubeListBuilder.create(), PartPose.offset(x, y, faceS));
                gunS.addOrReplaceChild("barrel", CubeListBuilder.create().texOffs(METAL, METAL_V).addBox(-barrelT * 0.5F, -barrelT * 0.5F, -barrelIn, barrelT, barrelT, barrelOut + barrelIn, d0), PartPose.ZERO);
                gunS.addOrReplaceChild("muzzle", CubeListBuilder.create().texOffs(METAL, METAL_V).addBox(-muzzleT * 0.5F, -muzzleT * 0.5F, barrelOut - muzzleL * 0.85F, muzzleT, muzzleT, muzzleL, d0), PartPose.ZERO);
            }
        }

        final float sternTop = DECK_Y + DECK_H + 0.12F;
        body.addOrReplaceChild("stern_body", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(STERN_X0, 0.0F, -NARROW_Z, AFT_X0 - STERN_X0, sternTop, NARROW_Z * 2.0F, d0), PartPose.ZERO);
        body.addOrReplaceChild("stern_mid", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(-54.5F, 0.15F, -8.5F, STERN_X0 - (-54.5F), sternTop - 0.15F, 17.0F, d0), PartPose.ZERO);
        body.addOrReplaceChild("stern_tip", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(-55.8F, 0.45F, -7.0F, -54.5F - (-55.8F), sternTop - 0.45F, 14.0F, d0), PartPose.ZERO);

        body.addOrReplaceChild("counter0", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(-54.5F, -2.2F, -7.5F, 5.0F, 2.8F, 15.0F, d0), PartPose.ZERO);
        body.addOrReplaceChild("counter1", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(-55.8F, -1.2F, -5.5F, 2.5F, 2.0F, 11.0F, d0), PartPose.ZERO);

        body.addOrReplaceChild("keel_stern", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(-54.0F, -3.5F, -5.5F, 17.0F, 3.5F, 11.0F, d0), PartPose.ZERO);

        final float swH = 2.45F;
        final float wT = 0.34F;

        final float bodyX0 = -52.0F;
        final float midX0 = -54.5F;
        final float tipX0 = -55.8F;
        final float tipFaceX = -56.0F;
        final float halfBody = 10.0F;
        final float halfMid = 8.5F;
        final float halfTip = 7.0F;
        for (int row = 0; row < 2; row++) {
            float yb = (row == 0 ? 4.5F : 8.6F) - swH * 0.5F;

            body.addOrReplaceChild("sw_mid_p_r" + row, CubeListBuilder.create().texOffs(WHITE, WHITE_V).addBox(midX0 - 0.15F, yb, -halfMid - wT, midX0 + 3.2F - (midX0 - 0.15F), swH, wT, d0), PartPose.ZERO);
            body.addOrReplaceChild("sw_mid_s_r" + row, CubeListBuilder.create().texOffs(WHITE, WHITE_V).addBox(midX0 - 0.15F, yb, halfMid, midX0 + 3.2F - (midX0 - 0.15F), swH, wT, d0), PartPose.ZERO);

            body.addOrReplaceChild("sw_tip_p_r" + row, CubeListBuilder.create().texOffs(WHITE, WHITE_V).addBox(tipX0 - 0.25F, yb, -halfTip - wT, -tipX0 + 0.25F + bodyX0 + 0.3F, swH, wT, d0), PartPose.ZERO);
            body.addOrReplaceChild("sw_tip_s_r" + row, CubeListBuilder.create().texOffs(WHITE, WHITE_V).addBox(tipX0 - 0.25F, yb, halfTip, -tipX0 + 0.25F + bodyX0 + 0.3F, swH, wT, d0), PartPose.ZERO);

            body.addOrReplaceChild("sw_face_r" + row, CubeListBuilder.create().texOffs(WHITE, WHITE_V).addBox(tipFaceX, yb, -halfTip - 0.05F, wT + 0.1F, swH, halfTip * 2.0F + 0.1F, d0), PartPose.ZERO);

            body.addOrReplaceChild("sw_corner_p_r" + row, CubeListBuilder.create().texOffs(WHITE, WHITE_V).addBox(tipFaceX, yb, -halfTip - wT, wT + 0.35F, swH, wT + 0.35F, d0), PartPose.ZERO);

            body.addOrReplaceChild("sw_corner_s_r" + row, CubeListBuilder.create().texOffs(WHITE, WHITE_V).addBox(tipFaceX, yb, halfTip - 0.35F, wT + 0.35F, swH, wT + 0.35F, d0), PartPose.ZERO);

            body.addOrReplaceChild("sw_step0_p_r" + row, CubeListBuilder.create().texOffs(WHITE, WHITE_V).addBox(bodyX0 - 0.2F, yb, -halfBody - 0.02F, 0.55F, swH, halfBody - halfMid + 0.15F, d0), PartPose.ZERO);
            body.addOrReplaceChild("sw_step0_s_r" + row, CubeListBuilder.create().texOffs(WHITE, WHITE_V).addBox(bodyX0 - 0.2F, yb, halfMid - 0.1F, 0.55F, swH, halfBody - halfMid + 0.15F, d0), PartPose.ZERO);

            body.addOrReplaceChild("sw_step1_p_r" + row, CubeListBuilder.create().texOffs(WHITE, WHITE_V).addBox(midX0 - 0.15F, yb, -halfMid - 0.02F, 0.55F, swH, halfMid - halfTip + 0.15F, d0), PartPose.ZERO);
            body.addOrReplaceChild("sw_step1_s_r" + row, CubeListBuilder.create().texOffs(WHITE, WHITE_V).addBox(midX0 - 0.15F, yb, halfTip - 0.1F, 0.55F, swH, halfMid - halfTip + 0.15F, d0), PartPose.ZERO);
        }

        final float stemTipX = PROW_X1 + 3.1F + 1.4F;
        final float bowGunY = 6.4F;
        final float[] bowGunZs = {-2.9F, 2.9F};
        final float bowPlaqueHalf = 1.65F;
        final float bowPlaqueT = 0.55F;

        final float bowPlaqueX = stemTipX - 0.08F;
        final float bowPortW = 1.7F;
        final float bowPortH = 1.55F;
        final float bowPortT = 0.30F;
        final float bowBarrelOut = 2.85F;
        final float bowBarrelIn = 0.55F;
        final float bowBarrelT = 0.95F;
        final float bowMuzzleT = 1.18F;
        final float bowMuzzleL = 0.58F;
        final float bowPlaqueFront = bowPlaqueX + bowPlaqueT;
        final float bowPortX = bowPlaqueFront + 0.03F;
        for (int i = 0; i < bowGunZs.length; i++) {
            float z = bowGunZs[i];
            body.addOrReplaceChild("bow_plaque_" + i, CubeListBuilder.create().texOffs(WHITE, WHITE_V).addBox(bowPlaqueX, bowGunY - bowPlaqueHalf, z - bowPlaqueHalf, bowPlaqueT, bowPlaqueHalf * 2.0F, bowPlaqueHalf * 2.0F, d0), PartPose.ZERO);
            body.addOrReplaceChild("bow_port_" + i, CubeListBuilder.create().texOffs(BLACK, BLACK_V).addBox(bowPortX, bowGunY - bowPortH * 0.5F, z - bowPortW * 0.5F, bowPortT, bowPortH, bowPortW, d0), PartPose.ZERO);
            PartDefinition bowGun = body.addOrReplaceChild("bow_gun_" + i, CubeListBuilder.create(), PartPose.offset(bowPlaqueFront, bowGunY, z));
            bowGun.addOrReplaceChild("barrel", CubeListBuilder.create().texOffs(METAL, METAL_V).addBox(-bowBarrelIn, -bowBarrelT * 0.5F, -bowBarrelT * 0.5F, bowBarrelOut + bowBarrelIn, bowBarrelT, bowBarrelT, d0), PartPose.ZERO);
            bowGun.addOrReplaceChild("muzzle", CubeListBuilder.create().texOffs(METAL, METAL_V).addBox(bowBarrelOut - bowMuzzleL * 0.85F, -bowMuzzleT * 0.5F, -bowMuzzleT * 0.5F, bowMuzzleL, bowMuzzleT, bowMuzzleT, d0), PartPose.ZERO);
        }

        final float deckY0 = DECK_Y + DECK_H;
        final float cabinBot = deckY0 - 0.10F;
        final float galMidY = 16.0F;
        final float cabinTop = 18.05F;
        final float poopY = cabinTop - 0.04F;
        final float gAft = -57.0F;
        final float gJoin = -38.0F;
        final float cabinHalfZ = 8.85F;
        final float galOuterZ = 9.35F;

        body.addOrReplaceChild("deck_stern", CubeListBuilder.create().texOffs(DECK, DECK_V).addBox(-54.0F, DECK_Y, -deckNarZ, AFT_X0 - (-54.0F), DECK_H, deckNarZ * 2.0F, d0), PartPose.ZERO);

        final float cabinX0 = gAft + 0.88F;
        final float cabinX1 = -37.6F;
        body.addOrReplaceChild("cabin_core", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(cabinX0, cabinBot, -cabinHalfZ, cabinX1 - cabinX0, cabinTop - cabinBot, cabinHalfZ * 2.0F, d0), PartPose.ZERO);

        body.addOrReplaceChild("stern_under_gal", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(gAft, 0.40F, -7.15F, -55.7F - gAft, sternTop - 0.40F, 14.3F, d0), PartPose.ZERO);
        body.addOrReplaceChild("stern_gal_join", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(gAft, sternTop - 0.35F, -(cabinHalfZ - 0.05F), AFT_X0 - gAft + 0.4F, cabinBot - (sternTop - 0.35F) + 0.25F, (cabinHalfZ - 0.05F) * 2.0F, d0), PartPose.ZERO);

        body.addOrReplaceChild("gal_lo_face", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(gAft, cabinBot, -(cabinHalfZ - 0.10F), 0.90F, galMidY - cabinBot, (cabinHalfZ - 0.10F) * 2.0F, d0), PartPose.ZERO);
        body.addOrReplaceChild("gal_lo_p", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(cabinX0 + 0.05F, cabinBot, -galOuterZ, 11.2F, galMidY - cabinBot, 0.50F, d0), PartPose.ZERO);
        body.addOrReplaceChild("gal_lo_s", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(cabinX0 + 0.05F, cabinBot, galOuterZ - 0.50F, 11.2F, galMidY - cabinBot, 0.50F, d0), PartPose.ZERO);

        body.addOrReplaceChild("gal_hi_face", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(gAft + 0.20F, galMidY + 0.02F, -(cabinHalfZ - 0.30F), 0.85F, cabinTop - galMidY - 0.04F, (cabinHalfZ - 0.30F) * 2.0F, d0), PartPose.ZERO);
        body.addOrReplaceChild("gal_hi_p", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(cabinX0 + 0.15F, galMidY + 0.02F, -galOuterZ + 0.06F, 10.8F, cabinTop - galMidY - 0.04F, 0.45F, d0), PartPose.ZERO);
        body.addOrReplaceChild("gal_hi_s", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(cabinX0 + 0.15F, galMidY + 0.02F, galOuterZ - 0.51F, 10.8F, cabinTop - galMidY - 0.04F, 0.45F, d0), PartPose.ZERO);

        body.addOrReplaceChild("poop_deck", CubeListBuilder.create().texOffs(DECK, DECK_V).addBox(gAft - 0.4F, poopY, -9.0F, -gAft + gJoin + 0.8F, 0.85F, 18.0F, d0), PartPose.ZERO);

        body.addOrReplaceChild("taffrail_aft", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(gAft - 0.35F, poopY + 0.85F, -8.8F, 0.95F, 1.35F, 17.6F, d0), PartPose.ZERO);
        body.addOrReplaceChild("taffrail_p", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(gAft + 0.25F, poopY + 0.85F, -9.35F, -gAft + gJoin - 0.6F, 1.30F, 0.65F, d0), PartPose.ZERO);
        body.addOrReplaceChild("taffrail_s", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(gAft + 0.25F, poopY + 0.85F, 8.70F, -gAft + gJoin - 0.6F, 1.30F, 0.65F, d0), PartPose.ZERO);

        body.addOrReplaceChild("rail_bridge_p", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(-42.0F, RAIL_Y, -10.40F, 5.5F, RAIL_H, 0.95F, d0), PartPose.ZERO);
        body.addOrReplaceChild("rail_bridge_s", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(-42.0F, RAIL_Y, 9.45F, 5.5F, RAIL_H, 0.95F, d0), PartPose.ZERO);

        body.addOrReplaceChild("rudder", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(-2.0F, -5.0F, -1.0F, 5.0F, 14.5F, 2.0F, d0), PartPose.offset(-55.0F, 2.0F, 0.0F));
        body.addOrReplaceChild("rudder_hinge", CubeListBuilder.create().texOffs(METAL, METAL_V).addBox(-0.4F, -3.0F, -0.4F, 0.8F, 8.0F, 0.8F, d0), PartPose.offset(-55.2F, 4.0F, 0.0F));

        final float deckY = DECK_Y + DECK_H;

        final float railTopY = RAIL_TOP;
        final float midRailZ = MID_Z + 0.40F - RAIL_T * 0.5F;
        final float bowRailZ = NARROW_Z + 0.40F - RAIL_T * 0.5F;
        final float aftRailZ = bowRailZ;

        final float mizX = -28.0F;
        final float mainX = -4.0F;
        final float foreX = 26.0F;

        final float mizH = 56.0F;
        final float mainH = 76.0F;
        final float foreH = 62.0F;

        final float funnelH = 10.5F;
        final float[] funnelXs = {(mizX + mainX) * 0.5F, (mainX + foreX) * 0.5F};
        final String[] funnelIds = {"aft", "fwd"};
        for (int f = 0; f < 2; f++) {
            float fx = funnelXs[f];
            String id = funnelIds[f];

            body.addOrReplaceChild("funnel_" + id + "_base", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 1.5F, 6.0F, d0), PartPose.offset(fx, deckY, 0.0F));
            body.addOrReplaceChild("funnel_" + id + "_stack", CubeListBuilder.create().texOffs(METAL, METAL_V).addBox(-2.35F, 1.5F, -2.35F, 4.7F, funnelH, 4.7F, d0), PartPose.offset(fx, deckY, 0.0F));
            body.addOrReplaceChild("funnel_" + id + "_band", CubeListBuilder.create().texOffs(BLACK, BLACK_V).addBox(-2.55F, 1.5F + funnelH * 0.45F, -2.55F, 5.1F, 2.0F, 5.1F, d0), PartPose.offset(fx, deckY, 0.0F));
            body.addOrReplaceChild("funnel_" + id + "_rim", CubeListBuilder.create().texOffs(BLACK, BLACK_V).addBox(-2.7F, 1.5F + funnelH, -2.7F, 5.4F, 1.3F, 5.4F, d0), PartPose.offset(fx, deckY, 0.0F));
        }

        final float foreTruckY = deckY + foreH;
        final float mainTruckY = deckY + mainH;
        final float mizTruckY = deckY + mizH;

        final float bspOx = 49.0F;
        final float bspOy = 10.2F;
        final float bspLen = 36.0F;
        final float bspHalf = 0.85F;
        final float bspZRot = 0.36F;
        final float bspCos = Mth.cos(bspZRot);
        final float bspSin = Mth.sin(bspZRot);

        final float tipX = bspOx + bspLen * bspCos;
        final float tipY = bspOy + bspLen * bspSin;
        final float tipZ = 0.0F;

        final float midT = 0.82F;
        final float midX = bspOx + midT * bspLen * bspCos;
        final float midY = bspOy + midT * bspLen * bspSin;

        final float stemX = 54.0F;
        final float stemY = 3.5F;

        body.addOrReplaceChild("bowsprit", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(0.0F, -bspHalf, -bspHalf, bspLen, bspHalf * 2.0F, bspHalf * 2.0F, d0), PartPose.offsetAndRotation(bspOx, bspOy, 0.0F, 0.0F, 0.0F, bspZRot));

        final float jackT = 0.50F;
        final float jackX = bspOx + jackT * bspLen * bspCos;
        final float jackY = bspOy + jackT * bspLen * bspSin + 0.40F;
        final float jackH = 5.8F;
        body.addOrReplaceChild("bow_jackstaff", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(-0.22F, 0.0F, -0.22F, 0.44F, jackH, 0.44F, d0), PartPose.offset(jackX, jackY, 0.0F));
        addFrenchFlag(body, "flag_bow", jackX, jackY + jackH, 0.22F, 0.78F, d0);

        addLineBetween(body, "forestay", tipX, tipY, tipZ, foreX, foreTruckY, 0.0F, 0.35F, 0.35F);
        addLineBetween(body, "forestay_2", midX, midY, 0.0F, foreX, foreTruckY - 1.5F, 0.0F, 0.25F, 0.35F);

        addLineBetween(body, "bobstay", tipX, tipY, tipZ, stemX, stemY, 0.0F, 0.30F, 0.30F);

        addLineBetween(body, "stay_fore_main", foreX, foreTruckY, 0.0F, mainX, mainTruckY, 0.0F, 0.35F, 0.35F);
        addLineBetween(body, "stay_main_miz", mainX, mainTruckY - 2.0F, 0.0F, mizX, mizTruckY, 0.0F, 0.35F, 0.35F);

        final float shroudInset = 2.0F;
        final float shroudY = railTopY + 0.02F;
        final float shMainZ = midRailZ;
        final float shForeZ = bowRailZ;
        final float shMizZ = aftRailZ;
        final float backstayX = -12.0F;
        for (String side : new String[] {"p", "s"}) {
            float sz = side.equals("p") ? -1.0F : 1.0F;
            body.addOrReplaceChild("shroud_peg_main_" + side, CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(-0.30F, -0.12F, -0.28F, 0.60F, 0.32F, 0.56F, d0), PartPose.offset(mainX, shroudY - 0.02F, sz * shMainZ));
            body.addOrReplaceChild("shroud_peg_fore_" + side, CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(-0.30F, -0.12F, -0.28F, 0.60F, 0.32F, 0.56F, d0), PartPose.offset(foreX, shroudY - 0.02F, sz * shForeZ));
            body.addOrReplaceChild("shroud_peg_miz_" + side, CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(-0.30F, -0.12F, -0.28F, 0.60F, 0.32F, 0.56F, d0), PartPose.offset(mizX, shroudY - 0.02F, sz * shMizZ));
            body.addOrReplaceChild("backstay_peg_" + side, CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(-0.30F, -0.12F, -0.28F, 0.60F, 0.32F, 0.56F, d0), PartPose.offset(backstayX, shroudY - 0.02F, sz * shMainZ));
        }

        addLineBetween(body, "shroud_main_p", mainX, shroudY, -shMainZ, mainX, mainTruckY - 1.2F, -shroudInset, 0.0F, 0.40F);
        addLineBetween(body, "shroud_main_s", mainX, shroudY, shMainZ, mainX, mainTruckY - 1.2F, shroudInset, 0.0F, 0.40F);
        addLineBetween(body, "shroud_fore_p", foreX, shroudY, -shForeZ, foreX, foreTruckY - 1.2F, -shroudInset, 0.0F, 0.40F);
        addLineBetween(body, "shroud_fore_s", foreX, shroudY, shForeZ, foreX, foreTruckY - 1.2F, shroudInset, 0.0F, 0.40F);
        addLineBetween(body, "shroud_miz_p", mizX, shroudY, -shMizZ, mizX, mizTruckY - 1.2F, -shroudInset, 0.0F, 0.40F);
        addLineBetween(body, "shroud_miz_s", mizX, shroudY, shMizZ, mizX, mizTruckY - 1.2F, shroudInset, 0.0F, 0.40F);

        addLineBetween(body, "backstay_main_p", backstayX, shroudY, -shMainZ, mainX, mainTruckY - 0.5F, -0.8F, 0.0F, 0.40F);
        addLineBetween(body, "backstay_main_s", backstayX, shroudY, shMainZ, mainX, mainTruckY - 0.5F, 0.8F, 0.0F, 0.40F);

        final float taffrailTopY = poopY + 0.85F + 1.35F;
        final float taffrailX = gAft - 0.35F + 0.47F;
        addLineBetween(body, "stay_stern_miz", taffrailX, taffrailTopY, 0.0F, mizX, mizTruckY + 0.35F, 0.0F, 0.15F, 0.40F);

        final float yardForeY = 46.0F;
        final float yardForeTopY = 56.0F;
        final float yardMainY = 52.0F;
        final float yardMainTopY = 68.0F;
        final float yardMizY = 42.0F;
        final float yardMizTopY = 52.0F;

        PartDefinition mastFore = body.addOrReplaceChild("mast_fore", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(-1.2F, 0.0F, -1.2F, 2.4F, foreH, 2.4F, d0), PartPose.offset(foreX, deckY, 0.0F));
        addYard(mastFore, "yard_fore", yardForeY, 15.0F, 1.05F, d0);
        addYard(mastFore, "yard_fore_top", yardForeTopY, 10.5F, 0.85F, d0);

        addSquareSail(mastFore, "sail_fore", 1.15F, yardForeY, 24.0F, 13.5F, 11.8F, 1.0F, 1.05F, d0);
        addSquareSail(mastFore, "sail_fore_top", 1.15F, yardForeTopY, 11.5F, 9.4F, 8.2F, 0.85F, 0.85F, d0);

        PartDefinition mastMain = body.addOrReplaceChild("mast_main", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(-1.8F, 0.0F, -1.8F, 3.6F, mainH, 3.6F, d0), PartPose.offset(mainX, deckY, 0.0F));
        addYard(mastMain, "yard_main", yardMainY, 18.0F, 1.2F, d0);
        addYard(mastMain, "yard_main_top", yardMainTopY, 12.0F, 0.95F, d0);
        addSquareSail(mastMain, "sail_main", 1.75F, yardMainY, 30.0F, 16.5F, 14.2F, 1.05F, 1.2F, d0);
        addSquareSail(mastMain, "sail_main_top", 1.75F, yardMainTopY, 13.5F, 10.8F, 9.4F, 0.9F, 0.95F, d0);

        final float ensignStaffH = 4.8F;
        mastMain.addOrReplaceChild("ensign_staff", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(-0.20F, mainH, -0.20F, 0.40F, ensignStaffH, 0.40F, d0), PartPose.ZERO);

        addFrenchFlag(mastMain, "flag_main", 0.0F, mainH + ensignStaffH, 0.22F, 0.90F, d0);

        PartDefinition mastMizzen = body.addOrReplaceChild("mast_mizzen", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(-1.2F, 0.0F, -1.2F, 2.4F, mizH, 2.4F, d0), PartPose.offset(mizX, deckY, 0.0F));
        addYard(mastMizzen, "yard_mizzen", yardMizY, 12.0F, 0.95F, d0);
        addYard(mastMizzen, "yard_miz_top", yardMizTopY, 8.5F, 0.8F, d0);
        addSquareSail(mastMizzen, "sail_mizzen", 1.15F, yardMizY, 20.0F, 10.6F, 9.2F, 0.95F, 0.95F, d0);
        addSquareSail(mastMizzen, "sail_miz_top", 1.15F, yardMizTopY, 10.0F, 7.5F, 6.5F, 0.8F, 0.8F, d0);

        PartDefinition helm = body.addOrReplaceChild("helm", CubeListBuilder.create(), PartPose.offset(HELM_X, HELM_Y_WORLD, 0.0F));
        helm.addOrReplaceChild("base", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(-2.2F, 0.0F, -2.2F, 4.4F, 0.65F, 4.4F, d0), PartPose.ZERO);
        helm.addOrReplaceChild("post", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(-0.65F, 0.65F, -0.65F, 1.3F, 3.2F, 1.3F, d0), PartPose.ZERO);
        helm.addOrReplaceChild("cap", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(-1.15F, 3.65F, -1.15F, 2.3F, 0.45F, 2.3F, d0), PartPose.ZERO);
        helm.addOrReplaceChild("axle", CubeListBuilder.create().texOffs(METAL, METAL_V).addBox(-0.4F, 3.95F, -0.4F, 0.8F, 1.0F, 0.8F, d0), PartPose.ZERO);

        PartDefinition wheel = helm.addOrReplaceChild("wheel", CubeListBuilder.create(), PartPose.offset(0.0F, 4.7F, 0.0F));

        final float R = 2.85F;
        final float t = 0.48F;
        final float hub = 1.05F;

        wheel.addOrReplaceChild("hub", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(-0.55F, -hub * 0.5F, -hub * 0.5F, 1.1F, hub, hub, d0), PartPose.ZERO);
        wheel.addOrReplaceChild("spoke_v", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(-t * 0.4F, -R, -t * 0.4F, t * 0.8F, R * 2.0F, t * 0.8F, d0), PartPose.ZERO);
        wheel.addOrReplaceChild("spoke_h", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(-t * 0.4F, -t * 0.4F, -R, t * 0.8F, t * 0.8F, R * 2.0F, d0), PartPose.ZERO);
        wheel.addOrReplaceChild("spoke_d1", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(-t * 0.35F, -R, -t * 0.35F, t * 0.7F, R * 2.0F, t * 0.7F, d0), PartPose.rotation(Mth.PI / 4.0F, 0.0F, 0.0F));
        wheel.addOrReplaceChild("spoke_d2", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(-t * 0.35F, -R, -t * 0.35F, t * 0.7F, R * 2.0F, t * 0.7F, d0), PartPose.rotation(-Mth.PI / 4.0F, 0.0F, 0.0F));

        wheel.addOrReplaceChild("rim_top", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(-t * 0.5F, R - t, -R, t, t, R * 2.0F, d0), PartPose.ZERO);
        wheel.addOrReplaceChild("rim_bot", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(-t * 0.5F, -R, -R, t, t, R * 2.0F, d0), PartPose.ZERO);
        wheel.addOrReplaceChild("rim_port", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(-t * 0.5F, -R, -R, t, R * 2.0F, t, d0), PartPose.ZERO);
        wheel.addOrReplaceChild("rim_stbd", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(-t * 0.5F, -R, R - t, t, R * 2.0F, t, d0), PartPose.ZERO);

        final float hLen = 0.95F;
        final float hT = 0.5F;
        wheel.addOrReplaceChild("handle_t", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(-hT * 0.5F, R - 0.1F, -hT * 0.5F, hT, hLen, hT, d0), PartPose.ZERO);
        wheel.addOrReplaceChild("handle_b", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(-hT * 0.5F, -R - hLen + 0.1F, -hT * 0.5F, hT, hLen, hT, d0), PartPose.ZERO);
        wheel.addOrReplaceChild("handle_p", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(-hT * 0.5F, -hT * 0.5F, -R - hLen + 0.1F, hT, hT, hLen, d0), PartPose.ZERO);
        wheel.addOrReplaceChild("handle_s", CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(-hT * 0.5F, -hT * 0.5F, R - 0.1F, hT, hT, hLen, d0), PartPose.ZERO);

        return LayerDefinition.create(mesh, TEX, TEX);
    }

    private static final float LINE = 0.24F;

    private static void addLineBetween(PartDefinition body, String name, float x0, float y0, float z0, float x1, float y1, float z1, float padStart, float padEnd) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float dz = z1 - z0;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.05F) {
            return;
        }
        float inv = 1.0F / len;
        float ux = dx * inv;
        float uy = dy * inv;
        float uz = dz * inv;

        x0 -= ux * padStart;
        y0 -= uy * padStart;
        z0 -= uz * padStart;

        x1 += ux * padEnd;
        y1 += uy * padEnd;
        z1 += uz * padEnd;
        dx = x1 - x0;
        dy = y1 - y0;
        dz = z1 - z0;
        len = Mth.sqrt(dx * dx + dy * dy + dz * dz);

        float xRot = (float) Math.acos(Mth.clamp(dy / len, -1.0F, 1.0F));
        float yRot = (float) Math.atan2(dx, dz);
        float h = LINE * 0.5F;
        body.addOrReplaceChild(name, CubeListBuilder.create().texOffs(RIG, RIG_V).addBox(-h, 0.0F, -h, LINE, len, LINE, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(x0, y0, z0, xRot, yRot, 0.0F));
    }

    private static void addYard(PartDefinition mast, String name, float yardY, float halfLen, float thick, CubeDeformation d0) {
        PartDefinition yard = mast.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.offset(0.0F, yardY, 0.0F));

        addYardSparMesh(yard, "spar", "sling", thick, halfLen, 0.0F, 0.0F, d0);
    }

    private static void addYardSparMesh(PartDefinition parent, String sparName, String slingName, float thick, float halfLen, float cx, float cy, CubeDeformation d0) {
        parent.addOrReplaceChild(sparName, CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(cx - thick * 0.55F, cy - thick * 0.55F, -halfLen, thick * 1.1F, thick * 1.1F, halfLen * 2.0F, d0), PartPose.ZERO);
        parent.addOrReplaceChild(slingName, CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(cx - thick * 0.85F, cy - thick * 0.75F, -thick * 1.2F, thick * 1.7F, thick * 1.5F, thick * 2.4F, d0), PartPose.ZERO);
    }

    private static void addSquareSail(PartDefinition mast, String name, float xOff, float yardY, float height, float halfSpanHead, float halfSpanFoot, float thickness, float yardThick, CubeDeformation d0) {

        float thick = Math.max(thickness, 0.55F);

        PartDefinition sail = mast.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.offset(0.0F, yardY, 0.0F));

        sail.addOrReplaceChild("head", CubeListBuilder.create().texOffs(SAIL, SAIL_V).addBox(xOff - 0.25F, -0.45F, -halfSpanHead, thick + 0.35F, 0.9F, halfSpanHead * 2.0F, d0), PartPose.ZERO);
        sail.addOrReplaceChild("head_lash_p", CubeListBuilder.create().texOffs(RIG, RIG_V).addBox(xOff - 0.1F, -0.55F, -halfSpanHead * 0.92F, 0.35F, 0.55F, 0.55F, d0), PartPose.ZERO);
        sail.addOrReplaceChild("head_lash_s", CubeListBuilder.create().texOffs(RIG, RIG_V).addBox(xOff - 0.1F, -0.55F, halfSpanHead * 0.92F - 0.55F, 0.35F, 0.55F, 0.55F, d0), PartPose.ZERO);

        PartDefinition cloth = sail.addOrReplaceChild("cloth", CubeListBuilder.create(), PartPose.offset(xOff, 0.0F, 0.0F));
        generateClothGrid(cloth, height, halfSpanHead, halfSpanFoot, thick, d0);

        float furledR = Math.max(1.1F, thick * 1.35F);
        sail.addOrReplaceChild("furled", CubeListBuilder.create().texOffs(SAIL, SAIL_V).addBox(xOff - furledR * 0.35F, -furledR * 0.55F, -halfSpanHead * 0.92F, furledR * 0.9F, furledR, halfSpanHead * 1.84F, d0), PartPose.ZERO);

        PartDefinition flying = sail.addOrReplaceChild("flying_gear", CubeListBuilder.create(), PartPose.ZERO);

        for (int r = 1; r <= 2; r++) {
            float rt = r / 3.0F;
            float hs = Mth.lerp(rt, halfSpanHead, halfSpanFoot) * 0.98F;
            float yReef = -height * rt;
            int uReef = SAIL + 8 + r * 12;
            int vReef = SAIL_V + 20 + r * 10;
            flying.addOrReplaceChild("reef" + r, CubeListBuilder.create().texOffs(uReef, vReef).addBox(xOff - 0.12F, yReef - 0.16F, -hs, thick + 0.35F, 0.32F, hs * 2.0F, d0), PartPose.ZERO);
        }

        float footSpan = halfSpanFoot;
        float sparThick = yardThick;

        float footHalfLen = footSpan + sparThick * 0.35F;

        float rowH = height / (float) SAIL_ROWS;
        float hemY = -height - rowH * 0.52F;

        float footCy = hemY - sparThick * 0.55F - 0.10F;

        float footCx = xOff - thick * 0.35F - 0.12F;
        addYardSparMesh(flying, "foot", "foot_sling", sparThick, footHalfLen, footCx, footCy, d0);

        float step = height / (float) SAIL_ROWS;
        float clewY = footCy - 0.05F;
        flying.addOrReplaceChild("clew_p", CubeListBuilder.create().texOffs(RIG, RIG_V).addBox(footCx - 0.2F, clewY - 0.20F, -footSpan - 0.15F, 0.7F, 0.7F, 0.7F, d0), PartPose.ZERO);
        flying.addOrReplaceChild("clew_s", CubeListBuilder.create().texOffs(RIG, RIG_V).addBox(footCx - 0.2F, clewY - 0.20F, footSpan - 0.55F, 0.7F, 0.7F, 0.7F, d0), PartPose.ZERO);
        flying.addOrReplaceChild("bunt", CubeListBuilder.create().texOffs(SAIL, SAIL_V).addBox(xOff - 0.15F, -height - 0.15F, -1.4F, 0.85F, step * 1.1F, 2.8F, d0), PartPose.ZERO);
    }

    private static void generateClothGrid(PartDefinition cloth, float height, float halfSpanHead, float halfSpanFoot, float thick, CubeDeformation d0) {
        float rowH = height / (float) SAIL_ROWS;

        float yOverlap = rowH * 0.52F;
        float panelH = rowH + yOverlap * 2.0F;
        float cellThick = Math.max(thick, 1.05F) * 1.75F;
        int uPad = 2;
        int vPad = 2;
        int uSpan = SAIL_ATLAS_W - uPad * 2;
        int vSpan = SAIL_ATLAS_H - vPad * 2;

        for (int row = 0; row < SAIL_ROWS; row++) {
            float tRow = (row + 0.5F) / (float) SAIL_ROWS;
            float halfSpan = Mth.lerp(tRow, halfSpanHead, halfSpanFoot);
            float colW = (halfSpan * 2.0F) / (float) SAIL_COLS;
            float zOverlap = colW * 0.50F;
            float panelW = colW + zOverlap * 2.0F;
            float yTop = -((row + 1) * rowH) - yOverlap;
            int v = SAIL_V + vPad + (row * vSpan) / SAIL_ROWS;

            for (int col = 0; col < SAIL_COLS; col++) {
                float z0 = -halfSpan + col * colW - zOverlap;
                int u = SAIL + uPad + (col * uSpan) / SAIL_COLS;
                cloth.addOrReplaceChild(cellName(col, row), CubeListBuilder.create().texOffs(u, v).addBox(-cellThick * 0.5F, yTop, z0, cellThick, panelH, panelW, d0), PartPose.ZERO);
            }
        }
    }

    private void animateClothGrid(int sailIndex, float age, float fill, float phase, boolean topsail) {
        float maxBelly = this.sailMaxBelly[sailIndex];
        float bellyScale = topsail ? 0.82F : 1.0F;

        float underWay = Mth.clamp(fill, 0.0F, 1.0F);

        float bagT = underWay * underWay * (3.0F - 2.0F * underWay);
        float mix = 0.68F + bagT * 0.30F;

        float idleBoost = (1.0F - bagT) * (1.0F - bagT);

        float wind = windField(age, phase);
        float retract = sparseRetract(wind) * bagT;
        float surge = sparseSurge(wind) * bagT;

        float continuous = 0.88F + 0.14F * wind + 0.06F * Mth.sin(age * 0.0091F + phase * 1.7F);
        float windMul = 1.0F;
        if (bagT > 0.02F) {

            windMul = continuous * (1.0F - retract * 0.58F) + surge * 0.22F;
            windMul = Mth.clamp(windMul, 0.42F, 1.22F);
        }

        float wayLuff = bagT * (0.20F + 0.40F * retract) * (topsail ? 0.72F : 1.0F);
        float luffAmp = (topsail ? 0.16F : 0.22F) * idleBoost + wayLuff;

        float windPhase = age * 0.0273F + phase + wind * 0.55F;
        float windPhase2 = age * 0.0417F + phase * 1.31F + wind * 0.35F;

        ModelPart[][] cells = this.sailCells[sailIndex];
        float[][] xs = new float[SAIL_COLS][SAIL_ROWS];

        for (int col = 0; col < SAIL_COLS; col++) {
            float tCol = col / (float) (SAIL_COLS - 1);
            float pinZ = Mth.sin(Mth.PI * tCol);
            for (int row = 0; row < SAIL_ROWS; row++) {
                float tRow = row / (float) (SAIL_ROWS - 1);
                float pinY = Mth.sin(Mth.PI * tRow);

                float pinYBag = pinY * (0.70F + 0.30F * pinY + bagT * 0.18F * pinY);

                float bagMul = (0.32F + bagT * 0.78F) * Mth.lerp(bagT, 1.0F, windMul);
                float spanTerm = 0.22F + 0.78F * pinZ;
                float airfoil = maxBelly * bellyScale * bagMul * pinYBag * spanTerm;

                float pin = pinY * (0.40F + 0.60F * pinZ);
                float luff = luffAmp * Mth.sin(0.18F * col + 0.12F * row + windPhase) * pin;
                luff += luffAmp * 0.28F * Mth.sin(0.10F * col - 0.08F * row + windPhase2) * pin;

                luff += bagT * (0.18F + 0.45F * retract) * Mth.sin(0.19F * col + 0.14F * row + age * 0.055F + phase * 0.8F + wind) * pin;

                float luffMix = 1.0F - mix * (1.0F - 0.55F * retract);
                xs[col][row] = airfoil * mix + luff * luffMix;
            }
        }

        final int passes = 4;
        float blend = 0.40F + bagT * 0.28F;
        for (int pass = 0; pass < passes; pass++) {
            float[][] next = new float[SAIL_COLS][SAIL_ROWS];
            for (int col = 0; col < SAIL_COLS; col++) {
                for (int row = 0; row < SAIL_ROWS; row++) {
                    float sum = xs[col][row];
                    float w = 1.0F;
                    if (col > 0) {
                        sum += xs[col - 1][row] * blend;
                        w += blend;
                    }
                    if (col < SAIL_COLS - 1) {
                        sum += xs[col + 1][row] * blend;
                        w += blend;
                    }
                    if (row > 0) {
                        sum += xs[col][row - 1] * blend;
                        w += blend;
                    }
                    if (row < SAIL_ROWS - 1) {
                        sum += xs[col][row + 1] * blend;
                        w += blend;
                    }
                    next[col][row] = sum / w;
                }
            }
            xs = next;

            for (int col = 0; col < SAIL_COLS; col++) {
                xs[col][0] = 0.0F;
                xs[col][SAIL_ROWS - 1] = 0.0F;
            }
        }

        final float maxDelta = 0.55F + bagT * 0.22F;
        for (int iter = 0; iter < 3; iter++) {
            for (int col = 0; col < SAIL_COLS; col++) {
                for (int row = 0; row < SAIL_ROWS; row++) {
                    if (col > 0) {
                        float d = xs[col][row] - xs[col - 1][row];
                        if (Math.abs(d) > maxDelta) {
                            float mid = 0.5F * (xs[col][row] + xs[col - 1][row]);
                            float half = Math.copySign(maxDelta * 0.5F, d);
                            xs[col][row] = mid + half;
                            xs[col - 1][row] = mid - half;
                        }
                    }
                    if (row > 0) {
                        float d = xs[col][row] - xs[col][row - 1];
                        if (Math.abs(d) > maxDelta) {
                            float mid = 0.5F * (xs[col][row] + xs[col][row - 1]);
                            float half = Math.copySign(maxDelta * 0.5F, d);
                            xs[col][row] = mid + half;
                            xs[col][row - 1] = mid - half;
                        }
                    }
                }
            }

            for (int col = 0; col < SAIL_COLS; col++) {
                xs[col][0] = 0.0F;
                xs[col][SAIL_ROWS - 1] = 0.0F;
            }
        }

        for (int col = 0; col < SAIL_COLS; col++) {
            for (int row = 0; row < SAIL_ROWS; row++) {

                float prev = cells[col][row].x;
                float target = xs[col][row];

                float follow = 0.24F + bagT * 0.52F;
                cells[col][row].x = prev + (target - prev) * follow;
                cells[col][row].y = 0.0F;
                cells[col][row].z = 0.0F;
                cells[col][row].xRot = 0.0F;
                cells[col][row].yRot = 0.0F;
                cells[col][row].zRot = 0.0F;
            }

            cells[col][0].x = 0.0F;
            cells[col][SAIL_ROWS - 1].x = 0.0F;
        }
    }

    private void scaleClothBagByDeploy(int sailIndex, float localDeploy) {
        float k = Mth.clamp(localDeploy, 0.0F, 1.0F);
        ModelPart[][] cells = this.sailCells[sailIndex];
        for (int c = 0; c < SAIL_COLS; c++) {
            for (int r = 0; r < SAIL_ROWS; r++) {
                cells[c][r].x *= k;
            }
        }
    }

    private static float windField(float age, float phase) {
        float t = age;
        float p = phase;
        float w = 0.0F;

        w += 0.40F * Mth.sin(t * 0.0143F + p * 0.91F);
        w += 0.30F * Mth.sin(t * 0.0237F + p * 1.37F + 1.17F);
        w += 0.22F * Mth.sin(t * 0.0371F + p * 0.53F + 2.41F);
        w += 0.16F * Mth.sin(t * 0.0513F + p * 1.81F + 0.63F);
        w += 0.14F * Mth.sin(t * 0.0089F + p * 2.17F + 3.07F);

        float gate = Mth.sin(t * 0.0067F + p * 0.4F) * Mth.sin(t * 0.0181F + p * 1.1F + 0.8F);
        float gate2 = Mth.sin(t * 0.0113F + p * 1.6F + 1.3F) * Mth.sin(t * 0.0293F + p * 0.3F);
        w = w * (0.75F + 0.40F * gate) + gate2 * 0.42F;

        w = Mth.clamp(w * 1.25F, -1.0F, 1.0F);
        return w;
    }

    private static float sparseRetract(float wind) {

        if (wind > 0.08F) {
            return 0.0F;
        }
        float t = Mth.clamp((-wind + 0.08F) / 1.08F, 0.0F, 1.0F);

        return t * t * (3.0F - 2.0F * t);
    }

    private static float sparseSurge(float wind) {
        if (wind < -0.05F) {
            return 0.0F;
        }
        float t = Mth.clamp((wind + 0.05F) / 1.05F, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static void animateSailRoot(ModelPart root, float age, float fill, float phase, boolean topsail) {

        float wind = windField(age, phase);
        float dip = sparseRetract(wind) * fill;
        float surge = sparseSurge(wind) * fill;
        float breath = Mth.sin(age * 0.0131F + phase * 0.8F);
        float breath2 = Mth.sin(age * 0.0197F + phase * 1.3F + 0.5F);
        float base = topsail ? 0.010F : 0.016F;
        float way = topsail ? 0.018F : 0.028F;
        float hang = (1.0F - fill) * 0.016F;
        float idle = 1.0F - fill * 0.7F;

        root.xRot = 0.0F;

        root.yRot = (breath * 0.012F + breath2 * 0.008F + wind * 0.010F * fill) * (idle + fill * 0.55F);

        float wayLean = way * fill * (1.0F - dip * 0.55F + surge * 0.22F);
        root.zRot = base + hang + wayLean + breath * 0.012F * (idle + fill * 0.50F) + breath2 * 0.008F * fill + wind * 0.014F * fill;
    }

    private static void addFrenchFlag(PartDefinition parent, String name, float ox, float oy, float oz, float scale, CubeDeformation d0) {
        PartDefinition flag = parent.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.offset(ox, oy, oz));

        float thick = 0.38F * scale;
        float h = 5.2F * scale;
        float segW = 1.70F * scale;

        float z0 = 0.0F;
        float overlap = 0.14F * scale;

        flag.addOrReplaceChild("hoist", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(-0.18F * scale, -0.15F, -0.12F, 0.36F * scale, 0.30F, 0.28F * scale, d0), PartPose.ZERO);

        for (int i = 0; i < FLAG_SEGS; i++) {
            int u;
            int v;
            if (i < 2) {
                u = BLUE;
                v = BLUE_V;
            } else if (i < 4) {
                u = WHITE;
                v = WHITE_V;
            } else {
                u = RED;
                v = RED_V;
            }
            float z = z0 + i * segW;

            flag.addOrReplaceChild("seg" + i, CubeListBuilder.create().texOffs(u, v).addBox(-thick * 0.5F, -h, z, thick, h, segW + overlap, d0), PartPose.ZERO);
        }
    }

    private static void animateFrenchFlag(ModelPart root, ModelPart[] segs, float age, float fill, float phase, float ampScale) {
        float speed = 0.040F + fill * 0.035F;
        float t = age * speed + phase;
        float motion = 0.40F + fill * 0.55F;

        root.xRot = Mth.sin(t * 0.5F + 1.2F) * 0.012F * motion * ampScale;
        root.yRot = (Mth.sin(t) * 0.055F + Mth.sin(t * 0.55F + 0.8F) * 0.020F) * motion * ampScale;
        root.zRot = Mth.sin(t * 0.72F + 0.4F) * 0.018F * motion * ampScale;

        float tipWave = Mth.sin(t * 0.95F) * (0.06F + fill * 0.10F) * ampScale;
        for (int i = 0; i < FLAG_SEGS; i++) {
            float u = i / (float) (FLAG_SEGS - 1);
            ModelPart seg = segs[i];

            seg.x = tipWave * u * u;
            seg.y = 0.0F;
            seg.z = 0.0F;
            seg.xRot = 0.0F;
            seg.yRot = 0.0F;
            seg.zRot = 0.0F;
        }
    }

    private static void addHullSeg(PartDefinition body, String name, float x0, float x1, float y0, float h, float halfZ, CubeDeformation d0) {
        float len = x1 - x0;
        if (len <= 0.05F) {
            return;
        }
        body.addOrReplaceChild(name, CubeListBuilder.create().texOffs(WOOD, WOOD_V).addBox(x0, y0, -halfZ, len, h, halfZ * 2.0F, d0), PartPose.ZERO);
    }

    private static void addDeckSeg(PartDefinition body, String name, float x0, float x1, float y, float h, float halfZ, CubeDeformation d0) {
        float len = x1 - x0;
        if (len <= 0.05F) {
            return;
        }
        body.addOrReplaceChild(name, CubeListBuilder.create().texOffs(DECK, DECK_V).addBox(x0, y, -halfZ, len, h, halfZ * 2.0F, d0), PartPose.ZERO);
    }

    private static float smooth01(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static float localSailDeploy(float globalDeploy, int sailIndex, boolean topsail, boolean setting) {
        if (setting) {

            return smooth01(globalDeploy);
        }

        float lag = topsail ? 0.0F : 0.10F;
        lag += (sailIndex % 2) * 0.025F;
        float span = Math.max(0.55F, 1.0F - lag);
        float d = (globalDeploy - lag) / span;
        return smooth01(d);
    }

    private void animateSailDeploy(int sailIndex, float localDeploy) {
        ModelPart cloth = this.sailCloths[sailIndex];
        ModelPart furled = this.sailFurled[sailIndex];
        ModelPart flying = this.sailFlyingGear[sailIndex];
        ModelPart sail = this.sailRoots[sailIndex];

        float open = Mth.clamp(localDeploy, 0.0F, 1.0F);

        float clothT = smooth01(open);

        float bundleT = 1.0F - clothT;

        cloth.y = 0.0F;
        furled.y = 0.0F;
        furled.zRot = 0.0F;
        flying.y = 0.0F;

        boolean sheetOut = clothT > 0.01F;
        cloth.visible = sheetOut;
        cloth.xScale = 1.0F;
        cloth.yScale = Math.max(0.001F, clothT);
        cloth.zScale = 0.80F + 0.20F * clothT;

        flying.visible = sheetOut;
        flying.xScale = 1.0F;
        flying.yScale = Math.max(0.001F, clothT);
        flying.zScale = 1.0F;

        furled.visible = bundleT > 0.01F;
        furled.xScale = 0.32F + 0.68F * bundleT;
        furled.yScale = 0.32F + 0.68F * bundleT;
        furled.zScale = 0.52F + 0.48F * bundleT;

        if (sail.hasChild("head")) {
            sail.getChild("head").visible = true;
        }
        if (sail.hasChild("head_lash_p")) {
            boolean lashes = clothT > 0.05F;
            sail.getChild("head_lash_p").visible = lashes;
            sail.getChild("head_lash_s").visible = lashes;
        }
    }

    @Override
    public void setupAnim(NapoleonShipRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;

        float fill = state.sailFill;
        if (fill < 0.0F) {
            fill = Mth.clamp(state.speed / 0.22F, 0.0F, 1.0F);
        }
        float deploy = Mth.clamp(state.sailDeploy, 0.0F, 1.0F);

        boolean setting = !state.sailsFurled;

        float[] phases = {0.0F, 0.4F, 1.1F, 1.5F, 2.3F, 2.7F};
        boolean[] tops = {false, true, false, true, false, true};
        for (int i = 0; i < SAIL_COUNT; i++) {
            float localDeploy = localSailDeploy(deploy, i, tops[i], setting);

            float open = smooth01(localDeploy);
            float effectiveFill = fill * open;
            animateClothGrid(i, age, effectiveFill, phases[i], tops[i]);
            scaleClothBagByDeploy(i, open);
            animateSailDeploy(i, localDeploy);
            animateSailRoot(this.sailRoots[i], age, effectiveFill, phases[i], tops[i]);
        }

        animateFrenchFlag(this.flagMainRoot, this.flagMainSegs, age, fill, 0.0F, 1.0F);
        animateFrenchFlag(this.flagBowRoot, this.flagBowSegs, age, fill, 1.7F, 0.9F);

        this.helmWheel.xRot = state.helmAngle;

        if (state.helmCockpit) {
            this.helm.xScale = HELM_SCALE_COCKPIT;
            this.helm.yScale = HELM_SCALE_COCKPIT;
            this.helm.zScale = HELM_SCALE_COCKPIT;
            this.helm.x = HELM_X;
            this.helm.y = HELM_Y_COCKPIT;
            this.helm.z = 0.0F;
        } else {
            this.helm.xScale = 1.0F;
            this.helm.yScale = 1.0F;
            this.helm.zScale = 1.0F;
            this.helm.x = HELM_X;
            this.helm.y = HELM_Y_WORLD;
            this.helm.z = 0.0F;
        }
    }
}
