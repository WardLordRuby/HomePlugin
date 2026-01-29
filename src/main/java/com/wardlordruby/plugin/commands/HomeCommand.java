package com.wardlordruby.plugin.commands;

import com.wardlordruby.plugin.models.PlayerHomeResult;
import com.wardlordruby.plugin.models.TeleportEntry;
import com.wardlordruby.plugin.models.Permissions;
import com.wardlordruby.plugin.HomePlugin;
import com.wardlordruby.plugin.managers.PlayerHomeManager;
import com.wardlordruby.plugin.utils.TeleportHistoryUtil;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.SingleArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.awt.Color;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class HomeCommand extends AbstractAsyncCommand {
    private final @Nonnull PlayerHomeManager playerHomes;

    private static final @Nonnull String WORLD_NOT_LOADED = "server.commands.teleport.worldNotLoaded";
    private static final @Nonnull String[] SUBCOMMAND_NAME_OR_ALIAS = {"list", "set", "add", "default", "remove", "delete"};
    private static final @Nonnull String SET_HOME_HELP_MSG = "Use `/home set <NAME>` to update your home location";

    @SuppressWarnings("null")
    private static final @Nonnull CompletableFuture<Void> COMPLETED_FUTURE = CompletableFuture.completedFuture(null);

    @SuppressWarnings("null")
    private static final @Nonnull SingleArgumentType<String> STRING_ARG_TYPE = ArgTypes.STRING;

    @SuppressWarnings("null")
    private static final @Nonnull Message PLAYER_ONLY_COMMAND = Message.raw("Wrong sender type, expecting Player").color(Color.RED);

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
            () -> TargetPlayerMeta.fromSender(context.sender()),
            playerID -> playerHomes.getDefault(playerID),
            (playerHomeRes, _playerData) -> "%s\n%s".formatted(playerHomeRes.display(), SET_HOME_HELP_MSG)
        ).exceptionally(HomeCommand::logException);
    }

    private record TargetPlayerMeta (
        @Nonnull UUID id,
        @Nonnull String username
    ) {
        /// Must be called from within a method that will catch `NullPointerExceptions`
        public static @Nonnull TargetPlayerMeta fromSender(@Nonnull CommandSender sender) {
            UUID id = Objects.requireNonNull(sender.getUuid());
            String username = Objects.requireNonNull(sender.getDisplayName());
            return new TargetPlayerMeta(id, username);
        }

        /// Must be called from within a method that will catch `NullPointerExceptions`
        public static @Nullable TargetPlayerMeta fromStringArg(
            @Nonnull CommandContext context,
            @Nonnull RequiredArg<String> playerNameArg
        ) {
            @Nonnull String targetPlayerName = Objects.requireNonNull(context.get(playerNameArg));
            PlayerRef targetPlayer = Universe.get().getPlayerByUsername(targetPlayerName, NameMatching.DEFAULT);
            UUID targetPlayerID = targetPlayer != null ? targetPlayer.getUuid() : HomePlugin.getCachedUUID(targetPlayerName);
            if (targetPlayerID == null) {
                @SuppressWarnings("null")
                @Nonnull String errMsg = "Could not find player: '%s'".formatted(targetPlayerName);
                context.sendMessage(Message.raw(errMsg));
                return null;
            }
            return new TargetPlayerMeta(targetPlayerID, targetPlayerName);
        }
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
            context.sendMessage(Message.raw(homeNameErr));
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
        Supplier<TargetPlayerMeta> targetPlayerDataFn,
        Function<UUID, PlayerHomeResult> targetHomeFn,
        BiFunction<PlayerHomeResult, TargetPlayerMeta, String> formatErrFn
    ) {
        if (!context.isPlayer()) {
            context.sendMessage(PLAYER_ONLY_COMMAND);
            return COMPLETED_FUTURE;
        }

        UUID senderID = Objects.requireNonNull(context.sender().getUuid());
        PlayerRef senderRef = Objects.requireNonNull(Universe.get().getPlayer(senderID));

        TargetPlayerMeta targetPlayer = targetPlayerDataFn.get();
        if (targetPlayer == null) return COMPLETED_FUTURE;

        PlayerHomeResult playerHomeRes = targetHomeFn.apply(targetPlayer.id);
        if (playerHomeRes == null) return COMPLETED_FUTURE;

        if (playerHomeRes.isError()) {
            @SuppressWarnings("null")
            @Nonnull String formattedErr = formatErrFn.apply(playerHomeRes, targetPlayer);
            context.sendMessage(Message.raw(formattedErr));
            return COMPLETED_FUTURE;
        }

        TeleportEntry playerHome = (TeleportEntry)((PlayerHomeResult.Success<?>)playerHomeRes).get();
        World targetWorld = Universe.get().getWorld(playerHome.world);

        if (targetWorld == null) {
            context.sendMessage(Message.translation(WORLD_NOT_LOADED));
            return COMPLETED_FUTURE;
        }

        UUID worldID = Objects.requireNonNull(senderRef.getWorldUuid());
        World world = Objects.requireNonNull(Universe.get().getWorld(worldID));

        Ref<EntityStore> ref = Objects.requireNonNull(senderRef.getReference());

        world.execute(() -> {
            Store<EntityStore> store = ref.getStore();
            TeleportHistoryUtil.append(ref, store, world, senderRef.getTransform());
            Teleport playerTeleport = Teleport.createForPlayer(targetWorld, playerHome.position, playerHome.rotation);
            store.addComponent(ref, Teleport.getComponentType(), playerTeleport);
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
        context.sendMessage(Message.raw(result.display()));

        if (result.isError()) {
            context.sendMessage(Message.raw((result instanceof PlayerHomeResult.NoSetHomes
                ? SET_HOME_HELP_MSG
                : playerHomes.list(playerID, false).display())
            ));
        }

        return COMPLETED_FUTURE;
    }

    private class SpecificHomeCommand extends AbstractAsyncCommand {
        private final @Nonnull RequiredArg<String> homeNameArg;

        public SpecificHomeCommand() {
            super("Teleport to a specific home");
            this.homeNameArg = withRequiredArg("name", "Name of your home", STRING_ARG_TYPE);
        }

        @SuppressWarnings("null")
        @Override
        protected @Nonnull CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
            return executeTeleport(
                context,
                () -> TargetPlayerMeta.fromSender(context.sender()),
                playerID -> playerHomeResultFromArg(context, homeNameArg, playerID),
                (playerHomeRes, playerData) -> "%s\n%s".formatted(
                    playerHomeRes.display(),
                    (playerHomeRes instanceof PlayerHomeResult.NoSetHomes
                        ? SET_HOME_HELP_MSG
                        : playerHomes.list(playerData.id, false).display()
                    )
                )
            ).exceptionally(HomeCommand::logException);
        }
    }

    private class PlayerSpecificHomeCommand extends AbstractAsyncCommand {
        private final @Nonnull RequiredArg<String> playerNameArg;
        private final @Nonnull RequiredArg<String> homeNameArg;

        public PlayerSpecificHomeCommand() {
            super("Teleport to a specific players home");
            this.playerNameArg = withRequiredArg("player", "Name of target player", STRING_ARG_TYPE);
            this.homeNameArg = withRequiredArg("name", "Name of your home", STRING_ARG_TYPE);
            this.requirePermission(Permissions.HOME_OTHERS);
        }

        @SuppressWarnings("null")
        @Override
        protected @Nonnull CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
            return executeTeleport(
                context,
                () -> TargetPlayerMeta.fromStringArg(context, playerNameArg),
                playerID -> playerHomeResultFromArg(context, homeNameArg, playerID),
                (playerHomeRes, playerData) -> playerHomeRes.displayForOther(playerData.username) +
                    (playerHomeRes instanceof PlayerHomeResult.HomeNotFound
                        ? "\n" + playerHomes.list(playerData.id, false).display()
                        : "")
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
            context.sendMessage(Message.raw(result.display()));

            return COMPLETED_FUTURE;
        }
    }

    private class PlayerListHomesCommand extends AbstractAsyncCommand {
        private final @Nonnull RequiredArg<String> playerNameArg;
        private final @Nonnull FlagArg verboseArg;

        public PlayerListHomesCommand() {
            super("List a specific players saved homes");
            this.playerNameArg = withRequiredArg("player", "Name of target player", STRING_ARG_TYPE);
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
            TargetPlayerMeta playerData = TargetPlayerMeta.fromStringArg(context, playerNameArg);
            if (playerData == null) return COMPLETED_FUTURE;

            PlayerHomeResult result = playerHomes.list(playerData.id, verbose);
            context.sendMessage(Message.raw(result.display()));

            return COMPLETED_FUTURE;
        }
    }

    private class SetHomeCommand extends AbstractAsyncCommand {
        private final @Nonnull RequiredArg<String> homeNameArg;

        public SetHomeCommand() {
            super("set", "Set home to your current position");
            this.homeNameArg = withRequiredArg("name", "Name of your home", STRING_ARG_TYPE);
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

            UUID playerID = Objects.requireNonNull(context.sender().getUuid());
            PlayerRef playerRef = Objects.requireNonNull(Universe.get().getPlayer(playerID));

            UUID worldID = Objects.requireNonNull(playerRef.getWorldUuid());
            World world = Objects.requireNonNull(Universe.get().getWorld(worldID));

            PlayerHomeResult result = playerHomes.insert(homeName, world, playerRef);
            context.sendMessage(Message.raw(result.display()));

            return COMPLETED_FUTURE;
        }
    }

    private class SetDefaultHomeCommand extends AbstractAsyncCommand {
        private final @Nonnull RequiredArg<String> homeNameArg;

        public SetDefaultHomeCommand() {
            super("default", "Set a saved home as default");
            this.homeNameArg = withRequiredArg("name", "Name of your home", STRING_ARG_TYPE);
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
            this.homeNameArg = withRequiredArg("name", "Name of your home", STRING_ARG_TYPE);
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
