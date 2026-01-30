package com.wardlordruby.plugin.utils;

import com.hypixel.hytale.builtin.teleport.components.TeleportHistory;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public final class TeleportHistoryUtil {
    public static void append(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull ComponentAccessor<EntityStore> storeAccessor,
        @Nonnull World world,
        @Nonnull Transform transform
    ) {
        TeleportHistory history = storeAccessor.getComponent(ref, TeleportHistory.getComponentType());

        if (history == null) {
            history = new TeleportHistory();
            storeAccessor.addComponent(ref, TeleportHistory.getComponentType(), history);
        }

        history.append(world, transform.getPosition().clone(), transform.getRotation().clone(), "");
    }
}
