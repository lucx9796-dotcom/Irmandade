package gg.irmandade.relic;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class RelicCommand {
    private RelicCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        // /Relics (e /relics) - aberto para todos. Para restringir, adicione .requires(...) aqui.
        for (String name : new String[]{"Relics", "relics"}) {
            d.register(Commands.literal(name).executes(ctx -> {
                RelicMenu.open(ctx.getSource().getPlayerOrException());
                return 1;
            }));
        }
    }
}
