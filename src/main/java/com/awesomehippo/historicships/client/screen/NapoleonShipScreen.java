package com.awesomehippo.historicships.client.screen;

import com.awesomehippo.historicships.HistoricShips;
import com.awesomehippo.historicships.menu.NapoleonShipMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class NapoleonShipScreen extends AbstractContainerScreen<NapoleonShipMenu> {
    private static final Identifier CARGO_TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final Identifier ENGINE_TEXTURE = Identifier.fromNamespaceAndPath(HistoricShips.MODID, "textures/gui/napoleon_engine.png");
    private static final Identifier LIT_SPRITE = Identifier.withDefaultNamespace("container/furnace/lit_progress");
    private static final Identifier ARROW_SPRITE = Identifier.withDefaultNamespace("container/furnace/burn_progress");
    private static final Identifier WATER_STILL = Identifier.withDefaultNamespace("block/water_still");
    private static final int WATER_COLOR = 0xFF3F76E4;

    private Button cargoBtn;
    private Button engineBtn;

    public NapoleonShipScreen(NapoleonShipMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, NapoleonShipMenu.GUI_WIDTH, menu.guiHeight());
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int tabX = this.leftPos - 70;
        this.cargoBtn = this.addRenderableWidget(Button.builder(Component.translatable("gui.historicships.tab.cargo"), b -> this.setTab(NapoleonShipMenu.TAB_CARGO)).bounds(tabX, this.topPos, 70, 20).build());
        this.engineBtn = this.addRenderableWidget(Button.builder(Component.translatable("gui.historicships.tab.engine"), b -> this.setTab(NapoleonShipMenu.TAB_ENGINE)).bounds(tabX, this.topPos + 20, 70, 20).build());
        this.refreshTabs();
    }

    private void setTab(int tab) {
        if (this.menu.clickMenuButton(this.minecraft.player, tab)) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, tab);
        }
        this.refreshTabs();
    }

    private void refreshTabs() {
        boolean engine = this.menu.getTab() == NapoleonShipMenu.TAB_ENGINE;
        this.cargoBtn.active = engine;
        this.engineBtn.active = !engine;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = this.leftPos;
        int y = this.topPos;
        if (this.menu.getTab() == NapoleonShipMenu.TAB_ENGINE) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, ENGINE_TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, NapoleonShipMenu.ENGINE_HEIGHT, 256, 256);
            this.drawEngine(graphics, x, y);
            return;
        }
        int rows = NapoleonShipMenu.CARGO_ROWS;
        graphics.blit(RenderPipelines.GUI_TEXTURED, CARGO_TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, rows * 18 + 17, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, CARGO_TEXTURE, x, y + rows * 18 + 17, 0.0F, 126.0F, this.imageWidth, 96, 256, 256);
    }

    private void drawEngine(GuiGraphicsExtractor graphics, int x, int y) {
        if (this.menu.isLit()) {
            int h = Mth.clamp(Mth.ceil(14.0F * this.menu.getLitTime() / this.menu.getLitTotal()), 1, 14);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, LIT_SPRITE, 14, 14, 0, 14 - h, x + 56, y + 36 + 14 - h, 14, h);
        }
        int arrow = Mth.ceil(24.0F * this.menu.getPressure() / 100.0F);
        if (arrow > 0) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARROW_SPRITE, 24, 16, 0, 0, x + 79, y + 34, arrow, 16);
        }
        int waterH = Mth.clamp(Mth.ceil(53.0F * this.menu.getWater() / this.menu.getMaxWater()), 0, 53);
        if (waterH > 0) {
            this.drawWater(graphics, x + 115, y + 70 - waterH, waterH);
        }
    }

    private void drawWater(GuiGraphicsExtractor graphics, int x, int y, int height) {
        TextureAtlasSprite sprite = this.minecraft.getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(WATER_STILL);
        graphics.enableScissor(x, y, x + 16, y + height);
        for (int ty = y + height - 16; ty > y - 16; ty -= 16) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, ty, 16, 16, WATER_COLOR);
        }
        graphics.disableScissor();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        Component title = this.menu.getTab() == NapoleonShipMenu.TAB_ENGINE
                ? Component.translatable("container.historicships.engine")
                : this.title;
        graphics.text(this.font, title, this.titleLabelX, this.titleLabelY, -12566464, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, -12566464, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (this.menu.getTab() != NapoleonShipMenu.TAB_ENGINE) {
            return;
        }
        int x = this.leftPos;
        int y = this.topPos;
        if (mouseX >= x + 114 && mouseX < x + 132 && mouseY >= y + 16 && mouseY < y + 71) {
            graphics.setTooltipForNextFrame(this.font, Component.translatable("gui.historicships.engine.water", this.menu.getWater(), this.menu.getMaxWater()), mouseX, mouseY);
        } else if (mouseX >= x + 79 && mouseX < x + 103 && mouseY >= y + 34 && mouseY < y + 50) {
            graphics.setTooltipForNextFrame(this.font, Component.translatable("gui.historicships.engine.pressure", this.menu.getPressure()), mouseX, mouseY);
        } else if (mouseX >= x + 56 && mouseX < x + 70 && mouseY >= y + 36 && mouseY < y + 50 && this.menu.isLit()) {
            graphics.setTooltipForNextFrame(this.font, Component.translatable("gui.historicships.engine.burning"), mouseX, mouseY);
        }
    }
}
