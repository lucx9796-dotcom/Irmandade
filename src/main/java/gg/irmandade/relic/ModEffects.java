package gg.irmandade.relic;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Efeito customizado "Espião" (textura: assets/irmandade/textures/mob_effect/spy.png). */
public final class ModEffects {
    public static Holder<MobEffect> SPY;

    private ModEffects() {}

    public static void register() {
        SPY = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,
                Identifier.fromNamespaceAndPath("irmandade", "spy"), new SpyEffect());
    }

    static class SpyEffect extends MobEffect {
        SpyEffect() {
            super(MobEffectCategory.NEUTRAL, 0xE09451);
        }
    }
}
