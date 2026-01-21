package com.wardlordruby.plugin;

import com.hypixel.hytale.logger.HytaleLogger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public class HomeManager {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<ConcurrentHashMap<UUID, TeleportEntry>>(){}.getType();
    
    public static void saveHomes(ConcurrentHashMap<UUID, TeleportEntry> homeMap, File file) {
        HytaleLogger logger = HytaleLogger.get("PluginManager");

        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdir()) {
                logger.atSevere().log("Failed to create plugin directory");
                return;
            }
        }

        try (Writer writer = new FileWriter(file)) {
            gson.toJson(homeMap, writer);
            logger.atInfo().log("Home locations saved to: " + file.getCanonicalPath().toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static ConcurrentHashMap<UUID, TeleportEntry> loadHomes(File file) {
        if (!file.exists()) {
            return new ConcurrentHashMap<>();
        }
        
        try (Reader reader = new FileReader(file)) {
            ConcurrentHashMap<UUID, TeleportEntry> map = gson.fromJson(reader, MAP_TYPE);

            if (map != null) {
                HytaleLogger.get("PluginManager").atInfo().log("Home locations loaded from: " + file.getCanonicalPath().toString());
                return map;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ConcurrentHashMap<>();
    }
}