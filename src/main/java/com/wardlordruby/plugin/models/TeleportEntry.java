package com.wardlordruby.plugin.models;

import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;

import javax.annotation.Nonnull;

public class TeleportEntry {
    public final @Nonnull String tag;
    public @Nonnull String world;
    public @Nonnull Vector3d position;
    public @Nonnull Vector3f rotation;

    public TeleportEntry(@Nonnull String tag, @Nonnull String world, @Nonnull Transform transform) {
        this.tag = tag;
        this.world = world;
        this.position = transform.getPosition().clone();
        this.rotation = transform.getRotation().clone();
    }

    public void update(@Nonnull String world, @Nonnull Transform transform) {
        this.world = world;
        this.position = transform.getPosition().clone();
        this.rotation = transform.getRotation().clone();
    }

    @SuppressWarnings("null")
    public @Nonnull String display() {
        return "'%s' is located in world '%s' at x: %.0f, y: %.0f, z: %.0f"
            .formatted(tag, world, position.x, position.y, position.z);
    }
}
