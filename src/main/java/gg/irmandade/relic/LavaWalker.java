package gg.irmandade.relic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Relíquia do Explorador: igual ao Frost Walker do "Better Frost Walker" (funciona mesmo sem tocar o chão
 * e com alcance maior), mas sobre LAVA: cria um caminho de magma que volta a ser lava depois de alguns segundos.
 */
public final class LavaWalker {
    private static final int RADIUS = 3;
    private static final List<Entry> ACTIVE = new ArrayList<>();

    private record Entry(ServerLevel level, BlockPos pos, long expiresAt) {}

    private LavaWalker() {}

    public static void tickPlayer(ServerPlayer p, long now) {
        if (p.isShiftKeyDown() || p.isSpectator()) return;
        ServerLevel level = p.level();
        BlockPos base = p.blockPosition().below();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                if (dx * dx + dz * dz > RADIUS * RADIUS + 1) continue;
                BlockPos pos = base.offset(dx, 0, dz);
                FluidState fluid = level.getFluidState(pos);
                if (!fluid.is(FluidTags.LAVA) || !fluid.isSource()) continue;
                if (!level.getBlockState(pos).is(Blocks.LAVA)) continue;
                if (!level.getBlockState(pos.above()).isAir()) continue;
                level.setBlockAndUpdate(pos, Blocks.MAGMA_BLOCK.defaultBlockState());
                ACTIVE.add(new Entry(level, pos.immutable(), now + 60 + level.getRandom().nextInt(60)));
            }
        }
    }

    public static void tickRevert(long now) {
        Iterator<Entry> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            if (now >= e.expiresAt) {
                revert(e);
                it.remove();
            }
        }
    }

    public static void revertAll(MinecraftServer server) {
        for (Entry e : ACTIVE) revert(e);
        ACTIVE.clear();
    }

    private static void revert(Entry e) {
        if (e.level.getBlockState(e.pos).is(Blocks.MAGMA_BLOCK)) {
            e.level.setBlockAndUpdate(e.pos, Blocks.LAVA.defaultBlockState());
        }
    }
}
