package com.wardlordruby.plugin.models;

public final class PluginConfig {
    public Modules enabledModules = new Modules();
    public HomeConfig homeConfig = new HomeConfig();
    public BackConfig backConfig = new BackConfig();

    public final class Modules {
        public boolean offlinePlayerCache = true;
        public boolean home = true;
        public boolean back = true;
    }

    public final class HomeConfig {
        public boolean adminCommands = true;
        public short baseHomeCount = 1;
        public int[] homeCountByRank = {2, 4, 10};
    }

    public final class BackConfig {
        public boolean backOnDeath = true;
    }
}
