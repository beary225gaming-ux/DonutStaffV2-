package com.donutstaff.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;

public class MessageUtil {

    private static final LegacyComponentSerializer SERIALIZER =
            LegacyComponentSerializer.legacyAmpersand();

    public static Component color(String message) {
        return SERIALIZER.deserialize(message);
    }

    public static void broadcast(String message) {
        Bukkit.getServer().sendMessage(color(message));
    }

    public static String replacePlaceholders(String template, String player, String punishment, String time) {
        return template
                .replace("{player}", player)
                .replace("{punishment}", punishment)
                .replace("{time}", time);
    }
}
