package com.awesomehippo.historicships.entity;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class SailPaint {
    // sail paint size
    public static final int WIDTH = 60;
    public static final int HEIGHT = 40;
    public static final int PIXELS = WIDTH * HEIGHT;

    // 0 = empty & 1-16 = dyes colors
    public static final int COLORS = 16;

    public static final Codec<Data> CODEC = Codec.BYTE.listOf().xmap(list -> new Data(toBytes(list)), data -> toList(data.pixels()));
    public static final StreamCodec<ByteBuf, Data> STREAM_CODEC = ByteBufCodecs.BYTE_ARRAY.map(Data::new, Data::pixels);

    private SailPaint() {}

    public record Data(byte[] pixels) {
        public Data {
            pixels = pixels.clone();
        }

        @Override
        public byte[] pixels() {
            return this.pixels.clone();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Data other && Arrays.equals(this.pixels, other.pixels);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(this.pixels);
        }
    }

    public static int argb(int index) {
        DyeColor[] dyes = DyeColor.values();
        if (index <= 0 || index > dyes.length) {
            return 0;
        }
        return dyes[index - 1].getTextureDiffuseColor();
    }

    public static boolean isValid(byte @Nullable [] pixels) {
        if (pixels == null || pixels.length != PIXELS) {
            return false;
        }
        for (byte b : pixels) {
            if (b < 0 || b > COLORS) {
                return false;
            }
        }
        return true;
    }

    public static boolean isBlank(byte[] pixels) {
        for (byte b : pixels) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    public static int[] toInts(byte[] paint) {
        int[] ints = new int[paint.length];
        for (int i = 0; i < paint.length; i++) {
            ints[i] = paint[i];
        }
        return ints;
    }

    public static byte @Nullable [] fromInts(int @Nullable [] ints) {
        if (ints == null || ints.length != PIXELS) {
            return null;
        }
        byte[] paint = new byte[PIXELS];
        for (int i = 0; i < PIXELS; i++) {
            if (ints[i] < 0 || ints[i] > COLORS) {
                return null;
            }
            paint[i] = (byte) ints[i];
        }
        return paint;
    }

    private static byte[] toBytes(List<Byte> list) {
        byte[] bytes = new byte[list.size()];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = list.get(i);
        }
        return bytes;
    }

    private static List<Byte> toList(byte[] bytes) {
        List<Byte> list = new ArrayList<>(bytes.length);
        for (byte b : bytes) {
            list.add(b);
        }
        return list;
    }
}
