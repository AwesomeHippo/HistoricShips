package com.awesomehippo.historicships.client.screen;

import com.awesomehippo.historicships.entity.QuinqueremeEntity;
import com.awesomehippo.historicships.entity.SailPaint;
import com.awesomehippo.historicships.network.SailPaintPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.Arrays;

public class SailPaintScreen extends Screen {
    private static final Component TITLE = Component.translatable("gui.historicships.sail_paint.title");
    // premade design! (1 color(+the bg) supported yet)
    private static final String[] ROMAN_EAGLE = {
        "...................##...................",
        "...................##...................",
        "########################################",
        "########################################",
        "##..##..##..##..########..##..##..##..##",
        "##..##..##..##..########..##..##..##..##",
        "....##..##..##..########..##..##..##....",
        "........##..##..########..##..##........",
        "............##..########..##............",
        "................########................",
        "..................####..................",
        ".................######.................",
        "................##.##.##................",
        "................##.##.##................"
    };

    private final QuinqueremeEntity ship;
    private final byte[] pixels = new byte[SailPaint.PIXELS];

    private int cell;
    private int canvasX;
    private int canvasY;
    private int paletteX;
    private int paletteY;
    private int swatch;

    private int selected = 1;
    private int dragButton = -1;
    private int lastCellX = -1;
    private int lastCellY = -1;

    public static void open(QuinqueremeEntity ship) {
        Minecraft.getInstance().setScreen(new SailPaintScreen(ship));
    }

    private SailPaintScreen(QuinqueremeEntity ship) {
        super(TITLE);
        this.ship = ship;
        byte[] current = ship.getSailPaint();
        if (SailPaint.isValid(current)) {
            System.arraycopy(current, 0, this.pixels, 0, SailPaint.PIXELS);
        }
    }

    @Override
    protected void init() {
        this.cell = Math.max(2, Math.min(8, (this.height - 130) / SailPaint.HEIGHT));
        int canvasW = SailPaint.WIDTH * this.cell;
        int canvasH = SailPaint.HEIGHT * this.cell;
        this.canvasX = (this.width - canvasW) / 2;
        this.canvasY = Math.max(18, (this.height - canvasH - 84) / 2 + 8);

        this.swatch = 12;
        int step = this.swatch + 2;
        int colorsW = 8 * step;
        this.paletteX = this.canvasX + (canvasW - colorsW) / 2;
        this.paletteY = this.canvasY + canvasH + 6;

        int stampBw = 80;
        int bw = 52;
        int gap = 4;
        int stampY = this.paletteY + 2 * (this.swatch + 2) + 6;
        int stampTotal = 2 * stampBw + gap;
        int sx = (this.width - stampTotal) / 2;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.historicships.sail_paint.roman_eagle"), b -> this.stampRomanEagle()).bounds(sx, stampY, stampBw, 16).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.historicships.sail_paint.spqr"), b -> this.stampSpqr()).bounds(sx + (stampBw + gap), stampY, stampBw, 16).build());

        int by = stampY + 20;
        int total = 4 * bw + 3 * gap;
        int bx = (this.width - total) / 2;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.historicships.sail_paint.fill"), b -> this.fill()).bounds(bx, by, bw, 16).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.historicships.sail_paint.clear"), b -> this.clear()).bounds(bx + (bw + gap), by, bw, 16).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.historicships.sail_paint.done"), b -> this.save()).bounds(bx + 2 * (bw + gap), by, bw, 16).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.historicships.sail_paint.cancel"), b -> this.onClose()).bounds(bx + 3 * (bw + gap), by, bw, 16).build());
    }

    private void fill() {
        Arrays.fill(this.pixels, (byte) this.selected);
    }

    private void clear() {
        Arrays.fill(this.pixels, (byte) 0);
    }

    private void stampRomanEagle() {
        Arrays.fill(this.pixels, (byte) dye(DyeColor.RED));
        this.stampPattern(ROMAN_EAGLE, (SailPaint.WIDTH - ROMAN_EAGLE[0].length()) / 2, (SailPaint.HEIGHT - ROMAN_EAGLE.length) / 2, dye(DyeColor.WHITE));
    }

    private void stampSpqr() {
        Arrays.fill(this.pixels, (byte) 0);
        int scale = 2;
        this.stampWord("SPQR", (SailPaint.WIDTH - wordWidth("SPQR", scale)) / 2, (SailPaint.HEIGHT - 7 * scale) / 2, dye(DyeColor.RED), scale);
    }

    private static int dye(DyeColor color) {
        return color.ordinal() + 1;
    }

    private static int wordWidth(String word, int scale) {
        return word.length() * 6 * scale + Math.max(0, word.length() - 1) * scale;
    }

    // epic helper for premade words (1 color)
    private void stampWord(String word, int ox, int oy, int color, int scale) {
        int lx = 0;
        for (int i = 0; i < word.length(); i++) {
            String[] g = glyph(word.charAt(i));
            for (int y = 0; y < g.length; y++) {
                String row = g[y];
                for (int x = 0; x < row.length(); x++) {
                    if (row.charAt(x) != '#') {
                        continue;
                    }
                    for (int dy = 0; dy < scale; dy++) {
                        for (int dx = 0; dx < scale; dx++) {
                            this.putPixel(ox + lx + x * scale + dx, oy + y * scale + dy, color);
                        }
                    }
                }
            }
            lx += 6 * scale + scale;
        }
    }

    private void stampPattern(String[] rows, int ox, int oy, int color) {
        for (int y = 0; y < rows.length; y++) {
            String row = rows[y];
            for (int x = 0; x < row.length(); x++) {
                if (row.charAt(x) == '#') {
                    this.putPixel(ox + x, oy + y, color);
                }
            }
        }
    }

    private void putPixel(int x, int y, int color) {
        if (x < 0 || x >= SailPaint.WIDTH || y < 0 || y >= SailPaint.HEIGHT) {
            return;
        }
        this.pixels[y * SailPaint.WIDTH + x] = (byte) color;
    }

    private static String[] glyph(char c) {
        return switch (c) {
            case 'P' -> new String[] {
                "#####.", "##..##", "##..##", "#####.", "##....", "##....", "##...."
            };
            case 'Q' -> new String[] {
                ".####.", "##..##", "##..##", "##..##", "##.##.", ".####.", "....##"
            };
            case 'R' -> new String[] {
                "#####.", "##..##", "##..##", "#####.", "##.#..", "##..##", "##..##"
            };
            case 'S' -> new String[] {
                ".####.", "##....", "##....", ".####.", "....##", "....##", ".####."
            };
            default -> new String[] {
                "......", "......", "......", "......", "......", "......", "......"
            };
        };
    }

    private void save() {
        if (!this.ship.isRemoved()) {
            byte[] result = SailPaint.isBlank(this.pixels) ? new byte[0] : this.pixels;
            ClientPacketDistributor.sendToServer(new SailPaintPacket(this.ship.getId(), result));
        }
        this.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.paintAt(event.x(), event.y(), event.button())) {
            this.dragButton = event.button();
            return true;
        }
        int sw = this.swatchAt(event.x(), event.y());
        if (sw >= 0) {
            this.selected = sw;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.dragButton >= 0) {
            this.paintAt(event.x(), event.y(), this.dragButton);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.dragButton = -1;
        this.lastCellX = -1;
        this.lastCellY = -1;
        return super.mouseReleased(event);
    }

    private boolean paintAt(double mx, double my, int button) {
        int cx = (int) Math.floor((mx - this.canvasX) / this.cell);
        int cy = (int) Math.floor((my - this.canvasY) / this.cell);
        if (cx < 0 || cx >= SailPaint.WIDTH || cy < 0 || cy >= SailPaint.HEIGHT) {
            this.lastCellX = -1;
            this.lastCellY = -1;
            return false;
        }
        int color = button == 0 ? this.selected : 0;
        if (this.lastCellX >= 0 && (this.lastCellX != cx || this.lastCellY != cy)) {
            paintLine(this.lastCellX, this.lastCellY, cx, cy, color);
        } else {
            this.pixels[cy * SailPaint.WIDTH + cx] = (byte) color;
        }
        this.lastCellX = cx;
        this.lastCellY = cy;
        return true;
    }

    private void paintLine(int x0, int y0, int x1, int y1, int color) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int i = 0; i <= steps; i++) {
            int x = x0 + (x1 - x0) * i / Math.max(1, steps);
            int y = y0 + (y1 - y0) * i / Math.max(1, steps);
            this.pixels[y * SailPaint.WIDTH + x] = (byte) color;
        }
    }

    private int swatchAt(double mx, double my) {
        for (int i = 0; i < SailPaint.COLORS; i++) {
            int sx = this.paletteX + (i % 8) * (this.swatch + 2);
            int sy = this.paletteY + (i / 8) * (this.swatch + 2);
            if (mx >= sx && mx < sx + this.swatch && my >= sy && my < sy + this.swatch) {
                return i + 1;
            }
        }
        return -1;
    }

    private void drawSwatchBorder(GuiGraphicsExtractor graphics, int sx, int sy, boolean selected) {
        int border = selected ? 0xFFFFFFFF : 0xFF202020;
        graphics.fill(sx - 1, sy - 1, sx + this.swatch + 1, sy, border);
        graphics.fill(sx - 1, sy + this.swatch, sx + this.swatch + 1, sy + this.swatch + 1, border);
        graphics.fill(sx - 1, sy, sx, sy + this.swatch, border);
        graphics.fill(sx + this.swatch, sy, sx + this.swatch + 1, sy + this.swatch, border);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        int canvasW = SailPaint.WIDTH * this.cell;
        int canvasH = SailPaint.HEIGHT * this.cell;

        graphics.fill(this.canvasX - 2, this.canvasY - 2, this.canvasX + canvasW + 2, this.canvasY + canvasH + 2, 0xFF000000);
        graphics.fill(this.canvasX - 1, this.canvasY - 1, this.canvasX + canvasW + 1, this.canvasY + canvasH + 1, 0xFF3F3F3F);

        for (int y = 0; y < SailPaint.HEIGHT; y++) {
            for (int x = 0; x < SailPaint.WIDTH; x++) {
                int color = this.pixels[y * SailPaint.WIDTH + x];
                int argb;
                if (color == 0) {
                    argb = ((x + y) & 1) == 0 ? 0xFF6E6E6E : 0xFF585858;
                } else {
                    argb = SailPaint.argb(color);
                }
                int px = this.canvasX + x * this.cell;
                int py = this.canvasY + y * this.cell;
                graphics.fill(px, py, px + this.cell, py + this.cell, argb);
            }
        }

        if (this.cell >= 4) {
            int hoverX = (int) Math.floor((mouseX - this.canvasX) / (double) this.cell);
            int hoverY = (int) Math.floor((mouseY - this.canvasY) / (double) this.cell);
            if (hoverX >= 0 && hoverX < SailPaint.WIDTH && hoverY >= 0 && hoverY < SailPaint.HEIGHT) {
                int hx = this.canvasX + hoverX * this.cell;
                int hy = this.canvasY + hoverY * this.cell;
                graphics.fill(hx, hy, hx + this.cell, hy + 1, 0xFFFFFFFF);
                graphics.fill(hx, hy + this.cell - 1, hx + this.cell, hy + this.cell, 0xFFFFFFFF);
                graphics.fill(hx, hy, hx + 1, hy + this.cell, 0xFFFFFFFF);
                graphics.fill(hx + this.cell - 1, hy, hx + this.cell, hy + this.cell, 0xFFFFFFFF);
            }
        }

        for (int i = 0; i < SailPaint.COLORS; i++) {
            int sx = this.paletteX + (i % 8) * (this.swatch + 2);
            int sy = this.paletteY + (i / 8) * (this.swatch + 2);
            graphics.fill(sx, sy, sx + this.swatch, sy + this.swatch, SailPaint.argb(i + 1));
            this.drawSwatchBorder(graphics, sx, sy, this.selected == i + 1);
        }

        graphics.centeredText(this.font, TITLE, this.width / 2, this.canvasY - 12, 0xFFFFFFFF);

        super.extractRenderState(graphics, mouseX, mouseY, a);
    }
}
