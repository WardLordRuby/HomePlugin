package com.wardlordruby.plugin.models;

import com.wardlordruby.plugin.HomePlugin;

import com.hypixel.hytale.server.core.universe.Universe;

import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public sealed interface PlayerHomeResult {
    @Nonnull String display();
    @Nonnull String display(@Nullable PlayerMetaData player);

    default boolean isSuccess() {
        return this instanceof Success<?>;
    }

    default boolean isError() {
        return !isSuccess();
    }

    record Success<T>(@Nonnull T value) implements PlayerHomeResult {
        public @Nonnull String display() {
            return display(null);
        }

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

    record IllegalWorld() implements PlayerHomeResult {
        public @Nonnull String display() {
            return display(null);
        }

        public @Nonnull String display(@Nullable PlayerMetaData player) {
            if (player != null) throw new IllegalStateException();
            return "You can not set your home in temporary worlds";
        }
    }

    record NoSetHomes() implements PlayerHomeResult {
        public @Nonnull String display() {
            return display(null);
        }

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
        public @Nonnull String display() {
            return display(null);
        }

        @SuppressWarnings("null")
        public @Nonnull String display(@Nullable PlayerMetaData player) {
            return player == null
                ? "No home with name '%s' exists".formatted(homeName)
                : "No home with name '%s' exists for user: %s".formatted(homeName, player.getUsername());
        }
    }

    record AlreadyDefault(@Nonnull String homeName) implements PlayerHomeResult {
        public @Nonnull String display() {
            return display(null);
        }

        @SuppressWarnings("null")
        public @Nonnull String display(@Nullable PlayerMetaData player) {
            if (player != null) throw new IllegalStateException();
            return "Home '%s' is already default".formatted(homeName);
        }
    }

    record MaxHomesReached(int curr, int max) implements PlayerHomeResult {
        public @Nonnull String display() {
            return display(null);
        }

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
