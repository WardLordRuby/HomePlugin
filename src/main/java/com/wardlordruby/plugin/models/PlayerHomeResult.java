package com.wardlordruby.plugin.models;

import javax.annotation.Nonnull;

public sealed interface PlayerHomeResult {
    @Nonnull String display();
    @Nonnull String displayForOther(@Nonnull String username);

    default boolean isSuccess() {
        return this instanceof Success<?>;
    }

    default boolean isError() {
        return !isSuccess();
    }

    record Success<T>(@Nonnull T value) implements PlayerHomeResult {
        @SuppressWarnings("null")
        public @Nonnull String display() {
            return switch (value) {
                case String s -> s;
                default -> value.toString();
            };
        }

        public @Nonnull String displayForOther(@Nonnull String username) {
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

        public @Nonnull String displayForOther(@Nonnull String username) {
            throw new IllegalStateException();
        }
    }

    record NoSetHomes() implements PlayerHomeResult {
        public @Nonnull String display() {
            return "You have no homes set!";
        }

        @SuppressWarnings("null")
        public @Nonnull String displayForOther(@Nonnull String username) {
            return "%s has no set homes".formatted(username);
        }
    }

    record HomeNotFound(@Nonnull String homeName) implements PlayerHomeResult {
        @SuppressWarnings("null")
        public @Nonnull String display() {
            return "No home with name '%s' exists".formatted(homeName);
        }

        @SuppressWarnings("null")
        public @Nonnull String displayForOther(@Nonnull String username) {
            return "No home with name '%s' exists for user: %s".formatted(homeName, username);
        }
    }

    record AlreadyDefault(@Nonnull String homeName) implements PlayerHomeResult {
        @SuppressWarnings("null")
        public @Nonnull String display() {
            return "Home '%s' is already default".formatted(homeName);
        }

        public @Nonnull String displayForOther(@Nonnull String username) {
            throw new IllegalStateException();
        }
    }

    record MaxHomesReached(int curr, int max) implements PlayerHomeResult {
        public @Nonnull String display() {
            return switch (Integer.compare(curr, max)) {
                case 0 -> "You've reached the maximum number of homes (" + max + ")";
                case 1 -> "You've exceeded the maximum number of homes by " + (curr - max)
                          + "\nYou will not be able to set or update your homes until you are in compliance";
                default -> throw new IllegalStateException();
            };
        }

        public @Nonnull String displayForOther(@Nonnull String username) {
            throw new IllegalStateException();
        }
    }
}
