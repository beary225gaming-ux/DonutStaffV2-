package com.donutstaff.commands;

import com.donutstaff.DonutStaffV2;
import com.donutstaff.managers.MessageUtil;
import com.donutstaff.managers.PunishmentManager;
import com.donutstaff.models.ActivePunishment;
import com.donutstaff.models.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.BanList;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class OffendCommand implements CommandExecutor, TabCompleter {

    private final DonutStaffV2 plugin;

    public OffendCommand(DonutStaffV2 plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.color(plugin.getConfig().getString(
                    "messages.usage_offend", "&cUsage: /offend <player> <punishment>")));
            return true;
        }

        String targetName = args[0];
        String punishmentName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        PunishmentManager pm = plugin.getPunishmentManager();

        String timeStr = pm.getOffendTime(punishmentName);
        if (timeStr == null) {
            sender.sendMessage(MessageUtil.color(
                    plugin.getConfig().getString("messages.punishment_not_found",
                            "&cPunishment not found: &f{punishment}").replace("{punishment}", punishmentName)));
            return true;
        }

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
                        plugin.getConfig().getString("messages.player_not_found",
                                "&cPlayer not found: &f{player}").replace("{player}", targetName)));
                return true;
            }
            targetUuid = offline.getUniqueId();
            resolvedName = offline.getName() != null ? offline.getName() : targetName;
        }

        long expiry = ActivePunishment.parseTime(timeStr);
        ActivePunishment punishment = new ActivePunishment(targetUuid, resolvedName, PunishmentType.OFFEND, punishmentName, expiry);
        pm.addPunishment(punishment);
        pm.savePunishments();

        // Actually ban the player using Bukkit's ban list
        Date expiryDate = expiry == -1L ? null : new Date(expiry);
        String banReason = "&4You have been banned: " + punishmentName;
        Bukkit.getBanList(BanList.Type.NAME).addBan(resolvedName, banReason, expiryDate, sender instanceof Player ? ((Player) sender).getName() : "Console");

        // Kick if online
        if (target != null && target.isOnline()) {
            String kickMsg = "&4You have been banned for: &f" + punishmentName +
                    "\n&4Duration: &f" + punishment.getTimeRemaining();
            target.kick(MessageUtil.color(kickMsg));
        }

        String broadcastMsg = plugin.getConfig().getString("messages.offend.format",
                "&l[BAN] &r&4Player: &f{player} &4got banned for &f{punishment}&4, and it will wear off in &f{time}&4.");
        broadcastMsg = MessageUtil.replacePlaceholders(broadcastMsg, resolvedName, punishmentName, punishment.getTimeRemaining());
        MessageUtil.broadcast(broadcastMsg);

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
            return plugin.getPunishmentManager().getOffendPunishmentNames();
        }
        return new ArrayList<>();
    }
}
