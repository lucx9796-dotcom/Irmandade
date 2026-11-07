package gg.irmandade.npc;

import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;

import java.util.Set;

/** Listener do NPC (baseado no NetHandlerPlayServerFake do Carpet, MIT). */
public class NpcPacketListener extends ServerGamePacketListenerImpl {
    public NpcPacketListener(MinecraftServer server, Connection connection, ServerPlayer player, CommonListenerCookie cookie) {
        super(server, connection, player, cookie);
    }

    @Override
    public void disconnect(Component message) {
        // Quando o jogador real entra de novo (login duplicado) o NPC some.
        if (message.getContents() instanceof TranslatableContents text
                && (text.getKey().equals("multiplayer.disconnect.idling")
                || text.getKey().equals("multiplayer.disconnect.duplicate_login"))) {
            ((NpcPlayer) player).kill(message);
        }
    }

    @Override
    public void teleport(PositionMoveRotation pos, Set<Relative> relatives) {
        super.teleport(pos, relatives);
        if (player.level().getPlayerByUUID(player.getUUID()) != null) {
            resetPosition();
            player.level().getChunkSource().move(player);
        }
    }
}
