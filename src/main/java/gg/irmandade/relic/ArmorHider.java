package gg.irmandade.relic;

import com.mojang.datafixers.util.Pair;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Esconde a armadura para os OUTROS jogadores enviando pacotes de equipamento "vazios".
 * Continuam aparecendo: elytra, cabeças de mobs/jogadores e abóbora esculpida.
 */
public final class ArmorHider {
    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private ArmorHider() {}

    private static boolean staysVisible(EquipmentSlot slot, ItemStack s) {
        if (s.isEmpty()) return false;
        if (slot == EquipmentSlot.CHEST) return s.is(Items.ELYTRA);
        if (slot == EquipmentSlot.HEAD) return s.is(ItemTags.SKULLS) || s.is(Items.CARVED_PUMPKIN);
        return false;
    }

    /** hidden=true esconde a armadura; hidden=false revela (envia o equipamento real). */
    public static void send(ServerPlayer target, boolean hidden) {
        List<Pair<EquipmentSlot, ItemStack>> list = new ArrayList<>();
        for (EquipmentSlot slot : ARMOR) {
            ItemStack real = target.getItemBySlot(slot);
            if (hidden && !staysVisible(slot, real)) list.add(Pair.of(slot, ItemStack.EMPTY));
            else list.add(Pair.of(slot, real.copy()));
        }
        ClientboundSetEquipmentPacket packet = new ClientboundSetEquipmentPacket(target.getId(), list);
        ServerLevel level = target.level();
        for (ServerPlayer other : level.players()) {
            if (other != target) other.connection.send(packet);
        }
    }
}
