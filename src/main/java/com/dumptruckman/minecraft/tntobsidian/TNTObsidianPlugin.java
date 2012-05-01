package com.dumptruckman.minecraft.tntobsidian;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TNTObsidianPlugin extends JavaPlugin implements Listener {

    private static final String RADIUS = "explosion_radius";
    private static final int RADIUS_DEFAULT = 3;
    private static final String OBSIDIAN_HITS = "obsidian_hits";
    private static final int HITS_DEFAULT = 3;
    private static final String IGNORED_BLOCKS = "ignored_blocks";
    private static final String CANNON_BLOCKS = "cannon_blocks";

    private File configFile;
    private Set<Integer> ignoreSet = new HashSet<Integer>();
    private Set<Integer> cannonSet = new HashSet<Integer>();

    private Map<Block, Integer> obsidianHealth = new HashMap<Block, Integer>();

    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            try {
                YamlConfiguration.loadConfiguration(getResource("config.yml")).save(configFile);
            } catch (IOException e) {
                getLogger().warning("Could not copy default config.yml!");
            }
        }
        List<Integer> ignoreList = getConfig().getIntegerList(IGNORED_BLOCKS);
        if (ignoreList != null) {
            ignoreSet.addAll(ignoreList);
        }
        List<Integer> cannonList = getConfig().getIntegerList(CANNON_BLOCKS);
        if (cannonList != null) {
            cannonSet.addAll(cannonList);
        }
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void entityExplode(EntityExplodeEvent event) {
        if (event.isCancelled()
                || event.getEntityType() != EntityType.PRIMED_TNT) {
            return;
        }

        int max_hits = getConfig().getInt(OBSIDIAN_HITS, HITS_DEFAULT);
        int radius = getConfig().getInt(RADIUS, RADIUS_DEFAULT);

        Block eventBlock = event.getLocation().getBlock();
        boolean cannon = false;
        if (eventBlock.getRelative(-1, 0, 0).getType() == Material.TNT
                || eventBlock.getRelative(1, 0, 0).getType() == Material.TNT
                || eventBlock.getRelative(0, -1, 0).getType() == Material.TNT
                || eventBlock.getRelative(0, 1, 0).getType() == Material.TNT
                || eventBlock.getRelative(0, 0, -1).getType() == Material.TNT
                || eventBlock.getRelative(0, 0, 1).getType() == Material.TNT) {
            cannon = true;
        }

        for (int x = 0 - radius; x <= radius; x++) {
            for (int y = 0 - radius; y <= radius; y++) {
                for (int z = 0 - radius; z <= radius; z++) {
                    Block block = eventBlock.getRelative(x, y, z);
                    if (block.getType() == Material.OBSIDIAN) {
                        if (getHits(block) >= max_hits) {
                            event.blockList().add(block);
                            obsidianHealth.remove(block);
                        } else {
                            addHit(block);
                        }
                    } else if (!ignoreSet.contains(block.getTypeId())) {
                        if (cannon) {
                            if (!cannonSet.contains(block.getTypeId())) {
                                event.blockList().add(block);
                            }
                        } else {
                            event.blockList().add(block);
                        }
                    }
                }
            }
        }
    }

    private int getHits(Block block) {
        if (!obsidianHealth.containsKey(block)) {
            obsidianHealth.put(block, 0);
        }
        return obsidianHealth.get(block);
    }

    private void addHit(Block block) {
        obsidianHealth.put(block, getHits(block) + 1);
    }
}
