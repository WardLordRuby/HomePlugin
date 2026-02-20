package com.wardlordruby.plugin.commands;

import com.hypixel.hytale.builtin.teleport.components.TeleportHistory;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class BackCommand extends AbstractPlayerCommand {
    private final @Nonnull ComponentType<EntityStore, TeleportHistory> teleportHistoryComponentType =
        TeleportHistory.getComponentType();

    public BackCommand() {
        super("back", "Return to your previous position");
    }

    @Override
    protected void execute(
        @Nonnull CommandContext commandContext,
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull PlayerRef playerRef,
        @Nonnull World world
    ) {
        TeleportHistory history = store.getComponent(ref, teleportHistoryComponentType);

        if (history == null || history.getBackSize() == 0) {
            playerRef.sendMessage(Message.raw("Teleport history is empty"));
            return;
        }

        history.back(ref, 1);
    }
}
