package com.wardlordruby.plugin.models;

import com.hypixel.hytale.math.vector.Location;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;

import javax.annotation.Nonnull;

public class HomeLocation extends Location {
    public final @Nonnull String id;

    public HomeLocation(@Nonnull String id, @Nonnull String world, @Nonnull Transform transform) {
        super(world, transform.getPosition().clone(), transform.getRotation().clone());
        this.id = id;
    }

    @Override
    public void setPosition(@Nonnull Vector3d position) {
        this.position = position.clone();
    }

    @Override
    public void setRotation(@Nonnull Vector3f rotation) {
        this.rotation = rotation.clone();
    }

    public void update(@Nonnull String world, @Nonnull Transform transform) {
        this.world = world;
        setPosition(transform.getPosition());
        setRotation(transform.getRotation());
    }

    @SuppressWarnings("null")
    public @Nonnull String display() {
        return "- '%s' is located in world '%s' at x: %.0f, y: %.0f, z: %.0f"
            .formatted(id, world, position.x, position.y, position.z);
    }
}
