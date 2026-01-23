package com.wardlordruby.plugin.models;

import java.lang.reflect.Type;
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

    public static final JsonResource<HomeMap> HOMES =
        new JsonResource<>(
            "homes.json",
            "Player homes",
            HomeMap.class,
            HomeMap::new
        );

    public static final JsonResource<PluginConfig> CONFIG =
        new JsonResource<>(
            "config.json",
            "Plugin config",
            PluginConfig.class,
            PluginConfig::new
        );
}
