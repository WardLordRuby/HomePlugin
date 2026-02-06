package com.wardlordruby.plugin.models;

import com.wardlordruby.plugin.HomePlugin;

import com.hypixel.hytale.server.core.universe.Universe;

import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;

public sealed interface PlayerHomeResult {
    @Nonnull String display();
    @Nonnull String displayForOther(@Nonnull PlayerMetaData player);

    default boolean isSuccess() {
        return this instanceof Success<?>;
    }

    default boolean isError() {
        return !isSuccess();
    }

    @SuppressWarnings("null")
    default @Nonnull String listFmt(boolean verbose) {
        return verbose ? "Available homes:\n%s".formatted(this.display()) : "Available homes: [%s]".formatted(this.display());
    }

    @SuppressWarnings("null")
    default @Nonnull String listFmtOther(@Nonnull PlayerMetaData player, boolean verbose) {
        return verbose
            ? "%s's homes:\n%s".formatted(player.getUsername(), this.display())
            : "%s's set homes: [%s]".formatted(player.getUsername(), this.display());
    }

    record Success<T>(@Nonnull T value) implements PlayerHomeResult {
        @SuppressWarnings("null")
        public @Nonnull String display() {
            return switch (value) {
                case String s -> s;
                default -> value.toString();
            };
        }

        public @Nonnull String displayForOther(@Nonnull PlayerMetaData player) {
            throw new IllegalStateException();
        }

        public @Nonnull T get() {
            return value;
        }
    }

    record IllegalWorld() implements PlayerHomeResult {
        public @Nonnull String display() {
            return "You can not set your home in temporary worlds";
        }

        public @Nonnull String displayForOther(@Nonnull PlayerMetaData player) {
            throw new IllegalStateException();
        }
    }

    record NoSetHomes() implements PlayerHomeResult {
        public @Nonnull String display() {
            return "You have no homes set!";
        }

        @SuppressWarnings("null")
        public @Nonnull String displayForOther(@Nonnull PlayerMetaData player) {
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
        public @Nonnull String display() {
            return "No home with name '%s' exists".formatted(homeName);
        }

        @SuppressWarnings("null")
        public @Nonnull String displayForOther(@Nonnull PlayerMetaData player) {
            return "No home with name '%s' exists for user: %s".formatted(homeName, player.getUsername());
        }
    }

    record AlreadyDefault(@Nonnull String homeName) implements PlayerHomeResult {
        @SuppressWarnings("null")
        public @Nonnull String display() {
            return "Home '%s' is already default".formatted(homeName);
        }

        public @Nonnull String displayForOther(@Nonnull PlayerMetaData player) {
            throw new IllegalStateException();
        }
    }

    record MaxHomesReached(int curr, int max) implements PlayerHomeResult {
        @SuppressWarnings("null")
        public @Nonnull String display() {
            return switch (Integer.compare(curr, max)) {
                case 0 -> "You've reached the maximum number of homes (%s)".formatted(max);
                case 1 -> """
                    You've exceeded the maximum number of homes by (%s)
                    You will not be able to set or update your homes until you are in compliance\
                    """.formatted(curr - max);
                default -> throw new IllegalStateException();
            };
        }

        public @Nonnull String displayForOther(@Nonnull PlayerMetaData player) {
            throw new IllegalStateException();
        }
    }
}
