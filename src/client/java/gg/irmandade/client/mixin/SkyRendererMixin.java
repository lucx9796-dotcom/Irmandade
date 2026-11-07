package gg.irmandade.client.mixin;

import gg.irmandade.client.ClientExpurgo;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Lua 10x maior durante o Dia do Expurgo.
 * O quad da lua vanilla tem 20 unidades de lado; trocamos por 200.
 * (require=0: se a Mojang mudar o método, o jogo só ignora em vez de crashar.)
 */
@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {
    @ModifyConstant(method = "renderMoon", constant = @Constant(floatValue = 20.0F), require = 0)
    private float irmandade$bigMoon(float original) {
        return ClientExpurgo.active ? original * 10.0F : original;
    }
}
