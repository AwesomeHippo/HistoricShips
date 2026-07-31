package com.awesomehippo.historicships.client;

import com.awesomehippo.historicships.NapoleonShipMod;
import com.awesomehippo.historicships.client.model.DrakkarModel;
import com.awesomehippo.historicships.client.model.NapoleonShipModel;
import com.awesomehippo.historicships.client.model.QuinqueremeModel;
import com.awesomehippo.historicships.client.renderer.DrakkarRenderer;
import com.awesomehippo.historicships.client.renderer.NapoleonShipRenderer;
import com.awesomehippo.historicships.client.renderer.QuinqueremeRenderer;
import com.awesomehippo.historicships.client.screen.ShipwrightScreen;
import com.awesomehippo.historicships.entity.DrakkarEntity;
import com.awesomehippo.historicships.entity.NapoleonShipEntity;
import com.awesomehippo.historicships.entity.QuinqueremeEntity;
import com.awesomehippo.historicships.network.FireBowShellPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = NapoleonShipMod.MODID, dist = Dist.CLIENT)
public class NapoleonShipClient {

    private static final float CONDUCTOR_FOV = 28.0F;

    private static final float OAR_CONDUCTOR_FOV = 32.0F;

    private static final float PASSENGER_FOV = 14.0F;
    private static final float OAR_PASSENGER_FOV = 16.0F;

    private static final float FOV_SMOOTH = 0.18F;

    private static final float SPEED_SMOOTH = 0.10F;

    private static final int SPEED_LABEL_INTERVAL_TICKS = 15;

    private static final float SPEED_DISPLAY_QUANT = 1.0F;

    private float rideFovBonus;

    private float smoothedBps;

    private float displayedBps = -1.0F;
    private String displayedSpeed = "0 b/s";
    private int speedVehicleId = -1;

    private int lastSpeedLabelTick = -999;

    public NapoleonShipClient(IEventBus modBus) {
        modBus.addListener(this::registerLayers);
        modBus.addListener(this::registerRenderers);
        modBus.addListener(this::registerScreens);
        modBus.addListener(NapoleonShipKeys::register);

        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onRenderGui);
        NeoForge.EVENT_BUS.addListener(this::onComputeFov);
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(NapoleonShipMod.SHIPWRIGHT_MENU.get(), ShipwrightScreen::new);
    }

    private void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(NapoleonShipModel.LAYER_LOCATION, NapoleonShipModel::createBodyLayer);
        event.registerLayerDefinition(DrakkarModel.LAYER_LOCATION, DrakkarModel::createBodyLayer);
        event.registerLayerDefinition(QuinqueremeModel.LAYER_LOCATION, QuinqueremeModel::createBodyLayer);
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(NapoleonShipMod.NAPOLEON_SHIP_ENTITY.get(), NapoleonShipRenderer::new);
        event.registerEntityRenderer(NapoleonShipMod.DRAKKAR_ENTITY.get(), DrakkarRenderer::new);
        event.registerEntityRenderer(NapoleonShipMod.QUINQUEREME_ENTITY.get(), QuinqueremeRenderer::new);

        event.registerEntityRenderer(NapoleonShipMod.CANNONBALL_ENTITY.get(), ctx -> new ThrownItemRenderer<>(ctx, 4.6F, true));
    }

    private void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) {
            return;
        }
        LocalPlayer player = mc.player;
        if (!(player.getVehicle() instanceof NapoleonShipEntity ship)) {
            return;
        }

        if (!ship.isConductor(player)) {
            return;
        }

        if (NapoleonShipKeys.FIRE_ALL.consumeClick()) {
            if (ship.tryFireAll()) {
                ClientPacketDistributor.sendToServer(new FireBowShellPacket(ship.getId()));
            }
        }
        if (NapoleonShipKeys.TOGGLE_SAILS.consumeClick()) {
            ship.toggleSails();
        }
    }

    private void onComputeFov(ViewportEvent.ComputeFov event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        float target = 0.0F;
        if (player != null && mc.options.getCameraType().isFirstPerson() && player.getVehicle() != null) {
            Entity vehicle = player.getVehicle();
            if (vehicle instanceof NapoleonShipEntity ship) {
                target = ship.isConductor(player) ? CONDUCTOR_FOV : PASSENGER_FOV;
            } else if (vehicle instanceof DrakkarEntity ship) {
                target = ship.isConductor(player) ? OAR_CONDUCTOR_FOV : OAR_PASSENGER_FOV;
            } else if (vehicle instanceof QuinqueremeEntity ship) {
                target = ship.isConductor(player) ? OAR_CONDUCTOR_FOV : OAR_PASSENGER_FOV;
            }
        }

        this.rideFovBonus += (target - this.rideFovBonus) * FOV_SMOOTH;
        if (this.rideFovBonus < 0.05F) {
            this.rideFovBonus = 0.0F;
            return;
        }
        event.setFOV(event.getFOV() + this.rideFovBonus);
    }

    private void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui) {
            return;
        }
        Entity vehicle = player.getVehicle();
        if (vehicle == null) {
            return;
        }

        GuiGraphicsExtractor g = event.getGuiGraphics();
        Font font = mc.font;
        int sw = mc.getWindow().getGuiScaledWidth();

        if (vehicle instanceof NapoleonShipEntity ship) {
            if (ship.isConductor(player)) {
                drawConductorPanel(g, font, sw, ship);
            } else {
                drawPassengerPanel(g, font, sw, "Passenger", ship.getPassengers().size() + "/" + NapoleonShipEntity.MAX_PASSENGERS, formatSpeed(ship));
            }
            return;
        }
        if (vehicle instanceof DrakkarEntity ship) {
            if (ship.isConductor(player)) {
                drawOarShipPanel(g, font, sw, "Drakkar", ship.isHardRowing(), ship.getPassengers().size(), DrakkarEntity.MAX_PASSENGERS, formatSpeed(ship));
            } else {
                drawPassengerPanel(g, font, sw, "Passenger", ship.getPassengers().size() + "/" + DrakkarEntity.MAX_PASSENGERS, formatSpeed(ship));
            }
            return;
        }
        if (vehicle instanceof QuinqueremeEntity ship) {
            if (ship.isConductor(player)) {
                drawOarShipPanel(g, font, sw, "Quinquereme", ship.isHardRowing(), ship.getPassengers().size(), QuinqueremeEntity.MAX_PASSENGERS, formatSpeed(ship));
            } else {
                drawPassengerPanel(g, font, sw, "Passenger", ship.getPassengers().size() + "/" + QuinqueremeEntity.MAX_PASSENGERS, formatSpeed(ship));
            }
        }
    }

    // smooth speed label updates so the hud doesn't flicker
    private String formatSpeed(Entity vehicle) {
        float instant = (float) (vehicle.getDeltaMovement().horizontalDistance() * 20.0D);
        int tick = vehicle.tickCount;
        if (vehicle.getId() != this.speedVehicleId) {
            this.speedVehicleId = vehicle.getId();
            this.smoothedBps = instant;
            this.displayedBps = quantizeSpeed(instant);
            this.displayedSpeed = formatSpeedLabel(this.displayedBps);
            this.lastSpeedLabelTick = tick;
            return this.displayedSpeed;
        }

        this.smoothedBps += (instant - this.smoothedBps) * SPEED_SMOOTH;
        if (this.smoothedBps < 0.20F && instant < 0.20F) {
            this.smoothedBps = 0.0F;
        }

        float quantized = quantizeSpeed(this.smoothedBps);
        boolean stopped = quantized <= 0.0F && this.displayedBps > 0.0F;
        boolean started = quantized > 0.0F && this.displayedBps <= 0.0F && this.smoothedBps > 0.5F;
        boolean intervalElapsed = tick - this.lastSpeedLabelTick >= SPEED_LABEL_INTERVAL_TICKS;

        boolean bigJump = Math.abs(quantized - this.displayedBps) >= 3.0F;

        if (stopped || started || (intervalElapsed && quantized != this.displayedBps) || bigJump) {
            this.displayedBps = quantized;
            this.displayedSpeed = formatSpeedLabel(quantized);
            this.lastSpeedLabelTick = tick;
        }
        return this.displayedSpeed;
    }

    private static float quantizeSpeed(float bps) {
        if (bps < 0.35F) {
            return 0.0F;
        }
        return Math.round(bps / SPEED_DISPLAY_QUANT) * SPEED_DISPLAY_QUANT;
    }

    private static String formatSpeedLabel(float bps) {
        return String.format(java.util.Locale.ROOT, "%.0f b/s", bps);
    }

    private void drawOarShipPanel(GuiGraphicsExtractor g, Font font, int sw, String title, boolean hardRowing, int crew, int maxCrew, String speed) {
        drawPanel(g, font, sw, title, new String[][] { {"Move", moveKeys()}, {"Full speed", sprintKey()}, {"Status", hardRowing ? "Full speed" : "Normal Speed"}, {"Speed", speed}, {"Crew", crew + "/" + maxCrew} });
    }

    private void drawConductorPanel(GuiGraphicsExtractor g, Font font, int sw, NapoleonShipEntity ship) {
        String kFire = NapoleonShipKeys.FIRE_ALL.getTranslatedKeyMessage().getString();
        String kSail = NapoleonShipKeys.TOGGLE_SAILS.getTranslatedKeyMessage().getString();

        String sailLabel = ship.areSailsFurled() ? "Set Sails" : "Furl Sails";
        String sailValue = kSail;

        boolean sailsOpen = !ship.areSailsFurled();
        String status;
        if (ship.getBroadsideCooldown() > 0) {
            status = "Reload " + (ship.getBroadsideCooldown() / 20 + 1) + "s";
        } else if (ship.isBoosting()) {
            status = sailsOpen ? "Full speed - Sails" : "Full speed";
        } else if (sailsOpen) {
            status = "Normal - Sails";
        } else {
            status = "Normal Speed";
        }

        drawPanel(g, font, sw, "Napoleon", new String[][] { {"Move", moveKeys()}, {"Full speed", sprintKey()}, {"Fire all", kFire}, {sailLabel, sailValue}, {"Status", status}, {"Speed", formatSpeed(ship)}, {"Crew", ship.getPassengers().size() + "/" + NapoleonShipEntity.MAX_PASSENGERS} });
    }

    private static String moveKeys() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) {
            return "?";
        }
        String f = keyName(mc.options.keyUp);
        String l = keyName(mc.options.keyLeft);
        String b = keyName(mc.options.keyDown);
        String r = keyName(mc.options.keyRight);

        if (f.length() == 1 && l.length() == 1 && b.length() == 1 && r.length() == 1) {
            return f + l + b + r;
        }
        return f + " / " + l + " / " + b + " / " + r;
    }

    private static String sprintKey() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) {
            return "?";
        }
        return keyName(mc.options.keySprint);
    }

    private static String keyName(net.minecraft.client.KeyMapping mapping) {
        return mapping.getTranslatedKeyMessage().getString();
    }

    private void drawPassengerPanel(GuiGraphicsExtractor g, Font font, int sw, String title, String crewLine, String speed) {
        drawPanel(g, font, sw, title, new String[][] { {"Speed", speed}, {"Crew", crewLine} });
    }

    private void drawPanel(GuiGraphicsExtractor g, Font font, int sw, String title, String[][] rows) {
        final int padX = 6;
        final int padY = 5;
        final int lineH = font.lineHeight + 1;

        final int boxW = 128;
        final int boxH = padY * 2 + lineH * (1 + rows.length) + 2;
        final int boxX = sw - boxW - 5;
        final int boxY = 5;

        g.fill(boxX, boxY, boxX + boxW, boxY + boxH, ARGB.color(140, 0, 0, 0));

        int y = boxY + padY;
        g.text(font, title, boxX + padX, y, 0xFFFFFFFF, true);
        y += lineH + 2;

        int valueRight = boxX + boxW - padX;
        for (String[] row : rows) {
            g.text(font, row[0], boxX + padX, y, 0xFFA0A0A0, true);
            int vw = font.width(row[1]);
            g.text(font, row[1], valueRight - vw, y, 0xFFE0E0E0, true);
            y += lineH;
        }
    }
}
