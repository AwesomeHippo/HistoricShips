package com.awesomehippo.historicships.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec2;

public final class ClientInput {
    private ClientInput() {}

    public static boolean sprintHeld() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) {
            return false;
        }
        if (mc.options.keySprint.isDown()) {
            return true;
        }
        if (mc.player instanceof LocalPlayer local) {
            if (local.isSprinting()) {
                return true;
            }
            if (local.input != null && local.input.keyPresses.sprint()) {
                return true;
            }
        }
        return false;
    }

    public static float[] moveAxes() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || !(mc.player instanceof LocalPlayer local) || local.input == null) {
            return new float[] {0.0F, 0.0F};
        }
        Vec2 move = local.input.getMoveVector();

        return new float[] {move.x, move.y};
    }
}
