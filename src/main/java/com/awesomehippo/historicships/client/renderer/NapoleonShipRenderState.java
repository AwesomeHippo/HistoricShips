package com.awesomehippo.historicships.client.renderer;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class NapoleonShipRenderState extends EntityRenderState {
    public float yRot;
    public float helmAngle;
    public float speed;
    public float sailFill = -1.0F;
    public float sailDeploy = 1.0F;
    public boolean sailsFurled;
    public boolean helmCockpit;
    public boolean localPassenger;
}
