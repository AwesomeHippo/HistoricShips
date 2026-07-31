package com.awesomehippo.historicships.client.renderer;

import com.awesomehippo.historicships.entity.OarShipEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class OarShipRenderState extends EntityRenderState {
    public float yRot;
    public float rowPhase;
    public float rowIntensity;
    public float hardAmount;
    public float sailFill;
    public boolean localPassenger;

    public void extractFrom(OarShipEntity entity, float partialTicks) {
        this.yRot = entity.getYRot(partialTicks);
        this.rowPhase = entity.getRowPhase(partialTicks);
        this.rowIntensity = entity.getRowIntensity(partialTicks);
        this.hardAmount = entity.getHardAmount(partialTicks);
        this.sailFill = entity.getSailFill();
        Minecraft mc = Minecraft.getInstance();
        this.localPassenger = mc.player != null && entity.hasPassenger(mc.player);
    }
}
