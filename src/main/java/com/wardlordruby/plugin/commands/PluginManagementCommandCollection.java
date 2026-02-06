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
                String successMsg = "'%s' reloaded!".formatted(JsonResource.CONFIG.fileName());
                context.sendMessage(HomePlugin.formatPlayerMessage(successMsg));
                HomePlugin.LOGGER.atInfo().log(successMsg);
            }).exceptionally(ex -> {
                Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
                HomePlugin.LOGGER.atWarning().log("Failed to read '%s'. %s", JsonResource.CONFIG.fileName(), cause.getMessage());
                return null;
            });
        }
    }
}
