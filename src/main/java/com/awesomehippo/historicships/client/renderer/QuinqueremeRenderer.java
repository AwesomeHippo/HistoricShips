package com.awesomehippo.historicships.client.renderer;

import com.awesomehippo.historicships.client.model.QuinqueremeModel;
import com.awesomehippo.historicships.client.model.QuinqueremePaintModel;
import com.awesomehippo.historicships.entity.QuinqueremeEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

public class QuinqueremeRenderer extends EntityRenderer<QuinqueremeEntity, OarShipRenderState> {
    public static final float SCALE = QuinqueremeEntity.MODEL_SCALE;
    public static final float MODEL_LOA = 100.0F;
    private final QuinqueremeModel model;
    private final QuinqueremePaintModel paintModel;

    public QuinqueremeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = MODEL_LOA * SCALE / 16.0F * 0.28F;
        this.model = new QuinqueremeModel(context.bakeLayer(QuinqueremeModel.LAYER_LOCATION));
        this.paintModel = new QuinqueremePaintModel(context.bakeLayer(QuinqueremePaintModel.LAYER_LOCATION));
    }

    @Override
    protected AABB getBoundingBoxForCulling(QuinqueremeEntity ship) {
        return ship.makeCullBox();
    }

    @Override
    public void submit(OarShipRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        ShipRenderPose.apply(poseStack, state.ageInTicks, state.yRot, SCALE, state.localPassenger, ShipRenderPose.QUINQUEREME, state.sinkProgress, state.sinkRollDir);
        Identifier texture = ShipDamageTextures.stage("quinquereme", state.damageStage);
        submitNodeCollector.submitModel(this.model, state, poseStack, texture, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        if (state.sailPaint != null) {
            submitNodeCollector.submitCustomGeometry(poseStack, SailPaintRenderType.of(state.sailPaint), (pose, buffer) -> {
                this.paintModel.setupAnim(state);
                SailPaintMesh.emit(pose, buffer, this.paintModel, state.lightCoords);
            });
        }
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    @Override
    public OarShipRenderState createRenderState() {
        return new OarShipRenderState();
    }

    @Override
    public void extractRenderState(QuinqueremeEntity entity, OarShipRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.extractFrom(entity, partialTicks);
    }
}
