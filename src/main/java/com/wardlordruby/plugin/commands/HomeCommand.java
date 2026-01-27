package com.wardlordruby.plugin.commands;

import com.wardlordruby.plugin.models.PlayerHomeResult;
import com.wardlordruby.plugin.models.TeleportEntry;
import com.wardlordruby.plugin.models.Constants;
import com.wardlordruby.plugin.HomePlugin;
import com.wardlordruby.plugin.managers.PlayerHomeManager;
import com.wardlordruby.plugin.utils.TeleportHistoryUtil;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class HomeCommand extends AbstractPlayerCommand {
    private final @Nonnull PlayerHomeManager playerHomes;
    private static final @Nonnull String WORLD_NOT_LOADED = "server.commands.teleport.worldNotLoaded";
    private static final @Nonnull String[] SUBCOMMAND_NAME_OR_ALIAS = {"list", "set", "add", "default", "remove", "delete"};
    private static final @Nonnull String SET_HOME_HELP_MSG = "Use `/home set <NAME>` to update your home location";

    public HomeCommand(@Nonnull PlayerHomeManager homeManager) {
        super("home", "Teleport back to your home");
        this.playerHomes = homeManager;

        this.addSubCommand(new ListHomesCommand());
        this.addSubCommand(new SetHomeCommand());
        this.addSubCommand(new SetDefaultHomeCommand());
        this.addSubCommand(new RemoveHomeCommand());
        this.addUsageVariant(new SpecificHomeCommand());
    }

    @Override
    protected void execute(
        @Nonnull CommandContext commandContext,
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull PlayerRef playerRef,
        @Nonnull World world
    ) {
        PlayerHomeResult playerHomeRes = playerHomes.getDefault(playerRef.getUuid());

        if (playerHomeRes.isError()) {
            playerRef.sendMessage(Message.raw(SET_HOME_HELP_MSG));
            return;
        }

        TeleportEntry playerHome = (TeleportEntry)((PlayerHomeResult.Success<?>)playerHomeRes).get();
        World targetWorld = Universe.get().getWorld(playerHome.world);

        if (targetWorld == null) {
            playerRef.sendMessage(Message.translation(WORLD_NOT_LOADED));
            return;
        }

        TeleportHistoryUtil.append(ref, store, world, playerRef.getTransform());
        Teleport playerTeleport = Teleport.createForPlayer(targetWorld, playerHome.position, playerHome.rotation);
        store.addComponent(ref, Teleport.getComponentType(), playerTeleport);
    }

    private static String parseHostNameArg(
        @Nonnull RequiredArg<String> arg,
        @Nonnull CommandContext context
    ) {
        String input = Objects.requireNonNull(context.get(arg));
        for (String quote : new String[]{"'", "\""}) {
            if (input.startsWith(quote) && input.endsWith(quote)) {
                input = input.substring(1, input.length() - 1);
                break;
            }
        }
        return input;
    }

    /// return value indicates success (`null`), or error with a message
    private static @Nullable String validateHostName(String hostName) {
        if (hostName.isEmpty()) {
            return "Home name can not be empty";
        }
        for (String name : SUBCOMMAND_NAME_OR_ALIAS) {
            if (hostName.equals(name)) {
                return "Can not set home name the same as a subcommand";
            }
        }
        return null;
    }

    private static Void logException(Throwable ex) {
        HomePlugin.LOGGER.atWarning().log(ex.getMessage());
        return null;
    }

    private record ValidateHomeNameString(
        @Nonnull UUID senderID,
        @Nonnull PlayerRef playerRef,
        @Nonnull String homeName
    ) {
        /// Must be called from within a method that will catch `NullPointerExceptions`
        public static @Nullable ValidateHomeNameString from(
            @Nonnull CommandContext context,
            @Nonnull RequiredArg<String> homeNameArg
        ) {
            UUID senderID = Objects.requireNonNull(context.sender().getUuid());
            PlayerRef playerRef = Objects.requireNonNull(Universe.get().getPlayer(senderID));

            String homeName = Objects.requireNonNull(parseHostNameArg(homeNameArg, context));
            String homeNameErr = validateHostName(homeName);

            if (homeNameErr != null) {
                playerRef.sendMessage(Message.raw(homeNameErr));
                return null;
            }

            return new ValidateHomeNameString(senderID, playerRef, homeName);
        }
    }

    /// Only to be used with `PlayerHomeManager` methods that have a return value of `Success<String>`
    private static CompletableFuture<Void> executeOnPlayerManager(
        @Nonnull CommandContext context,
        @Nonnull RequiredArg<String> homeNameArg,
        @Nonnull PlayerHomeManager playerHomes,
        @Nonnull BiFunction<String, UUID, PlayerHomeResult> modifyFn
    ) {
        ValidateHomeNameString meta = ValidateHomeNameString.from(context, homeNameArg);
        if (meta == null) return CompletableFuture.completedFuture(null);

        PlayerHomeResult result = modifyFn.apply(meta.homeName, meta.senderID);
        meta.playerRef.sendMessage(Message.raw(result.display()));

        if (result.isError()) {
            meta.playerRef.sendMessage(Message.raw((result instanceof PlayerHomeResult.NoSetHomes
                ? SET_HOME_HELP_MSG
                : playerHomes.list(meta.senderID, false).display())
            ));
        }

        return CompletableFuture.completedFuture(null);
    }

    private class SpecificHomeCommand extends AbstractAsyncCommand {
        private final @Nonnull RequiredArg<String> homeNameArg;

        @SuppressWarnings("null")
        public SpecificHomeCommand() {
            super("Teleport to a specific home");
            this.homeNameArg = withRequiredArg("name", "Name of your home", ArgTypes.STRING);
        }

        @SuppressWarnings("null")
        @Override
        protected @Nonnull CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
            return executeTeleport(context).exceptionally(HomeCommand::logException);
        }

        private CompletableFuture<Void> executeTeleport(@Nonnull CommandContext context) {
            ValidateHomeNameString meta = ValidateHomeNameString.from(context, homeNameArg);
            if (meta == null) return CompletableFuture.completedFuture(null);

            PlayerHomeResult playerHomeRes = playerHomes.get(meta.homeName, meta.senderID);

            if (playerHomeRes.isError()) {
                meta.playerRef.sendMessage(Message.raw((playerHomeRes.display() + "\n" +
                    (playerHomeRes instanceof PlayerHomeResult.NoSetHomes
                        ? SET_HOME_HELP_MSG
                        : playerHomes.list(meta.senderID, false).display())
                )));
                return CompletableFuture.completedFuture(null);
            }

            TeleportEntry playerHome = (TeleportEntry)((PlayerHomeResult.Success<?>)playerHomeRes).get();
            World targetWorld = Universe.get().getWorld(playerHome.world);

            if (targetWorld == null) {
                meta.playerRef.sendMessage(Message.translation(WORLD_NOT_LOADED));
                return CompletableFuture.completedFuture(null);
            }

            UUID worldID = Objects.requireNonNull(meta.playerRef.getWorldUuid());
            World world = Objects.requireNonNull(Universe.get().getWorld(worldID));

            Ref<EntityStore> ref = Objects.requireNonNull(context.senderAsPlayerRef());

            world.execute(() -> {
                Store<EntityStore> store = ref.getStore();
                TeleportHistoryUtil.append(ref, store, world, meta.playerRef.getTransform());
                Teleport playerTeleport = Teleport.createForPlayer(targetWorld, playerHome.position, playerHome.rotation);
                store.addComponent(ref, Teleport.getComponentType(), playerTeleport);
            });

            return CompletableFuture.completedFuture(null);
        }
    }

    private class ListHomesCommand extends AbstractAsyncCommand {
        private final @Nonnull FlagArg verboseArg;

        public ListHomesCommand() {
            super("list", "List all your saved homes");
            this.verboseArg = withFlagArg("verbose", "Show detailed home information");
            requirePermission(Constants.HOME_PERM);
        }

        @SuppressWarnings("null")
        @Override
        protected @Nonnull CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
            return executeListHomes(context).exceptionally(HomeCommand::logException);
        }

        private CompletableFuture<Void> executeListHomes(@Nonnull CommandContext context) {
            UUID senderID = Objects.requireNonNull(context.sender().getUuid());
            PlayerRef playerRef = Objects.requireNonNull(Universe.get().getPlayer(senderID));
            boolean verbose = verboseArg.get(context);

            PlayerHomeResult result = playerHomes.list(senderID, verbose);
            playerRef.sendMessage(Message.raw(result.display()));

            return CompletableFuture.completedFuture(null);
        }
    }

    private class SetHomeCommand extends AbstractAsyncCommand {
        private final @Nonnull RequiredArg<String> homeNameArg;

        @SuppressWarnings("null")
        public SetHomeCommand() {
            super("set", "Set home to your current position");
            this.homeNameArg = withRequiredArg("name", "Name of your home", ArgTypes.STRING);
            requirePermission(Constants.HOME_PERM);
            addAliases("add");
        }

        @SuppressWarnings("null")
        @Override
        protected @Nonnull CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
            return executeSetHome(context).exceptionally(HomeCommand::logException);
        }

        private CompletableFuture<Void> executeSetHome(@Nonnull CommandContext context) {
            ValidateHomeNameString meta = ValidateHomeNameString.from(context, homeNameArg);
            if (meta == null) return CompletableFuture.completedFuture(null);

            UUID worldID = Objects.requireNonNull(meta.playerRef.getWorldUuid());
            World world = Objects.requireNonNull(Universe.get().getWorld(worldID));

            PlayerHomeResult result = playerHomes.insert(meta.homeName, world, meta.playerRef);
            meta.playerRef.sendMessage(Message.raw(result.display()));

            return CompletableFuture.completedFuture(null);
        }
    }

    private class SetDefaultHomeCommand extends AbstractAsyncCommand {
        private final @Nonnull RequiredArg<String> homeNameArg;

        @SuppressWarnings("null")
        public SetDefaultHomeCommand() {
            super("default", "Set a saved home as default");
            this.homeNameArg = withRequiredArg("name", "Name of your home", ArgTypes.STRING);
            requirePermission(Constants.HOME_PERM);
        }

        @SuppressWarnings("null")
        @Override
        protected @Nonnull CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
            return executeOnPlayerManager(context, homeNameArg, playerHomes, playerHomes::setDefault)
                .exceptionally(HomeCommand::logException);
        }
    }

    private class RemoveHomeCommand extends AbstractAsyncCommand {
        private final @Nonnull RequiredArg<String> homeNameArg;

        @SuppressWarnings("null")
        public RemoveHomeCommand() {
            super("remove", "Delete a saved home");
            this.homeNameArg = withRequiredArg("name", "Name of your home", ArgTypes.STRING);
            requirePermission(Constants.HOME_PERM);
            addAliases("delete");
        }

        @SuppressWarnings("null")
        @Override
        protected @Nonnull CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
            return executeOnPlayerManager(context, homeNameArg, playerHomes, playerHomes::remove)
                .exceptionally(HomeCommand::logException);
        }
    }
}
