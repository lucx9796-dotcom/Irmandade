package gg.irmandade.mixin;

import gg.irmandade.relic.Relics;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DeathProtection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Relíquia da Imortalidade: totem infinito com cooldown de 10s.
 * Em cooldown ela NÃO salva (a relíquia não tem DEATH_PROTECTION, então o totem vanilla nunca a consome)
 * e o cooldown dela não afeta os totens normais.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityTotemMixin {

    @Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
    private void irmandade$relicTotem(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayer player)) return;
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;

        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (!Relics.is(stack, Relics.IMORTALIDADE)) continue;
            if (player.getCooldowns().isOnCooldown(stack)) continue; // em cooldown: não salva

            player.getCooldowns().addCooldown(stack, 200); // 10s
            player.setHealth(1.0F);
            DeathProtection.TOTEM_OF_UNDYING.applyEffects(stack, player);
            player.level().broadcastEntityEvent(player, (byte) 35);
            cir.setReturnValue(true);
            return;
        }
    }
}
