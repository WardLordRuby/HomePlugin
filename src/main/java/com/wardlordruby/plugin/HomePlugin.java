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

    private final File homeFile;
    private final ConcurrentHashMap<UUID, TeleportEntry> homeMap;

    public HomePlugin(@Nonnull JavaPluginInit init) {
        super(init);
        this.homeFile = getDataDirectory().resolve(HOMES_FILE).toFile();
        this.homeMap = HomeManager.loadHomes(homeFile);
    }

    @Nullable
    public TeleportEntry getPlayerHome(@Nonnull PlayerRef playerRef) {
        return homeMap.get(playerRef.getUuid());
    }

    public void insertHome(@Nonnull World world, @Nonnull PlayerRef playerRef) {
        homeMap.put(playerRef.getUuid(), new TeleportEntry(world, playerRef.getTransform()));
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
}
