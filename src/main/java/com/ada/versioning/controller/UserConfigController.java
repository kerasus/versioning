package com.ada.versioning.controller;

import com.ada.versioning.service.UserConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;

import java.util.Optional;

@RestController
@RequestMapping("/api/config")
public class UserConfigController {

    private final UserConfigService userConfigService;

    @Autowired
    public UserConfigController(UserConfigService userConfigService) {
        this.userConfigService = userConfigService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<JsonNode> getUserConfigDiff(
            @PathVariable String userId,
            @RequestParam String currentVersion) {

        Optional<JsonNode> diff = userConfigService.getLatestConfigDiff(userId, currentVersion);

        if (diff.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        return ResponseEntity.ok(diff.get());
    }

    @PostMapping("/{userId}")
    public ResponseEntity<JsonNode> saveUserConfig(
            @PathVariable String userId,
            @RequestParam String version,
            @RequestBody JsonNode config) {

        // Combine version into config JSON (or store it separately)
        ((ObjectNode) config).put("version", version);

        JsonNode savedConfig = userConfigService.saveUserConfig(userId, config);
        return ResponseEntity.ok(savedConfig);
    }
}
