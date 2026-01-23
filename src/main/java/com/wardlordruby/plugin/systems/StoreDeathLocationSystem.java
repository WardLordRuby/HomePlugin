package com.wardlordruby.plugin.systems;

import com.wardlordruby.plugin.HomePlugin;
import com.wardlordruby.plugin.utils.TeleportHistoryUtil;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems.OnDeathSystem;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class StoreDeathLocationSystem extends OnDeathSystem {
    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @Override
    public void onComponentAdded(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull DeathComponent component,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> buffer
    ) {
        PlayerRef player = store.getComponent(ref, PlayerRef.getComponentType());

        if (HomePlugin.getConfig().backOnDeath
            || PermissionsModule.get().hasPermission(player.getUuid(), "com.wardlordruby.homeplugin.backOnDeath"))
        {
            UUID worldUUID = player.getWorldUuid();
            if (worldUUID == null) return;
            World world = Universe.get().getWorld(worldUUID);
            if (world == null) return;
            Ref<EntityStore> playerRef = player.getReference();
            if (playerRef == null) return;

            world.execute(() -> TeleportHistoryUtil.append(playerRef, playerRef.getStore(), world, player.getTransform()));
        }
    }
}
