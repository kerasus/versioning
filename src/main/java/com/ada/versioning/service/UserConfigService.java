package com.ada.versioning.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.diff.JsonDiff;
import com.fasterxml.jackson.databind.JsonNode;
import com.ada.versioning.storage.UserConfigStorage;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Optional;

@Service
public class UserConfigService {


    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserConfigStorage userConfigStorage;

    @Autowired
    public UserConfigService(@Qualifier("fileSystemStorage") UserConfigStorage userConfigStorage) {
        this.userConfigStorage = userConfigStorage;
    }

    /**
     * Retrieves the user config for the given userId and optional version.
     * If no version is provided, it returns the latest config.
     *
     * @param userId  the user identifier
     * @param version the optional version of the config
     * @return an Optional containing the user config if found, empty otherwise
     */
    public Optional<JsonNode> getUserConfig(String userId, Optional<String> version) {
        return userConfigStorage.getUserConfig(userId, version);
    }

    /**
     * Saves the user config for the given userId.
     *
     * @param userId  the user identifier
     * @param config  the config to save
     * @return the saved config
     */
    public JsonNode saveUserConfig(String userId, JsonNode config) {
        return userConfigStorage.saveUserConfig(userId, config);
    }

    /**
     * Retrieves the diff between the current config version and the latest config version.
     *
     * @param userId         the user identifier
     * @param currentVersion  the current version of the config
     * @return an Optional containing the diff if found, empty otherwise
     */
    public Optional<JsonNode> getLatestConfigDiff(String userId, String currentVersion) {
        // Fetch current config based on userId and currentVersion
        Optional<JsonNode> currentConfigOpt = getUserConfig(userId, Optional.of(currentVersion));
        if (currentConfigOpt.isEmpty()) {
            return Optional.empty();
        }

        JsonNode currentConfig = currentConfigOpt.get();

        // Fetch the latest config (no version means latest)
        Optional<JsonNode> latestConfigOpt = getUserConfig(userId, Optional.empty());
        if (latestConfigOpt.isEmpty()) {
            return Optional.empty();
        }

        JsonNode latestConfig = latestConfigOpt.get();
        String latestVersion = latestConfig.get("version").asText();

        // If the versions are the same, return an empty diff
        if (latestVersion.equals(currentVersion)) {
            return Optional.of(objectMapper.createObjectNode()); // Return empty diff
        }

        // Compute the diff between current and latest config
        JsonNode diff = computeDiff(currentConfig, latestConfig);
        return Optional.of(diff);
    }

    /**
     * Compute the difference between two JSON nodes.
     *
     * @param currentConfig the current config
     * @param latestConfig  the latest config
     * @return a JsonNode representing the difference
     */
    private JsonNode computeDiff(JsonNode currentConfig, JsonNode latestConfig) {
        try {
            // Compute the diff using JsonPatch
            JsonNode patch = JsonDiff.asJson(currentConfig, latestConfig);
            return patch;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to compute JSON diff", e);
        }
    }
}
