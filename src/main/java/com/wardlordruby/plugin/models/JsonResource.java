package com.wardlordruby.plugin.models;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final record JsonResource<T>(
    @Nonnull String fileName,
    @Nonnull String displayName,
    Type type,
    @Nonnull Supplier<T> defaultValue,
    @Nullable Function<T, String> validator
) {
    public @Nonnull T createDefault() {
        T value = defaultValue.get();
        if (value == null) {
            throw new AssertionError("Default supplier for %s returned null".formatted(fileName));
        }
        return value;
    }

    public static final JsonResource<HomeMap> HOMES =
        new JsonResource<>(
            "homes.json",
            "Player homes",
            new TypeToken<HomeMap>() {}.getType(),
            HomeMap::new,
            HomeMap::validator
        );

    public static final JsonResource<PluginConfig> CONFIG =
        new JsonResource<>(
            "config.json",
            "Plugin config",
            new TypeToken<PluginConfig>() {}.getType(),
            PluginConfig::new,
            PluginConfig::validator
        );
}
