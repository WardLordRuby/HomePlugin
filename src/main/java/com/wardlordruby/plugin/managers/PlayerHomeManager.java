package com.wardlordruby.plugin.managers;

import com.wardlordruby.plugin.HomePlugin;
import com.wardlordruby.plugin.models.HomeMap;
import com.wardlordruby.plugin.models.JsonResource;
import com.wardlordruby.plugin.models.Permissions;
import com.wardlordruby.plugin.models.PlayerHomeResult;
import com.wardlordruby.plugin.models.PluginConfig.HomeConfig;
import com.wardlordruby.plugin.models.TeleportEntry;
import com.wardlordruby.plugin.services.JsonStorageService;

import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;

public class PlayerHomeManager {
    private static final String TMP_WORLD_INDICATOR = "instance-";
    private static final short MAX_HOMES = 100;

    private final @Nonnull HomeMap homeMap;

    public PlayerHomeManager(@Nonnull HomeMap map) {
        this.homeMap = map;
    }

    public void write(@Nonnull JsonStorageService fileManager) {
        fileManager.write(homeMap, JsonResource.HOMES);
    }

    /// Success value is guaranteed to contain a `TeleportEntry`
    @SuppressWarnings("null") // `get` ensures `playerHomes` is not empty by this point
    public @Nonnull PlayerHomeResult getDefault(@Nonnull UUID playerID) {
        return get(playerID, playerHomes -> new PlayerHomeResult.Success<>(playerHomes.get(0)));
    }

    /// Success value is guaranteed to contain a `TeleportEntry`
    @SuppressWarnings("null") // the passed in fn only gets called if `playerHomes` is not null
    public @Nonnull PlayerHomeResult get(@Nonnull String tag, @Nonnull UUID playerID) {
        return get(playerID, playerHomes -> query(tag, playerHomes)
            .<PlayerHomeResult>map(home -> new PlayerHomeResult.Success<>(home))
            .orElseGet(() -> new PlayerHomeResult.HomeNotFound(tag))
        );
    }

    /// Success value is guaranteed to contain a `String` (success message)
    public @Nonnull PlayerHomeResult insert(
        @Nonnull String tag,
        @Nonnull String worldName,
        @Nonnull UUID playerID,
        @Nonnull Transform playerTransform
    ) {
        if (worldName.startsWith(TMP_WORLD_INDICATOR)) {
            return new PlayerHomeResult.IllegalWorld();
        }

        int homeLimit = getHomeLimit(playerID);

        List<TeleportEntry> playerHomes = homeMap.computeIfAbsent(
            playerID,
            k -> new ArrayList<>()
        );

        if (playerHomes.size() >= homeLimit) {
            return new PlayerHomeResult.MaxHomesReached(playerHomes.size(), homeLimit);
        }

        Optional<TeleportEntry> matchingHome = query(tag, playerHomes);
        matchingHome.ifPresentOrElse(
            playerHome -> playerHome.update(worldName, playerTransform),
            () -> playerHomes.add(new TeleportEntry(tag, worldName, playerTransform))
        );

        return new PlayerHomeResult.Success<>(
            "Home: '" + tag + (matchingHome.isPresent() ? "' updated!" :"' saved!")
        );
    }

    /// Success value is guaranteed to contain a `String` (success message)
    public @Nonnull PlayerHomeResult setDefault(@Nonnull String tag, @Nonnull UUID playerID) {
        return findHomeIndex(tag, playerID, (homes, i) -> {
            if (i == 0) {
                return new PlayerHomeResult.AlreadyDefault(tag);
            }
            Collections.swap(homes, i, 0);
            return new PlayerHomeResult.Success<>("Set home: '" + tag + "' as default!");
        });
    }

    /// Success value is guaranteed to contain a `String` (success message)
    public @Nonnull PlayerHomeResult remove(@Nonnull String tag, @Nonnull UUID playerID) {
        return findHomeIndex(tag, playerID, (homes, i) -> {
            homes.remove((int)i);
            return new PlayerHomeResult.Success<>("Home: '" + tag + "' removed!");
        });
    }

    /// Success value is guaranteed to contain a `String` (formatted home list)
    @SuppressWarnings("null")
    public @Nonnull PlayerHomeResult list(@Nonnull UUID playerID, boolean verbose) {
        List<TeleportEntry> playerHomes = homeMap.get(playerID);
        if (playerHomes == null || playerHomes.isEmpty()) return new PlayerHomeResult.NoSetHomes();

        String list = verbose
            ? playerHomes.stream().map(TeleportEntry::display).collect(Collectors.joining("\n"))
            : playerHomes.stream().map(home -> home.tag).collect(Collectors.joining(", "));

        return new PlayerHomeResult.Success<>(new PlayerHomeResult.List(list, verbose));
    }

    @SuppressWarnings("null") // Trust that the supplied function doesn't return null
    private @Nonnull PlayerHomeResult get(
        @Nonnull UUID playerID,
        @Nonnull Function<List<TeleportEntry>, PlayerHomeResult> fn
    ) {
        List<TeleportEntry> playerHomes = homeMap.get(playerID);
        return playerHomes == null || playerHomes.isEmpty() ? new PlayerHomeResult.NoSetHomes() : fn.apply(playerHomes);
    }

    private Optional<TeleportEntry> query(@Nonnull String tag, @Nonnull List<TeleportEntry> playerHomes) {
        return playerHomes.stream().filter(entry -> entry.tag.equalsIgnoreCase(tag)).findFirst();
    }

    @SuppressWarnings("null")
    private @Nonnull PlayerHomeResult findHomeIndex(
        @Nonnull String tag,
        @Nonnull UUID playerID,
        @Nonnull BiFunction<List<TeleportEntry>, Integer, PlayerHomeResult> onFound
    ) {
        List<TeleportEntry> playerHomes = homeMap.get(playerID);
        if (playerHomes == null || playerHomes.isEmpty()) return new PlayerHomeResult.NoSetHomes();

        OptionalInt index = IntStream.range(0, playerHomes.size())
            .filter(i -> playerHomes.get(i).tag.equalsIgnoreCase(tag))
            .findFirst();

        return index.isPresent() ? onFound.apply(playerHomes, index.getAsInt()) : new PlayerHomeResult.HomeNotFound(tag);
    }

    private static int getHomeLimit(@Nonnull UUID playerID) {
        PermissionsModule permManager = PermissionsModule.get();

        if (permManager.hasPermission(playerID, Permissions.HOME_RANK + ".*")) {
            return MAX_HOMES;
        }

        HomeConfig homeConfig = HomePlugin.getConfig().homeConfig;
        int[] homeLimits = homeConfig.homeCountByRank;

        for (int i = homeLimits.length - 1; i >= 0; i--) {
            if (permManager.hasPermission(playerID, Permissions.HOME_RANK + "." + (i + 1))) {
                return Math.min(homeLimits[i], MAX_HOMES);
            }
        }

        return Math.min(homeConfig.baseHomeCount, MAX_HOMES);
    }
}
