package com.donutstaff.listeners;

import com.donutstaff.DonutStaffV2;
import com.donutstaff.managers.MessageUtil;
import com.donutstaff.models.ActivePunishment;
import com.donutstaff.models.PunishmentType;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Optional;

public class ChatListener implements Listener {

    private final DonutStaffV2 plugin;

    public ChatListener(DonutStaffV2 plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event) {
        Optional<ActivePunishment> mute = plugin.getPunishmentManager()
                .getActivePunishment(event.getPlayer().getUniqueId(), PunishmentType.TEMPMUTE);

        if (mute.isPresent()) {
            event.setCancelled(true);
            String msg = plugin.getConfig().getString("messages.muted_attempt",
                    "&cYou are muted and cannot chat. Mute expires in: &f{time}&c.");
            msg = msg.replace("{time}", mute.get().getTimeRemaining());
            event.getPlayer().sendMessage(MessageUtil.color(msg));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Reapply VC mute if still active
        if (plugin.getPunishmentManager().hasActivePunishment(
                event.getPlayer().getUniqueId(), PunishmentType.VCTEMPMUTE)) {
            plugin.getVcMuteManager().reapplyMuteOnJoin(event.getPlayer().getUniqueId());
        }
    }
}
