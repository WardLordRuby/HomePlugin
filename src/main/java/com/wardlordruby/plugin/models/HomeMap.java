package com.wardlordruby.plugin.models;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HomeMap extends ConcurrentHashMap<UUID, TeleportEntry> {}
