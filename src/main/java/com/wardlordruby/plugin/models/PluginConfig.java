package com.wardlordruby.plugin.models;

import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Stream;

import javax.annotation.Nullable;

public final class PluginConfig {
    public static final short MAX_HOMES = 100;

    public Modules enabledModules = new Modules();
    public HomeConfig homeConfig = new HomeConfig();
    public BackConfig backConfig = new BackConfig();
    public TeleportConfig teleportConfig = new TeleportConfig();

    public final class Modules {
        public boolean home = true;
        public boolean back = true;
    }

    public final class HomeConfig {
        public boolean adminCommands = true;
        public HashSet<String> bannedHomeWorlds = new HashSet<>();
        public short baseHomeCount = 1;
        public Short[] homeCountByRank = {2, 4, 10};
    }

    public final class BackConfig {
        public boolean backOnDeath = true;
    }

    public final class TeleportConfig {
        public double tpHistoryMinDistance = 20;
    }

    public static @Nullable String validator(PluginConfig config) {
        Stream<Number> unsigned = Stream.concat(
            Stream.of(
                config.homeConfig.baseHomeCount,
                config.teleportConfig.tpHistoryMinDistance
            ),
            Arrays.stream(config.homeConfig.homeCountByRank)
        );

        if (unsigned.anyMatch(num -> num.doubleValue() < 0)) {
            return "Found negative number";
        }

        Stream<Number> capped = Stream.concat(
            Stream.of(config.homeConfig.baseHomeCount),
            Arrays.stream(config.homeConfig.homeCountByRank)
        );

        if (capped.anyMatch(num -> num.doubleValue() > MAX_HOMES)) {
            return "Found home count larger than maximum allowed (%s)".formatted(MAX_HOMES);
        }

        for (String world : config.homeConfig.bannedHomeWorlds) {
            if (world.isEmpty()) return "Found empty string in banned home world list";
        }

        return null;
    }
}
