package com.jalebi.smp;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public final class PayMenu {

    private static final int PER_PAGE = 8; // 2 columns x 4 rows
    private static final ClickCallback.Options OPTS = ClickCallback.Options.builder()
            .uses(ClickCallback.UNLIMITED_USES)
            .lifetime(Duration.ofMinutes(10))
            .build();

    private PayMenu() {}

    private static ActionButton button(Component label, DialogAction action) {
        return ActionButton.builder(label).width(150).action(action).build();
    }

    // ---------- Page 1: player list with search ----------
    public static void open(Player viewer, int page, String search) {
        String q = search == null ? "" : search.trim();

        List<OfflinePlayer> players = Arrays.stream(Bukkit.getOfflinePlayers())
                .filter(o -> o.getName() != null)
                .filter(o -> !o.getUniqueId().equals(viewer.getUniqueId()))
                .filter(o -> q.isEmpty() || o.getName().toLowerCase().contains(q.toLowerCase()))
                .sorted(Comparator.comparing(OfflinePlayer::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        int pages = Math.max(1, (int) Math.ceil(players.size() / (double) PER_PAGE));
        int cur = Math.max(0, Math.min(page, pages - 1));

        List<ActionButton> buttons = new ArrayList<>();
        int from = cur * PER_PAGE;
        int to = Math.min(players.size(), from + PER_PAGE);
        for (int i = from; i < to; i++) {
            OfflinePlayer target = players.get(i);
            buttons.add(button(Component.text(target.getName()),
                    DialogAction.customClick((view, audience) -> openAmount(viewer, target, q, cur), OPTS)));
        }
        // keep the footer buttons below the player grid, aligned
        while (buttons.size() < PER_PAGE) {
            buttons.add(ActionButton.builder(Component.text("-", NamedTextColor.DARK_GRAY)).width(150).build());
        }

        // footer: Search / Next page / Previous page
        buttons.add(button(Component.text("Search"),
                DialogAction.customClick((view, audience) -> {
                    String text = view.getText("search");
                    open(viewer, 0, text == null ? "" : text);
                }, OPTS)));
        if (cur < pages - 1) {
            buttons.add(button(Component.text("Next page"),
                    DialogAction.customClick((view, audience) -> open(viewer, cur + 1, q), OPTS)));
        } else {
            buttons.add(ActionButton.builder(Component.text("Next page", NamedTextColor.DARK_GRAY)).width(150).build());
        }
        if (cur > 0) {
            buttons.add(button(Component.text("Previous page"),
                    DialogAction.customClick((view, audience) -> open(viewer, cur - 1, q), OPTS)));
            buttons.add(ActionButton.builder(Component.text("Page " + (cur + 1) + "/" + pages)).width(150).build());
        }

        Dialog dialog = Dialog.create(f -> f.empty()
                .base(DialogBase.builder(Component.text("Pay a Player", NamedTextColor.GREEN))
                        .inputs(List.of(DialogInput.text("search", Component.text("Search player"))
                                .width(300)
                                .initial(q)
                                .maxLength(16)
                                .build()))
                        .build())
                .type(DialogType.multiAction(buttons)
                        .columns(2)
                        .exitAction(button(Component.text("Back"),
                                DialogAction.customClick((view, audience) -> JalebiSMP.openMain(viewer), OPTS)))
                        .build()));
        viewer.showDialog(dialog);
    }

    // ---------- Page 2: amount ----------
    private static void openAmount(Player viewer, OfflinePlayer target, String search, int page) {
        ActionButton pay = button(Component.text("Pay", NamedTextColor.GREEN),
                DialogAction.customClick((view, audience) -> {
                    String raw = view.getText("amount");
                    doPay(viewer, target, raw);
                }, OPTS));

        Dialog dialog = Dialog.create(f -> f.empty()
                .base(DialogBase.builder(Component.text("Pay " + target.getName(), NamedTextColor.GREEN))
                        .inputs(List.of(DialogInput.text("amount", Component.text("Amount"))
                                .width(300)
                                .maxLength(16)
                                .build()))
                        .build())
                .type(DialogType.multiAction(List.of(pay))
                        .columns(1)
                        .exitAction(button(Component.text("Back"),
                                DialogAction.customClick((view, audience) -> open(viewer, page, search), OPTS)))
                        .build()));
        viewer.showDialog(dialog);
    }

    private static void doPay(Player from, OfflinePlayer to, String raw) {
        Economy eco = JalebiSMP.get().economy();
        if (eco == null) {
            from.sendMessage(Component.text("Economy is not available.", NamedTextColor.RED));
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(raw == null ? "" : raw.trim());
        } catch (NumberFormatException e) {
            from.sendMessage(Component.text("Enter a valid number.", NamedTextColor.RED));
            return;
        }
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            from.sendMessage(Component.text("Amount must be greater than 0.", NamedTextColor.RED));
            return;
        }
        amount = Math.floor(amount * 100) / 100.0;
        if (amount <= 0) {
            from.sendMessage(Component.text("Amount is too small.", NamedTextColor.RED));
            return;
        }
        if (!eco.has(from, amount)) {
            from.sendMessage(Component.text("You don't have enough money.", NamedTextColor.RED));
            return;
        }
        var w = eco.withdrawPlayer(from, amount);
        if (!w.transactionSuccess()) {
            from.sendMessage(Component.text("Payment failed.", NamedTextColor.RED));
            return;
        }
        var d = eco.depositPlayer(to, amount);
        if (!d.transactionSuccess()) {
            eco.depositPlayer(from, amount); // refund
            from.sendMessage(Component.text("Payment failed, you were refunded.", NamedTextColor.RED));
            return;
        }
        String fmt = eco.format(amount);
        from.sendMessage(Component.text("You paid " + fmt + " to " + to.getName() + ".", NamedTextColor.GREEN));
        Player online = to.getPlayer();
        if (online != null) {
            online.sendMessage(Component.text(from.getName() + " paid you " + fmt + ".", NamedTextColor.GREEN));
        }
    }
}
