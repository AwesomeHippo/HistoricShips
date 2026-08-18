package com.awesomehippo.historicships.entity;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;

import org.jetbrains.annotations.Nullable;

public enum DrakkarSailStripe {
    RED((byte) 0, "drakkar"),
    BLUE((byte) 1, "drakkar_sail_blue"),
    GREEN((byte) 2, "drakkar_sail_green"),
    PURPLE((byte) 3, "drakkar_sail_purple");

    private final byte id;
    private final Identifier pristine;
    private final Identifier damaged;
    private final Identifier wrecked;

    DrakkarSailStripe(byte id, String path) {
        this.id = id;
        this.pristine = idOf(path);
        this.damaged = idOf(path + "_damaged");
        this.wrecked = idOf(path + "_wrecked");
    }

    private static Identifier idOf(String path) {
        return Identifier.fromNamespaceAndPath("historicships", "textures/entity/" + path + ".png");
    }

    public byte id() {
        return this.id;
    }

    public Identifier texture() {
        return this.pristine;
    }

    public Identifier texture(int damageStage) {
        return switch (damageStage) {
            case 1 -> this.damaged;
            case 2 -> this.wrecked;
            default -> this.pristine;
        };
    }

    public static DrakkarSailStripe byId(byte id) {
        for (DrakkarSailStripe s : values()) {
            if (s.id == id) {
                return s;
            }
        }
        return RED;
    }

    public static @Nullable DrakkarSailStripe fromDye(@Nullable DyeColor dye) {
        if (dye == null) {
            return null;
        }
        return switch (dye) {
            case RED -> RED;
            case BLUE -> BLUE;
            case GREEN -> GREEN;
            case PURPLE -> PURPLE;
            default -> null;
        };
    }
}
