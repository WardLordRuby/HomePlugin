package com.wardlordruby.plugin;

import com.wardlordruby.plugin.managers.JsonFileManager;
import com.wardlordruby.plugin.models.PluginConfig;
import com.wardlordruby.plugin.models.TeleportEntry;
import com.wardlordruby.plugin.models.JsonResource;
import com.wardlordruby.plugin.commands.HomeCommand;
import com.wardlordruby.plugin.commands.BackCommand;
import com.wardlordruby.plugin.systems.StoreDeathLocationSystem;

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
    public static final String NAME = HomePlugin.class.getSimpleName();
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final String TMP_WORLD_INDICATOR = "instance-";

    private static PluginConfig config;
    private final JsonFileManager fileManager;
    private final @Nonnull ConcurrentHashMap<UUID, TeleportEntry> homeMap;

    public HomePlugin(@Nonnull JavaPluginInit init) {
        super(init);
        this.fileManager = new JsonFileManager(getDataDirectory());
        this.homeMap = fileManager.read(JsonResource.HOMES);
        config = fileManager.read(JsonResource.CONFIG);
    }

    @Override
    protected void setup() {
        super.setup();
        this.getCommandRegistry().registerCommand(new HomeCommand(this));
        this.getCommandRegistry().registerCommand(new BackCommand());
        this.getEntityStoreRegistry().registerSystem(new StoreDeathLocationSystem());
    }

    @Override
    protected void shutdown() {
        fileManager.write(homeMap, JsonResource.HOMES);
        fileManager.write(getConfig(), JsonResource.CONFIG);
        super.shutdown();
    }

    public static @Nonnull PluginConfig getConfig() {
        if (config == null) {
            throw new IllegalStateException(NAME + " config not yet loaded");
        }
        return config;
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
