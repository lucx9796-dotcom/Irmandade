package gg.irmandade.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

/** Marca d'água no meio do canto direito do F3. */
public final class F3Watermark {
    private static final String LINE_1 = "Irmandade By LucxMC";
    private static final String LINE_2 = "-. .. -. --. ..- . -- / . ... - .- / ... . --. ..- .-. --- /";
    private static final int LIGHT_PURPLE = 0xFFFF55FF;
    private static final int LIGHT_RED = 0xFFFF5555;
    private static final int BG = 0x90505050;

    private F3Watermark() {}

    public static void register() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("irmandade", "f3_watermark"), (graphics, tickCounter) -> render(graphics));
    }

    private static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || !mc.getDebugOverlay().showDebugScreen()) return;
        Font font = mc.font;
        int w = g.guiWidth();
        int h = g.guiHeight();
        int lh = 9;
        int y = h / 2 - lh;
        drawLine(g, font, LINE_1, w, y, LIGHT_PURPLE);
        drawLine(g, font, LINE_2, w, y + lh + 1, LIGHT_RED);
    }

    private static void drawLine(GuiGraphics g, Font font, String text, int screenW, int y, int color) {
        int tw = font.width(text);
        int x = screenW - 2 - tw;
        g.fill(x - 1, y - 1, x + tw + 1, y + 9, BG);
        g.drawString(font, text, x, y, color, false);
    }
}
