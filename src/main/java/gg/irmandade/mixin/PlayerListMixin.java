package gg.irmandade.mixin;

import gg.irmandade.npc.NpcPacketListener;
import gg.irmandade.npc.NpcPlayer;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Shadow @Final private MinecraftServer server;

    // Igual ao Carpet: troca o listener de rede quando o jogador é um NPC.
    @Redirect(method = "placeNewPlayer", at = @At(value = "NEW",
            target = "(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/network/CommonListenerCookie;)Lnet/minecraft/server/network/ServerGamePacketListenerImpl;"))
    private ServerGamePacketListenerImpl irmandade$replaceHandler(MinecraftServer s, Connection c, ServerPlayer p, CommonListenerCookie cookie) {
        if (p instanceof NpcPlayer npc) return new NpcPacketListener(this.server, c, npc, cookie);
        return new ServerGamePacketListenerImpl(this.server, c, p, cookie);
    }
}
