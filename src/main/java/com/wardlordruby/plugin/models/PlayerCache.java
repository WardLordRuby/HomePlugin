package com.wardlordruby.plugin.models;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerCache extends ConcurrentHashMap<String, UUID> {
    public PlayerCache() {
        super();
    }

    public PlayerCache(Map<String, UUID> map) {
        super(map);
    }
}
