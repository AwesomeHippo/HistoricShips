package com.awesomehippo.historicships.client.screen;

import com.awesomehippo.historicships.NapoleonShipMod;
import com.awesomehippo.historicships.menu.NapoleonEngineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class NapoleonEngineScreen extends AbstractContainerScreen<NapoleonEngineMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(NapoleonShipMod.MODID, "textures/gui/napoleon_engine.png");
    private static final Identifier LIT_SPRITE = Identifier.withDefaultNamespace("container/furnace/lit_progress");
    private static final Identifier ARROW_SPRITE = Identifier.withDefaultNamespace("container/furnace/burn_progress");

    public NapoleonEngineScreen(NapoleonEngineMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, NapoleonEngineMenu.GUI_WIDTH, NapoleonEngineMenu.GUI_HEIGHT);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = this.leftPos;
        int y = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

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
            graphics.fill(x + 115, y + 70 - waterH, x + 131, y + 70, 0xFF3F76E4);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
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
