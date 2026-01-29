package com.wardlordruby.plugin.models;

import javax.annotation.Nonnull;

public final class Permissions {
    private static final @Nonnull String PLUGIN = "com.wardlordruby.homeplugin";

    public static final @Nonnull String HOME = PLUGIN + ".command.home";
    public static final @Nonnull String HOME_OTHERS = HOME + ".others";
    public static final @Nonnull String BACK = PLUGIN + ".command.back";

    public static final @Nonnull String HOME_RANK = PLUGIN + ".config.homerank";
    public static final @Nonnull String BACK_ON_DEATH = PLUGIN + ".config.backOnDeath";
}
