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
    private static final float CLOTH_HALF = SquareSail.solidCellThick(QuinqueremeModel.SAIL_THICK) * 0.5F;
    private static final float GAP = 0.12F;
    private static final float LAYER = 0.10F;
    private static final float TINY = 0.06F;
    private static final float INSET = 0.12F;
    private static final float Y_FRONT = 1.05F;
    private static final float Y_FRONT2 = 1.55F;

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
        Vector3f nPort = normalMat.transform(new Vector3f(0.0F, 0.0F, -1.0F)).normalize();
        Vector3f nStbd = normalMat.transform(new Vector3f(0.0F, 0.0F, 1.0F)).normalize();
        Vector3f nUp = normalMat.transform(new Vector3f(0.0F, 1.0F, 0.0F)).normalize();
        Vector3f nDown = normalMat.transform(new Vector3f(0.0F, -1.0F, 0.0F)).normalize();

        Matrix4f cellMat = new Matrix4f();
        Vector3f pos = new Vector3f();

        for (int row = 0; row < SquareSail.ROWS; row++) {
            float tRow = (row + 0.5F) / (float) SquareSail.ROWS;
            float halfSpan = Mth.lerp(tRow, QuinqueremeModel.SAIL_HALF_HEAD, QuinqueremeModel.SAIL_HALF_FOOT);
            float colW = halfSpan * 2.0F / (float) SquareSail.COLS;
            float zOver = colW * SquareSail.GRID_OVERLAP;

            float yT = -row * ROW_H + Y_OVER;
            float yB = -(row + 1) * ROW_H - Y_OVER;
            boolean edgeHead = row == 0;
            boolean edgeFoot = row == SquareSail.ROWS - 1;

            for (int col = 0; col < SquareSail.COLS; col++) {
                float z0 = -halfSpan + col * colW - zOver;
                float z1 = -halfSpan + (col + 1) * colW + zOver;

                float xBias = (((col + row) & 1) == 0) ? -SquareSail.SOLID_MICRO : SquareSail.SOLID_MICRO;
                float xFront = xBias + CLOTH_HALF + GAP;
                float xLid = xBias - CLOTH_HALF + INSET;
                boolean edgePort = col == 0;
                boolean edgeStbd = col == SquareSail.COLS - 1;

                cellMat.set(base).translate(model.sailCells()[col][row].x / 16.0F, 0.0F, 0.0F);

                float yU = edgeHead ? yT : yT + Y_FRONT;
                float yD = edgeFoot ? yB : yB - Y_FRONT;
                float zL = edgePort ? z0 : z0 - zOver;
                float zR = edgeStbd ? z1 : z1 + zOver;
                quad(buffer, cellMat, pos, nEast, light, xFront, yU, yD, zL, zR);

                float yU2 = edgeHead ? yT : yT + Y_FRONT2;
                float yD2 = edgeFoot ? yB : yB - Y_FRONT2;
                float zL2 = edgePort ? z0 : z0 - zOver - 0.45F;
                float zR2 = edgeStbd ? z1 : z1 + zOver + 0.45F;
                quad(buffer, cellMat, pos, nEast, light, xFront + LAYER, yU2, yD2, zL2, zR2);

                lidY(buffer, cellMat, pos, nUp, light, true, xLid, xFront, yT + TINY, z0, z1);
                lidY(buffer, cellMat, pos, nDown, light, false, xLid, xFront, yB - TINY, z0, z1);

                sideZ(buffer, cellMat, pos, nPort, light, xFront, xBias, yB, yT, z0);
                sideZ(buffer, cellMat, pos, nStbd, light, xBias, xFront, yB, yT, z1);
            }
        }
    }

    private static float uAt(float z) {
        return Mth.clamp((z + QuinqueremeModel.SAIL_HALF_HEAD) / (2.0F * QuinqueremeModel.SAIL_HALF_HEAD), 0.0F, 1.0F);
    }

    private static float vAt(float y) {
        return Mth.clamp(-y / QuinqueremeModel.SAIL_HEIGHT, 0.0F, 1.0F);
    }

    private static void quad(VertexConsumer buffer, Matrix4f m, Vector3f pos, Vector3f normal, int light, float x, float yT, float yB, float z0, float z1) {
        vertex(buffer, m, pos, normal, light, x, yB, z0, uAt(z0), vAt(yB));
        vertex(buffer, m, pos, normal, light, x, yB, z1, uAt(z1), vAt(yB));
        vertex(buffer, m, pos, normal, light, x, yT, z1, uAt(z1), vAt(yT));
        vertex(buffer, m, pos, normal, light, x, yT, z0, uAt(z0), vAt(yT));
    }

    private static void sideZ(VertexConsumer buffer, Matrix4f m, Vector3f pos, Vector3f normal, int light, float x0, float x1, float y0, float y1, float z) {
        float u = uAt(z);
        vertex(buffer, m, pos, normal, light, x0, y0, z, u, vAt(y0));
        vertex(buffer, m, pos, normal, light, x1, y0, z, u, vAt(y0));
        vertex(buffer, m, pos, normal, light, x1, y1, z, u, vAt(y1));
        vertex(buffer, m, pos, normal, light, x0, y1, z, u, vAt(y1));
    }

    private static void lidY(VertexConsumer buffer, Matrix4f m, Vector3f pos, Vector3f normal, int light, boolean up, float xBack, float xFront, float y, float z0, float z1) {
        float v = vAt(y);
        if (up) {
            vertex(buffer, m, pos, normal, light, xFront, y, z0, uAt(z0), v);
            vertex(buffer, m, pos, normal, light, xBack, y, z0, uAt(z0), v);
            vertex(buffer, m, pos, normal, light, xBack, y, z1, uAt(z1), v);
            vertex(buffer, m, pos, normal, light, xFront, y, z1, uAt(z1), v);
        } else {
            vertex(buffer, m, pos, normal, light, xFront, y, z1, uAt(z1), v);
            vertex(buffer, m, pos, normal, light, xBack, y, z1, uAt(z1), v);
            vertex(buffer, m, pos, normal, light, xBack, y, z0, uAt(z0), v);
            vertex(buffer, m, pos, normal, light, xFront, y, z0, uAt(z0), v);
        }
    }

    private static void vertex(VertexConsumer buffer, Matrix4f m, Vector3f pos, Vector3f normal, int light, float x, float y, float z, float u, float v) {
        m.transformPosition(x / 16.0F, y / 16.0F, z / 16.0F, pos);
        buffer.addVertex(pos.x, pos.y, pos.z).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(normal.x, normal.y, normal.z);
    }
}
