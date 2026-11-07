package gg.irmandade.client;

import gg.irmandade.net.ExpurgoPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class IrmandadeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        F3Watermark.register();

        ClientPlayNetworking.registerGlobalReceiver(ExpurgoPayload.TYPE, (payload, context) -> {
            ClientExpurgo.active = payload.active();
            BloodMoon.apply(payload.active());
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (ClientExpurgo.active) {
                ClientExpurgo.active = false;
                BloodMoon.apply(false);
            }
        });
    }
}
