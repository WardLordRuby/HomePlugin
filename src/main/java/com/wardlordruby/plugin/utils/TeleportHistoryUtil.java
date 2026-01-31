package com.wardlordruby.plugin.utils;

import com.wardlordruby.plugin.HomePlugin;

import com.hypixel.hytale.builtin.teleport.components.TeleportHistory;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.math.vector.Vector3d;

import java.util.Deque;
import java.lang.reflect.Field;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class TeleportHistoryUtil {
    public static void append(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull ComponentAccessor<EntityStore> storeAccessor,
        @Nonnull World world,
        @Nonnull Transform transform,
        @Nullable String nextWorld,
        @Nullable Vector3d nextPosition
    ) {
        TeleportHistory history = storeAccessor.getComponent(ref, TeleportHistory.getComponentType());
        Vector3d playerPosition = transform.getPosition();

        double historyMinDistance = HomePlugin.getConfig().teleportConfig.tpHistoryMinDistance;

        if (historyMinDistance > 0) {
            if (nextPosition != null && nextWorld != null) {
                if (playerWithinThreshold(world.getName(), playerPosition, nextWorld, nextPosition, historyMinDistance)) return;
            } else {
                TeleportHistory.Waypoint lastEntry = getLastWaypoint(history);
                if (lastEntry != null) {
                    Vector3d lastTpPos = getDataByField(lastEntry, "position");
                    String lastTpWorldName = getDataByField(lastEntry, "world");

                    if (lastTpPos != null
                        && lastTpWorldName != null
                        && playerWithinThreshold(world.getName(), playerPosition, lastTpWorldName, lastTpPos, historyMinDistance))
                    {
                        return;
                    }
                }
            }
        }

        if (history == null) {
            history = new TeleportHistory();
            storeAccessor.addComponent(ref, TeleportHistory.getComponentType(), history);
        }

        history.append(world, playerPosition.clone(), transform.getRotation().clone(), "");
    }

    private static boolean playerWithinThreshold(
        @Nonnull String worldA,
        @Nonnull Vector3d posA,
        @Nonnull String worldB,
        @Nonnull Vector3d posB,
        double configuredDistance
    ) {
        return worldA.equals(worldB) && posA.distanceTo(posB) <= configuredDistance;
    }

    private static @Nullable TeleportHistory.Waypoint getLastWaypoint(TeleportHistory history) {
        if (history == null || history.getBackSize() <= 0) return null;

        try {
            Field backField = TeleportHistory.class.getDeclaredField("back");
            backField.setAccessible(true);

            @SuppressWarnings("unchecked")
            var back = (Deque<TeleportHistory.Waypoint>)backField.get(history);

            return back.peek();
        } catch (Exception e) {
            HomePlugin.LOGGER.atWarning().log("Failed to access back `Deque` in `TeleportHistory`, %s", e.getMessage());
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private static @Nullable <T> T getDataByField(TeleportHistory.Waypoint waypoint, String fieldName) {
        try {
            Field field  = TeleportHistory.Waypoint.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T)field.get(waypoint);
        } catch (Exception e) {
            HomePlugin.LOGGER.atWarning().log("Failed to access the %s field in `TeleportHistory.Waypoint`, %s", fieldName, e.getMessage());
        }

        return null;
    }
}
