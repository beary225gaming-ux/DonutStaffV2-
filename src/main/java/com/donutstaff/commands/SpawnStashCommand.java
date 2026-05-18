package com.donutstaff.commands;

import com.donutstaff.DonutStaffV2;
import com.donutstaff.managers.MessageUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;

public class SpawnStashCommand implements CommandExecutor {

    private final DonutStaffV2 plugin;
    private final Random random = new Random();

    public SpawnStashCommand(DonutStaffV2 plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        FileConfiguration cfg = plugin.getSpawnstashConfig();

        // ---- Spawner block at player location ----
        Location loc = player.getLocation().getBlock().getLocation();
        Block spawnerBlock = loc.getWorld().getBlockAt(loc);

        // Pick random spawner type from config
        List<String> spawnerTypes = cfg.getStringList("spawnstash.spawner_types");
        EntityType chosenEntity = EntityType.ZOMBIE;
        if (!spawnerTypes.isEmpty()) {
            String picked = spawnerTypes.get(random.nextInt(spawnerTypes.size()));
            try {
                chosenEntity = EntityType.valueOf(picked.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[SpawnStash] Unknown entity type in config: " + picked + ". Defaulting to ZOMBIE.");
            }
        }

        spawnerBlock.setType(Material.SPAWNER);
        if (spawnerBlock.getState() instanceof CreatureSpawner cs) {
            cs.setSpawnedType(chosenEntity);
            cs.update(true);
        }

        // ---- Side block (1 left, same Y) ----
        String directionStr = cfg.getString("spawnstash.side_block_direction", "WEST").toUpperCase();
        String sideBlockMat = cfg.getString("spawnstash.side_block", "GOLD_BLOCK").toUpperCase();

        Location sideLocation = getRelativeLocation(loc, directionStr);
        Block sideBlock = sideLocation.getWorld().getBlockAt(sideLocation);

        Material sideMaterial = getMaterial(sideBlockMat, Material.GOLD_BLOCK);
        sideBlock.setType(sideMaterial);

        // ---- Top block (on top of side block) ----
        String topBlockMat = cfg.getString("spawnstash.top_block", "AMETHYST_CLUSTER").toUpperCase();
        String topBlockFallback = cfg.getString("spawnstash.top_block_fallback", "AMETHYST_BLOCK").toUpperCase();

        Location topLocation = sideLocation.clone().add(0, 1, 0);
        Block topBlock = topLocation.getWorld().getBlockAt(topLocation);

        Material topMaterial = getMaterial(topBlockMat, null);
        if (topMaterial == null || !topMaterial.isBlock()) {
            topMaterial = getMaterial(topBlockFallback, Material.AMETHYST_BLOCK);
        }
        topBlock.setType(topMaterial);

        // ---- Success message ----
        String successMsg = cfg.getString("spawnstash.success_message", "&aStash spawned successfully at your location!");
        player.sendMessage(MessageUtil.color(successMsg));

        return true;
    }

    private Location getRelativeLocation(Location origin, String direction) {
        return switch (direction) {
            case "NORTH" -> origin.clone().add(0, 0, -1);
            case "SOUTH" -> origin.clone().add(0, 0, 1);
            case "EAST"  -> origin.clone().add(1, 0, 0);
            case "WEST"  -> origin.clone().add(-1, 0, 0);
            default      -> origin.clone().add(-1, 0, 0); // default west
        };
    }

    private Material getMaterial(String name, Material fallback) {
        try {
            Material mat = Material.valueOf(name);
            return mat;
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[SpawnStash] Unknown material: " + name);
            return fallback;
        }
    }
}
