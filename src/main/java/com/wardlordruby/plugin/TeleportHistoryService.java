package com.wardlordruby.plugin;

import com.hypixel.hytale.builtin.teleport.components.TeleportHistory;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public final class TeleportHistoryService {
    public static void append(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull World world,
        @Nonnull Transform transform
    ) {
        TeleportHistory history = store.getComponent(ref, TeleportHistory.getComponentType());

        if (history == null) {
            history = new TeleportHistory();
            store.addComponent(ref, TeleportHistory.getComponentType(), history);
        }

        history.append(world, transform.getPosition().clone(), transform.getRotation().clone(), "");
    }
}
