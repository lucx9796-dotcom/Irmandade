package gg.irmandade.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Servidor -> cliente: o Dia do Expurgo está ativo? (usado para a lua de sangue). */
public record ExpurgoPayload(boolean active) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ExpurgoPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("irmandade", "expurgo"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ExpurgoPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, ExpurgoPayload::active, ExpurgoPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
