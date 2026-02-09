package com.wardlordruby.plugin.managers;

import com.wardlordruby.plugin.HomePlugin;
import com.wardlordruby.plugin.models.HomeLocation;
import com.wardlordruby.plugin.models.HomeMap;
import com.wardlordruby.plugin.models.JsonResource;
import com.wardlordruby.plugin.models.Permissions;
import com.wardlordruby.plugin.models.PlayerHomeResult;
import com.wardlordruby.plugin.models.PluginConfig;
import com.wardlordruby.plugin.models.PluginConfig.HomeConfig;
import com.wardlordruby.plugin.services.JsonStorageService;

import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;

public class PlayerHomeManager {
    private static final String TMP_WORLD_INDICATOR = "instance-";

    private final @Nonnull HomeMap homeMap;

    public PlayerHomeManager(@Nonnull HomeMap map) {
        this.homeMap = map;
    }

    public void write(@Nonnull JsonStorageService fileManager) {
        fileManager.write(homeMap, JsonResource.HOMES);
    }

    /// Success value is guaranteed to contain a `HomeLocation`
    @SuppressWarnings("null") // `get` ensures `playerHomes` is not empty by this point
    public @Nonnull PlayerHomeResult getDefault(@Nonnull UUID playerID) {
        return get(playerID, playerHomes -> new PlayerHomeResult.Success<>(playerHomes.get(0)));
    }

    /// Success value is guaranteed to contain a `HomeLocation`
    @SuppressWarnings("null") // the passed in fn only gets called if `playerHomes` is not null
    public @Nonnull PlayerHomeResult get(@Nonnull String id, @Nonnull UUID playerID) {
        return get(playerID, playerHomes -> query(id, playerHomes)
            .<PlayerHomeResult>map(home -> new PlayerHomeResult.Success<>(home))
            .orElseGet(() -> new PlayerHomeResult.HomeNotFound(id))
        );
    }

    /// Success value is guaranteed to contain a `String` (success message)
    public @Nonnull PlayerHomeResult insert(
        @Nonnull String id,
        @Nonnull String worldName,
        @Nonnull UUID playerID,
        @Nonnull Transform playerTransform
    ) {
        if (worldName.startsWith(TMP_WORLD_INDICATOR)) {
            return PlayerHomeResult.IllegalWorld.temporary();
        }

        Set<String> bannedWorlds = HomePlugin.getConfig().homeConfig.bannedHomeWorlds;
        if (bannedWorlds.contains(worldName)) return PlayerHomeResult.IllegalWorld.banned(worldName);

        int homeLimit = getHomeLimit(playerID);

        List<HomeLocation> playerHomes = homeMap.computeIfAbsent(
            playerID,
            k -> new ArrayList<>()
        );

        if (playerHomes.size() >= homeLimit) {
            return new PlayerHomeResult.MaxHomesReached(playerHomes.size(), homeLimit);
        }

        Optional<HomeLocation> matchingHome = query(id, playerHomes);
        matchingHome.ifPresentOrElse(
            playerHome -> playerHome.update(worldName, playerTransform),
            () -> playerHomes.add(new HomeLocation(id, worldName, playerTransform))
        );

        return new PlayerHomeResult.Success<>(
            "Home: '" + id + (matchingHome.isPresent() ? "' updated!" :"' saved!")
        );
    }

    /// Success value is guaranteed to contain a `String` (success message)
    public @Nonnull PlayerHomeResult setDefault(@Nonnull String id, @Nonnull UUID playerID) {
        return findHomeIndex(id, playerID, (homes, i) -> {
            if (i == 0) {
                return new PlayerHomeResult.AlreadyDefault(id);
            }
            Collections.swap(homes, i, 0);
            return new PlayerHomeResult.Success<>("Set home: '" + id + "' as default!");
        });
    }

    /// Success value is guaranteed to contain a `String` (success message)
    public @Nonnull PlayerHomeResult remove(@Nonnull String id, @Nonnull UUID playerID) {
        return findHomeIndex(id, playerID, (homes, i) -> {
            homes.remove((int)i);
            return new PlayerHomeResult.Success<>("Home: '" + id + "' removed!");
        });
    }

    /// Success value is guaranteed to contain a `String` (formatted home list)
    @SuppressWarnings("null")
    public @Nonnull PlayerHomeResult list(@Nonnull UUID playerID, boolean verbose) {
        List<HomeLocation> playerHomes = homeMap.get(playerID);
        if (playerHomes == null || playerHomes.isEmpty()) return new PlayerHomeResult.NoSetHomes();

        String list = verbose
            ? playerHomes.stream().map(HomeLocation::display).collect(Collectors.joining("\n"))
            : playerHomes.stream().map(home -> home.id).collect(Collectors.joining(", "));

        return new PlayerHomeResult.Success<>(new PlayerHomeResult.List(list, verbose));
    }

    @SuppressWarnings("null") // Trust that the supplied function doesn't return null
    private @Nonnull PlayerHomeResult get(
        @Nonnull UUID playerID,
        @Nonnull Function<List<HomeLocation>, PlayerHomeResult> fn
    ) {
        List<HomeLocation> playerHomes = homeMap.get(playerID);
        return playerHomes == null || playerHomes.isEmpty() ? new PlayerHomeResult.NoSetHomes() : fn.apply(playerHomes);
    }

    private Optional<HomeLocation> query(@Nonnull String id, @Nonnull List<HomeLocation> playerHomes) {
        return playerHomes.stream().filter(entry -> entry.id.equalsIgnoreCase(id)).findFirst();
    }

    @SuppressWarnings("null")
    private @Nonnull PlayerHomeResult findHomeIndex(
        @Nonnull String id,
        @Nonnull UUID playerID,
        @Nonnull BiFunction<List<HomeLocation>, Integer, PlayerHomeResult> onFound
    ) {
        List<HomeLocation> playerHomes = homeMap.get(playerID);
        if (playerHomes == null || playerHomes.isEmpty()) return new PlayerHomeResult.NoSetHomes();

        OptionalInt index = IntStream.range(0, playerHomes.size())
            .filter(i -> playerHomes.get(i).id.equalsIgnoreCase(id))
            .findFirst();

        return index.isPresent() ? onFound.apply(playerHomes, index.getAsInt()) : new PlayerHomeResult.HomeNotFound(id);
    }

    private static int getHomeLimit(@Nonnull UUID playerID) {
        PermissionsModule permManager = PermissionsModule.get();

        if (permManager.hasPermission(playerID, Permissions.HOME_RANK + ".*")) {
            return PluginConfig.MAX_HOMES;
        }

        HomeConfig homeConfig = HomePlugin.getConfig().homeConfig;
        Short[] homeLimits = homeConfig.homeCountByRank;

        for (int i = homeLimits.length - 1; i >= 0; i--) {
            if (permManager.hasPermission(playerID, Permissions.HOME_RANK + "." + (i + 1))) {
                return Math.min(homeLimits[i], PluginConfig.MAX_HOMES);
            }
        }

        return Math.min(homeConfig.baseHomeCount, PluginConfig.MAX_HOMES);
    }
}
