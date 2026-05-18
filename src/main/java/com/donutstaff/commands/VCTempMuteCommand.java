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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class VCTempMuteCommand implements CommandExecutor, TabCompleter {

    private final DonutStaffV2 plugin;

    public VCTempMuteCommand(DonutStaffV2 plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.color(plugin.getConfig().getString(
                    "messages.usage_vctempmute", "&cUsage: /vctempmute <player> <punishment>")));
            return true;
        }

        String targetName = args[0];
        String punishmentName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        PunishmentManager pm = plugin.getPunishmentManager();

        String timeStr = pm.getVCTempmuteTime(punishmentName);
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
        ActivePunishment punishment = new ActivePunishment(targetUuid, resolvedName, PunishmentType.VCTEMPMUTE, punishmentName, expiry);
        pm.addPunishment(punishment);
        pm.savePunishments();

        // Actually mute in Simple Voice Chat
        plugin.getVcMuteManager().mutePlayer(targetUuid);

        String broadcastMsg = plugin.getConfig().getString("messages.vctempmute.format",
                "&l[VCMUTE] &r&5Player: &f{player} &5got vc muted for &f{punishment}&5, and it will wear off in &f{time}&5.");
        broadcastMsg = MessageUtil.replacePlaceholders(broadcastMsg, resolvedName, punishmentName, punishment.getTimeRemaining());
        MessageUtil.broadcast(broadcastMsg);

        if (target != null && target.isOnline()) {
            String notify = plugin.getConfig().getString("messages.vctempmute_notify",
                    "&5You have been VC muted for: &f{punishment}&5. Duration: &f{time}&5.");
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
            return plugin.getPunishmentManager().getVCTempmutePunishmentNames();
        }
        return new ArrayList<>();
    }
}
