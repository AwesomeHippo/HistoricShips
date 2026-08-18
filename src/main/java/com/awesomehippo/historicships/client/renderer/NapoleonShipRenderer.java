package com.awesomehippo.historicships.client.renderer;

import com.awesomehippo.historicships.client.model.NapoleonShipModel;
import com.awesomehippo.historicships.entity.NapoleonShipEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public class NapoleonShipRenderer extends EntityRenderer<NapoleonShipEntity, NapoleonShipRenderState> {
    public static final float SCALE = NapoleonShipEntity.MODEL_SCALE;
    private final NapoleonShipModel model;

    public NapoleonShipRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 7.81F;
        this.model = new NapoleonShipModel(context.bakeLayer(NapoleonShipModel.LAYER_LOCATION));
    }

    @Override
    public void submit(NapoleonShipRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        ShipRenderPose.apply(poseStack, state.ageInTicks, state.yRot, SCALE, state.localPassenger, ShipRenderPose.STANDARD, state.sinkProgress, state.sinkRollDir);
        Identifier texture = ShipDamageTextures.stage("napoleon_ship", state.damageStage);
        submitNodeCollector.submitModel(this.model, state, poseStack, texture, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    @Override
    public NapoleonShipRenderState createRenderState() {
        return new NapoleonShipRenderState();
    }

    @Override
    public void extractRenderState(NapoleonShipEntity entity, NapoleonShipRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot(partialTicks);
        state.helmAngle = entity.getHelmAngle(partialTicks);
        state.speed = (float) entity.getDeltaMovement().horizontalDistance();
        state.sailFill = entity.getSailFill();
        state.sailDeploy = entity.getSailDeploy();
        state.sailsFurled = entity.areSailsFurled();
        state.damageStage = entity.getDamageStage();
        state.sinkProgress = entity.getSinkProgress(partialTicks);
        state.sinkRollDir = entity.getSinkRollDir();
        Minecraft mc = Minecraft.getInstance();
        state.localPassenger = mc.player != null && entity.hasPassenger(mc.player);
        state.helmCockpit = mc.options.getCameraType().isFirstPerson() && state.localPassenger && entity.isConductor(mc.player);
    }
}
