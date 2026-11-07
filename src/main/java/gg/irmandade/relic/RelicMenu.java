package gg.irmandade.relic;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** GUI de 27 slots do /Relics. Clicar em uma relíquia dá uma cópia; o item permanece na GUI. */
public class RelicMenu extends ChestMenu {

    private RelicMenu(int id, Inventory inv, Container container) {
        super(MenuType.GENERIC_9x3, id, inv, container, 3);
    }

    public static Component title() {
        return Component.empty()
                .append(Component.literal("IMDD").withStyle(Style.EMPTY.withBold(true).withItalic(false).withColor(TextColor.fromRgb(0x8844eb))))
                .append(Component.literal("»").withStyle(Style.EMPTY.withItalic(false).withColor(TextColor.fromRgb(0xAAAAAA))))
                .append(Component.literal(" Relíquia").withStyle(Style.EMPTY.withItalic(false).withColor(TextColor.fromRgb(0x2e2e2e))));
    }

    public static void open(ServerPlayer player) {
        SimpleContainer container = new SimpleContainer(27);
        List<ItemStack> relics = Relics.all(player.registryAccess());
        for (int i = 0; i < relics.size() && i < 27; i++) container.setItem(i, relics.get(i));
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new RelicMenu(id, inv, container), title()));
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // Só reage a clique botão esquerdo/direito (PICKUP) em slot da GUI.
        if (slotId >= 0 && slotId < 27 && clickType == ClickType.PICKUP && (button == 0 || button == 1)) {
            ItemStack shown = this.getContainer().getItem(slotId);
            if (!shown.isEmpty()) {
                ItemStack copy = shown.copy();
                if (!player.getInventory().add(copy)) {
                    player.drop(copy, false);
                }
            }
        }
        // Nenhum clique mexe nos itens da GUI.
        this.sendAllDataToRemote();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canDragTo(net.minecraft.world.inventory.Slot slot) {
        return false;
    }
}
