package com.wardlordruby.plugin.models;

import com.hypixel.hytale.math.vector.Location;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.math.vector.Transform;

import javax.annotation.Nonnull;

import org.joml.Vector3d;

public class HomeLocation extends Location {
    public final @Nonnull String id;

    public HomeLocation(@Nonnull String id, @Nonnull String world, @Nonnull Transform transform) {
        super(world, new Vector3d(transform.getPosition().x, transform.getPosition().y, transform.getPosition().z),
            transform.getRotation().clone());
        this.id = id;
    }

    @Override
    public void setPosition(@Nonnull Vector3d position) {
        this.position = new Vector3d(position.x, position.y, position.z);
    }

    @Override
    public void setRotation(@Nonnull Rotation3f rotation) {
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
