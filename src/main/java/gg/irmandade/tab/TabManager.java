package gg.irmandade.tab;

import gg.irmandade.expurgo.ExpurgoManager;
import gg.irmandade.npc.NpcPlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

/** Tab igual ao print: linha ciano, Ping/TPS, "Feito por: LucxMC_", irmandade.gg e o Dia do Expurgo. */
public final class TabManager {
    private TabManager() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(TabManager::tick);
    }

    private static MutableComponent line() {
        return Component.literal(" ".repeat(40)).withStyle(ChatFormatting.AQUA, ChatFormatting.STRIKETHROUGH);
    }

    private static MutableComponent gray(String s) {
        return Component.literal(s).withStyle(ChatFormatting.GRAY);
    }

    private static int tint(double value, double okBelow, double warnBelow) {
        if (value < okBelow) return 0x55FF55;
        if (value < warnBelow) return 0xFFFF55;
        return 0xFF5555;
    }

    private static MutableComponent colored(String s, int rgb) {
        return Component.literal(s).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)));
    }

    private static Component expurgoLine() {
        long ms = ExpurgoManager.remainingMillis();
        long s = ms / 1000;
        String time = String.format(Locale.ROOT, "%02d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60);
        return Component.empty()
                .append(Component.literal("Dia do Expurgo").withStyle(Style.EMPTY.withBold(true).withColor(TextColor.fromRgb(0xFF0000))))
                .append(Component.literal("\n"))
                .append(Component.literal(time).withStyle(ChatFormatting.GRAY));
    }

    private static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) return;

        double mspt = server.getAverageTickTimeNanos() / 1_000_000.0;
        double tps = Math.min(20.0, 1000.0 / Math.max(mspt, 50.0));
        boolean expurgo = ExpurgoManager.isActive();
        boolean expurgoOnTop = (server.getTickCount() / 100) % 2 == 0; // alterna a cada 5s: em cima / embaixo

        Component header = Component.empty().append(line());

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p instanceof NpcPlayer) continue;
            int ping = p.connection.latency();

            MutableComponent footer = Component.empty().append(line()).append("\n");
            if (expurgo && expurgoOnTop) footer.append(expurgoLine()).append("\n");
            footer.append(gray("Ping: "))
                    .append(colored(ping + "ms", tint(ping, 80, 200)))
                    .append(gray(" – TPS: "))
                    .append(colored(String.format(Locale.ROOT, "%.1f", tps), tps >= 19.0 ? 0x55FF55 : tps >= 15.0 ? 0xFFFF55 : 0xFF5555))
                    .append("\n")
                    .append(gray("Feito por: "))
                    .append(Component.literal("LucxMC_").withStyle(ChatFormatting.AQUA))
                    .append("\n\n")
                    .append(gray("irmandade.gg"));
            if (expurgo && !expurgoOnTop) footer.append("\n\n").append(expurgoLine());

            p.connection.send(new ClientboundTabListPacket(header, footer));
        }
    }
}
