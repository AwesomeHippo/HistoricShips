package com.awesomehippo.historicships.client.renderer;

import com.awesomehippo.historicships.entity.QuinqueremeEntity;
import com.awesomehippo.historicships.entity.SailPaint;
import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class SailPaintTextures {
    private static final Map<Integer, Entry> BY_ENTITY = new HashMap<>();
    private static int nextId;

    private SailPaintTextures() {}

    @Nullable
    public static Identifier get(QuinqueremeEntity ship) {
        byte[] paint = ship.getSailPaint();
        if (paint == null || !SailPaint.isValid(paint)) {
            return null;
        }
        Entry entry = BY_ENTITY.get(ship.getId());
        if (entry == null) {
            Identifier id = Identifier.fromNamespaceAndPath("historicships", "sail_paint/" + (nextId++));
            DynamicTexture texture = new DynamicTexture(() -> "historicships_sail_paint", SailPaint.WIDTH, SailPaint.HEIGHT, true);
            Minecraft.getInstance().getTextureManager().register(id, texture);
            entry = new Entry(id, texture);
            BY_ENTITY.put(ship.getId(), entry);
        }
        if (entry.version != ship.getSailPaintVersion()) {
            entry.version = ship.getSailPaintVersion();
            fill(entry.texture, paint);
        }
        return entry.id;
    }

    public static void release(int entityId) {
        Entry entry = BY_ENTITY.remove(entityId);
        if (entry != null) {
            Minecraft.getInstance().getTextureManager().release(entry.id);
        }
    }

    public static void clear() {
        for (Entry entry : BY_ENTITY.values()) {
            Minecraft.getInstance().getTextureManager().release(entry.id);
        }
        BY_ENTITY.clear();
    }

    private static void fill(DynamicTexture texture, byte[] paint) {
        NativeImage image = texture.getPixels();
        for (int y = 0; y < SailPaint.HEIGHT; y++) {
            for (int x = 0; x < SailPaint.WIDTH; x++) {
                byte b = paint[y * SailPaint.WIDTH + x];
                image.setPixel(x, y, b == 0 ? 0xFFF4E8CE : SailPaint.argb(b));
            }
        }
        texture.upload();
    }

    private static final class Entry {
        final Identifier id;
        final DynamicTexture texture;
        int version = -1;

        Entry(Identifier id, DynamicTexture texture) {
            this.id = id;
            this.texture = texture;
        }
    }
}
