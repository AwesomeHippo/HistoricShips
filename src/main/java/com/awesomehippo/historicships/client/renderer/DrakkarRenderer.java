package com.awesomehippo.historicships.client.renderer;

import com.awesomehippo.historicships.client.model.DrakkarModel;
import com.awesomehippo.historicships.entity.DrakkarEntity;
import com.awesomehippo.historicships.entity.DrakkarSailStripe;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

public class DrakkarRenderer extends EntityRenderer<DrakkarEntity, OarShipRenderState> {
    public static final float SCALE = DrakkarEntity.MODEL_SCALE;
    private final DrakkarModel model;

    public DrakkarRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 3.24F;
        this.model = new DrakkarModel(context.bakeLayer(DrakkarModel.LAYER_LOCATION));
    }

    @Override
    protected AABB getBoundingBoxForCulling(DrakkarEntity ship) {
        return ship.makeCullBox();
    }

    @Override
    public void submit(OarShipRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        ShipRenderPose.apply(poseStack, state.ageInTicks, state.yRot, SCALE, state.localPassenger, ShipRenderPose.STANDARD, state.sinkProgress, state.sinkRollDir);
        Identifier texture = DrakkarSailStripe.byId(state.sailStripe).texture(state.damageStage);
        submitNodeCollector.submitModel(this.model, state, poseStack, texture, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    @Override
    public OarShipRenderState createRenderState() {
        return new OarShipRenderState();
    }

    @Override
    public void extractRenderState(DrakkarEntity entity, OarShipRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.extractFrom(entity, partialTicks);
    }
}
