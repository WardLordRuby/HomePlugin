package com.wardlordruby.plugin.systems;

import com.wardlordruby.plugin.HomePlugin;
import com.wardlordruby.plugin.utils.TeleportHistoryUtil;
import com.wardlordruby.plugin.models.Permissions;

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
        UUID playerID = player.getUuid();
        PermissionsModule permModule = PermissionsModule.get();

        if (permModule.hasPermission(playerID, Permissions.BACK)
            && HomePlugin.getConfig().backConfig.backOnDeath
            || permModule.hasPermission(playerID, Permissions.BACK_ON_DEATH))
        {
            UUID worldID = player.getWorldUuid();
            if (worldID == null) return;
            World world = Universe.get().getWorld(worldID);
            if (world == null) return;

            world.execute(() -> TeleportHistoryUtil.append(ref, buffer, world, player.getTransform(), null, null));
        }
    }
}
