package gg.irmandade.npc;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import gg.irmandade.expurgo.ExpurgoManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Gerencia as cópias (NPCs) deixadas por jogadores que saem durante o Dia do Expurgo. */
public final class NpcManager {
    private record Pending(GameProfile profile, ClientInformation info) {}

    private static final List<Pending> PENDING = new ArrayList<>();
    private static final Map<UUID, NpcPlayer> NPCS = new HashMap<>();

    private NpcManager() {}

    public static void register() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer p = handler.getPlayer();
            if (p instanceof NpcPlayer) return;
            if (!ExpurgoManager.isActive()) return;
            PENDING.add(new Pending(p.getGameProfile(), p.clientInformation()));
        });
    }

    /** Chamado todo tick pelo Expurgo. */
    public static void tick(MinecraftServer server) {
        // cria as cópias (um tick depois da saída, para o save do jogador já ter acontecido)
        if (!PENDING.isEmpty()) {
            List<Pending> copy = new ArrayList<>(PENDING);
            PENDING.clear();
            for (Pending pd : copy) spawn(server, pd);
        }
        NPCS.values().removeIf(n -> n.isRemoved());
    }

    private static void spawn(MinecraftServer server, Pending pd) {
        UUID id = pd.profile().id();
        if (server.getPlayerList().getPlayer(id) != null) return; // já voltou
        NpcPlayer npc = new NpcPlayer(server, server.overworld(), pd.profile(), pd.info());
        server.getPlayerList().placeNewPlayer(
                new NpcConnection(PacketFlow.SERVERBOUND), npc,
                new CommonListenerCookie(pd.profile(), 0, pd.info(), false));
        NPCS.put(id, npc);
    }

    /** Remove todas as cópias (fim do Expurgo): elas "saem" do servidor sem perder nada. */
    public static void removeAll(MinecraftServer server) {
        PENDING.clear();
        for (NpcPlayer npc : new ArrayList<>(NPCS.values())) {
            npc.kill(Component.literal("Fim do Dia do Expurgo"));
        }
        NPCS.clear();
    }
}
