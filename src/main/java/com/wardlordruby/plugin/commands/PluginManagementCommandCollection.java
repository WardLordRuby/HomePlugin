package com.wardlordruby.plugin.commands;

import com.wardlordruby.plugin.HomePlugin;
import com.wardlordruby.plugin.models.JsonResource;
import com.wardlordruby.plugin.services.JsonStorageService;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import javax.annotation.Nonnull;

public class PluginManagementCommandCollection extends AbstractCommandCollection {
    public PluginManagementCommandCollection() {
        super("homeplugin", "HomePlugin admin commands");
        this.addSubCommand(new ReloadCommand());
    }

    private class ReloadCommand extends AbstractAsyncCommand {
        public ReloadCommand() {
            super("reload", "Reloads the config\nChanges to active modules will require a server restart");
        }

        @SuppressWarnings("null")
        @Override
        protected @Nonnull CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
            JsonStorageService fileManager = HomePlugin.getFileManager();

            return CompletableFuture.supplyAsync(() -> {
                return fileManager.read(JsonResource.CONFIG);
            }).thenAccept(config -> {
                HomePlugin.setConfig(config);
                String successMsg = "'%s' reloaded!".formatted(JsonResource.CONFIG.displayName());
                HomePlugin.LOGGER.atInfo().log(successMsg);
                context.sendMessage(HomePlugin.formatPlayerMessage(successMsg));
            }).exceptionally(ex -> {
                Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
                String errMessage = "Failed to reload '%s'. %s".formatted(JsonResource.CONFIG.fileName(), cause.getMessage());
                HomePlugin.LOGGER.atSevere().log(errMessage);
                context.sendMessage(HomePlugin.formatPlayerMessage(errMessage));
                return null;
            });
        }
    }
}
