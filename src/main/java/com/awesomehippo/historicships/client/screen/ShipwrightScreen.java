package com.awesomehippo.historicships.client.screen;

import com.awesomehippo.historicships.NapoleonShipMod;
import com.awesomehippo.historicships.menu.ShipwrightMenu;
import com.awesomehippo.historicships.recipe.ShipAssemblyRecipe;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class ShipwrightScreen extends AbstractContainerScreen<ShipwrightMenu> {
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(NapoleonShipMod.MODID, "textures/gui/shipwright.png");

    private static final int READY = 0xFF2E8B2E;

    private static final int PARTIAL = 0xFFCC7A00;
    private static final int MISSING = 0xFF6B6B6B;

    private static final int TIP_READY = 0xFF55FF55;
    private static final int TIP_PARTIAL = 0xFFFFAA00;
    private static final int TIP_MISSING = 0xFFFF5555;

    private static final float BUTTON_LABEL_SCALE = 0.75F;

    public ShipwrightScreen(ShipwrightMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, ShipwrightMenu.GUI_WIDTH, ShipwrightMenu.GUI_HEIGHT);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        var recipes = ShipAssemblyRecipe.all();
        for (int i = 0; i < recipes.size(); i++) {
            final int recipeIndex = recipes.get(i).index();

            int by = this.topPos + ShipwrightMenu.RECIPE_Y0 + i * ShipwrightMenu.RECIPE_H + 2;
            this.addRenderableWidget(Button.builder(Component.translatable("gui.historicships.assemble"), b -> this.tryAssemble(recipeIndex)).bounds(this.leftPos + ShipwrightMenu.BUTTON_X, by, ShipwrightMenu.BUTTON_W, ShipwrightMenu.BUTTON_H).build(AssembleButton::new));
        }
    }

    private void tryAssemble(int recipeIndex) {
        ShipAssemblyRecipe recipe = ShipAssemblyRecipe.byIndex(recipeIndex);
        if (recipe == null) {
            return;
        }
        if (!recipe.matches(this.menu.getContainer()) || !recipe.canPlaceResult(this.menu.getContainer())) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 1.0F));
            return;
        }
        if (this.menu.clickMenuButton(this.minecraft.player, recipeIndex)) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, recipeIndex);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = (this.width - this.imageWidth) / 2;
        int yo = (this.height - this.imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        for (int i = 0; i < ShipAssemblyRecipe.all().size(); i++) {
            ShipAssemblyRecipe recipe = ShipAssemblyRecipe.all().get(i);
            int ry = yo + ShipwrightMenu.RECIPE_Y0 + i * ShipwrightMenu.RECIPE_H;
            int ix = xo + ShipwrightMenu.PREVIEW_X;

            var container = this.menu.getContainer();

            boolean ready = recipe.matches(container);
            boolean partial = !ready && recipe.isPartial(container);

            ItemStack icon = recipe.resultStack();
            graphics.item(icon, ix, ry);
            graphics.itemDecorations(this.font, icon, ix, ry);

            int nameColor = ready ? READY : (partial ? PARTIAL : MISSING);

            String name = recipe.title.getString();
            float scale = 0.8F;
            int maxW = (int) (68 / scale);
            name = this.font.plainSubstrByWidth(name, maxW);
            int tx = xo + 28;
            int ty = ry + 6;
            graphics.pose().pushMatrix();
            graphics.pose().translate(tx, ty);
            graphics.pose().scale(scale, scale);

            graphics.text(this.font, name, 0, 0, nameColor, false);
            graphics.pose().popMatrix();
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        for (int i = 0; i < ShipAssemblyRecipe.all().size(); i++) {
            int rx = this.leftPos + ShipwrightMenu.PREVIEW_X;
            int ry = this.topPos + ShipwrightMenu.RECIPE_Y0 + i * ShipwrightMenu.RECIPE_H;
            if (mouseX >= rx && mouseX < rx + 16 && mouseY >= ry && mouseY < ry + 16) {
                ShipAssemblyRecipe recipe = ShipAssemblyRecipe.all().get(i);
                java.util.List<Component> lines = new java.util.ArrayList<>();
                lines.add(recipe.title);
                lines.add(Component.translatable("gui.historicships.requires"));
                for (ShipAssemblyRecipe.CountedNeed need : recipe.needs) {
                    int have = ShipAssemblyRecipe.countMatching(this.menu.getContainer(), need);
                    int needCount = need.count();

                    int c;
                    if (have >= needCount) {
                        c = TIP_READY;
                    } else if (have > 0) {
                        c = TIP_PARTIAL;
                    } else {
                        c = TIP_MISSING;
                    }
                    Component counts = Component.literal(have + "/" + needCount + " ").withColor(c);
                    Component label = need.label().copy().withColor(c);
                    lines.add(Component.empty().append(counts).append(label));
                }
                graphics.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
                break;
            }
        }
    }

    private static final class AssembleButton extends Button.Plain {
        AssembleButton(Button.Builder builder) {
            super(builder);
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            this.extractDefaultSprite(graphics);

            Font font = Minecraft.getInstance().font;
            String text = this.getMessage().getString();
            int rgb = this.active ? 0xFFFFFF : 0xA0A0A0;
            int color = ARGB.color(Math.round(this.alpha * 255.0F), rgb);

            float scale = BUTTON_LABEL_SCALE;
            float cx = this.getX() + this.getWidth() * 0.5F;
            float cy = this.getY() + (this.getHeight() - font.lineHeight * scale) * 0.5F;

            graphics.pose().pushMatrix();
            graphics.pose().translate(cx, cy);
            graphics.pose().scale(scale, scale);
            graphics.centeredText(font, text, 0, 0, color);
            graphics.pose().popMatrix();
        }
    }
}
