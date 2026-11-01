package com.jalebi.smp;

import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;

import java.util.List;

/** Builds the main "Jalebi SMP" dialog. Used for both the pause-menu button and /smp. */
public final class SmpMenu {

    public static final Key KEY = Key.key("jalebi", "smp");
    public static final Component TITLE = Component.text("Jalebi SMP");

    /** Change this if you'd rather use another command for spawn, e.g. "spawn". */
    private static final String SPAWN_COMMAND = "warp spawn";

    private SmpMenu() {}

    private static ActionButton cmd(String label, String command) {
        return ActionButton.builder(Component.text(label))
                .width(150)
                .action(DialogAction.staticAction(ClickEvent.runCommand("/" + command)))
                .build();
    }

    /** Pay opens our custom dialog through a plain command handled by the plugin. */
    public static void fill(DialogRegistryEntry.Builder builder) {
        List<ActionButton> buttons = List.of(
                cmd("Spawn", SPAWN_COMMAND),
                cmd("Homes", "homes"),
                cmd("Auction", "ah"),
                cmd("Shop", "shop"),
                cmd("Orders", "order"),
                cmd("Friends", "friends"),
                cmd("Sell", "sell"),
                cmd("Pay", "jalebipay"),
                cmd("Stats", "stats"),
                cmd("Leaderboard", "leaderboard"),
                cmd("Settings", "settings")
        );

        builder.base(DialogBase.builder(TITLE)
                .externalTitle(TITLE)      // text on the pause-menu button
                .canCloseWithEscape(true)
                .build());

        builder.type(DialogType.multiAction(buttons)
                .columns(2)
                .exitAction(ActionButton.builder(Component.text("Back")).width(150).build())
                .build());
    }
}
