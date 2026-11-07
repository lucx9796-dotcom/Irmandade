package gg.irmandade.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.InputStream;

/** Troca a textura da lua por uma versão vermelho-sangue enquanto o Expurgo estiver ativo. */
public final class BloodMoon {
    private static final Identifier MOON = Identifier.withDefaultNamespace("textures/environment/moon_phases.png");

    private BloodMoon() {}

    public static void apply(boolean blood) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            try {
                if (blood) {
                    try (InputStream in = mc.getResourceManager().getResourceOrThrow(MOON).open()) {
                        NativeImage img = NativeImage.read(in);
                        for (int y = 0; y < img.getHeight(); y++) {
                            for (int x = 0; x < img.getWidth(); x++) {
                                int argb = img.getPixel(x, y);
                                int a = (argb >>> 24) & 0xFF;
                                int r = (argb >>> 16) & 0xFF, g = (argb >>> 8) & 0xFF, b = argb & 0xFF;
                                int lum = (r + g + b) / 3;
                                int red = Math.min(255, 70 + lum * 185 / 255);
                                int dark = lum / 8;
                                img.setPixel(x, y, (a << 24) | (red << 16) | (dark << 8) | dark);
                            }
                        }
                        mc.getTextureManager().register(MOON, new DynamicTexture(() -> "irmandade_blood_moon", img));
                    }
                } else {
                    // remove a textura trocada; o jogo recarrega a original do resource pack quando precisar
                    mc.getTextureManager().release(MOON);
                }
            } catch (Exception e) {
                gg.irmandade.Irmandade.LOGGER.warn("Não foi possível trocar a textura da lua", e);
            }
        });
    }
}
