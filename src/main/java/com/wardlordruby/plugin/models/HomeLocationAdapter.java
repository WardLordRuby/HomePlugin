package com.wardlordruby.plugin.models;

import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class HomeLocationAdapter extends TypeAdapter<HomeLocation> {
    @Override
    public void write(JsonWriter out, HomeLocation entry) throws IOException {
        out.beginObject();
        out.name("id").value(entry.id);
        out.name("world").value(entry.getWorld());

        Vector3d position = entry.getPosition();
        out.name("x").value(position.x);
        out.name("y").value(position.y);
        out.name("z").value(position.z);

        Vector3f rotation = entry.getRotation();
        out.name("pitch").value(rotation.x);
        out.name("yaw").value(rotation.y);
        out.name("roll").value(rotation.z);
        out.endObject();
    }

    @Override
    public HomeLocation read(JsonReader in) throws IOException {
        String id = null;
        String world = null;
        double x = 0, y = 0, z = 0;
        float yaw = 0, pitch = 0, roll = 0;

        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "id" -> id = in.nextString();
                case "world" -> world = in.nextString();
                case "x" -> x = in.nextDouble();
                case "y" -> y = in.nextDouble();
                case "z" -> z = in.nextDouble();
                case "pitch" -> pitch = (float)in.nextDouble();
                case "yaw" -> yaw = (float)in.nextDouble();
                case "roll" -> roll = (float)in.nextDouble();
                default -> in.skipValue();
            }
        }
        in.endObject();

        Vector3d position = new Vector3d(x, y, z);
        Vector3f rotation = new Vector3f(pitch, yaw, roll);
        Transform transform = new Transform(position, rotation);

        if (id == null) throw new IllegalStateException("found id set as null");
        if (world == null) throw new IllegalStateException("found world set as null");

        return new HomeLocation(id, world, transform);
    }
}
