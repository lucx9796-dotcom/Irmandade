package gg.irmandade.npc;

import gg.irmandade.mixin.ConnectionAccessor;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;

/** Conexão falsa (baseada no FakeClientConnection do Carpet, MIT). */
public class NpcConnection extends Connection {
    public NpcConnection(PacketFlow flow) {
        super(flow);
        ((ConnectionAccessor) this).irmandade$setChannel(new EmbeddedChannel());
    }

    @Override
    public void setReadOnly() {}

    @Override
    public void send(Packet<?> packet, ChannelFutureListener listener, boolean flush) {}

    @Override
    public void handleDisconnection() {}

    @Override
    public void setListenerForServerboundHandshake(PacketListener listener) {}

    @Override
    public <T extends PacketListener> void setupInboundProtocol(ProtocolInfo<T> info, T listener) {}
}
