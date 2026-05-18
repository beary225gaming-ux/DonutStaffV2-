package com.donutstaff.commands;

import com.donutstaff.DonutStaffV2;
import com.donutstaff.managers.MessageUtil;
import com.donutstaff.managers.PunishmentManager;
import com.donutstaff.models.ActivePunishment;
import com.donutstaff.models.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WarnCommand implements CommandExecutor, TabCompleter {

    private final DonutStaffV2 plugin;

    public WarnCommand(DonutStaffV2 plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.color(plugin.getConfig().getString(
                    "messages.usage_warn", "&cUsage: /warn <player> <punishment>")));
            return true;
        }

        String targetName = args[0];
        String punishmentName = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));

        PunishmentManager pm = plugin.getPunishmentManager();

        String timeStr = pm.getWarnTime(punishmentName);
        if (timeStr == null) {
            sender.sendMessage(MessageUtil.color(
                    plugin.getConfig().getString("messages.punishment_not_found", "&cPunishment not found: &f{punishment}")
                            .replace("{punishment}", punishmentName)));
            return true;
        }

        // Find player (online or offline by name)
        Player target = Bukkit.getPlayerExact(targetName);
        UUID targetUuid;
        String resolvedName;

        if (target != null) {
            targetUuid = target.getUniqueId();
            resolvedName = target.getName();
        } else {
            @SuppressWarnings("deprecation")
            org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
            if (!offline.hasPlayedBefore() && !offline.isOnline()) {
                sender.sendMessage(MessageUtil.color(
                        plugin.getConfig().getString("messages.player_not_found", "&cPlayer not found: &f{player}")
                                .replace("{player}", targetName)));
                return true;
            }
            targetUuid = offline.getUniqueId();
            resolvedName = offline.getName() != null ? offline.getName() : targetName;
        }

        long expiry = ActivePunishment.parseTime(timeStr);
        ActivePunishment punishment = new ActivePunishment(targetUuid, resolvedName, PunishmentType.WARN, punishmentName, expiry);
        pm.addPunishment(punishment);
        pm.savePunishments();

        // Broadcast
        String broadcastMsg = plugin.getConfig().getString("messages.warn.format",
                "&l[WARN] &r&cPlayer: &f{player} &cgot warned for &f{punishment}&c, and it will wear off in &f{time}&c.");
        broadcastMsg = MessageUtil.replacePlaceholders(broadcastMsg, resolvedName, punishmentName, punishment.getTimeRemaining());
        MessageUtil.broadcast(broadcastMsg);

        // Notify player directly
        if (target != null && target.isOnline()) {
            String notify = plugin.getConfig().getString("messages.warn_notify",
                    "&cYou have been warned for: &f{punishment}&c. Duration: &f{time}&c.");
            notify = MessageUtil.replacePlaceholders(notify, resolvedName, punishmentName, punishment.getTimeRemaining());
            target.sendMessage(MessageUtil.color(notify));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                      @NotNull String label, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
            return names;
        }
        if (args.length >= 2) {
            return plugin.getPunishmentManager().getWarnPunishmentNames();
        }
        return new ArrayList<>();
    }
}
