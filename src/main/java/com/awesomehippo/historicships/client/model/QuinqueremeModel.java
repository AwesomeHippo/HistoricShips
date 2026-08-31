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

public class QuinqueremeModel extends EntityModel<OarShipRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(Identifier.fromNamespaceAndPath("historicships", "quinquereme"), "main");

    public static final int TEX = 256;

    private static final int TAN = 8;
    private static final int TAN_V = 8;

    private static final int BRONZE = 132;
    private static final int BRONZE_V = 8;

    private static final int GOLD = 180;
    private static final int GOLD_V = 4;
    private static final int RED = 168;
    private static final int RED_V = 8;
    private static final int SAIL = 8;
    private static final int SAIL_V = 104;
    private static final int STONE = 136;
    private static final int STONE_V = 96;
    private static final int DECK = 8;
    private static final int DECK_V = 184;
    private static final int MAST = 112;
    private static final int MAST_V = 184;
    private static final int OAR = 168;
    private static final int OAR_V = 184;
    private static final int IRON = 180;
    private static final int IRON_V = 100;

    private static final int BLACK = 248;
    private static final int BLACK_V = 8;
    private static final int DARK = 234;
    private static final int DARK_V = 4;

    private static final int CREAM = 16;
    private static final int CREAM_V = 110;
    private static final int EYE = 212;
    private static final int EYE_V = 100;

    private static final int OAR_PAIRS = 16;
    private static final float REST_PITCH = 0.62F;
    public static final float SAIL_MAX_BELLY = 6.5F;
    private static final float ART_SAIL_MAX_BELLY = 3.8F;

    public static final float MAST_BASE_Y = 5.55F;
    public static final float YARD_Y = 23.0F;
    public static final float SAIL_X_OFF = 1.10F;
    public static final float SAIL_THICK = 1.0F;
    public static final float SAIL_HEIGHT = 17.5F;
    public static final float SAIL_HALF_HEAD = 14.0F;
    public static final float SAIL_HALF_FOOT = 13.0F;
    private static final float LINE = 0.30F;
    private static final float OAR_X0 = -32.0F;
    private static final float OAR_PITCH = 4.0F;
    private static final int SHIELD_COUNT = OAR_PAIRS - 1;
    private static final float SHIELD_X0 = OAR_X0 + OAR_PITCH * 0.5F;

    private static final float SEG = 12.0F;

    private final ModelPart[] oarsPort;
    private final ModelPart[] oarsStbd;
    private final ModelPart sailRoot;
    private final ModelPart sailCloth;
    private final ModelPart[][] sailCells;
    private final ModelPart artSailRoot;
    private final ModelPart[][] artSailCells;

    public QuinqueremeModel(ModelPart root) {
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
        this.sailCloth = this.sailRoot.getChild("cloth");
        this.sailCells = SquareSail.resolveCells(this.sailRoot);
        ModelPart artemon = body.getChild("artemon");
        this.artSailRoot = artemon.getChild("sail_artemon");
        this.artSailCells = SquareSail.resolveCells(this.artSailRoot);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation d0 = new CubeDeformation(0.0F);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);

        final float hb = 5.0F;
        final float bilgeH = 3.5F;
        final float redY = 3.2F;
        final float redH = 2.15F;
        final float deckY = redY + redH - 0.15F;
        final float mid0 = -42.0F;
        final float midEnd = 40.0F;
        final float midLenA = 42.0F;
        final float midLenB = midEnd;

        final float bilgeZ0 = -hb + 0.25F;
        final float bilgeZ = (hb - 0.25F) * 2.0F;
        addSegX(body, "bilge", mid0, 0.0F, bilgeZ0, midLenA + midLenB + 0.4F, bilgeH, bilgeZ, TAN, TAN_V, d0, 0.15F);

        final float wallT = 1.05F;
        final float fullMid = midLenA + midLenB + 0.4F;
        addSegX(body, "red_p", mid0, redY, -hb - 0.20F, fullMid, redH, wallT, RED, RED_V, d0, 0.15F);
        addSegX(body, "red_s", mid0, redY, hb - wallT + 0.20F, fullMid, redH, wallT, RED, RED_V, d0, 0.15F);

        final float deckZ0 = -hb + 1.0F;
        final float deckZ = (hb - 1.0F) * 2.0F;
        final float b0 = midEnd - 0.35F;
        addSegX(body, "deck", mid0, deckY, deckZ0, b0 - mid0, 0.50F, deckZ, DECK, DECK_V, d0, 0.0F);

        addSegX(body, "rail_p", mid0, deckY, -hb - 0.10F, fullMid, 0.65F, 0.65F, RED, RED_V, d0, 0.15F);
        addSegX(body, "rail_s", mid0, deckY, hb - 0.55F, fullMid, 0.65F, 0.65F, RED, RED_V, d0, 0.15F);

        addSegX(body, "keel", mid0 + 2.0F, -2.0F, -2.4F, fullMid - 4.0F, 2.0F, 4.8F, TAN, TAN_V, d0, 0.15F);

        final float ha = hb - 0.15F;
        body.addOrReplaceChild("bowA_tan", CubeListBuilder.create().texOffs(TAN, TAN_V).addBox(b0, 0.0F, -ha, 8.2F, bilgeH, ha * 2.0F, d0), PartPose.ZERO);
        body.addOrReplaceChild("bowA_red_p", CubeListBuilder.create().texOffs(RED, RED_V).addBox(b0, redY, -ha - 0.18F, 8.2F, redH, wallT, d0), PartPose.ZERO);
        body.addOrReplaceChild("bowA_red_s", CubeListBuilder.create().texOffs(RED, RED_V).addBox(b0, redY, ha - wallT + 0.18F, 8.2F, redH, wallT, d0), PartPose.ZERO);
        body.addOrReplaceChild("bowA_deck", CubeListBuilder.create().texOffs(DECK, DECK_V).addBox(b0, deckY, -ha + 0.95F, 7.6F, 0.50F, (ha - 0.95F) * 2.0F, d0), PartPose.ZERO);
        body.addOrReplaceChild("bowA_rail_p", CubeListBuilder.create().texOffs(RED, RED_V).addBox(b0, deckY, -ha - 0.05F, 8.0F, 0.60F, 0.55F, d0), PartPose.ZERO);
        body.addOrReplaceChild("bowA_rail_s", CubeListBuilder.create().texOffs(RED, RED_V).addBox(b0, deckY, ha - 0.50F, 8.0F, 0.60F, 0.55F, d0), PartPose.ZERO);

        body.addOrReplaceChild("bowA_gold_p", CubeListBuilder.create().texOffs(GOLD, GOLD_V).addBox(b0, 0.10F, -ha - 0.30F, 8.2F, 0.55F, 0.32F, d0), PartPose.ZERO);
        body.addOrReplaceChild("bowA_gold_s", CubeListBuilder.create().texOffs(GOLD, GOLD_V).addBox(b0, 0.10F, ha - 0.02F, 8.2F, 0.55F, 0.32F, d0), PartPose.ZERO);

        final float b1 = b0 + 7.6F;
        final float hb1 = 3.55F;
        body.addOrReplaceChild("bowB_tan", CubeListBuilder.create().texOffs(TAN, TAN_V).addBox(b1, 0.05F, -hb1, 6.4F, bilgeH - 0.10F, hb1 * 2.0F, d0), PartPose.ZERO);
        body.addOrReplaceChild("bowB_red_p", CubeListBuilder.create().texOffs(RED, RED_V).addBox(b1, redY, -hb1 - 0.15F, 6.4F, redH - 0.05F, 0.95F, d0), PartPose.ZERO);
        body.addOrReplaceChild("bowB_red_s", CubeListBuilder.create().texOffs(RED, RED_V).addBox(b1, redY, hb1 - 0.80F, 6.4F, redH - 0.05F, 0.95F, d0), PartPose.ZERO);
        body.addOrReplaceChild("bowB_deck", CubeListBuilder.create().texOffs(DECK, DECK_V).addBox(b1, deckY, -hb1 + 0.70F, 5.9F, 0.50F, (hb1 - 0.70F) * 2.0F, d0), PartPose.ZERO);
        body.addOrReplaceChild("bowB_rail_p", CubeListBuilder.create().texOffs(RED, RED_V).addBox(b1, deckY, -hb1 - 0.02F, 6.2F, 0.55F, 0.50F, d0), PartPose.ZERO);
        body.addOrReplaceChild("bowB_rail_s", CubeListBuilder.create().texOffs(RED, RED_V).addBox(b1, deckY, hb1 - 0.48F, 6.2F, 0.55F, 0.50F, d0), PartPose.ZERO);
        body.addOrReplaceChild("bowB_gold_p", CubeListBuilder.create().texOffs(GOLD, GOLD_V).addBox(b1, 0.10F, -hb1 - 0.25F, 6.4F, 0.50F, 0.30F, d0), PartPose.ZERO);
        body.addOrReplaceChild("bowB_gold_s", CubeListBuilder.create().texOffs(GOLD, GOLD_V).addBox(b1, 0.10F, hb1 - 0.05F, 6.4F, 0.50F, 0.30F, d0), PartPose.ZERO);

        final float b2 = b1 + 5.9F;
        final float hb2 = 2.15F;
        body.addOrReplaceChild("bowC_tan", CubeListBuilder.create().texOffs(TAN, TAN_V).addBox(b2, 0.10F, -hb2, 4.6F, bilgeH - 0.20F, hb2 * 2.0F, d0), PartPose.ZERO);
        body.addOrReplaceChild("bowC_red_p", CubeListBuilder.create().texOffs(RED, RED_V).addBox(b2, redY, -hb2 - 0.10F, 4.4F, redH - 0.10F, 0.80F, d0), PartPose.ZERO);
        body.addOrReplaceChild("bowC_red_s", CubeListBuilder.create().texOffs(RED, RED_V).addBox(b2, redY, hb2 - 0.70F, 4.4F, redH - 0.10F, 0.80F, d0), PartPose.ZERO);
        body.addOrReplaceChild("bowC_deck", CubeListBuilder.create().texOffs(DECK, DECK_V).addBox(b2, deckY, -hb2 + 0.50F, 4.2F, 0.50F, (hb2 - 0.50F) * 2.0F, d0), PartPose.ZERO);
        body.addOrReplaceChild("bowC_rail_p", CubeListBuilder.create().texOffs(RED, RED_V).addBox(b2, deckY, -hb2 - 0.02F, 4.2F, 0.52F, 0.45F, d0), PartPose.ZERO);
        body.addOrReplaceChild("bowC_rail_s", CubeListBuilder.create().texOffs(RED, RED_V).addBox(b2, deckY, hb2 - 0.43F, 4.2F, 0.52F, 0.45F, d0), PartPose.ZERO);

        body.addOrReplaceChild("cutwater", CubeListBuilder.create().texOffs(TAN, TAN_V).addBox(b2 + 1.1F, -0.70F, -1.30F, 2.4F, 1.55F, 2.60F, d0), PartPose.ZERO);
        body.addOrReplaceChild("keel_bow", CubeListBuilder.create().texOffs(TAN, TAN_V).addBox(b0, -1.55F, -1.9F, 16.5F, 1.65F, 3.8F, d0), PartPose.ZERO);

        final float rx = b2 + 3.05F;
        final float ry = 0.05F;
        body.addOrReplaceChild("ram_cowl0", CubeListBuilder.create().texOffs(BRONZE, BRONZE_V).addBox(rx - 0.20F, ry - 0.95F, -1.15F, 1.95F, 2.20F, 2.30F, d0), PartPose.ZERO);
        body.addOrReplaceChild("ram_cowl1", CubeListBuilder.create().texOffs(BRONZE, BRONZE_V).addBox(rx + 1.55F, ry - 0.82F, -0.98F, 1.70F, 1.95F, 1.96F, d0), PartPose.ZERO);
        body.addOrReplaceChild("ram_cowl2", CubeListBuilder.create().texOffs(BRONZE, BRONZE_V).addBox(rx + 3.05F, ry - 0.70F, -0.82F, 1.55F, 1.70F, 1.64F, d0), PartPose.ZERO);
        body.addOrReplaceChild("ram_cowl3", CubeListBuilder.create().texOffs(BRONZE, BRONZE_V).addBox(rx + 4.40F, ry - 0.58F, -0.70F, 1.80F, 1.48F, 1.40F, d0), PartPose.ZERO);

        final float hx = rx + 6.00F;
        body.addOrReplaceChild("ram_c", CubeListBuilder.create().texOffs(BRONZE, BRONZE_V).addBox(hx, ry - 0.08F, -0.42F, 2.20F, 0.48F, 0.84F, d0), PartPose.ZERO);
        body.addOrReplaceChild("ram_c_tip", CubeListBuilder.create().texOffs(BRONZE, BRONZE_V).addBox(hx + 2.10F, ry - 0.04F, -0.28F, 2.40F, 0.40F, 0.56F, d0), PartPose.ZERO);
        body.addOrReplaceChild("ram_u", CubeListBuilder.create().texOffs(BRONZE, BRONZE_V).addBox(hx, ry + 0.46F, -0.36F, 1.90F, 0.36F, 0.72F, d0), PartPose.ZERO);
        body.addOrReplaceChild("ram_u_tip", CubeListBuilder.create().texOffs(BRONZE, BRONZE_V).addBox(hx + 1.80F, ry + 0.50F, -0.24F, 2.00F, 0.32F, 0.48F, d0), PartPose.ZERO);
        body.addOrReplaceChild("ram_l", CubeListBuilder.create().texOffs(BRONZE, BRONZE_V).addBox(hx, ry - 0.48F, -0.36F, 1.90F, 0.36F, 0.72F, d0), PartPose.ZERO);
        body.addOrReplaceChild("ram_l_tip", CubeListBuilder.create().texOffs(BRONZE, BRONZE_V).addBox(hx + 1.80F, ry - 0.44F, -0.24F, 2.00F, 0.32F, 0.48F, d0), PartPose.ZERO);
        body.addOrReplaceChild("ram_web", CubeListBuilder.create().texOffs(BRONZE, BRONZE_V).addBox(hx + 2.40F, ry - 0.42F, -0.18F, 1.00F, 1.28F, 0.36F, d0), PartPose.ZERO);

        final float stemX = b2 + 2.4F;

        body.addOrReplaceChild("stem_core", CubeListBuilder.create().texOffs(TAN, TAN_V).addBox(stemX - 0.4F, 0.2F, -1.25F, 3.0F, deckY + 0.4F, 2.50F, d0), PartPose.ZERO);
        body.addOrReplaceChild("stem_red", CubeListBuilder.create().texOffs(RED, RED_V).addBox(stemX - 0.55F, redY - 0.15F, -1.40F, 3.3F, redH + 0.35F, 2.80F, d0), PartPose.ZERO);

        body.addOrReplaceChild("acro_base", CubeListBuilder.create().texOffs(RED, RED_V).addBox(stemX - 0.35F, deckY + 0.3F, -1.30F, 3.0F, 3.4F, 2.60F, d0), PartPose.ZERO);
        body.addOrReplaceChild("acro_rise", CubeListBuilder.create().texOffs(RED, RED_V).addBox(stemX - 0.15F, deckY + 3.4F, -1.15F, 2.7F, 3.0F, 2.30F, d0), PartPose.ZERO);

        body.addOrReplaceChild("acro_peak", CubeListBuilder.create().texOffs(RED, RED_V).addBox(stemX + 0.05F, deckY + 6.0F, -1.00F, 2.4F, 2.4F, 2.00F, d0), PartPose.ZERO);

        body.addOrReplaceChild("volute_1", CubeListBuilder.create().texOffs(RED, RED_V).addBox(stemX - 1.6F, deckY + 7.6F, -0.95F, 3.4F, 2.0F, 1.90F, d0), PartPose.ZERO);
        body.addOrReplaceChild("volute_2", CubeListBuilder.create().texOffs(RED, RED_V).addBox(stemX - 3.2F, deckY + 8.5F, -0.90F, 2.6F, 1.85F, 1.80F, d0), PartPose.ZERO);
        body.addOrReplaceChild("volute_3", CubeListBuilder.create().texOffs(RED, RED_V).addBox(stemX - 4.3F, deckY + 7.8F, -0.85F, 1.9F, 1.7F, 1.70F, d0), PartPose.ZERO);

        body.addOrReplaceChild("volute_eye", CubeListBuilder.create().texOffs(RED, RED_V).addBox(stemX - 4.6F, deckY + 6.9F, -0.75F, 1.55F, 1.35F, 1.50F, d0), PartPose.ZERO);

        body.addOrReplaceChild("volute_gold_a", CubeListBuilder.create().texOffs(GOLD, GOLD_V).addBox(stemX + 2.15F, deckY + 1.0F, -0.45F, 0.40F, 5.5F, 0.90F, d0), PartPose.ZERO);
        body.addOrReplaceChild("volute_gold_b", CubeListBuilder.create().texOffs(GOLD, GOLD_V).addBox(stemX - 1.4F, deckY + 8.0F, -0.55F, 2.8F, 0.40F, 1.10F, d0), PartPose.ZERO);
        body.addOrReplaceChild("volute_gold_c", CubeListBuilder.create().texOffs(GOLD, GOLD_V).addBox(stemX - 4.0F, deckY + 8.7F, -0.50F, 1.8F, 0.35F, 1.00F, d0), PartPose.ZERO);

        final float eyeX = b1 + 2.2F;
        final float eyeY = 2.15F;
        body.addOrReplaceChild("eye_p_white", CubeListBuilder.create().texOffs(EYE, EYE_V).addBox(eyeX, eyeY, -hb1 - 0.22F, 2.0F, 1.55F, 0.35F, d0), PartPose.ZERO);
        body.addOrReplaceChild("eye_p_pupil", CubeListBuilder.create().texOffs(BLACK, BLACK_V).addBox(eyeX + 0.55F, eyeY + 0.35F, -hb1 - 0.30F, 0.85F, 0.85F, 0.28F, d0), PartPose.ZERO);
        body.addOrReplaceChild("eye_p_lid", CubeListBuilder.create().texOffs(GOLD, GOLD_V).addBox(eyeX - 0.15F, eyeY + 1.40F, -hb1 - 0.18F, 2.3F, 0.28F, 0.28F, d0), PartPose.ZERO);
        body.addOrReplaceChild("eye_s_white", CubeListBuilder.create().texOffs(EYE, EYE_V).addBox(eyeX, eyeY, hb1 - 0.13F, 2.0F, 1.55F, 0.35F, d0), PartPose.ZERO);
        body.addOrReplaceChild("eye_s_pupil", CubeListBuilder.create().texOffs(BLACK, BLACK_V).addBox(eyeX + 0.55F, eyeY + 0.35F, hb1 - 0.05F, 0.85F, 0.85F, 0.28F, d0), PartPose.ZERO);
        body.addOrReplaceChild("eye_s_lid", CubeListBuilder.create().texOffs(GOLD, GOLD_V).addBox(eyeX - 0.15F, eyeY + 1.40F, hb1 - 0.10F, 2.3F, 0.28F, 0.28F, d0), PartPose.ZERO);

        final float artBaseX = stemX + 0.35F;
        final float artBaseY = deckY + 2.4F;
        final float artSparH = 18.5F;
        final float artSailH = 8.2F;
        final float artHalfH = 7.2F;
        final float artHalfF = 6.5F;

        PartDefinition artemon = body.addOrReplaceChild("artemon", CubeListBuilder.create(), PartPose.offsetAndRotation(artBaseX, artBaseY, 0.0F, 0.0F, 0.0F, -0.45F));

        artemon.addOrReplaceChild("spar", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(-0.70F, 0.0F, -0.70F, 1.40F, artSparH, 1.40F, d0), PartPose.ZERO);
        artemon.addOrReplaceChild("spar_mid", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(-0.55F, artSparH * 0.35F, -0.55F, 1.10F, artSparH * 0.40F, 1.10F, d0), PartPose.ZERO);
        artemon.addOrReplaceChild("spar_tip", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(-0.48F, artSparH - 1.2F, -0.48F, 0.96F, 1.5F, 0.96F, d0), PartPose.ZERO);

        final float artYardHalf = artHalfH + (artHalfH * 2.0F / SquareSail.COLS) * SquareSail.GRID_OVERLAP + 0.25F;
        final float artYardSeg = 8.0F;
        int ayi = 0;
        for (float z = -artYardHalf; z < artYardHalf - 0.01F; z += artYardSeg) {
            float len = Math.min(artYardSeg, artYardHalf - z);
            artemon.addOrReplaceChild("yard_" + (ayi++), CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(-0.55F, artSparH - 0.55F, z, 1.10F, 1.10F, len, d0), PartPose.ZERO);
        }

        SquareSail.addMapped(artemon, "sail_artemon", artSparH, 0.55F, artSailH, artHalfH, artHalfF, 1.05F, SAIL, SAIL_V, 0, 0, MAST, MAST_V, d0, false);

        final float stemPeakX = stemX - 0.4F;
        final float stemPeakY = deckY + 9.3F;

        body.addOrReplaceChild("stern_tan", CubeListBuilder.create().texOffs(TAN, TAN_V).addBox(-51.0F, 0.15F, -4.3F, 9.2F, bilgeH, 8.6F, d0), PartPose.ZERO);
        body.addOrReplaceChild("stern_red_p", CubeListBuilder.create().texOffs(RED, RED_V).addBox(-51.0F, redY, -4.55F, 9.2F, redH, wallT, d0), PartPose.ZERO);
        body.addOrReplaceChild("stern_red_s", CubeListBuilder.create().texOffs(RED, RED_V).addBox(-51.0F, redY, 4.55F - wallT, 9.2F, redH, wallT, d0), PartPose.ZERO);
        body.addOrReplaceChild("stern_deck", CubeListBuilder.create().texOffs(DECK, DECK_V).addBox(-50.0F, deckY, -3.6F, 8.0F, 0.50F, 7.2F, d0), PartPose.ZERO);
        body.addOrReplaceChild("stern_cabin", CubeListBuilder.create().texOffs(CREAM, CREAM_V).addBox(-50.5F, deckY + 0.35F, -3.2F, 6.5F, 3.6F, 6.4F, d0), PartPose.ZERO);
        body.addOrReplaceChild("keel_stern", CubeListBuilder.create().texOffs(TAN, TAN_V).addBox(-50.0F, -1.6F, -1.8F, 10.0F, 1.8F, 3.6F, d0), PartPose.ZERO);

        body.addOrReplaceChild("shrimp_base", CubeListBuilder.create().texOffs(RED, RED_V).addBox(-54.5F, 3.0F, -2.8F, 6.5F, 5.0F, 5.6F, d0), PartPose.ZERO);
        body.addOrReplaceChild("shrimp_rise", CubeListBuilder.create().texOffs(RED, RED_V).addBox(-56.5F, 6.5F, -2.4F, 5.0F, 6.0F, 4.8F, d0), PartPose.ZERO);
        body.addOrReplaceChild("shrimp_neck", CubeListBuilder.create().texOffs(RED, RED_V).addBox(-57.0F, 11.0F, -2.0F, 4.5F, 5.5F, 4.0F, d0), PartPose.ZERO);

        body.addOrReplaceChild("shrimp_head", CubeListBuilder.create().texOffs(RED, RED_V).addBox(-54.0F, 14.5F, -1.9F, 7.0F, 3.6F, 3.8F, d0), PartPose.ZERO);

        body.addOrReplaceChild("shrimp_jaw_top", CubeListBuilder.create().texOffs(RED, RED_V).addBox(-47.5F, 16.2F, -1.0F, 4.0F, 1.3F, 2.0F, d0), PartPose.ZERO);
        body.addOrReplaceChild("shrimp_jaw_bot", CubeListBuilder.create().texOffs(RED, RED_V).addBox(-47.5F, 14.8F, -1.0F, 3.5F, 1.1F, 2.0F, d0), PartPose.ZERO);

        body.addOrReplaceChild("shrimp_belly", CubeListBuilder.create().texOffs(TAN, TAN_V).addBox(-53.5F, 2.5F, -2.2F, 5.0F, 1.8F, 4.4F, d0), PartPose.ZERO);

        addTower(body, "tower_aft", -18.0F, deckY + 0.45F, d0);

        final float shieldY = deckY + 0.10F;
        final float shieldZ = hb + 0.34F;
        for (int i = 0; i < SHIELD_COUNT; i++) {
            float x = SHIELD_X0 + i * OAR_PITCH;
            RomanOvalShield.add(body, "sh_p" + i, x, shieldY, -shieldZ, -1, RED, RED_V, BRONZE, BRONZE_V, d0);
            RomanOvalShield.add(body, "sh_s" + i, x, shieldY, shieldZ, +1, RED, RED_V, BRONZE, BRONZE_V, d0);
        }

        final float mastBaseY = MAST_BASE_Y;
        final float mastH = 26.0F;
        final float yardY = YARD_Y;
        final float sailH = SAIL_HEIGHT;
        final float sailHalf = SAIL_HALF_HEAD;

        PartDefinition mast = body.addOrReplaceChild("mast", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(-1.05F, 0.0F, -1.05F, 2.1F, mastH, 2.1F, d0), PartPose.offset(0.0F, mastBaseY, 0.0F));
        mast.addOrReplaceChild("truck", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(-0.85F, mastH - 0.3F, -0.85F, 1.7F, 1.5F, 1.7F, d0), PartPose.ZERO);

        PartDefinition yard = mast.addOrReplaceChild("yard", CubeListBuilder.create(), PartPose.offset(0.0F, yardY, 0.0F));
        final float yardHalf = 15.5F;
        final float yardSeg = 10.0F;
        int yi = 0;
        for (float z = -yardHalf; z < yardHalf - 0.01F; z += yardSeg) {
            float len = Math.min(yardSeg, yardHalf - z);
            yard.addOrReplaceChild("ys" + (yi++), CubeListBuilder.create().texOffs(RED, RED_V).addBox(-0.65F, -0.65F, z, 1.3F, 1.3F, len, d0), PartPose.ZERO);
        }

        SquareSail.addMapped(mast, "sail_fixed", yardY, SAIL_X_OFF, sailH, sailHalf, SAIL_HALF_FOOT, SAIL_THICK, SAIL, SAIL_V, 0, 0, MAST, MAST_V, d0, true);

        final float truckAbsY = mastBaseY + mastH + 0.4F;
        addLineBetween(body, "stay_bow", stemPeakX, stemPeakY, 0.0F, 0.9F, truckAbsY, 0.0F);
        addLineBetween(body, "stay_stern", -52.0F, 14.5F, 0.0F, -0.9F, truckAbsY, 0.0F);

        final float oarY = redY + 0.55F;
        final float oarZ = hb + 0.20F;
        final float oarLen = 11.5F;

        for (int i = 0; i < OAR_PAIRS; i++) {
            float x = OAR_X0 + i * OAR_PITCH;
            ShipOars.addOar(body, "oar_p" + i, x, oarY, -oarZ, true, oarLen, OAR, OAR_V, d0);
            ShipOars.addOar(body, "oar_s" + i, x, oarY, oarZ, false, oarLen, OAR, OAR_V, d0);
        }

        return LayerDefinition.create(mesh, TEX, TEX);
    }

    private static void addSegX(PartDefinition body, String prefix, float x0, float y, float z0, float totalLen, float h, float depth, int u, int v, CubeDeformation d0, float seam) {
        int i = 0;
        float x = x0;
        float end = x0 + totalLen;
        while (x < end - 0.01F) {
            float len = Math.min(SEG, end - x);

            float draw = len + (x + len < end - 0.01F ? seam : 0.0F);
            body.addOrReplaceChild(prefix + "_" + i, CubeListBuilder.create().texOffs(u, v).addBox(x, y, z0, draw, h, depth, d0), PartPose.ZERO);
            x += len;
            i++;
        }
    }

    private static void addTower(PartDefinition body, String name, float x, float y, CubeDeformation d0) {
        PartDefinition t = body.addOrReplaceChild(name, CubeListBuilder.create().texOffs(STONE, STONE_V).addBox(-2.55F, 0.0F, -2.55F, 5.1F, 5.4F, 5.1F, d0), PartPose.offset(x, y, 0.0F).withScale(1.22F));

        t.addOrReplaceChild("band", CubeListBuilder.create().texOffs(IRON, IRON_V).addBox(-2.7F, 2.4F, -2.7F, 5.4F, 0.55F, 5.4F, d0), PartPose.ZERO);
        t.addOrReplaceChild("door", CubeListBuilder.create().texOffs(DARK, DARK_V).addBox(-1.15F, 0.35F, -2.72F, 2.3F, 2.7F, 0.35F, d0), PartPose.ZERO);
        t.addOrReplaceChild("slit_p", CubeListBuilder.create().texOffs(DARK, DARK_V).addBox(-2.72F, 3.0F, -0.7F, 0.35F, 1.6F, 1.4F, d0), PartPose.ZERO);
        t.addOrReplaceChild("slit_s", CubeListBuilder.create().texOffs(DARK, DARK_V).addBox(2.37F, 3.0F, -0.7F, 0.35F, 1.6F, 1.4F, d0), PartPose.ZERO);
        t.addOrReplaceChild("embrasure", CubeListBuilder.create().texOffs(DARK, DARK_V).addBox(2.35F, 3.15F, -1.0F, 0.4F, 1.5F, 2.0F, d0), PartPose.ZERO);

        t.addOrReplaceChild("merlon_fl", CubeListBuilder.create().texOffs(STONE, STONE_V).addBox(-2.65F, 5.2F, -2.65F, 1.5F, 1.7F, 1.5F, d0), PartPose.ZERO);
        t.addOrReplaceChild("merlon_fr", CubeListBuilder.create().texOffs(STONE, STONE_V).addBox(1.15F, 5.2F, -2.65F, 1.5F, 1.7F, 1.5F, d0), PartPose.ZERO);
        t.addOrReplaceChild("merlon_bl", CubeListBuilder.create().texOffs(STONE, STONE_V).addBox(-2.65F, 5.2F, 1.15F, 1.5F, 1.7F, 1.5F, d0), PartPose.ZERO);
        t.addOrReplaceChild("merlon_br", CubeListBuilder.create().texOffs(STONE, STONE_V).addBox(1.15F, 5.2F, 1.15F, 1.5F, 1.7F, 1.5F, d0), PartPose.ZERO);
        t.addOrReplaceChild("cap", CubeListBuilder.create().texOffs(STONE, STONE_V).addBox(-2.75F, 5.0F, -2.75F, 5.5F, 0.5F, 5.5F, d0), PartPose.ZERO);

        t.addOrReplaceChild("frame_base", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(-1.6F, 5.45F, -0.9F, 3.2F, 0.4F, 1.8F, d0), PartPose.ZERO);
        t.addOrReplaceChild("frame_arm_p", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(-1.55F, 5.7F, -0.25F, 0.4F, 1.5F, 0.5F, d0), PartPose.ZERO);
        t.addOrReplaceChild("frame_arm_s", CubeListBuilder.create().texOffs(MAST, MAST_V).addBox(1.15F, 5.7F, -0.25F, 0.4F, 1.5F, 0.5F, d0), PartPose.ZERO);
        t.addOrReplaceChild("frame_beam", CubeListBuilder.create().texOffs(IRON, IRON_V).addBox(-1.7F, 7.0F, -0.35F, 3.4F, 0.35F, 0.7F, d0), PartPose.ZERO);
        t.addOrReplaceChild("sling", CubeListBuilder.create().texOffs(CREAM, CREAM_V).addBox(-0.45F, 6.3F, 0.15F, 0.9F, 0.9F, 1.1F, d0), PartPose.ZERO);
    }

    private static void addLineBetween(PartDefinition body, String name, float x0, float y0, float z0, float x1, float y1, float z1) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float dz = z1 - z0;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.05F) {
            return;
        }
        final float pad = 0.35F;
        float inv = 1.0F / len;
        x0 -= dx * inv * pad;
        y0 -= dy * inv * pad;
        z0 -= dz * inv * pad;
        x1 += dx * inv * pad;
        y1 += dy * inv * pad;
        z1 += dz * inv * pad;
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
            ShipOars.poseOar(this.oarsPort[i], true, phase, intensity, hard, REST_PITCH);
            ShipOars.poseOar(this.oarsStbd[i], false, phase, intensity, hard, REST_PITCH);
        }

        this.sailCloth.visible = state.sailPaint == null;
        SquareSail.animate(this.sailRoot, this.sailCells, age, fill, SAIL_MAX_BELLY, 0.35F);

        SquareSail.animate(this.artSailRoot, this.artSailCells, age, fill, ART_SAIL_MAX_BELLY, 1.17F, 1.45F, 1.35F, 0.28F);
    }
}
