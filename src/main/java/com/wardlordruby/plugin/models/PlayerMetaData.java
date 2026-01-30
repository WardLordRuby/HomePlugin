package com.wardlordruby.plugin.models;

import com.hypixel.hytale.server.core.auth.ProfileServiceClient.PublicGameProfile;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;

import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nonnull;

public sealed interface PlayerMetaData {
    @Nonnull UUID getUuid();
    @Nonnull String getUsername();

    /// Must be called from within a method that will catch `NullPointerExceptions`
    public static @Nonnull PlayerMetaData fromSender(@Nonnull CommandSender sender) {
        return new Sender(Objects.requireNonNull(sender.getUuid()), sender);
    }

    /// Must be called from within a method that will catch `NullPointerExceptions`
    public static @Nonnull PlayerMetaData fromProfileArg(
        @Nonnull CommandContext context,
        @Nonnull RequiredArg<PublicGameProfile> playerProfileArg
    ) {
        @Nonnull PublicGameProfile targetPlayerProfile = Objects.requireNonNull(context.get(playerProfileArg));
        return new Profile(
            Objects.requireNonNull(targetPlayerProfile.getUuid()),
            Objects.requireNonNull(targetPlayerProfile)
        );
    }

    record Sender(@Nonnull UUID id, @Nonnull CommandSender sender) implements PlayerMetaData {
        public @Nonnull UUID getUuid() { return id; }
        public @Nonnull String getUsername() { return Objects.requireNonNull(sender.getDisplayName()); }
    }

    record Profile(@Nonnull UUID id, @Nonnull PublicGameProfile profile) implements PlayerMetaData {
        public @Nonnull UUID getUuid() { return id; }
        public @Nonnull String getUsername() { return Objects.requireNonNull(profile.getUsername()); }
    }
}
