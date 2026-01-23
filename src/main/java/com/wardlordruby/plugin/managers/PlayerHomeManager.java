package com.wardlordruby.plugin.managers;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.wardlordruby.plugin.models.HomeMap;
import com.wardlordruby.plugin.models.JsonResource;
import com.wardlordruby.plugin.models.TeleportEntry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PlayerHomeManager {
    private static final String TMP_WORLD_INDICATOR = "instance-";
    private @Nonnull HomeMap homeMap;

    public PlayerHomeManager(@Nonnull HomeMap map) {
        this.homeMap = map;
    }

    public void write(@Nonnull JsonFileManager fileManager) {
        fileManager.write(homeMap, JsonResource.HOMES);
    }

    public @Nullable TeleportEntry get(@Nonnull PlayerRef playerRef) {
        return homeMap.get(playerRef.getUuid());
    }

    /// Return value indicates success or failure with an error message
    public @Nullable String insert(@Nonnull World world, @Nonnull PlayerRef playerRef) {
        String worldName = world.getName();

        if (worldName.startsWith(TMP_WORLD_INDICATOR)) {
            return "You can not set your home in temporary worlds";
        }

        homeMap.put(playerRef.getUuid(), new TeleportEntry(worldName, playerRef.getTransform()));
        return null;
    }
}
