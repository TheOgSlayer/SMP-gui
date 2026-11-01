package com.jalebi.smp;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class JalebiSMP extends JavaPlugin {

    private static JalebiSMP instance;
    private Economy economy;

    public static JalebiSMP get() { return instance; }
    public Economy economy() { return economy; }

    @Override
    public void onEnable() {
        instance = this;

        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) economy = rsp.getProvider();
        }
        if (economy == null) getLogger().warning("No Vault economy found - the Pay menu will not work.");

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(Commands.literal("smp").executes(ctx -> {
                if (ctx.getSource().getSender() instanceof Player p) openMain(p);
                return Command.SINGLE_SUCCESS;
            }).build(), "Open the Jalebi SMP menu");

            // Internal command used by the Pay button (dialog buttons can only run commands)
            event.registrar().register(Commands.literal("jalebipay").executes(ctx -> {
                if (ctx.getSource().getSender() instanceof Player p) PayMenu.open(p, 0, "");
                return Command.SINGLE_SUCCESS;
            }).build(), "Open the Jalebi pay menu");
        });
    }

    public static void openMain(Player p) {
        Dialog dialog = RegistryAccess.registryAccess().getRegistry(RegistryKey.DIALOG).get(SmpMenu.KEY);
        if (dialog != null) p.showDialog(dialog);
    }
}
