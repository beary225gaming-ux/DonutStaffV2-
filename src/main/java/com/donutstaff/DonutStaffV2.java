package com.donutstaff;

import com.donutstaff.commands.*;
import com.donutstaff.listeners.ChatListener;
import com.donutstaff.managers.PunishmentManager;
import com.donutstaff.managers.VCMuteManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class DonutStaffV2 extends JavaPlugin {

    private static DonutStaffV2 instance;
    private PunishmentManager punishmentManager;
    private VCMuteManager vcMuteManager;
    private FileConfiguration spawnstashConfig;

    @Override
    public void onEnable() {
        instance = this;

        // Save default configs
        saveDefaultConfig();
        saveResource("spawnstash.yml", false);

        // Load spawnstash config
        loadSpawnstashConfig();

        // Init managers
        punishmentManager = new PunishmentManager(this);
        vcMuteManager = new VCMuteManager(this);

        // Load persisted punishments
        punishmentManager.loadPunishments();

        // Register commands
        getCommand("warn").setExecutor(new WarnCommand(this));
        getCommand("warn").setTabCompleter(new WarnCommand(this));
        getCommand("warned").setExecutor(new WarnedListCommand(this));

        getCommand("tempmute").setExecutor(new TempMuteCommand(this));
        getCommand("tempmute").setTabCompleter(new TempMuteCommand(this));
        getCommand("tempmuted").setExecutor(new TempMutedListCommand(this));

        getCommand("offend").setExecutor(new OffendCommand(this));
        getCommand("offend").setTabCompleter(new OffendCommand(this));
        getCommand("offended").setExecutor(new OffendedListCommand(this));

        getCommand("vctempmute").setExecutor(new VCTempMuteCommand(this));
        getCommand("vctempmute").setTabCompleter(new VCTempMuteCommand(this));
        getCommand("vctempmuted").setExecutor(new VCTempMutedListCommand(this));

        getCommand("spawnstash").setExecutor(new SpawnStashCommand(this));

        // Register listeners
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        // Start expiry task (runs every 10 seconds)
        getServer().getScheduler().runTaskTimer(this, () -> punishmentManager.checkExpired(), 200L, 200L);

        getLogger().info("Donut Staff V2 has been enabled!");
    }

    @Override
    public void onDisable() {
        if (punishmentManager != null) {
            punishmentManager.savePunishments();
        }
        getLogger().info("Donut Staff V2 has been disabled.");
    }

    private void loadSpawnstashConfig() {
        File file = new File(getDataFolder(), "spawnstash.yml");
        spawnstashConfig = YamlConfiguration.loadConfiguration(file);
    }

    public void reloadSpawnstashConfig() {
        File file = new File(getDataFolder(), "spawnstash.yml");
        spawnstashConfig = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getSpawnstashConfig() {
        return spawnstashConfig;
    }

    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }

    public VCMuteManager getVcMuteManager() {
        return vcMuteManager;
    }

    public static DonutStaffV2 getInstance() {
        return instance;
    }
}
