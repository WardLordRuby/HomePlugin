package com.wardlordruby.plugin;

import com.wardlordruby.plugin.commands.BackCommand;
import com.wardlordruby.plugin.commands.HomeCommand;
import com.wardlordruby.plugin.services.JsonStorageService;
import com.wardlordruby.plugin.managers.PlayerHomeManager;
import com.wardlordruby.plugin.models.PluginConfig;
import com.wardlordruby.plugin.models.JsonResource;
import com.wardlordruby.plugin.systems.StoreDeathLocationSystem;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class HomePlugin extends JavaPlugin {
    public static final String NAME = HomePlugin.class.getSimpleName();
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String HOME_MODULE_REQUIRED = "`PlayerHomeManager` is only used when module enabled";

    @SuppressWarnings("null") // Read will either throw or return a Nonnull
    private static @Nonnull PluginConfig config;

    private final @Nonnull JsonStorageService fileManager;
    private @Nullable PlayerHomeManager playerHomes;

    public HomePlugin(@Nonnull JavaPluginInit init) {
        super(init);
        this.fileManager = new JsonStorageService(getDataDirectory());
        config = fileManager.read(JsonResource.CONFIG);
        if (config.enabledModules.home) this.playerHomes = new PlayerHomeManager(fileManager.read(JsonResource.HOMES));
    }

    @Override
    protected void setup() {
        super.setup();
        if (config.enabledModules.home) {
            HomeCommand homeCommand = new HomeCommand(Objects.requireNonNull(playerHomes, HOME_MODULE_REQUIRED));
            this.getCommandRegistry().registerCommand(homeCommand);
        }
        if (config.enabledModules.back) {
            this.getCommandRegistry().registerCommand(new BackCommand());
            this.getEntityStoreRegistry().registerSystem(new StoreDeathLocationSystem());
        }
    }

    @Override
    protected void shutdown() {
        if (config.enabledModules.home) {
            Objects.requireNonNull(playerHomes, HOME_MODULE_REQUIRED).write(fileManager);
        }
        fileManager.write(config, JsonResource.CONFIG);
        super.shutdown();
    }

    public static @Nonnull PluginConfig getConfig() {
        return config;
    }
}
