package com.donutstaff.managers;

import com.donutstaff.DonutStaffV2;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Manages Simple Voice Chat muting.
 * Uses player permissions as a proxy mute flag since the API changed significantly in 2.6+.
 * The actual mic blocking is handled by SVC reading a config flag we set via command.
 * If SVC is not installed, all methods are no-ops.
 */
public class VCMuteManager {

    private final DonutStaffV2 plugin;
    private boolean svcAvailable = false;

    public VCMuteManager(DonutStaffV2 plugin) {
        this.plugin = plugin;
        if (Bukkit.getPluginManager().getPlugin("voicechat") != null) {
            svcAvailable = true;
            plugin.getLogger().info("[DonutStaffV2] Simple Voice Chat found.");
        } else {
            plugin.getLogger().warning("[DonutStaffV2] Simple Voice Chat not found. /vctempmute will track punishments but cannot mute players.");
        }
    }

    public void mutePlayer(UUID uuid) {
        if (!svcAvailable) return;
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;
        // Revoke the voicechat.speak permission to block mic
        player.addAttachment(plugin, "voicechat.speak", false);
        plugin.getLogger().info("[DonutStaffV2] VC muted: " + player.getName());
    }

    public void unmutePlayer(UUID uuid) {
        if (!svcAvailable) return;
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;
        // Remove permission override — SVC will restore normal behaviour
        player.getEffectivePermissions().stream()
                .filter(a -> a.getPermission().equals("voicechat.speak"))
                .forEach(a -> {
                    if (a.getAttachment() != null) a.getAttachment().remove();
                });
        plugin.getLogger().info("[DonutStaffV2] VC unmuted: " + player.getName());
    }

    public void reapplyMuteOnJoin(UUID uuid) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> mutePlayer(uuid), 20L);
    }
}
