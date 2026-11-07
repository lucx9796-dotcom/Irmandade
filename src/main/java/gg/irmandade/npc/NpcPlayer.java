package gg.irmandade.npc;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;

/**
 * Cópia do jogador que fica parada no lugar dele durante o Dia do Expurgo.
 * Baseado no EntityPlayerMPFake do Carpet (MIT). Usa o mesmo GameProfile (skin/nome/UUID) do jogador real,
 * então carrega o inventário/posição salvos e, se for morta, dropa os itens e o jogador volta sem nada.
 */
public class NpcPlayer extends ServerPlayer {

    public NpcPlayer(MinecraftServer server, ServerLevel level, GameProfile profile, ClientInformation info) {
        super(server, level, profile, info);
    }

    @Override
    public void onEquipItem(EquipmentSlot slot, ItemStack previous, ItemStack stack) {
        if (!isUsingItem()) super.onEquipItem(slot, previous, stack);
    }

    @Override
    public void kill(ServerLevel level) {
        kill(Component.literal("Expurgo"));
    }

    public void kill(Component reason) {
        shakeOff();
        MinecraftServer server = this.level().getServer();
        server.schedule(new TickTask(server.getTickCount(), () -> this.connection.onDisconnect(new DisconnectionDetails(reason))));
    }

    @Override
    public void tick() {
        if (this.level().getServer().getTickCount() % 10 == 0) {
            this.connection.resetPosition();
            this.level().getChunkSource().move(this);
        }
        try {
            super.tick();
            this.doTick();
        } catch (NullPointerException ignored) {
        }
    }

    private void shakeOff() {
        if (getVehicle() instanceof Player) stopRiding();
        for (Entity passenger : getIndirectPassengers()) {
            if (passenger instanceof Player) passenger.stopRiding();
        }
    }

    @Override
    public void die(DamageSource cause) {
        shakeOff();
        super.die(cause); // dropa os itens e anuncia a morte
        setHealth(20);
        this.foodData = new FoodData();
        kill(this.getCombatTracker().getDeathMessage());
    }

    @Override
    public String getIpAddress() {
        return "127.0.0.1";
    }

    @Override
    public boolean allowsListing() {
        return true;
    }

    @Override
    public ServerPlayer teleport(TeleportTransition transition) {
        super.teleport(transition);
        if (wonGame) {
            connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
        }
        if (connection.player.isChangingDimension()) {
            connection.player.hasChangedDimension();
        }
        return connection.player;
    }
}
