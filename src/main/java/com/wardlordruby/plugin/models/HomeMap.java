package com.wardlordruby.plugin.models;

import com.wardlordruby.plugin.HomePlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

public final class HomeMap extends ConcurrentHashMap<UUID, ArrayList<TeleportEntry>> {
    public static @Nullable String validator(HomeMap map) {
        Set<String> bannedWorlds = HomePlugin.getConfig().homeConfig.bannedHomeWorlds;

        map.entrySet().forEach(entry -> {
            List<TeleportEntry> playerHomes = entry.getValue();

            for (int i = playerHomes.size() - 1; i >= 0; i--) {
                String homeWorld = playerHomes.get(i).world;
                if (bannedWorlds.contains(homeWorld)) {
                    HomePlugin.LOGGER.atInfo().log("Removed illegal home in banned world: %s, from player: %s", homeWorld, entry.getKey());
                    playerHomes.remove(i);
                }
            }
        });

        return null;
    }
}
