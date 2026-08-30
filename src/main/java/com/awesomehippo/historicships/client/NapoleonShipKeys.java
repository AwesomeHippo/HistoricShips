package com.awesomehippo.historicships.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

public final class NapoleonShipKeys {
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath("historicships", "ship"));
    public static final KeyMapping FIRE_FRONT = new KeyMapping("key.historicships.fire_front", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, 86, CATEGORY);
    public static final KeyMapping FIRE_LEFT = new KeyMapping("key.historicships.fire_left", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, 78, CATEGORY);
    public static final KeyMapping FIRE_RIGHT = new KeyMapping("key.historicships.fire_right", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, 66, CATEGORY);
    public static final KeyMapping TOGGLE_SAILS = new KeyMapping("key.historicships.toggle_sails", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, 70, CATEGORY);
    public static final KeyMapping OPEN_ENGINE = new KeyMapping("key.historicships.open_engine", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, 72, CATEGORY);

    public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(FIRE_FRONT);
        event.register(FIRE_LEFT);
        event.register(FIRE_RIGHT);
        event.register(TOGGLE_SAILS);
        event.register(OPEN_ENGINE);
    }
}
