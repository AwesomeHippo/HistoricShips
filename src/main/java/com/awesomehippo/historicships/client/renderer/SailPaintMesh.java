package com.awesomehippo.historicships.client.renderer;

import com.awesomehippo.historicships.client.model.QuinqueremeModel;
import com.awesomehippo.historicships.client.model.QuinqueremePaintModel;
import com.awesomehippo.historicships.client.model.SquareSail;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class SailPaintMesh {
    private static final float ROW_H = QuinqueremeModel.SAIL_HEIGHT / SquareSail.ROWS;
    private static final float Y_OVER = ROW_H * SquareSail.GRID_OVERLAP;
    private static final float SPAN_SCALE = 1.0F + 2.0F * SquareSail.GRID_OVERLAP / SquareSail.COLS;
    private static final float CLOTH_HALF = SquareSail.solidCellThick(QuinqueremeModel.SAIL_THICK) * 0.5F;
    private static final float GAP = 0.25F;

    private SailPaintMesh() {}

    public static void emit(PoseStack.Pose pose, VertexConsumer buffer, QuinqueremePaintModel model, int light) {
        Matrix4f base = new Matrix4f(pose.pose());
        base.translate(0.0F, QuinqueremeModel.MAST_BASE_Y / 16.0F, 0.0F);
        base.translate(0.0F, QuinqueremeModel.YARD_Y / 16.0F, 0.0F);
        base.rotateZ(model.sailRoot().zRot);
        base.rotateY(model.sailRoot().yRot);
        base.translate(QuinqueremeModel.SAIL_X_OFF / 16.0F, 0.0F, 0.0F);

        Matrix3f normalMat = new Matrix3f(pose.normal());
        normalMat.rotateZ(model.sailRoot().zRot);
        normalMat.rotateY(model.sailRoot().yRot);

        Vector3f nEast = normalMat.transform(new Vector3f(1.0F, 0.0F, 0.0F)).normalize();
        Vector3f pos = new Vector3f();

        float[][] paintX = new float[SquareSail.COLS + 1][SquareSail.ROWS + 1];
        for (int col = 0; col <= SquareSail.COLS; col++) {
            for (int row = 0; row <= SquareSail.ROWS; row++) {
                paintX[col][row] = paintX(model, col, row);
            }
        }

        for (int row = 0; row < SquareSail.ROWS; row++) {
            float v0 = row / (float) SquareSail.ROWS;
            float v1 = (row + 1) / (float) SquareSail.ROWS;
            float yT = -v0 * QuinqueremeModel.SAIL_HEIGHT;
            float yB = -v1 * QuinqueremeModel.SAIL_HEIGHT;
            if (row == 0) {
                yT += Y_OVER;
            }
            if (row == SquareSail.ROWS - 1) {
                yB -= Y_OVER;
            }
            float halfT = Mth.lerp(v0, QuinqueremeModel.SAIL_HALF_HEAD, QuinqueremeModel.SAIL_HALF_FOOT) * SPAN_SCALE;
            float halfB = Mth.lerp(v1, QuinqueremeModel.SAIL_HALF_HEAD, QuinqueremeModel.SAIL_HALF_FOOT) * SPAN_SCALE;

            for (int col = 0; col < SquareSail.COLS; col++) {
                float u0 = col / (float) SquareSail.COLS;
                float u1 = (col + 1) / (float) SquareSail.COLS;
                float zTL = Mth.lerp(u0, -halfT, halfT);
                float zTR = Mth.lerp(u1, -halfT, halfT);
                float zBL = Mth.lerp(u0, -halfB, halfB);
                float zBR = Mth.lerp(u1, -halfB, halfB);

                vertex(buffer, base, pos, nEast, light, paintX[col][row + 1], yB, zBL, u0, v1);
                vertex(buffer, base, pos, nEast, light, paintX[col + 1][row + 1], yB, zBR, u1, v1);
                vertex(buffer, base, pos, nEast, light, paintX[col + 1][row], yT, zTR, u1, v0);
                vertex(buffer, base, pos, nEast, light, paintX[col][row], yT, zTL, u0, v0);
            }
        }
    }

    private static float paintX(QuinqueremePaintModel model, int col, int row) {
        float x = Float.NEGATIVE_INFINITY;
        int firstCol = Math.max(0, col - 1);
        int lastCol = Math.min(SquareSail.COLS - 1, col);
        int firstRow = Math.max(0, row - 1);
        int lastRow = Math.min(SquareSail.ROWS - 1, row);
        for (int cellCol = firstCol; cellCol <= lastCol; cellCol++) {
            for (int cellRow = firstRow; cellRow <= lastRow; cellRow++) {
                float bias = (((cellCol + cellRow) & 1) == 0) ? -SquareSail.SOLID_MICRO : SquareSail.SOLID_MICRO;
                x = Math.max(x, model.sailCells()[cellCol][cellRow].x + bias + CLOTH_HALF);
            }
        }
        return x + GAP;
    }

    private static void vertex(VertexConsumer buffer, Matrix4f m, Vector3f pos, Vector3f normal, int light, float x, float y, float z, float u, float v) {
        m.transformPosition(x / 16.0F, y / 16.0F, z / 16.0F, pos);
        buffer.addVertex(pos.x, pos.y, pos.z).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(normal.x, normal.y, normal.z);
    }
}
