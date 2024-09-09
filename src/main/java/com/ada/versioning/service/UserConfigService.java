package com.ada.versioning.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ada.versioning.storage.UserConfigStorage;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Optional;

@Service
public class UserConfigService {

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
     * @param currentVersion the current version of the config
     * @return an Optional containing the diff if found, empty otherwise
     */
    public Optional<JsonNode> getLatestConfigDiff(String userId, String currentVersion) {
        return userConfigStorage.getLatestConfigDiff(userId, currentVersion);
    }
}
