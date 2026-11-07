package gg.irmandade.mixin;

import gg.irmandade.expurgo.ExpurgoManager;
import gg.irmandade.relic.Relics;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void irmandade$hurt(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ItemEntity self = (ItemEntity) (Object) this;
        ItemStack stack = self.getItem();

        // Indestrutível: qualquer item com unbreakable={} não sofre nenhum dano (explosão, cacto, fogo, lava...)
        if (Relics.isIndestructible(stack)) {
            cir.setReturnValue(false);
            return;
        }

        // Queimar uma nether star ativa / estende o Dia do Expurgo
        if (stack.is(Items.NETHER_STAR) && source.is(DamageTypeTags.IS_FIRE)) {
            int count = stack.getCount();
            self.discard();
            ExpurgoManager.onNetherStarBurned(level, self.position(), count);
            cir.setReturnValue(false);
        }
    }
}
