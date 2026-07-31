package com.awesomehippo.historicships.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

public final class SquareSail {

    public static final int COLS = 10;
    public static final int ROWS = 5;

    public static PartDefinition add(PartDefinition yardParent, String name, float yardY, float xOff, float height, float halfSpanH, float halfSpanF, float thick, int solidU, int solidV, int stripeU, int stripeV, int rigU, int rigV, CubeDeformation d0) {
        if (stripeU >= 0) {
            return addStriped(yardParent, name, yardY, xOff, height, halfSpanH, halfSpanF, thick, solidU, solidV, stripeU, stripeV, rigU, rigV, d0);
        }
        return addMapped(yardParent, name, yardY, xOff, height, halfSpanH, halfSpanF, thick, solidU, solidV, 0, 0, rigU, rigV, d0);
    }

    public static PartDefinition addMapped(PartDefinition yardParent, String name, float yardY, float xOff, float height, float halfSpanH, float halfSpanF, float thick, int solidU, int solidV, int atlasW, int atlasH, int rigU, int rigV, CubeDeformation d0) {
        thick = Math.max(thick, 0.55F);
        PartDefinition sail = yardParent.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.offset(0.0F, yardY, 0.0F));

        int edgeU = solidU + Math.max(2, atlasW > 0 ? atlasW / 8 : 8);
        int edgeV = solidV + Math.max(2, atlasH > 0 ? 4 : 8);

        float cellThick = Math.max(thick, 1.05F) * 1.55F;
        float sparX = xOff - cellThick * 0.35F - 0.12F;
        float sparD = Math.max(0.70F, thick * 0.90F);
        sail.addOrReplaceChild("head", CubeListBuilder.create().texOffs(edgeU, edgeV).addBox(sparX - sparD * 0.5F, -0.45F, -halfSpanH - 0.15F, sparD, 0.90F, halfSpanH * 2.0F + 0.30F, d0), PartPose.ZERO);

        PartDefinition cloth = sail.addOrReplaceChild("cloth", CubeListBuilder.create(), PartPose.offset(xOff, 0.0F, 0.0F));
        generateSolidGrid(cloth, height, halfSpanH, halfSpanF, thick, solidU, solidV, atlasW, atlasH, d0);

        float rowH = height / (float) ROWS;
        float yOverlap = rowH * 0.48F;
        float hemY = -height - yOverlap;
        float footY = hemY - 0.50F;
        sail.addOrReplaceChild("foot", CubeListBuilder.create().texOffs(edgeU, edgeV).addBox(sparX - sparD * 0.5F, footY - 0.42F, -halfSpanF - 0.15F, sparD, 0.85F, halfSpanF * 2.0F + 0.30F, d0), PartPose.ZERO);
        sail.addOrReplaceChild("clew_p", CubeListBuilder.create().texOffs(rigU, rigV).addBox(sparX - 0.22F, footY - 0.20F, -halfSpanF - 0.18F, 0.55F, 0.55F, 0.55F, d0), PartPose.ZERO);
        sail.addOrReplaceChild("clew_s", CubeListBuilder.create().texOffs(rigU, rigV).addBox(sparX - 0.22F, footY - 0.20F, halfSpanF - 0.37F, 0.55F, 0.55F, 0.55F, d0), PartPose.ZERO);
        return sail;
    }

    private static void generateSolidGrid(PartDefinition cloth, float height, float halfSpanHead, float halfSpanFoot, float thick, int solidU, int solidV, int atlasW, int atlasH, CubeDeformation d0) {
        float rowH = height / (float) ROWS;
        boolean mapped = atlasW > 4 && atlasH > 4;
        boolean wideFabric = mapped && atlasW >= 96;
        int uPad = mapped ? (wideFabric ? 2 : 1) : 8;
        int vPad = mapped ? (wideFabric ? 2 : 1) : 8;
        int uSpan = mapped ? atlasW - uPad * 2 : 0;
        int vSpan = mapped ? atlasH - vPad * 2 : 0;

        float yOverlap = rowH * 0.48F;
        float panelH = rowH + yOverlap * 2.0F;
        float cellThick = Math.max(thick, 1.05F) * 1.55F;

        // small fix so cloth faces don't z-fight
        final float microDepth = 0.04F;

        for (int row = 0; row < ROWS; row++) {
            float tRow = (row + 0.5F) / (float) ROWS;
            float halfSpan = Mth.lerp(tRow, halfSpanHead, halfSpanFoot);
            float colW = (halfSpan * 2.0F) / (float) COLS;
            float zOverlap = colW * 0.48F;
            float panelW = colW + zOverlap * 2.0F;

            float yTop = -((row + 1) * rowH) - yOverlap;
            int faceV = mapped ? solidV + vPad + (row * vSpan) / ROWS : solidV + vPad;

            for (int col = 0; col < COLS; col++) {
                float z0 = -halfSpan + col * colW - zOverlap;
                int faceU = mapped ? solidU + uPad + (col * uSpan) / COLS : solidU + uPad;

                int texU = faceU;
                int texV = faceV;
                float xBias = (((col + row) & 1) == 0) ? -microDepth : microDepth;
                cloth.addOrReplaceChild(cellName(col, row), CubeListBuilder.create().texOffs(texU, texV).addBox(xBias - cellThick * 0.5F, yTop, z0, cellThick, panelH, panelW, d0), PartPose.ZERO);
            }
        }
    }

    public static String cellName(int col, int row) {
        return "c" + col + "_r" + row;
    }

    public static ModelPart[][] resolveCells(ModelPart sailRoot) {
        ModelPart cloth = sailRoot.getChild("cloth");
        ModelPart[][] cells = new ModelPart[COLS][ROWS];
        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r < ROWS; r++) {
                cells[c][r] = cloth.getChild(cellName(c, r));
            }
        }
        return cells;
    }

    public static void animate(ModelPart sailRoot, ModelPart[][] cells, float age, float fill, float maxBelly, float phase) {
        animate(sailRoot, cells, age, fill, maxBelly, phase, 1.0F, 1.0F, 0.0F);
    }

    public static void animate(ModelPart sailRoot, ModelPart[][] cells, float age, float fill, float maxBelly, float phase, float leanMul, float followMul, float idleFloor) {
        fill = Mth.clamp(fill, 0.0F, 1.0F);
        float live = Mth.clamp(Math.max(fill, idleFloor), 0.0F, 1.0F);

        float bagT = live * live * (3.0F - 2.0F * live);
        float mix = 0.66F + bagT * 0.30F;
        float idleBoost = (1.0F - bagT) * (1.0F - bagT);

        float luffAmp = 0.28F * idleBoost + bagT * 0.32F;
        float windPhase = age * 0.027F + phase;
        float windPhase2 = age * 0.041F + phase * 1.31F;
        float windPhase3 = age * 0.063F + phase * 2.07F;
        float bagMul = 0.34F + bagT * 0.78F;

        float[][] xs = new float[COLS][ROWS];
        for (int col = 0; col < COLS; col++) {
            float tCol = col / (float) (COLS - 1);
            float pinZ = Mth.sin(Mth.PI * tCol);
            for (int row = 0; row < ROWS; row++) {

                float tRow = row / (float) (ROWS - 1);
                float pinY = Mth.sin(Mth.PI * tRow);
                float pinYBag = pinY * (0.68F + 0.32F * pinY + bagT * 0.22F * pinY);
                float spanTerm = 0.20F + 0.80F * pinZ;
                float airfoil = maxBelly * bagMul * pinYBag * spanTerm;

                float pin = pinY * (0.38F + 0.62F * pinZ);
                float luff = luffAmp * Mth.sin(0.18F * col + 0.12F * row + windPhase) * pin;
                luff += luffAmp * 0.34F * Mth.sin(0.10F * col - 0.08F * row + windPhase2) * pin;

                luff += (0.16F + bagT * 0.28F) * Mth.sin(0.19F * col + 0.14F * row + age * 0.055F + phase * 0.8F) * pin;

                luff += luffAmp * 0.22F * Mth.sin(0.27F * col + 0.21F * row + windPhase3) * pin * pinY;

                xs[col][row] = airfoil * mix + luff * (1.0F - mix * 0.48F);
            }
        }

        float blend = 0.32F + bagT * 0.22F;
        for (int pass = 0; pass < 3; pass++) {
            float[][] next = new float[COLS][ROWS];
            for (int col = 0; col < COLS; col++) {
                for (int row = 0; row < ROWS; row++) {
                    float sum = xs[col][row];
                    float w = 1.0F;
                    if (col > 0) {
                        sum += xs[col - 1][row] * blend;
                        w += blend;
                    }
                    if (col < COLS - 1) {
                        sum += xs[col + 1][row] * blend;
                        w += blend;
                    }
                    if (row > 0) {
                        sum += xs[col][row - 1] * blend;
                        w += blend;
                    }
                    if (row < ROWS - 1) {
                        sum += xs[col][row + 1] * blend;
                        w += blend;
                    }
                    next[col][row] = sum / w;
                }
            }
            xs = next;

            for (int col = 0; col < COLS; col++) {
                xs[col][0] = 0.0F;
                xs[col][ROWS - 1] = 0.0F;
            }
        }

        final float maxDeltaCol = 0.52F + bagT * 0.22F;
        final float maxDeltaRow = 0.70F + bagT * 0.28F;
        for (int iter = 0; iter < 3; iter++) {
            for (int col = 0; col < COLS; col++) {
                for (int row = 0; row < ROWS; row++) {
                    if (col > 0) {
                        float d = xs[col][row] - xs[col - 1][row];
                        if (Math.abs(d) > maxDeltaCol) {
                            float mid = 0.5F * (xs[col][row] + xs[col - 1][row]);
                            float half = Math.copySign(maxDeltaCol * 0.5F, d);
                            xs[col][row] = mid + half;
                            xs[col - 1][row] = mid - half;
                        }
                    }
                    if (row > 0) {
                        float d = xs[col][row] - xs[col][row - 1];
                        if (Math.abs(d) > maxDeltaRow) {
                            float mid = 0.5F * (xs[col][row] + xs[col][row - 1]);
                            float half = Math.copySign(maxDeltaRow * 0.5F, d);
                            xs[col][row] = mid + half;
                            xs[col][row - 1] = mid - half;
                        }
                    }
                }
            }

            for (int col = 0; col < COLS; col++) {
                xs[col][0] = 0.0F;
                xs[col][ROWS - 1] = 0.0F;
            }
        }

        for (int col = 0; col < COLS; col++) {
            xs[col][0] = 0.0F;
            xs[col][ROWS - 1] = 0.0F;
            if (ROWS > 2) {
                xs[col][1] *= 0.85F;
                xs[col][ROWS - 2] *= 0.90F;
            }
        }

        float follow = (0.34F + bagT * 0.58F) * followMul;
        follow = Mth.clamp(follow, 0.18F, 0.95F);
        for (int col = 0; col < COLS; col++) {
            for (int row = 0; row < ROWS; row++) {
                ModelPart cell = cells[col][row];

                float target = (row == 0 || row == ROWS - 1) ? 0.0F : xs[col][row];
                float prev = cell.x;
                cell.x = (row == 0 || row == ROWS - 1) ? 0.0F : prev + (target - prev) * follow;
                cell.y = 0.0F;
                cell.z = 0.0F;
                cell.xRot = 0.0F;
                cell.yRot = 0.0F;
                cell.zRot = 0.0F;
            }
        }

        float breath = Mth.sin(age * 0.018F + phase);
        float breath2 = Mth.sin(age * 0.011F + phase * 0.7F);
        float breath3 = Mth.sin(age * 0.033F + phase * 1.4F);
        float idle = 1.0F - fill * 0.55F;
        sailRoot.xRot = 0.0F;
        sailRoot.yRot = (breath * 0.012F + breath2 * 0.006F + breath3 * 0.003F) * idle * leanMul;
        sailRoot.zRot = (0.014F + (1.0F - fill) * 0.018F + fill * 0.016F + breath * 0.010F * idle + breath3 * 0.005F * idle) * leanMul;
    }

    public static PartDefinition addStriped(PartDefinition yardParent, String name, float yardY, float xOff, float height, float halfSpanH, float halfSpanF, float thick, int solidU, int solidV, int stripeU, int stripeV, int rigU, int rigV, CubeDeformation d0) {

        thick = Math.max(thick, 0.85F);

        PartDefinition sail = yardParent.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.offset(0.0F, yardY, 0.0F));

        final float yardBottom = -0.80F;
        final float clothTopY = yardBottom + 0.28F;

        PartDefinition cloth = sail.addOrReplaceChild("cloth", CubeListBuilder.create(), PartPose.offset(xOff, 0.0F, 0.0F));
        generateStripedGrid(cloth, height, halfSpanH, halfSpanF, thick, solidU, solidV, stripeU, stripeV, clothTopY, d0);

        float rowH = height / (float) ROWS;
        float yOverlap = rowH * 0.28F;

        float hemY = clothTopY - height - yOverlap;
        float cellThick = Math.max(thick, 0.95F) * 1.35F;
        float sparH = 0.72F;
        float sparD = 0.70F;

        float footY = hemY - sparH * 0.5F + 0.06F;
        float footX = xOff - cellThick * 0.32F - 0.08F;
        PartDefinition boom = sail.addOrReplaceChild("boom", CubeListBuilder.create(), PartPose.ZERO);
        boom.addOrReplaceChild("foot", CubeListBuilder.create().texOffs(rigU, rigV).addBox(footX - sparD * 0.5F, footY - sparH * 0.5F, -halfSpanF - 0.20F, sparD, sparH, halfSpanF * 2.0F + 0.40F, d0), PartPose.ZERO);
        boom.addOrReplaceChild("clew_p", CubeListBuilder.create().texOffs(rigU, rigV).addBox(footX - 0.22F, footY - 0.22F, -halfSpanF - 0.18F, 0.50F, 0.50F, 0.50F, d0), PartPose.ZERO);
        boom.addOrReplaceChild("clew_s", CubeListBuilder.create().texOffs(rigU, rigV).addBox(footX - 0.22F, footY - 0.22F, halfSpanF - 0.32F, 0.50F, 0.50F, 0.50F, d0), PartPose.ZERO);
        return sail;
    }

    private static void generateStripedGrid(PartDefinition cloth, float height, float halfSpanHead, float halfSpanFoot, float thick, int solidU, int solidV, int stripeU, int stripeV, float topY, CubeDeformation d0) {
        float rowH = height / (float) ROWS;

        float yOverlap = rowH * 0.28F;
        float panelH = rowH + yOverlap * 2.0F;

        float cellThick = Math.max(thick, 0.95F) * 1.35F;

        final float stripeDepth = 0.07F;

        for (int row = 0; row < ROWS; row++) {
            float tRow = (row + 0.5F) / (float) ROWS;
            float halfSpan = Mth.lerp(tRow, halfSpanHead, halfSpanFoot);
            float colW = (halfSpan * 2.0F) / (float) COLS;

            float zInset = Math.min(0.06F, colW * 0.025F);
            float panelW = Math.max(0.45F, colW - zInset);
            float yTop = topY - ((row + 1) * rowH) - yOverlap;

            for (int col = 0; col < COLS; col++) {

                float z0 = -halfSpan + col * colW + (colW - panelW) * 0.5F;
                // alternate stripe uv by column for drakkar sail
                boolean alt = (col % 2 == 1);

                int u = (alt ? stripeU : solidU) + 8;
                int v = (alt ? stripeV : solidV) + 8;
                float xBias = alt ? stripeDepth : -stripeDepth;
                cloth.addOrReplaceChild(cellName(col, row), CubeListBuilder.create().texOffs(u, v).addBox(xBias - cellThick * 0.5F, yTop, z0, cellThick, panelH, panelW, d0), PartPose.ZERO);
            }
        }
    }
}
