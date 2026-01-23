package com.wardlordruby.plugin.commands;

import com.wardlordruby.plugin.models.TeleportEntry;
import com.wardlordruby.plugin.managers.PlayerHomeManager;
import com.wardlordruby.plugin.utils.TeleportHistoryUtil;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class HomeCommand extends AbstractPlayerCommand {
    private final @Nonnull PlayerHomeManager playerHomes;

    public HomeCommand(@Nonnull PlayerHomeManager homeManager) {
        super("home", "Teleport back to your home");
        this.addSubCommand(new SetHomeCommand());
        this.playerHomes = homeManager;
    }

    @Override
    protected void execute(
        @Nonnull CommandContext commandContext,
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull PlayerRef playerRef,
        @Nonnull World world
    ) {
        TeleportEntry playerHome = playerHomes.get(playerRef);

        if (playerHome == null) {
            playerRef.sendMessage(Message.raw("Use `/home set` to update your home location"));
            return;
        }

        World targetWorld = Universe.get().getWorld(playerHome.world);

        if (targetWorld == null) {
            playerRef.sendMessage(Message.translation("server.commands.teleport.worldNotLoaded"));
            return;
        }

        TeleportHistoryUtil.append(ref, store, world, playerRef.getTransform());
        Teleport playerTeleport = Teleport.createForPlayer(targetWorld, playerHome.position, playerHome.rotation);
        store.addComponent(ref, Teleport.getComponentType(), playerTeleport);
    }

    private class SetHomeCommand extends AbstractPlayerCommand {
        public SetHomeCommand() {
            super("set", "Set home to your current position");
            this.requirePermission("com.wardlordruby.homeplugin.command.home");
        }

        @Override
        protected void execute(
            @Nonnull CommandContext commandContext,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
        ) {
            String err = playerHomes.insert(world, playerRef);

            if (err != null) {
                playerRef.sendMessage(Message.raw(err));
                return;
            }

            playerRef.sendMessage(Message.raw("Home set!"));
        }
    }
}
