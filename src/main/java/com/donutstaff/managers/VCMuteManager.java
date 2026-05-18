package com.donutstaff.managers;

import com.donutstaff.DonutStaffV2;
import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.logging.Level;

/**
 * Manages Simple Voice Chat muting via its API.
 * If Simple Voice Chat is not installed, all methods are no-ops and log a warning.
 */
public class VCMuteManager {

    private final DonutStaffV2 plugin;
    private boolean svcAvailable = false;
    private VoicechatApi vcApi = null;

    public VCMuteManager(DonutStaffV2 plugin) {
        this.plugin = plugin;
        tryHookSVC();
    }

    private void tryHookSVC() {
        if (Bukkit.getPluginManager().getPlugin("voicechat") == null) {
            plugin.getLogger().warning("[DonutStaffV2] Simple Voice Chat not found. /vctempmute will track punishments but cannot API-mute players.");
            return;
        }
        try {
            BukkitVoicechatService service = plugin.getServer().getServicesManager()
                    .load(BukkitVoicechatService.class);
            if (service != null) {
                vcApi = service.getVoicechatApi();
                svcAvailable = true;
                plugin.getLogger().info("[DonutStaffV2] Simple Voice Chat hooked successfully.");
            } else {
                plugin.getLogger().warning("[DonutStaffV2] Simple Voice Chat service not available yet.");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[DonutStaffV2] Failed to hook Simple Voice Chat.", e);
        }
    }

    public void mutePlayer(UUID uuid) {
        if (!svcAvailable || vcApi == null) return;
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;
        try {
            VoicechatConnection conn = vcApi.getConnectionOf(player.getUniqueId());
            if (conn != null) {
                conn.setDisabled(true); // disables microphone (can still hear)
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[DonutStaffV2] Error muting player in SVC.", e);
        }
    }

    public void unmutePlayer(UUID uuid) {
        if (!svcAvailable || vcApi == null) return;
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;
        try {
            VoicechatConnection conn = vcApi.getConnectionOf(player.getUniqueId());
            if (conn != null) {
                conn.setDisabled(false);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[DonutStaffV2] Error unmuting player in SVC.", e);
        }
    }

    /**
     * Called when a VC-muted player joins: reapply the mute.
     */
    public void reapplyMuteOnJoin(UUID uuid) {
        // Small delay to let SVC register the connection
        Bukkit.getScheduler().runTaskLater(plugin, () -> mutePlayer(uuid), 20L);
    }
}
