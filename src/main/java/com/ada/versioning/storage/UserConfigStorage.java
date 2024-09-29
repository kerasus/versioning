package com.ada.versioning.storage;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

public interface UserConfigStorage {

    /**
     * Retrieves the user config for the given userId and version.
     * If no version is provided, it retrieves the latest config.
     *
     * @param userId  the user identifier
     * @param version the optional version of the config to retrieve
     * @return an Optional containing the user config if found, empty otherwise
     */
    Optional<JsonNode> getUserConfig(String userId, Optional<String> version);

    /**
     * Saves the user config for a given userId and version.
     *
     * @param userId  the user identifier
     * @param config  the user config to save
     * @return the saved config
     */
    JsonNode saveUserConfig(String userId, JsonNode config);
}
