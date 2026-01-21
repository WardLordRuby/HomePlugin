package com.wardlordruby.plugin;

import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.World;

import javax.annotation.Nonnull;

public class TeleportEntry {
    public final @Nonnull String world;
    public final @Nonnull Vector3d position;
    public final @Nonnull Vector3f rotation;

    public TeleportEntry(@Nonnull World world, @Nonnull Transform transform) {
        this.world = world.getName();
        this.position = transform.getPosition().clone();
        this.rotation = transform.getRotation().clone();
    }
}
