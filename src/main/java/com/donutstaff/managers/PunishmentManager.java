package com.donutstaff.managers;

import com.donutstaff.DonutStaffV2;
import com.donutstaff.models.ActivePunishment;
import com.donutstaff.models.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class PunishmentManager {

    private final DonutStaffV2 plugin;
    // Map of UUID -> list of active punishments
    private final Map<UUID, List<ActivePunishment>> activePunishments = new HashMap<>();
    private File dataFile;
    private FileConfiguration dataConfig;

    public PunishmentManager(DonutStaffV2 plugin) {
        this.plugin = plugin;
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    // ---- ADDING PUNISHMENTS ----

    public void addPunishment(ActivePunishment punishment) {
        activePunishments.computeIfAbsent(punishment.getPlayerUuid(), k -> new ArrayList<>())
                .add(punishment);
    }

    public boolean hasActivePunishment(UUID uuid, PunishmentType type) {
        List<ActivePunishment> list = activePunishments.get(uuid);
        if (list == null) return false;
        return list.stream().anyMatch(p -> p.getType() == type && !p.isExpired());
    }

    public Optional<ActivePunishment> getActivePunishment(UUID uuid, PunishmentType type) {
        List<ActivePunishment> list = activePunishments.get(uuid);
        if (list == null) return Optional.empty();
        return list.stream().filter(p -> p.getType() == type && !p.isExpired()).findFirst();
    }

    public void removePunishment(UUID uuid, PunishmentType type) {
        List<ActivePunishment> list = activePunishments.get(uuid);
        if (list != null) {
            list.removeIf(p -> p.getType() == type);
        }
    }

    // ---- LIST ALL ACTIVE BY TYPE ----

    public List<ActivePunishment> getAllActive(PunishmentType type) {
        List<ActivePunishment> result = new ArrayList<>();
        for (List<ActivePunishment> list : activePunishments.values()) {
            for (ActivePunishment p : list) {
                if (p.getType() == type && !p.isExpired()) {
                    result.add(p);
                }
            }
        }
        return result;
    }

    // ---- EXPIRY CHECK ----

    public void checkExpired() {
        for (Map.Entry<UUID, List<ActivePunishment>> entry : activePunishments.entrySet()) {
            Iterator<ActivePunishment> it = entry.getValue().iterator();
            while (it.hasNext()) {
                ActivePunishment p = it.next();
                if (p.isExpired()) {
                    it.remove();
                    // If it was an offend (ban), unban the player
                    if (p.getType() == PunishmentType.OFFEND) {
                        Bukkit.getScheduler().runTask(plugin, () ->
                                Bukkit.unbanPlayer(p.getPlayerName()));
                    }
                }
            }
        }
        savePunishments();
    }

    // ---- CONFIG LOOKUPS ----

    public String getWarnTime(String punishmentName) {
        return getPunishmentTime("warn_punishments", "punishment", punishmentName, "warn_time");
    }

    public String getTempmuteTime(String punishmentName) {
        return getPunishmentTime("tempmute_punishments", "punishment", punishmentName, "tempmute_time");
    }

    public String getOffendTime(String punishmentName) {
        return getPunishmentTime("offend_punishments", "punishment", punishmentName, "ban_time");
    }

    public String getVCTempmuteTime(String punishmentName) {
        return getPunishmentTime("vctempmute_punishments", "punishment", punishmentName, "vctempmute_time");
    }

    private String getPunishmentTime(String section, String nameKey, String punishmentName, String timeKey) {
        var list = plugin.getConfig().getMapList(section);
        for (var map : list) {
            Object name = map.get(nameKey);
            if (name != null && name.toString().equalsIgnoreCase(punishmentName)) {
                Object time = map.get(timeKey);
                return time != null ? time.toString() : null;
            }
        }
        return null;
    }

    public List<String> getWarnPunishmentNames() {
        return getPunishmentNames("warn_punishments");
    }

    public List<String> getTempmutePunishmentNames() {
        return getPunishmentNames("tempmute_punishments");
    }

    public List<String> getOffendPunishmentNames() {
        return getPunishmentNames("offend_punishments");
    }

    public List<String> getVCTempmutePunishmentNames() {
        return getPunishmentNames("vctempmute_punishments");
    }

    private List<String> getPunishmentNames(String section) {
        var list = plugin.getConfig().getMapList(section);
        return list.stream()
                .map(m -> m.get("punishment"))
                .filter(Objects::nonNull)
                .map(Object::toString)
                .collect(Collectors.toList());
    }

    // ---- PERSISTENCE ----

    public void savePunishments() {
        dataConfig.set("punishments", null); // clear
        int i = 0;
        for (List<ActivePunishment> list : activePunishments.values()) {
            for (ActivePunishment p : list) {
                if (!p.isExpired()) {
                    String base = "punishments." + i;
                    dataConfig.set(base + ".uuid", p.getPlayerUuid().toString());
                    dataConfig.set(base + ".name", p.getPlayerName());
                    dataConfig.set(base + ".type", p.getType().name());
                    dataConfig.set(base + ".punishment", p.getPunishmentName());
                    dataConfig.set(base + ".expiry", p.getExpiryTimeMillis());
                    i++;
                }
            }
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadPunishments() {
        activePunishments.clear();
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection section = dataConfig.getConfigurationSection("punishments");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            String base = "punishments." + key;
            try {
                UUID uuid = UUID.fromString(dataConfig.getString(base + ".uuid"));
                String name = dataConfig.getString(base + ".name");
                PunishmentType type = PunishmentType.valueOf(dataConfig.getString(base + ".type"));
                String punName = dataConfig.getString(base + ".punishment");
                long expiry = dataConfig.getLong(base + ".expiry");
                ActivePunishment p = new ActivePunishment(uuid, name, type, punName, expiry);
                if (!p.isExpired()) {
                    activePunishments.computeIfAbsent(uuid, k -> new ArrayList<>()).add(p);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load punishment entry: " + key);
            }
        }
    }
}
