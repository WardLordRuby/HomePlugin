package com.wardlordruby.plugin.services;

import com.wardlordruby.plugin.HomePlugin;
import com.wardlordruby.plugin.models.JsonResource;
import com.wardlordruby.plugin.models.TeleportEntry;
import com.wardlordruby.plugin.models.TeleportEntryAdapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.Nonnull;

public class JsonStorageService {
    private static final Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(TeleportEntry.class, new TeleportEntryAdapter())
        .create();

    private final File baseDirectory;

    public JsonStorageService(@Nonnull Path baseDirectory) {
        this.baseDirectory = baseDirectory.toFile();
        initializeDirectory();
    }

    private void initializeDirectory() {
        if (!baseDirectory.exists() && !baseDirectory.mkdirs()) {
            HomePlugin.LOGGER.atSevere().log("Failed to create directory: %s", baseDirectory.getPath());
            throw new IllegalStateException("Cannot initialize plugin directory");
        }
    }

    public <T> void write(@Nonnull T data, JsonResource<T> resource) {
        File file = new File(baseDirectory, resource.fileName());

        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath())) {
            gson.toJson(data, resource.type(), writer);
            HomePlugin.LOGGER.atInfo().log("%s saved", resource.displayName());
        } catch (Exception e) {
            HomePlugin.LOGGER.atSevere().log("Failed to write: %s\n%s", file.getAbsolutePath(), e.getMessage());
        }
    }

    public @Nonnull <T> T read(JsonResource<T> resource) {
        File file = new File(baseDirectory, resource.fileName());

        if (!file.exists()) return resource.createDefault();

        T data = null;

        try (BufferedReader reader = Files.newBufferedReader(file.toPath())) {
            data = gson.fromJson(reader, resource.type());
        } catch (Exception e) {
            HomePlugin.LOGGER.atSevere().log("Failed to read: %s\n%s", file.getAbsolutePath(), e.getMessage());
            throw new IllegalStateException("Cannot read data file: " + resource.fileName(), e);
        }

        if (data == null) return resource.createDefault();

        var validator = resource.validator();
        if (validator != null) {
            String validationErr = validator.apply(data);
            if (validationErr != null) {
                throw new IllegalStateException("%s in '%s'".formatted(validationErr, resource.fileName()));
            }
        }

        HomePlugin.LOGGER.atInfo().log("%s loaded", resource.displayName());
        return data;
    }
}