package com.wardlordruby.plugin.models;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerCache extends ConcurrentHashMap<String, UUID> {}
