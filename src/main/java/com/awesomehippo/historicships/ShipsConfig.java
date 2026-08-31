package com.awesomehippo.historicships;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ShipsConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue DRAKKAR;
    public static final ModConfigSpec.BooleanValue QUINQUEREME;
    public static final ModConfigSpec.BooleanValue NAPOLEON_SHIP;
    public static final ModConfigSpec.BooleanValue KEEP_CARGO;
    private static final ModConfigSpec SPEC;

    static {
        BUILDER.comment("Which ships can be assembled at the shipwright workbench and placed in the world").push("ships");
        DRAKKAR = BUILDER.define("drakkar", true);
        QUINQUEREME = BUILDER.define("quinquereme", true);
        NAPOLEON_SHIP = BUILDER.define("napoleon_ship", true);
        BUILDER.pop();
        KEEP_CARGO = BUILDER.comment("Keep cargo in the ship item when you pick it up, or drop the content").define("keep_cargo", true);
        SPEC = BUILDER.build();
    }

    private ShipsConfig() {}

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, SPEC);
    }
}
