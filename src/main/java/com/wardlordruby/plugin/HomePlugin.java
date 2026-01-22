package com.wardlordruby.plugin;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class HomePlugin extends JavaPlugin {
    public static final HytaleLogger logger = HytaleLogger.get(HomePlugin.class.getSimpleName());

    private static final String TMP_WORLD_INDICATOR = "instance-";

    private final JsonFileManager fileManager;
    private final @Nonnull ConcurrentHashMap<UUID, TeleportEntry> homeMap;

    public HomePlugin(@Nonnull JavaPluginInit init) {
        super(init);
        this.fileManager = new JsonFileManager(getDataDirectory());
        this.homeMap = fileManager.read(JsonResource.HOMES);
    }

    @Override
    protected void setup() {
        super.setup();
        this.getCommandRegistry().registerCommand(new HomeCommand(this));
        this.getCommandRegistry().registerCommand(new BackCommand());
    }

    @Override
    protected void shutdown() {
        fileManager.write(homeMap, JsonResource.HOMES);
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
