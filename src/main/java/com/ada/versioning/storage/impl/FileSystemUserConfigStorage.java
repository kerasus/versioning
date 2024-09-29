package com.ada.versioning.storage.impl;

import com.ada.versioning.storage.UserConfigStorage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Service("fileSystemStorage")
public class FileSystemUserConfigStorage implements UserConfigStorage {

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Value("${config.file.path}")
    private String configPath;

    // This method will be executed after the bean is initialized
    @PostConstruct
    public void init() {
        File directory = new File(configPath);
        if (!directory.exists()) {
            directory.mkdirs();  // Ensure that all parent directories are created
        }
    }

    @Override
    public Optional<JsonNode> getUserConfig(String userId, Optional<String> version) {
        String fileName = version.map(v -> userId + "_" + v + ".json")
                .orElse(userId + "_latest.json");  // Default to latest version if not provided
        Path userConfigPath = Paths.get(configPath, fileName);

        if (Files.exists(userConfigPath)) {
            try {
                String content = Files.readString(userConfigPath);
                JsonNode config = objectMapper.readTree(content);
                return Optional.of(config);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Optional.empty();
    }

    @Override
    public JsonNode saveUserConfig(String userId, JsonNode config) {
        String version = config.get("version").asText();
        String fileName = userId + "_" + version + ".json";
        Path userConfigPath = Paths.get(configPath, fileName);
        Path latestConfigPath = Paths.get(configPath, userId + "_latest.json");

        try {
            Files.createDirectories(userConfigPath.getParent());

            // Save the specific version
            Files.writeString(userConfigPath, config.toString());

            // Save or update the latest version
            Files.writeString(latestConfigPath, config.toString());

            return config;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not save user config", e);
        }
    }
}
