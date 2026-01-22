package com.wardlordruby.plugin.models;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

public final record JsonResource<T>(
    @Nonnull String fileName,
    @Nonnull String displayName,
    Type type,
    Supplier<T> defaultValue
) {
    public @Nonnull T createDefault() {
        T value = defaultValue.get();
        if (value == null) {
            throw new AssertionError("Default supplier for " + fileName + " returned null");
        }
        return value;
    }

    public static final JsonResource<ConcurrentHashMap<UUID, TeleportEntry>> HOMES =
        new JsonResource<>(
            "homes.json",
            "Player homes",
            new TypeToken<ConcurrentHashMap<UUID, TeleportEntry>>() {}.getType(),
            ConcurrentHashMap::new
        );

    public static final JsonResource<PluginConfig> CONFIG =
        new JsonResource<>(
            "config.json",
            "Plugin config",
            PluginConfig.class,
            PluginConfig::new
        );
}
