package gg.irmandade.expurgo;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import gg.irmandade.net.ExpurgoPayload;
import gg.irmandade.npc.NpcManager;
import gg.irmandade.npc.NpcPlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Dia do Expurgo: queimar uma nether star ativa 24h (tempo REAL) de "sem regras".
 * Queimar outra durante o evento soma +24h.
 */
public final class ExpurgoManager {
    public static final long DAY_MS = 24L * 60 * 60 * 1000;

    private static final Gson GSON = new Gson();
    private static MinecraftServer SERVER;
    private static long endMillis = 0L;
    private static boolean lastSent = false;

    private ExpurgoManager() {}

    // ------------------------------------------------------------ comandos/mensagens exatos do pedido
    private static final String TITLE_JSON =
            "[\"\",{\"text\":\"☠\",\"color\":\"#b30000\"},\" \",{\"text\":\"Dia do Expurgo\",\"bold\":true,\"color\":\"#b30000\"},\" \",{\"text\":\"☠\",\"color\":\"#b30000\"}]";
    private static final String SUBTITLE_JSON =
            "[{\"text\":\"24 Horas Sem Regras ativado!\",\"color\":\"#757575\"}]";
    private static final String ENDER_CHEST_JSON =
            "[\"\",{\"text\":\"IMDD\",\"color\":\"light_purple\"},\" \",{\"text\":\"»\",\"color\":\"#a096b2\"},\" \",{\"text\":\"Você não pode abrir o baú do fim durante o\",\"color\":\"#dc5663\"},\" \",{\"text\":\"Dia do Expurgo\",\"bold\":true,\"color\":\"#9f0206\"}]";

    // ------------------------------------------------------------ estado
    public static boolean isActive() {
        return endMillis > System.currentTimeMillis();
    }

    public static long remainingMillis() {
        return Math.max(0L, endMillis - System.currentTimeMillis());
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            SERVER = server;
            load(server);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> save(server));
        ServerTickEvents.END_SERVER_TICK.register(ExpurgoManager::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendState(handler.getPlayer(), isActive()));
        NpcManager.register();
    }

    // ------------------------------------------------------------ gatilho
    public static void onNetherStarBurned(ServerLevel level, Vec3 pos, int stars) {
        MinecraftServer server = level.getServer();
        boolean wasActive = isActive();
        long base = wasActive ? endMillis : System.currentTimeMillis();
        endMillis = base + DAY_MS * Math.max(1, stars);
        save(server);

        // raio no fogo que queimou a nether star
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt != null) {
            bolt.snapTo(pos.x, pos.y, pos.z);
            level.addFreshEntity(bolt);
        }

        if (!wasActive) {
            // vira noite de lua cheia (time set 18000 = dia 0 = lua cheia, meia-noite)
            run(server, "time set 18000");
        }
        run(server, "title @a title " + TITLE_JSON);
        run(server, "title @a subtitle " + SUBTITLE_JSON);
        sendStateToAll(true);
    }

    public static void sendEnderChestBlocked(ServerPlayer p) {
        run(p.level().getServer(), "tellraw " + p.getGameProfile().name() + " " + ENDER_CHEST_JSON);
    }

    // ------------------------------------------------------------ tick
    private static void tick(MinecraftServer server) {
        int t = server.getTickCount();
        NpcManager.tick(server);

        boolean active = isActive();
        if (t % 20 == 0) {
            if (!active && lastSent) {
                endExpurgo(server);
            }
        }
        if (active && t % 100 == 0) {
            run(server, "time set 18000"); // mantém a noite de lua cheia durante todo o evento
        }
    }

    private static void endExpurgo(MinecraftServer server) {
        endMillis = 0L;
        save(server);
        NpcManager.removeAll(server);
        run(server, "time set day");
        sendStateToAll(false);
    }

    // ------------------------------------------------------------ rede
    private static void sendStateToAll(boolean active) {
        lastSent = active;
        if (SERVER == null) return;
        for (ServerPlayer p : SERVER.getPlayerList().getPlayers()) sendState(p, active);
    }

    private static void sendState(ServerPlayer p, boolean active) {
        if (p instanceof NpcPlayer) return;
        if (active) lastSent = true;
        if (ServerPlayNetworking.canSend(p, ExpurgoPayload.TYPE)) {
            ServerPlayNetworking.send(p, new ExpurgoPayload(active));
        }
    }

    // ------------------------------------------------------------ util
    private static void run(MinecraftServer server, String command) {
        CommandSourceStack src = server.createCommandSourceStack().withSuppressedOutput();
        server.getCommands().performPrefixedCommand(src, command);
    }

    private static Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("irmandade_expurgo.json");
    }

    private static void load(MinecraftServer server) {
        try {
            Path f = file(server);
            if (Files.exists(f)) {
                JsonObject o = GSON.fromJson(Files.readString(f), JsonObject.class);
                endMillis = o.has("end") ? o.get("end").getAsLong() : 0L;
            }
        } catch (Exception ignored) {
            endMillis = 0L;
        }
        lastSent = isActive();
    }

    private static void save(MinecraftServer server) {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("end", endMillis);
            Files.writeString(file(server), GSON.toJson(o));
        } catch (Exception ignored) {
        }
    }
}
