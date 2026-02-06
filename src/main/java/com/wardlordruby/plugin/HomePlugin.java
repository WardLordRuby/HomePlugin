package com.wardlordruby.plugin;

import com.wardlordruby.plugin.commands.BackCommand;
import com.wardlordruby.plugin.commands.HomeCommand;
import com.wardlordruby.plugin.commands.PluginManagementCommandCollection;
import com.wardlordruby.plugin.managers.PlayerHomeManager;
import com.wardlordruby.plugin.models.JsonResource;
import com.wardlordruby.plugin.models.PluginConfig;
import com.wardlordruby.plugin.services.JsonStorageService;
import com.wardlordruby.plugin.systems.StoreDeathLocationSystem;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class HomePlugin extends JavaPlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String HOME_MODULE_REQUIRED = "`PlayerHomeManager` is only used when module enabled";
    private static final String PLUGIN_NAME = HomePlugin.class.getSimpleName();

    @SuppressWarnings("null") // Read will either throw or return a Nonnull
    private static @Nonnull PluginConfig config;
    @SuppressWarnings("null") // getDataDirectory nor JsonStorageService will return a Nonnull
    private static @Nonnull JsonStorageService fileManager;

    private final @Nullable PlayerHomeManager playerHomes;

    public HomePlugin(@Nonnull JavaPluginInit init) {
        super(init);
        fileManager = new JsonStorageService(getDataDirectory());
        config = fileManager.read(JsonResource.CONFIG);
        this.playerHomes = config.enabledModules.home ? new PlayerHomeManager(fileManager.read(JsonResource.HOMES)) : null;
    }

    @Override
    protected void setup() {
        super.setup();
        this.getCommandRegistry().registerCommand(new PluginManagementCommandCollection());
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
        if (playerHomes != null) playerHomes.write(fileManager);
        fileManager.write(config, JsonResource.CONFIG);
        super.shutdown();
    }

    @SuppressWarnings("null")
    public static @Nonnull Message formatPlayerMessage(@Nonnull String msg) {
        return Message.raw("[%s] %s".formatted(PLUGIN_NAME, msg));
    }

    public static @Nonnull JsonStorageService getFileManager() {
        return fileManager;
    }

    public static @Nonnull PluginConfig getConfig() {
        return config;
    }

    public static void setConfig(@Nonnull PluginConfig updated) {
        config = updated;
    }
}
