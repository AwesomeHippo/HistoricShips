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
    private final ModelPart artSailRoot;
    private final ModelPart[][] artSailCells;

    public QuinqueremePaintModel(ModelPart root) {
        super(root);
        ModelPart mast = root.getChild("body").getChild("mast");
        this.sailRoot = mast.getChild("sail_fixed");
        this.sailCells = SquareSail.resolveCells(this.sailRoot);
        ModelPart artemon = root.getChild("body").getChild("artemon");
        this.artSailRoot = artemon.getChild("sail_artemon");
        this.artSailCells = SquareSail.resolveCells(this.artSailRoot);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition body = mesh.getRoot().addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition mast = body.addOrReplaceChild("mast", CubeListBuilder.create(), PartPose.offset(0.0F, QuinqueremeModel.MAST_BASE_Y, 0.0F));
        PartDefinition sail = mast.addOrReplaceChild("sail_fixed", CubeListBuilder.create(), PartPose.offset(0.0F, QuinqueremeModel.YARD_Y, 0.0F));
        PartDefinition cloth = sail.addOrReplaceChild("cloth", CubeListBuilder.create(), PartPose.offset(QuinqueremeModel.SAIL_X_OFF, 0.0F, 0.0F));
        addCells(cloth);
        PartDefinition artemon = body.addOrReplaceChild("artemon", CubeListBuilder.create(), PartPose.offsetAndRotation(QuinqueremeModel.ART_BASE_X, QuinqueremeModel.ART_BASE_Y, 0.0F, 0.0F, 0.0F, QuinqueremeModel.ART_Z_ROT));
        PartDefinition artSail = artemon.addOrReplaceChild("sail_artemon", CubeListBuilder.create(), PartPose.offset(0.0F, QuinqueremeModel.ART_SAIL_Y, 0.0F));
        PartDefinition artCloth = artSail.addOrReplaceChild("cloth", CubeListBuilder.create(), PartPose.offset(QuinqueremeModel.ART_SAIL_X_OFF, 0.0F, 0.0F));
        addCells(artCloth);
        return LayerDefinition.create(mesh, 16, 16);
    }

    private static void addCells(PartDefinition cloth) {
        for (int col = 0; col < SquareSail.COLS; col++) {
            for (int row = 0; row < SquareSail.ROWS; row++) {
                cloth.addOrReplaceChild(SquareSail.cellName(col, row), CubeListBuilder.create(), PartPose.ZERO);
            }
        }
    }

    public ModelPart sailRoot() {
        return this.sailRoot;
    }

    public ModelPart[][] sailCells() {
        return this.sailCells;
    }

    public ModelPart artSailRoot() {
        return this.artSailRoot;
    }

    public ModelPart[][] artSailCells() {
        return this.artSailCells;
    }

    @Override
    public void setupAnim(OarShipRenderState state) {
        super.setupAnim(state);
        SquareSail.animate(this.sailRoot, this.sailCells, state.ageInTicks, state.sailFill, QuinqueremeModel.SAIL_MAX_BELLY, 0.35F);
        SquareSail.animate(this.artSailRoot, this.artSailCells, state.ageInTicks, state.sailFill, QuinqueremeModel.ART_SAIL_MAX_BELLY, 1.17F, 1.45F, 1.35F, 0.28F);
    }
}
