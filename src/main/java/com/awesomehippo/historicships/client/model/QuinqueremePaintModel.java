package com.awesomehippo.historicships.client.model;

import com.awesomehippo.historicships.client.renderer.OarShipRenderState;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

public class QuinqueremePaintModel extends EntityModel<OarShipRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(Identifier.fromNamespaceAndPath("historicships", "quinquereme_paint"), "main");

    private final ModelPart sailRoot;
    private final ModelPart[][] sailCells;

    public QuinqueremePaintModel(ModelPart root) {
        super(root);
        ModelPart mast = root.getChild("body").getChild("mast");
        this.sailRoot = mast.getChild("sail_fixed");
        this.sailCells = SquareSail.resolveCells(this.sailRoot);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition body = mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition mast = body.addOrReplaceChild("mast", CubeListBuilder.create(), PartPose.offset(0.0F, QuinqueremeModel.MAST_BASE_Y, 0.0F));
        PartDefinition sail = mast.addOrReplaceChild("sail_fixed", CubeListBuilder.create(), PartPose.offset(0.0F, QuinqueremeModel.YARD_Y, 0.0F));
        PartDefinition cloth = sail.addOrReplaceChild("cloth", CubeListBuilder.create(), PartPose.offset(QuinqueremeModel.SAIL_X_OFF, 0.0F, 0.0F));
        for (int col = 0; col < SquareSail.COLS; col++) {
            for (int row = 0; row < SquareSail.ROWS; row++) {
                cloth.addOrReplaceChild(SquareSail.cellName(col, row), CubeListBuilder.create(), PartPose.ZERO);
            }
        }
        return LayerDefinition.create(mesh, 16, 16);
    }

    public ModelPart sailRoot() {
        return this.sailRoot;
    }

    public ModelPart[][] sailCells() {
        return this.sailCells;
    }

    @Override
    public void setupAnim(OarShipRenderState state) {
        super.setupAnim(state);
        SquareSail.animate(this.sailRoot, this.sailCells, state.ageInTicks, state.sailFill, QuinqueremeModel.SAIL_MAX_BELLY, 0.35F);
    }
}
