package com.donutstaff.commands;

import com.donutstaff.DonutStaffV2;
import com.donutstaff.managers.MessageUtil;
import com.donutstaff.models.ActivePunishment;
import com.donutstaff.models.PunishmentType;
import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class OffendedListCommand implements CommandExecutor {

    private final DonutStaffV2 plugin;

    public OffendedListCommand(DonutStaffV2 plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, String[] args) {
        if (args.length < 1 || !args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(MessageUtil.color(
                    plugin.getConfig().getString("messages.usage_list", "&cUsage: /{cmd} list")
                            .replace("{cmd}", label)));
            return true;
        }

        List<ActivePunishment> list = plugin.getPunishmentManager().getAllActive(PunishmentType.OFFEND);
        if (list.isEmpty()) {
            sender.sendMessage(MessageUtil.color(
                    plugin.getConfig().getString("messages.no_active_punishments", "&7No active punishments found.")));
            return true;
        }

        sender.sendMessage(MessageUtil.color("&4&l--- Offended (Banned) Players ---"));
        for (ActivePunishment p : list) {
            sender.sendMessage(MessageUtil.color(
                    "&4Player: &f" + p.getPlayerName() +
                    " &4| Punishment: &f" + p.getPunishmentName() +
                    " &4| Remaining: &f" + p.getTimeRemaining()));
        }
        return true;
    }
}
