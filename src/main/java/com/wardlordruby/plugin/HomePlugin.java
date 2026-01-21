package com.wardlordruby.plugin;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class HomePlugin extends JavaPlugin {

    private static final String HOMES_FILE = "homes.json";
    private static final String TMP_WORLD_INDICATOR = "instance-";

    private final File homeFile;
    private final ConcurrentHashMap<UUID, TeleportEntry> homeMap;

    public HomePlugin(@Nonnull JavaPluginInit init) {
        super(init);
        this.homeFile = getDataDirectory().resolve(HOMES_FILE).toFile();
        this.homeMap = HomeManager.loadHomes(homeFile);
    }

    @Override
    protected void setup() {
        super.setup();
        this.getCommandRegistry().registerCommand(new HomeCommand(this));
        this.getCommandRegistry().registerCommand(new BackCommand());
    }

    @Override
    protected void shutdown() {
        HomeManager.saveHomes(homeMap, homeFile);
        super.shutdown();
    }

    public @Nullable TeleportEntry getPlayerHome(@Nonnull PlayerRef playerRef) {
        return homeMap.get(playerRef.getUuid());
    }

    /// Return value indicates success or failure with an error message
    public @Nullable String insertHome(@Nonnull World world, @Nonnull PlayerRef playerRef) {
        String worldName = world.getName();

        if (worldName.startsWith(TMP_WORLD_INDICATOR)) {
            return "You can not set your home in temporary worlds";
        }

        homeMap.put(playerRef.getUuid(), new TeleportEntry(worldName, playerRef.getTransform()));
        return null;
    }
}
