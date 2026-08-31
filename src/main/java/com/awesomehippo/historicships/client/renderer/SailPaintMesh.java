package com.awesomehippo.historicships.client.renderer;

import com.awesomehippo.historicships.client.model.QuinqueremeModel;
import com.awesomehippo.historicships.client.model.QuinqueremePaintModel;
import com.awesomehippo.historicships.client.model.SquareSail;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class SailPaintMesh {
    private static final float SPAN_SCALE = 1.0F + 2.0F * SquareSail.GRID_OVERLAP / SquareSail.COLS;
    private static final float GAP = 0.12F;
    private static final float TINY = 0.06F;
    private static final float INSET = 0.12F;

    private SailPaintMesh() {}

    public static void emit(PoseStack.Pose pose, VertexConsumer buffer, QuinqueremePaintModel model, int light, boolean front) {
        if (front) {
            emitSail(pose, buffer, model.artSailRoot(), model.artSailCells(), light, QuinqueremeModel.ART_BASE_X, QuinqueremeModel.ART_BASE_Y, QuinqueremeModel.ART_Z_ROT, QuinqueremeModel.ART_SAIL_Y, QuinqueremeModel.ART_SAIL_X_OFF, QuinqueremeModel.ART_SAIL_HEIGHT, QuinqueremeModel.ART_SAIL_HALF_HEAD, QuinqueremeModel.ART_SAIL_HALF_FOOT, QuinqueremeModel.ART_SAIL_THICK);
        } else {
            emitSail(pose, buffer, model.sailRoot(), model.sailCells(), light, 0.0F, QuinqueremeModel.MAST_BASE_Y, 0.0F, QuinqueremeModel.YARD_Y, QuinqueremeModel.SAIL_X_OFF, QuinqueremeModel.SAIL_HEIGHT, QuinqueremeModel.SAIL_HALF_HEAD, QuinqueremeModel.SAIL_HALF_FOOT, QuinqueremeModel.SAIL_THICK);
        }
    }

    private static void emitSail(PoseStack.Pose pose, VertexConsumer buffer, ModelPart sailRoot, ModelPart[][] cells, int light, float originX, float originY, float originZRot, float yardY, float xOff, float height, float halfHead, float halfFoot, float thick) {
        float clothHalf = SquareSail.solidCellThick(thick) * 0.5F;
        float sheet = 2.0F * clothHalf + GAP - INSET;
        float yOver = (height / SquareSail.ROWS) * SquareSail.GRID_OVERLAP;

        Matrix4f base = new Matrix4f(pose.pose());
        base.translate(originX / 16.0F, originY / 16.0F, 0.0F);
        base.rotateZ(originZRot);
        base.translate(0.0F, yardY / 16.0F, 0.0F);
        base.rotateZ(sailRoot.zRot);
        base.rotateY(sailRoot.yRot);
        base.translate(xOff / 16.0F, 0.0F, 0.0F);

        Matrix3f normalMat = new Matrix3f(pose.normal());
        normalMat.rotateZ(originZRot);
        normalMat.rotateZ(sailRoot.zRot);
        normalMat.rotateY(sailRoot.yRot);
        Vector3f nEast = normalMat.transform(new Vector3f(1.0F, 0.0F, 0.0F)).normalize();
        Vector3f nWest = normalMat.transform(new Vector3f(-1.0F, 0.0F, 0.0F)).normalize();
        Vector3f nPort = normalMat.transform(new Vector3f(0.0F, 0.0F, -1.0F)).normalize();
        Vector3f nStbd = normalMat.transform(new Vector3f(0.0F, 0.0F, 1.0F)).normalize();
        Vector3f nUp = normalMat.transform(new Vector3f(0.0F, 1.0F, 0.0F)).normalize();
        Vector3f nDown = normalMat.transform(new Vector3f(0.0F, -1.0F, 0.0F)).normalize();
        Vector3f pos = new Vector3f();

        int cw = SquareSail.COLS + 1;
        int rw = SquareSail.ROWS + 1;
        float[][] frontX = new float[cw][rw];
        float[][] y = new float[cw][rw];
        float[][] z = new float[cw][rw];
        float[][] u = new float[cw][rw];
        float[][] v = new float[cw][rw];
        for (int col = 0; col <= SquareSail.COLS; col++) {
            float ut = col / (float) SquareSail.COLS;
            for (int row = 0; row <= SquareSail.ROWS; row++) {
                float vt = row / (float) SquareSail.ROWS;
                float yy = -vt * height;
                if (row == 0) {
                    yy += yOver;
                }
                if (row == SquareSail.ROWS) {
                    yy -= yOver;
                }
                float half = Mth.lerp(vt, halfHead, halfFoot) * SPAN_SCALE;
                frontX[col][row] = paintX(cells, clothHalf, col, row);
                y[col][row] = yy;
                z[col][row] = Mth.lerp(ut, -half, half);
                u[col][row] = ut;
                v[col][row] = vt;
            }
        }

        for (int row = 0; row < SquareSail.ROWS; row++) {
            for (int col = 0; col < SquareSail.COLS; col++) {
                int c1 = col + 1;
                int r1 = row + 1;
                vertex(buffer, base, pos, nEast, light, frontX[col][r1], y[col][r1], z[col][r1], u[col][r1], v[col][r1]);
                vertex(buffer, base, pos, nEast, light, frontX[c1][r1], y[c1][r1], z[c1][r1], u[c1][r1], v[c1][r1]);
                vertex(buffer, base, pos, nEast, light, frontX[c1][row], y[c1][row], z[c1][row], u[c1][row], v[c1][row]);
                vertex(buffer, base, pos, nEast, light, frontX[col][row], y[col][row], z[col][row], u[col][row], v[col][row]);

                vertex(buffer, base, pos, nWest, light, frontX[c1][r1] - sheet, y[c1][r1], z[c1][r1], u[c1][r1], v[c1][r1]);
                vertex(buffer, base, pos, nWest, light, frontX[col][r1] - sheet, y[col][r1], z[col][r1], u[col][r1], v[col][r1]);
                vertex(buffer, base, pos, nWest, light, frontX[col][row] - sheet, y[col][row], z[col][row], u[col][row], v[col][row]);
                vertex(buffer, base, pos, nWest, light, frontX[c1][row] - sheet, y[c1][row], z[c1][row], u[c1][row], v[c1][row]);
            }
        }

        for (int col = 0; col < SquareSail.COLS; col++) {
            int c1 = col + 1;
            lid(buffer, base, pos, nUp, light, sheet, frontX[col][0], frontX[c1][0], y[col][0] + TINY, z[col][0], z[c1][0], u[col][0], u[c1][0], v[col][0], true);
            int last = SquareSail.ROWS;
            lid(buffer, base, pos, nDown, light, sheet, frontX[col][last], frontX[c1][last], y[col][last] - TINY, z[col][last], z[c1][last], u[col][last], u[c1][last], v[col][last], false);
        }
        for (int row = 0; row < SquareSail.ROWS; row++) {
            int r1 = row + 1;
            side(buffer, base, pos, nPort, light, sheet, frontX[0][row], frontX[0][r1], y[0][row], y[0][r1], z[0][row], u[0][row], v[0][row], v[0][r1]);
            int last = SquareSail.COLS;
            side(buffer, base, pos, nStbd, light, sheet, frontX[last][row], frontX[last][r1], y[last][row], y[last][r1], z[last][row], u[last][row], v[last][row], v[last][r1]);
        }
    }

    private static float paintX(ModelPart[][] cells, float clothHalf, int col, int row) {
        float x = Float.NEGATIVE_INFINITY;
        int firstCol = Math.max(0, col - 1);
        int lastCol = Math.min(SquareSail.COLS - 1, col);
        int firstRow = Math.max(0, row - 1);
        int lastRow = Math.min(SquareSail.ROWS - 1, row);
        for (int cellCol = firstCol; cellCol <= lastCol; cellCol++) {
            for (int cellRow = firstRow; cellRow <= lastRow; cellRow++) {
                float bias = (((cellCol + cellRow) & 1) == 0) ? -SquareSail.SOLID_MICRO : SquareSail.SOLID_MICRO;
                x = Math.max(x, cells[cellCol][cellRow].x + bias + clothHalf);
            }
        }
        return x + GAP;
    }

    private static void lid(VertexConsumer buffer, Matrix4f m, Vector3f pos, Vector3f normal, int light, float sheet, float x0, float x1, float y, float z0, float z1, float u0, float u1, float v, boolean up) {
        if (up) {
            vertex(buffer, m, pos, normal, light, x0, y, z0, u0, v);
            vertex(buffer, m, pos, normal, light, x0 - sheet, y, z0, u0, v);
            vertex(buffer, m, pos, normal, light, x1 - sheet, y, z1, u1, v);
            vertex(buffer, m, pos, normal, light, x1, y, z1, u1, v);
        } else {
            vertex(buffer, m, pos, normal, light, x1, y, z1, u1, v);
            vertex(buffer, m, pos, normal, light, x1 - sheet, y, z1, u1, v);
            vertex(buffer, m, pos, normal, light, x0 - sheet, y, z0, u0, v);
            vertex(buffer, m, pos, normal, light, x0, y, z0, u0, v);
        }
    }

    private static void side(VertexConsumer buffer, Matrix4f m, Vector3f pos, Vector3f normal, int light, float sheet, float x0, float x1, float y0, float y1, float z, float u, float v0, float v1) {
        vertex(buffer, m, pos, normal, light, x0, y0, z, u, v0);
        vertex(buffer, m, pos, normal, light, x0 - sheet, y0, z, u, v0);
        vertex(buffer, m, pos, normal, light, x1 - sheet, y1, z, u, v1);
        vertex(buffer, m, pos, normal, light, x1, y1, z, u, v1);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f m, Vector3f pos, Vector3f normal, int light, float x, float y, float z, float u, float v) {
        m.transformPosition(x / 16.0F, y / 16.0F, z / 16.0F, pos);
        buffer.addVertex(pos.x, pos.y, pos.z).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(normal.x, normal.y, normal.z);
    }
}
