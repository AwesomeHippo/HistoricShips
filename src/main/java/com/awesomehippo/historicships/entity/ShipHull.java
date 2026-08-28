package com.awesomehippo.historicships.entity;

import net.minecraft.util.Mth;

public final class ShipHull {
    private final float[] along;
    private final float[] half;
    private final float bow;
    private final float stern;
    private final float maxHalf;

    public ShipHull(float[] along, float[] half) {
        this.along = along;
        this.half = half;
        this.bow = along[along.length - 1];
        this.stern = -along[0];
        float max = 0.0F;
        for (float h : half) {
            if (h > max) {
                max = h;
            }
        }
        this.maxHalf = max;
    }

    public static ShipHull rect(float halfLoa, float halfBeam) {
        return new ShipHull(new float[] {-halfLoa, halfLoa}, new float[] {halfBeam, halfBeam});
    }

    public static ShipHull ofModel(float u, float... alongHalf) {
        int n = alongHalf.length / 2;
        float[] along = new float[n];
        float[] half = new float[n];
        for (int i = 0; i < n; i++) {
            along[i] = alongHalf[i * 2] * u;
            half[i] = alongHalf[i * 2 + 1] * u + 0.03F;
        }
        return new ShipHull(along, half);
    }

    public float bow() {
        return this.bow;
    }

    public float stern() {
        return this.stern;
    }

    public float maxHalf() {
        return this.maxHalf;
    }

    public float halfAt(float x) {
        float[] a = this.along;
        float[] h = this.half;
        if (x <= a[0]) {
            return h[0];
        }
        int last = a.length - 1;
        if (x >= a[last]) {
            return h[last];
        }
        for (int i = 1; i < a.length; i++) {
            if (x <= a[i]) {
                float dx = a[i] - a[i - 1];
                if (dx * dx < 1.0E-8F) {
                    return Math.max(h[i - 1], h[i]);
                }
                float t = (x - a[i - 1]) / dx;
                return h[i - 1] + t * (h[i] - h[i - 1]);
            }
        }
        return h[last];
    }

    public boolean contains(double x, double z, double radius) {
        if (x < this.along[0] - radius || x > this.along[this.along.length - 1] + radius) {
            return false;
        }
        return Math.abs(z) <= this.halfAt((float) x) + radius;
    }

    public double distance(double x, double z) {
        float x0 = this.along[0];
        float x1 = this.along[this.along.length - 1];
        double dx = 0.0;
        if (x < x0) {
            dx = x0 - x;
        } else if (x > x1) {
            dx = x - x1;
        }
        float at = (float) Mth.clamp(x, x0, x1);
        double dz = Math.max(0.0, Math.abs(z) - this.halfAt(at));
        return Math.sqrt(dx * dx + dz * dz);
    }

    public int vertCount() {
        return this.along.length * 2;
    }

    public void writeVerts(double ox, double oz, float bowX, float bowZ, float stbdX, float stbdZ, double[] vx, double[] vz) {
        int n = this.along.length;
        for (int i = 0; i < n; i++) {
            vx[i] = ox + this.along[i] * bowX + this.half[i] * stbdX;
            vz[i] = oz + this.along[i] * bowZ + this.half[i] * stbdZ;
        }
        for (int i = 0; i < n; i++) {
            int s = n - 1 - i;
            vx[n + i] = ox + this.along[s] * bowX - this.half[s] * stbdX;
            vz[n + i] = oz + this.along[s] * bowZ - this.half[s] * stbdZ;
        }
    }

    public static boolean overlapPush(ShipHull a, double ax, double az, float aBowX, float aBowZ, float aStbdX, float aStbdZ, ShipHull b, double bx, double bz, float bBowX, float bBowZ, float bStbdX, float bStbdZ, double[] out) {
        int na = a.vertCount();
        int nb = b.vertCount();
        double[] avx = new double[na];
        double[] avz = new double[na];
        double[] bvx = new double[nb];
        double[] bvz = new double[nb];
        a.writeVerts(ax, az, aBowX, aBowZ, aStbdX, aStbdZ, avx, avz);
        b.writeVerts(bx, bz, bBowX, bBowZ, bStbdX, bStbdZ, bvx, bvz);

        out[0] = 0.0;
        out[1] = 0.0;
        out[2] = Double.POSITIVE_INFINITY;
        double cdx = bx - ax;
        double cdz = bz - az;
        if (testEdges(avx, avz, na, bvx, bvz, nb, cdx, cdz, out) || testEdges(bvx, bvz, nb, avx, avz, na, cdx, cdz, out)) {
            return false;
        }
        return out[2] < Double.POSITIVE_INFINITY;
    }

    private static boolean testEdges(double[] vx, double[] vz, int n, double[] ox, double[] oz, int on, double cdx, double cdz, double[] best) {
        for (int i = 0; i < n; i++) {
            int j = i + 1 < n ? i + 1 : 0;
            double ex = vx[j] - vx[i];
            double ez = vz[j] - vz[i];
            double ux = -ez;
            double uz = ex;
            double len = Math.sqrt(ux * ux + uz * uz);
            if (len < 1.0E-8) {
                continue;
            }
            ux /= len;
            uz /= len;
            double minA = Double.POSITIVE_INFINITY;
            double maxA = Double.NEGATIVE_INFINITY;
            for (int k = 0; k < n; k++) {
                double p = vx[k] * ux + vz[k] * uz;
                if (p < minA) {
                    minA = p;
                }
                if (p > maxA) {
                    maxA = p;
                }
            }
            double minB = Double.POSITIVE_INFINITY;
            double maxB = Double.NEGATIVE_INFINITY;
            for (int k = 0; k < on; k++) {
                double p = ox[k] * ux + oz[k] * uz;
                if (p < minB) {
                    minB = p;
                }
                if (p > maxB) {
                    maxB = p;
                }
            }
            double o1 = maxA - minB;
            double o2 = maxB - minA;
            if (o1 <= 0.0 || o2 <= 0.0) {
                return true;
            }
            double o = Math.min(o1, o2);
            if (o < best[2]) {
                if (cdx * ux + cdz * uz < 0.0) {
                    ux = -ux;
                    uz = -uz;
                }
                best[0] = ux;
                best[1] = uz;
                best[2] = o;
            }
        }
        return false;
    }
}
