package com.donutstaff.models;

import java.util.UUID;

public class ActivePunishment {

    private final UUID playerUuid;
    private final String playerName;
    private final PunishmentType type;
    private final String punishmentName;
    private final long expiryTimeMillis; // -1 = permanent

    public ActivePunishment(UUID playerUuid, String playerName, PunishmentType type,
                            String punishmentName, long expiryTimeMillis) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.type = type;
        this.punishmentName = punishmentName;
        this.expiryTimeMillis = expiryTimeMillis;
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public PunishmentType getType() { return type; }
    public String getPunishmentName() { return punishmentName; }
    public long getExpiryTimeMillis() { return expiryTimeMillis; }

    public boolean isPermanent() { return expiryTimeMillis == -1L; }

    public boolean isExpired() {
        if (isPermanent()) return false;
        return System.currentTimeMillis() >= expiryTimeMillis;
    }

    /** Returns a human-readable string of remaining time */
    public String getTimeRemaining() {
        if (isPermanent()) return "Permanent";
        long diff = expiryTimeMillis - System.currentTimeMillis();
        if (diff <= 0) return "Expired";
        return formatMillis(diff);
    }

    public static String formatMillis(long millis) {
        long seconds = millis / 1000;
        if (seconds < 60) return seconds + "s";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m " + (seconds % 60) + "s";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h " + (minutes % 60) + "m";
        long days = hours / 24;
        return days + "d " + (hours % 24) + "h";
    }

    /** Parse a time string like 30m, 2h, 7d, 1h30m into milliseconds. Returns -1 for "permanent". */
    public static long parseTime(String input) {
        if (input == null) return -1L;
        String lower = input.trim().toLowerCase();
        if (lower.equals("permanent") || lower.equals("perm")) return -1L;

        long total = 0;
        StringBuilder num = new StringBuilder();
        for (char c : lower.toCharArray()) {
            if (Character.isDigit(c)) {
                num.append(c);
            } else {
                if (num.length() == 0) continue;
                long value = Long.parseLong(num.toString());
                num.setLength(0);
                switch (c) {
                    case 's' -> total += value * 1000L;
                    case 'm' -> total += value * 60_000L;
                    case 'h' -> total += value * 3_600_000L;
                    case 'd' -> total += value * 86_400_000L;
                }
            }
        }
        return total > 0 ? System.currentTimeMillis() + total : -1L;
    }
}
