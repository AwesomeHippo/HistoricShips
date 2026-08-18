package com.awesomehippo.historicships.client.renderer;

import com.awesomehippo.historicships.entity.DrakkarEntity;
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
    public byte sailStripe;
    public int damageStage;
    public float sinkProgress;
    public float sinkRollDir;

    public void extractFrom(OarShipEntity entity, float partialTicks) {
        this.yRot = entity.getYRot(partialTicks);
        this.rowPhase = entity.getRowPhase(partialTicks);
        this.rowIntensity = entity.getRowIntensity(partialTicks);
        this.hardAmount = entity.getHardAmount(partialTicks);
        this.sailFill = entity.getSailFill();
        this.damageStage = entity.getDamageStage();
        this.sinkProgress = entity.getSinkProgress(partialTicks);
        this.sinkRollDir = entity.getSinkRollDir();
        Minecraft mc = Minecraft.getInstance();
        this.localPassenger = mc.player != null && entity.hasPassenger(mc.player);
        if (entity instanceof DrakkarEntity drakkar) {
            this.sailStripe = drakkar.getSailStripe().id();
        } else {
            this.sailStripe = 0;
        }
    }
}
