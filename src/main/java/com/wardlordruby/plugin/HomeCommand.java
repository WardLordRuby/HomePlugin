package com.wardlordruby.plugin;

import com.hypixel.hytale.builtin.teleport.components.TeleportHistory;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
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
    private final HomePlugin plugin;

    public HomeCommand(HomePlugin parent) {
        super("home", "Teleport back to your home");
        this.addSubCommand(new SetHomeCommand());
        this.plugin = parent;
    }

    @Override
    protected void execute(
        @Nonnull CommandContext commandContext,
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull PlayerRef playerRef,
        @Nonnull World world
    ) {
        TeleportEntry playerHome = plugin.getPlayerHome(playerRef);

        if (playerHome == null) {
            playerRef.sendMessage(Message.raw("Use `/home set` to update your home location"));
            return;
        }

        World targetWorld = Universe.get().getWorld(playerHome.world);

        if (targetWorld == null) {
            playerRef.sendMessage(Message.translation("server.commands.teleport.worldNotLoaded"));
            return;
        }
        
        TeleportHistory history = store.getComponent(ref, TeleportHistory.getComponentType());

        if (history == null) {
            history = new TeleportHistory();
            store.addComponent(ref, TeleportHistory.getComponentType(), history);
        }

        Transform currTransform = playerRef.getTransform();
        history.append(world, currTransform.getPosition().clone(), currTransform.getRotation().clone(), "");
        store.addComponent(ref, Teleport.getComponentType(), Teleport.createForPlayer(targetWorld, playerHome.position, playerHome.rotation));
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
            plugin.insertHome(world, playerRef);
            playerRef.sendMessage(Message.raw("Home set!"));
        }
    }
}

