package com.wardlordruby.plugin.commands;

import com.wardlordruby.plugin.HomePlugin;
import com.wardlordruby.plugin.managers.PlayerHomeManager;
import com.wardlordruby.plugin.models.HomeLocation;
import com.wardlordruby.plugin.models.Permissions;
import com.wardlordruby.plugin.models.PlayerHomeResult;
import com.wardlordruby.plugin.models.PlayerMetaData;
import com.wardlordruby.plugin.utils.TeleportHistoryUtil;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.auth.ProfileServiceClient.PublicGameProfile;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.SingleArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.awt.Color;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class HomeCommand extends AbstractAsyncCommand {
    private final @Nonnull PlayerHomeManager playerHomes;

    private static final @Nonnull String WORLD_NOT_LOADED = "server.commands.teleport.worldNotLoaded";
    private static final @Nonnull String[] SUBCOMMAND_NAME_OR_ALIAS = {"list", "set", "add", "default", "remove", "delete"};

    @SuppressWarnings("null")
    private static final @Nonnull Message PLAYER_ONLY_COMMAND = HomePlugin.formatPlayerMessage("Wrong sender type, expecting Player")
        .color(Color.RED);
    private static final @Nonnull String SET_HOME_HELP_MSG = "Use `/home set <NAME>` to update your home location";

    @SuppressWarnings("null")
    private static final @Nonnull CompletableFuture<Void> COMPLETED_FUTURE = CompletableFuture.completedFuture(null);
    @SuppressWarnings("null")
    private static final @Nonnull SingleArgumentType<String> TYPE_STRING = ArgTypes.STRING;
    @SuppressWarnings("null")
    private static final @Nonnull SingleArgumentType<PublicGameProfile> TYPE_PUB_PROFILE = ArgTypes.GAME_PROFILE_LOOKUP;

    @SuppressWarnings("null")
    private final @Nonnull ComponentType<EntityStore, TransformComponent> transformComponentType = TransformComponent.getComponentType();
    private final @Nonnull ComponentType<EntityStore, Teleport> teleportComponentType = Teleport.getComponentType();

    public HomeCommand(@Nonnull PlayerHomeManager homeManager) {
        super("home", "Teleport back to your home");
        this.playerHomes = homeManager;

        this.addSubCommand(new ListHomesCommand());
        this.addSubCommand(new SetHomeCommand());
        this.addSubCommand(new SetDefaultHomeCommand());
        this.addSubCommand(new RemoveHomeCommand());
        this.addUsageVariant(new SpecificHomeCommand());
        if (HomePlugin.getConfig().homeConfig.adminCommands) {
            this.addUsageVariant(new PlayerSpecificHomeCommand());
        }
    }

    @SuppressWarnings("null")
    @Override
    protected @Nonnull CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
        return executeTeleport(
            context,
            () -> PlayerMetaData.fromSender(context.sender()),
            playerID -> playerHomes.getDefault(playerID),
            (playerHomeRes, _playerData) -> "%s\n%s".formatted(playerHomeRes.display(), SET_HOME_HELP_MSG)
        ).exceptionally(HomeCommand::logException);
    }

    private static String parseHomeNameArg(
        @Nonnull RequiredArg<String> arg,
        @Nonnull CommandContext context
    ) {
        String input = context.get(arg);
        for (String quote : new String[]{"'", "\""}) {
            if (input.startsWith(quote) && input.endsWith(quote)) {
                input = input.substring(1, input.length() - 1);
                break;
            }
        }
        return input;
    }

    /// return value indicates success (`null`), or error with a message
    private static @Nullable String validateHomeName(@Nonnull String hostName) {
        if (hostName.isEmpty()) {
            return "Home name can not be empty";
        }
        for (String name : SUBCOMMAND_NAME_OR_ALIAS) {
            if (hostName.equalsIgnoreCase(name)) {
                return "Can not set home name the same as a subcommand";
            }
        }
        return null;
    }

    /// Must be called from within a method that will catch `NullPointerExceptions`
    private static @Nullable <T> T mapValidatedHomeName(
        @Nonnull CommandContext context,
        @Nonnull RequiredArg<String> homeNameArg,
        Function<String, T> mapFn
    ) {
        String homeName = Objects.requireNonNull(parseHomeNameArg(homeNameArg, context));
        String homeNameErr = validateHomeName(homeName);

        if (homeNameErr != null) {
            context.sendMessage(HomePlugin.formatPlayerMessage(homeNameErr));
            return null;
        }

        return mapFn.apply(homeName);
    }

    /// Must be called from within a method that will catch `NullPointerExceptions`
    private static @Nullable String getValidatedHomeName(
        @Nonnull CommandContext context,
        @Nonnull RequiredArg<String> homeNameArg
    ) {
        return mapValidatedHomeName(context, homeNameArg, Function.identity());
    }

    /// Must be called from within a method that will catch `NullPointerExceptions`
    @SuppressWarnings("null")
    private @Nullable PlayerHomeResult playerHomeResultFromArg(
        @Nonnull CommandContext context,
        @Nonnull RequiredArg<String> homeNameArg,
        @Nonnull UUID playerID
    ) {
        return mapValidatedHomeName(
            context,
            homeNameArg,
            homeName -> playerHomes.get(homeName, playerID)
        );
    }

    private static Void logException(Throwable ex) {
        HomePlugin.LOGGER.atWarning().log(ex.getMessage());
        return null;
    }

    /// Must be called from within a method that will catch `NullPointerExceptions`
    private @Nonnull CompletableFuture<Void> executeTeleport(
        @Nonnull CommandContext context,
        Supplier<PlayerMetaData> targetPlayerDataFn,
        Function<UUID, PlayerHomeResult> targetHomeFn,
        BiFunction<PlayerHomeResult, PlayerMetaData, String> formatErrFn
    ) {
        if (!context.isPlayer()) {
            context.sendMessage(PLAYER_ONLY_COMMAND);
            return COMPLETED_FUTURE;
        }

        Ref<EntityStore> ref = Objects.requireNonNull(context.senderAsPlayerRef());

        PlayerMetaData targetPlayer = targetPlayerDataFn.get();
        if (targetPlayer == null) return COMPLETED_FUTURE;

        PlayerHomeResult playerHomeRes = targetHomeFn.apply(targetPlayer.getUuid());
        if (playerHomeRes == null) return COMPLETED_FUTURE;

        if (playerHomeRes.isError()) {
            @SuppressWarnings("null")
            @Nonnull String formattedErr = formatErrFn.apply(playerHomeRes, targetPlayer);
            context.sendMessage(HomePlugin.formatPlayerMessage(formattedErr));
            return COMPLETED_FUTURE;
        }

        HomeLocation playerHome = (HomeLocation)((PlayerHomeResult.Success<?>)playerHomeRes).get();
        String homeWorld = playerHome.getWorld();

        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();

        World targetWorld = world.getName().equals(homeWorld) ? world : Universe.get().getWorld(homeWorld);

        if (targetWorld == null) {
            context.sendMessage(Message.translation(WORLD_NOT_LOADED));
            return COMPLETED_FUTURE;
        }

        world.execute(() -> {
            TransformComponent senderTransform = Objects.requireNonNull(store.getComponent(ref, transformComponentType));
            TeleportHistoryUtil.append(ref, store, world, senderTransform.getTransform(), targetWorld.getName(), playerHome.getPosition());
            Teleport playerTeleport = Teleport.createForPlayer(targetWorld, playerHome.getPosition(), playerHome.getRotation());
            store.addComponent(ref, teleportComponentType, playerTeleport);
        });

        return COMPLETED_FUTURE;
    }

    /// Only to be used with `PlayerHomeManager` methods that have a return value of `Success<String>`
    /// Must be called from within a method that will catch `NullPointerExceptions`
    private static @Nonnull CompletableFuture<Void> executeOnPlayerManager(
        @Nonnull CommandContext context,
        @Nonnull RequiredArg<String> homeNameArg,
        @Nonnull PlayerHomeManager playerHomes,
        @Nonnull BiFunction<String, UUID, PlayerHomeResult> modifyFn
    ) {
        if (!context.isPlayer()) {
            context.sendMessage(PLAYER_ONLY_COMMAND);
            return COMPLETED_FUTURE;
        }

        String homeName = getValidatedHomeName(context, homeNameArg);
        if (homeName == null) return COMPLETED_FUTURE;

        UUID playerID = Objects.requireNonNull(context.sender().getUuid());
        PlayerHomeResult result = modifyFn.apply(homeName, playerID);

        if (result.isError()) {
            @SuppressWarnings("null")
            @Nonnull String errMsg = "%s\n%s".formatted(result.display(), result instanceof PlayerHomeResult.NoSetHomes
                ? SET_HOME_HELP_MSG
                : playerHomes.list(playerID, false).display());

            context.sendMessage(HomePlugin.formatPlayerMessage(errMsg));
        } else {
            context.sendMessage(HomePlugin.formatPlayerMessage(result.display()));
        }

        return COMPLETED_FUTURE;
    }

    private class SpecificHomeCommand extends AbstractAsyncCommand {
        private final @Nonnull RequiredArg<String> homeNameArg;

        public SpecificHomeCommand() {
            super("Teleport to a specific home");
            this.homeNameArg = withRequiredArg("name", "Name of your home", TYPE_STRING);
        }

        @SuppressWarnings("null")
        @Override
        protected @Nonnull CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
            return executeTeleport(
                context,
                () -> PlayerMetaData.fromSender(context.sender()),
                playerID -> playerHomeResultFromArg(context, homeNameArg, playerID),
                (playerHomeRes, playerData) -> "%s\n%s".formatted(
                    playerHomeRes.display(),
                    (playerHomeRes instanceof PlayerHomeResult.NoSetHomes
                        ? SET_HOME_HELP_MSG
                        : playerHomes.list(playerData.getUuid(), false).display()
                    )
                )
            ).exceptionally(HomeCommand::logException);
        }
    }

    private class PlayerSpecificHomeCommand extends AbstractAsyncCommand {
        private final @Nonnull RequiredArg<PublicGameProfile> playerProfileArg;
        private final @Nonnull RequiredArg<String> homeNameArg;

        public PlayerSpecificHomeCommand() {
            super("Teleport to a specific players home");
            this.playerProfileArg = withRequiredArg("player", "Name of target player", TYPE_PUB_PROFILE);
            this.homeNameArg = withRequiredArg("name", "Name of your home", TYPE_STRING);
            this.requirePermission(Permissions.HOME_OTHERS);
        }

        @SuppressWarnings("null")
        @Override
        protected @Nonnull CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
            return executeTeleport(
                context,
                () -> PlayerMetaData.fromProfileArg(context, playerProfileArg),
                playerID -> playerHomeResultFromArg(context, homeNameArg, playerID),
                (playerHomeRes, playerData) -> {
                    StringBuilder errMsg = new StringBuilder().append(playerHomeRes.display(playerData));
                    if (playerHomeRes instanceof PlayerHomeResult.HomeNotFound) {
                        errMsg.append('\n').append(playerHomes.list(playerData.getUuid(), false).display(playerData));
                    }
                    return errMsg.toString();
                }
            ).exceptionally(HomeCommand::logException);
        }
    }

    private class ListHomesCommand extends AbstractAsyncCommand {
        private final @Nonnull FlagArg verboseArg;

        public ListHomesCommand() {
            super("list", "List all your saved homes");
            this.verboseArg = withFlagArg("verbose", "Show detailed home information");
            this.requirePermission(Permissions.HOME);
            if (HomePlugin.getConfig().homeConfig.adminCommands) {
                this.addUsageVariant(new PlayerListHomesCommand());
            }
        }

        @SuppressWarnings("null")
        @Override
        protected @Nonnull CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
            return executeListHomes(context).exceptionally(HomeCommand::logException);
        }

        private @Nonnull CompletableFuture<Void> executeListHomes(@Nonnull CommandContext context) {
            if (!context.isPlayer()) {
                context.sendMessage(PLAYER_ONLY_COMMAND);
                return COMPLETED_FUTURE;
            }

            UUID senderID = Objects.requireNonNull(context.sender().getUuid());
            boolean verbose = verboseArg.get(context);

            PlayerHomeResult result = playerHomes.list(senderID, verbose);
            context.sendMessage(HomePlugin.formatPlayerMessage(result.display()));

            return COMPLETED_FUTURE;
        }
    }

    private class PlayerListHomesCommand extends AbstractAsyncCommand {
        private final @Nonnull RequiredArg<PublicGameProfile> playerNameArg;
        private final @Nonnull FlagArg verboseArg;

        public PlayerListHomesCommand() {
            super("List a specific players saved homes");
            this.playerNameArg = withRequiredArg("player", "Name of target player", TYPE_PUB_PROFILE);
            this.verboseArg = withFlagArg("verbose", "Show detailed home information");
            this.requirePermission(Permissions.HOME_OTHERS);
        }

        @SuppressWarnings("null")
        @Override
        protected @Nonnull CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
            return executeListHomes(context).exceptionally(HomeCommand::logException);
        }

        private @Nonnull CompletableFuture<Void> executeListHomes(@Nonnull CommandContext context) {
            boolean verbose = verboseArg.get(context);
            PlayerMetaData playerData = PlayerMetaData.fromProfileArg(context, playerNameArg);

            PlayerHomeResult result = playerHomes.list(playerData.getUuid(), verbose);
            context.sendMessage(HomePlugin.formatPlayerMessage(result.display(playerData)));

            return COMPLETED_FUTURE;
        }
    }

    private class SetHomeCommand extends AbstractAsyncCommand {
        private final @Nonnull RequiredArg<String> homeNameArg;

        public SetHomeCommand() {
            super("set", "Set home to your current position");
            this.homeNameArg = withRequiredArg("name", "Name of your home", TYPE_STRING);
            this.requirePermission(Permissions.HOME);
            this.addAliases("add");
        }

        @SuppressWarnings("null")
        @Override
        protected @Nonnull CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
            return executeSetHome(context).exceptionally(HomeCommand::logException);
        }

        private @Nonnull CompletableFuture<Void> executeSetHome(@Nonnull CommandContext context) {
            if (!context.isPlayer()) {
                context.sendMessage(PLAYER_ONLY_COMMAND);
                return COMPLETED_FUTURE;
            }

            String homeName = getValidatedHomeName(context, homeNameArg);
            if (homeName == null) return COMPLETED_FUTURE;

            Ref<EntityStore> ref = Objects.requireNonNull(context.senderAsPlayerRef());
            UUID playerID = Objects.requireNonNull(context.sender().getUuid());

            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();

            world.execute(() -> {
                TransformComponent playerTransform = Objects.requireNonNull(store.getComponent(ref, transformComponentType));
                PlayerHomeResult result = playerHomes.insert(homeName, world.getName(), playerID, playerTransform.getTransform());
                context.sendMessage(HomePlugin.formatPlayerMessage(result.display()));
            });

            return COMPLETED_FUTURE;
        }
    }

    private class SetDefaultHomeCommand extends AbstractAsyncCommand {
        private final @Nonnull RequiredArg<String> homeNameArg;

        public SetDefaultHomeCommand() {
            super("default", "Set a saved home as default");
            this.homeNameArg = withRequiredArg("name", "Name of your home", TYPE_STRING);
            this.requirePermission(Permissions.HOME);
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

        public RemoveHomeCommand() {
            super("remove", "Delete a saved home");
            this.homeNameArg = withRequiredArg("name", "Name of your home", TYPE_STRING);
            this.requirePermission(Permissions.HOME);
            this.addAliases("delete");
        }

        @SuppressWarnings("null")
        @Override
        protected @Nonnull CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
            return executeOnPlayerManager(context, homeNameArg, playerHomes, playerHomes::remove)
                .exceptionally(HomeCommand::logException);
        }
    }
}
