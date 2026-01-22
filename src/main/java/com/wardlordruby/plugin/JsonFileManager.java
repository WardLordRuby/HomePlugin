package com.wardlordruby.plugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;

import javax.annotation.Nonnull;

public class JsonFileManager {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File baseDirectory;

    public JsonFileManager(@Nonnull Path baseDirectory) {
        this.baseDirectory = baseDirectory.toFile();
        initializeDirectory();
    }

    private void initializeDirectory() {
        if (!baseDirectory.exists() && !baseDirectory.mkdirs()) {
            HomePlugin.logger.atSevere().log("Failed to create directory: " + baseDirectory);
            throw new IllegalStateException("Cannot initialize plugin directory");
        }
    }

    public <T> void write(@Nonnull T data, JsonResource<T> resource) {
        File file = new File(baseDirectory, resource.fileName());

        try (Writer writer = new FileWriter(file)) {
            gson.toJson(data, resource.type(), writer);
            HomePlugin.logger.atInfo().log(resource.displayName() + " saved");
        } catch (Exception e) {
            HomePlugin.logger.atSevere().log("Failed to write: " + file.getAbsolutePath() + "\n" + e.getMessage());
        }
    }

    public @Nonnull <T> T read(JsonResource<T> resource) {
        File file = new File(baseDirectory, resource.fileName());

        if (!file.exists()) return resource.createDefault();

        try (Reader reader = new FileReader(file)) {
            T data = gson.fromJson(reader, resource.type());
            if (data == null) return resource.createDefault(); else {
                HomePlugin.logger.atInfo().log(resource.displayName() + " loaded");
                return data;
            }
        } catch (Exception e) {
            HomePlugin.logger.atSevere().log("Failed to read: " + file.getAbsolutePath() + "\n" + e.getMessage());
            throw new IllegalStateException("Cannot read data file: " + resource.fileName(), e);
        }
    }
}