package gg.irmandade.client;

/** Estado do Dia do Expurgo no cliente (recebido do servidor). */
public final class ClientExpurgo {
    public static volatile boolean active = false;

    private ClientExpurgo() {}
}
