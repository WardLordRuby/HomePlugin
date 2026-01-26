package com.wardlordruby.plugin.models;

import javax.annotation.Nonnull;

public final class Constants {
    private static final @Nonnull String PLUGIN = "com.wardlordruby.homeplugin";

    public static final @Nonnull String HOME_PERM = PLUGIN + ".command.home";
    public static final @Nonnull String BACK_PERM = PLUGIN + ".command.back";

    public static final @Nonnull String HOME_RANK_PERM = PLUGIN + ".config.homerank";
    public static final @Nonnull String BACK_ON_DEATH_PERM = PLUGIN + ".config.backOnDeath";
}
