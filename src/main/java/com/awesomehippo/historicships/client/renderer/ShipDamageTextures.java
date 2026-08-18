package com.awesomehippo.historicships.client.renderer;

import net.minecraft.resources.Identifier;

public final class ShipDamageTextures {
    private ShipDamageTextures() {}

    public static Identifier stage(String basePath, int damageStage) {
        String path = switch (damageStage) {
            case 1 -> basePath + "_damaged";
            case 2 -> basePath + "_wrecked";
            default -> basePath;
        };
        return Identifier.fromNamespaceAndPath("historicships", "textures/entity/" + path + ".png");
    }
}
