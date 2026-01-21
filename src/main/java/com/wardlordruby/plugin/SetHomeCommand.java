package com.wardlordruby.plugin;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class SetHomeCommand extends AbstractPlayerCommand {
    private final HomePlugin plugin;

    public SetHomeCommand(HomePlugin plugin) {
        super("set", "Set home to your current position");
        this.requirePermission("com.wardlordruby.homeplugin.command.home");
        this.plugin = plugin;
    }

    @Override
    protected void execute(
        @Nonnull CommandContext commandContext,
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull PlayerRef playerRef,
        @Nonnull World world
    ) {
        plugin.insertHome(world, playerRef);
        playerRef.sendMessage(Message.raw("Home set!"));
    }

}