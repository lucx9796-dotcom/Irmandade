package gg.irmandade.relic;

import gg.irmandade.expurgo.ExpurgoManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.windcharge.WindCharge;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Todas as propriedades únicas das relíquias. */
public final class RelicAbilities {
    private static final Identifier SPY_SCALE_ID = Identifier.fromNamespaceAndPath("irmandade", "spy_scale");

    // Invasor: próximo tick (do servidor) em que o Bad Omen pode ser aplicado
    private static final Map<UUID, Long> NEXT_OMEN = new HashMap<>();
    // Invisibilidade: máquina de estados
    private static final Map<UUID, InvisState> INVIS = new HashMap<>();

    private static final class InvisState {
        int phase = 0; // 0 off, 1 escondendo armadura, 2 ativo, 3 revelando armadura
        int timer = 0;
    }

    private RelicAbilities() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(RelicAbilities::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(LavaWalker::revertAll);

        // Indestrutível: sem despawn
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof ItemEntity item && Relics.isIndestructible(item.getItem())) {
                item.setUnlimitedLifetime();
            }
        });

        // Stomper (sem dano de queda na mão principal) / Explorador (sem dano do magma)
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer p) {
                if (source.is(DamageTypeTags.IS_FALL) && Relics.is(p.getMainHandItem(), Relics.STOMPER)) return false;
                if (source.is(DamageTypes.HOT_FLOOR) && Relics.is(p.getItemBySlot(EquipmentSlot.FEET), Relics.EXPLORADOR)) return false;
            }
            return true;
        });

        // Defesa: espinhos 360° (mesmo bloqueando, de qualquer direção)
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> {
            if (!(entity instanceof ServerPlayer p)) return;
            if (!(p.level() instanceof ServerLevel level)) return;
            if (source.is(DamageTypes.THORNS)) return;
            boolean has = Relics.is(p.getMainHandItem(), Relics.DEFESA) || Relics.is(p.getOffhandItem(), Relics.DEFESA);
            if (!has) return;
            Entity attacker = source.getEntity();
            if (!(attacker instanceof LivingEntity victim) || attacker == p) return;
            if (p.getRandom().nextFloat() < 0.75F) { // Thorns V = 75%
                victim.hurtServer(level, level.damageSources().thorns(p), 1 + p.getRandom().nextInt(4));
            }
        });

        // Botão direito no ar
        UseItemCallback.EVENT.register((player, level, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.isEmpty()) return InteractionResult.PASS;
            String id = Relics.idOf(stack);
            if (id.isEmpty()) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer sp)) {
                // cliente: só impede o uso vanilla (comer biscoito, arremessar charge etc.)
                return switch (id) {
                    case Relics.ESPIAO, Relics.VENTO, Relics.WARDEN, Relics.DRAGAO -> InteractionResult.SUCCESS;
                    default -> InteractionResult.PASS;
                };
            }
            return switch (id) {
                case Relics.ESPIAO -> useSpy(sp, stack);
                case Relics.VENTO -> useWind(sp, stack);
                case Relics.WARDEN -> useWarden(sp, stack);
                case Relics.DRAGAO -> { dragonTeleport(sp); yield InteractionResult.SUCCESS; }
                default -> InteractionResult.PASS;
            };
        });

        // Botão direito em bloco: Ovo do Dragão não pode ser colocado / baú do fim no Expurgo
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (level.getBlockState(hit.getBlockPos()).is(Blocks.ENDER_CHEST) && ExpurgoManager.isActive()) {
                if (player instanceof ServerPlayer sp) ExpurgoManager.sendEnderChestBlocked(sp);
                return InteractionResult.FAIL;
            }
            ItemStack stack = player.getItemInHand(hand);
            if (Relics.is(stack, Relics.DRAGAO)) {
                if (player instanceof ServerPlayer sp) dragonTeleport(sp);
                return InteractionResult.SUCCESS; // cancela a colocação: o ovo "some do chão e fica na mão"
            }
            return InteractionResult.PASS;
        });
    }

    // ------------------------------------------------------------------ tick

    private static void tick(MinecraftServer server) {
        long now = server.getTickCount();
        LavaWalker.tickRevert(now);

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            tickPlayer(server, p, now);
        }

        // Itens indestrutíveis que caíram no void ficam flutuando em vez de sumir
        if (now % 10 == 0) {
            for (ServerLevel level : server.getAllLevels()) {
                for (Entity e : level.getAllEntities()) {
                    if (e instanceof ItemEntity item && Relics.isIndestructible(item.getItem())
                            && item.getY() < level.getMinY() - 16) {
                        item.setNoGravity(true);
                        item.setDeltaMovement(Vec3.ZERO);
                        item.setPos(item.getX(), level.getMinY() - 16, item.getZ());
                    }
                }
            }
        }
    }

    private static void tickPlayer(MinecraftServer server, ServerPlayer p, long now) {
        if (p.isSpectator()) return;

        ItemStack legs = p.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack feet = p.getItemBySlot(EquipmentSlot.FEET);
        ItemStack head = p.getItemBySlot(EquipmentSlot.HEAD);

        // Fazendeiro: Herói da Vila V
        if (now % 20 == 0 && Relics.is(legs, Relics.FAZENDEIRO)) {
            p.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 60, 4, true, false, true));
        }
        // Nether: Resistência ao Fogo
        if (now % 20 == 0 && Relics.is(head, Relics.NETHER)) {
            p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, true, false, true));
        }
        // Sorte: Sorte X enquanto estiver no inventário
        if (now % 20 == 0 && p.getInventory().contains(s -> Relics.is(s, Relics.SORTE))) {
            p.addEffect(new MobEffectInstance(MobEffects.LUCK, 60, 9, true, false, true));
        }

        // Invasor: Bad Omen 5s ligado / 10s desligado (não ativa com Trial Omen / Raid Omen)
        if (Relics.is(legs, Relics.INVASOR)) {
            long next = NEXT_OMEN.getOrDefault(p.getUUID(), 0L);
            if (now >= next) {
                boolean blocked = p.hasEffect(MobEffects.RAID_OMEN) || p.hasEffect(MobEffects.TRIAL_OMEN);
                if (!blocked) {
                    p.addEffect(new MobEffectInstance(MobEffects.BAD_OMEN, 100, 0, false, true, true));
                    NEXT_OMEN.put(p.getUUID(), now + 300); // 5s ativo + 10s inativo
                } else {
                    NEXT_OMEN.put(p.getUUID(), now + 20);
                }
            }
        } else {
            NEXT_OMEN.remove(p.getUUID());
        }

        // Explorador: caminho de magma
        if (now % 2 == 0 && Relics.is(feet, Relics.EXPLORADOR)) {
            LavaWalker.tickPlayer(p, now);
        }

        tickInvisibility(p, feet);
        tickSpyScale(p);
    }

    /** Efeito Espião: scale 0.2 enquanto o efeito estiver ativo; volta ao normal quando acabar. */
    private static void tickSpyScale(ServerPlayer p) {
        AttributeInstance scale = p.getAttribute(Attributes.SCALE);
        if (scale == null) return;
        boolean spy = ModEffects.SPY != null && p.hasEffect(ModEffects.SPY);
        boolean has = scale.hasModifier(SPY_SCALE_ID);
        if (spy && !has) {
            scale.addTransientModifier(new AttributeModifier(SPY_SCALE_ID, -0.8, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        } else if (!spy && has) {
            scale.removeModifier(SPY_SCALE_ID);
        }
    }

    /** Invisibilidade XI: armadura some (10 ticks) -> jogador some; ao tirar: armadura aparece -> efeito acaba. */
    private static void tickInvisibility(ServerPlayer p, ItemStack feet) {
        InvisState st = INVIS.computeIfAbsent(p.getUUID(), k -> new InvisState());
        boolean equipped = Relics.is(feet, Relics.INVISIBILIDADE);
        switch (st.phase) {
            case 0 -> {
                if (equipped) {
                    ArmorHider.send(p, true);
                    st.phase = 1;
                    st.timer = 10;
                }
            }
            case 1 -> {
                if (!equipped) {
                    ArmorHider.send(p, false);
                    st.phase = 0;
                } else if (--st.timer <= 0) {
                    p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 10, true, false, true));
                    st.phase = 2;
                }
            }
            case 2 -> {
                if (equipped) {
                    if (p.tickCount % 20 == 0) {
                        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 10, true, false, true));
                    }
                    if (p.tickCount % 5 == 0) ArmorHider.send(p, true); // mantém escondida mesmo se o vanilla reenviar
                } else {
                    ArmorHider.send(p, false); // armadura aparece
                    st.phase = 3;
                    st.timer = 10;
                }
            }
            case 3 -> {
                if (equipped) {
                    ArmorHider.send(p, true);
                    st.phase = 1;
                    st.timer = 10;
                } else if (--st.timer <= 0) {
                    MobEffectInstance cur = p.getEffect(MobEffects.INVISIBILITY);
                    if (cur != null && cur.getAmplifier() == 10) p.removeEffect(MobEffects.INVISIBILITY);
                    st.phase = 0;
                }
            }
            default -> st.phase = 0;
        }
    }

    // ------------------------------------------------------------------ usos

    private static InteractionResult useSpy(ServerPlayer p, ItemStack stack) {
        if (p.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
        p.addEffect(new MobEffectInstance(ModEffects.SPY, 8 * 60 * 20, 1, false, true, true)); // Espião II, 8 min
        p.getCooldowns().addCooldown(stack, 20 * 60 * 20); // cooldown 20 min
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult useWind(ServerPlayer p, ItemStack stack) {
        if (p.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
        ServerLevel level = p.level();
        for (int i = 0; i < 2; i++) {
            WindCharge charge = new WindCharge(p, level, p.getX(), p.getEyeY(), p.getZ());
            float yawOffset = i == 0 ? -4.0F : 4.0F;
            charge.shootFromRotation(p, p.getXRot(), p.getYRot() + yawOffset, 0.0F, 1.5F, 1.0F);
            level.addFreshEntity(charge);
        }
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.WIND_CHARGE_THROW, SoundSource.PLAYERS,
                0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        p.getCooldowns().addCooldown(stack, 5); // infinito, cooldown reduzido (vanilla = 10 ticks)
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult useWarden(ServerPlayer p, ItemStack stack) {
        if (p.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
        ServerLevel level = p.level();
        Vec3 eye = p.getEyePosition();
        Vec3 dir = p.getLookAngle().normalize();
        double range = 15.0;
        HitResult hit = level.clip(new ClipContext(eye, eye.add(dir.scale(range)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (hit.getType() != HitResult.Type.MISS) range = Math.max(1.0, hit.getLocation().distanceTo(eye));

        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 3.0F, 1.0F);
        for (int i = 1; i < Mth.floor(range) + 1; i++) {
            Vec3 v = eye.add(dir.scale(i));
            level.sendParticles(ParticleTypes.SONIC_BOOM, v.x, v.y, v.z, 1, 0.0, 0.0, 0.0, 0.0);
        }

        AABB box = new AABB(eye, eye.add(dir.scale(range))).inflate(1.5);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != p && e.isAlive())) {
            Vec3 c = target.getBoundingBox().getCenter();
            double t = Mth.clamp(c.subtract(eye).dot(dir), 0.0, range);
            Vec3 closest = eye.add(dir.scale(t));
            if (closest.distanceTo(c) > 1.2 + target.getBbWidth() / 2.0) continue;
            if (target.hurtServer(level, level.damageSources().sonicBoom(p), 10.0F)) {
                double res = 1.0 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
                target.push(dir.x * 2.5 * res, dir.y * 0.5 * res, dir.z * 2.5 * res);
            }
        }
        p.getCooldowns().addCooldown(stack, 200); // 10s
        return InteractionResult.SUCCESS;
    }

    /** Teleporte aleatório igual à fruta do côro (chorus fruit), com partículas e sem cooldown. */
    private static void dragonTeleport(ServerPlayer p) {
        ServerLevel level = p.level();
        double x = p.getX(), y = p.getY(), z = p.getZ();
        for (int i = 0; i < 16; i++) {
            double nx = p.getX() + (p.getRandom().nextDouble() - 0.5) * 16.0;
            double ny = Mth.clamp(p.getY() + (p.getRandom().nextInt(16) - 8),
                    level.getMinY(), level.getMinY() + level.getLogicalHeight() - 1);
            double nz = p.getZ() + (p.getRandom().nextDouble() - 0.5) * 16.0;
            if (p.isPassenger()) p.stopRiding();
            Vec3 old = p.position();
            if (p.randomTeleport(nx, ny, nz, true)) {
                level.gameEvent(GameEvent.TELEPORT, old, GameEvent.Context.of(p));
                level.playSound(null, x, y, z, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
                p.playSound(SoundEvents.CHORUS_FRUIT_TELEPORT, 1.0F, 1.0F);
                level.sendParticles(ParticleTypes.PORTAL, x, y + 1.0, z, 40, 0.4, 0.8, 0.4, 0.4);
                level.sendParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1.0, p.getZ(), 40, 0.4, 0.8, 0.4, 0.4);
                p.resetFallDistance();
                break;
            }
        }
    }
}
