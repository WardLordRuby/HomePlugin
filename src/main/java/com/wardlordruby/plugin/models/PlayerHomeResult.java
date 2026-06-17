package com.wardlordruby.plugin.models;

import com.wardlordruby.plugin.HomePlugin;
import com.wardlordruby.plugin.managers.PlayerHomeManager;

import com.hypixel.hytale.server.core.universe.Universe;

import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public sealed interface PlayerHomeResult {
    static final @Nonnull String HELP_MSG_SET_HOME = "Use `/home set <NAME>` to update your home location";

    @Nonnull String display(@Nullable PlayerMetaData player);

    default @Nonnull String display() {
        return display(null);
    }

    @SuppressWarnings("null")
    private @Nonnull String displayErrorWithString(@Nonnull Supplier<String> additional, @Nullable PlayerMetaData player) {
        if (this.isSuccess()) throw new IllegalStateException();
        String error = this.display(player);
        String rest = additional.get();

        return rest != null
            ? "%s\n%s".formatted(error, rest)
            : error;
    }

    private @Nonnull String displayErrorWithString(@Nonnull Supplier<String> additional) {
        return displayErrorWithString(additional, null);
    }

    default @Nonnull String displayErrorWithHomeContext(@Nonnull PlayerHomeManager playerHomes, @Nonnull UUID playerID) {
        return displayErrorWithString(() -> this instanceof PlayerHomeResult.NoSetHomes
            ? HELP_MSG_SET_HOME
            : playerHomes.list(playerID).display());
    }

    default @Nonnull String displayErrorWithSpecificHomeContext(@Nonnull PlayerHomeManager playerHomes, @Nonnull PlayerMetaData player) {
        return displayErrorWithString(
            () -> this instanceof PlayerHomeResult.HomeNotFound
                ? playerHomes.list(player.getUuid()).display(player)
                : null,
            player
        );
    }

    default @Nonnull String displayErrorWithSetHomeMsg() {
        return displayErrorWithString(() -> HELP_MSG_SET_HOME);
    }

    default boolean isSuccess() {
        return this instanceof Success<?>;
    }

    default boolean isError() {
        return !isSuccess();
    }

    record Success<T>(@Nonnull T value) implements PlayerHomeResult {
        @SuppressWarnings("null")
        public @Nonnull String display(@Nullable PlayerMetaData player) {
            if (player != null && !(value instanceof List)) throw new IllegalStateException();

            return switch (value) {
                case String s -> s;
                case List format -> {
                    String predicate = player == null ? "Available" : "%s's".formatted(player.getUsername());
                    yield format.verbose
                        ? "%s homes:\n%s".formatted(predicate, format.list)
                        : "%s homes: [%s]".formatted(predicate, format.list);
                }
                default -> value.toString();
            };
        }

        public @Nonnull T get() {
            return value;
        }
    }

    record List(@Nonnull String list, boolean verbose) {}

    record IllegalWorld(@Nullable String world) implements PlayerHomeResult {
        public static @Nonnull IllegalWorld temporary() {
            return new IllegalWorld(null);
        }

        public static @Nonnull IllegalWorld banned(@Nonnull String world) {
            return new IllegalWorld(world);
        }

        @SuppressWarnings("null")
        public @Nonnull String display(@Nullable PlayerMetaData player) {
            if (player != null) throw new IllegalStateException();

            return world == null
                ? "You can not set your home in temporary worlds"
                : "You can not set your home in the banned world '%s'".formatted(world);
        }
    }

    record NoSetHomes() implements PlayerHomeResult {
        @SuppressWarnings("null")
        public @Nonnull String display(@Nullable PlayerMetaData player) {
            if (player == null) return "You have no homes set!";

            try {
                Set<UUID> seenPlayers = Universe.get().getPlayerStorage().getPlayers();
                UUID playerID = player.getUuid();
                if (!seenPlayers.contains(playerID)) {
                    return "'%s' (%s) has never previously connected".formatted(player.getUsername(), playerID);
                }
            } catch (Exception e) {
                HomePlugin.LOGGER.atSevere().log(e.getLocalizedMessage());
            }

            return "%s has no set homes".formatted(player.getUsername());
        }
    }

    record HomeNotFound(@Nonnull String homeName) implements PlayerHomeResult {
        @SuppressWarnings("null")
        public @Nonnull String display(@Nullable PlayerMetaData player) {
            return player == null
                ? "No home with name '%s' exists".formatted(homeName)
                : "No home with name '%s' exists for user: %s".formatted(homeName, player.getUsername());
        }
    }

    record AlreadyDefault(@Nonnull String homeName) implements PlayerHomeResult {
        @SuppressWarnings("null")
        public @Nonnull String display(@Nullable PlayerMetaData player) {
            if (player != null) throw new IllegalStateException();
            return "Home '%s' is already default".formatted(homeName);
        }
    }

    record MaxHomesReached(int curr, int max) implements PlayerHomeResult {
        @SuppressWarnings("null")
        public @Nonnull String display(@Nullable PlayerMetaData player) {
            if (player != null) throw new IllegalStateException();
            return switch (Integer.compare(curr, max)) {
                case 0 -> "You've reached the maximum number of homes (%s)".formatted(max);
                case 1 -> """
                    You've exceeded the maximum number of homes by (%s)
                    You will not be able to set or update your homes until you are in compliance\
                    """.formatted(curr - max);
                default -> throw new IllegalStateException();
            };
        }
    }
}
