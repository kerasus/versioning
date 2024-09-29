package com.ada.versioning.storage.impl;

import com.ada.versioning.storage.UserConfigStorage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.lib.ObjectLoader;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Service("gitStorage")
public class GitUserConfigStorage implements UserConfigStorage {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Git git;

    @Value("${config.git.path}")
    private String configPath;

    @PostConstruct
    public void init() {
        try {
            File repoDir = new File(configPath);
            if (!repoDir.exists()) {
                repoDir.mkdirs(); // Create directory if it does not exist
            }
            // Initialize Git repository
            Repository repository = new FileRepositoryBuilder()
                    .setGitDir(new File(repoDir, ".git"))
                    .build();
            git = new Git(repository);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not initialize Git repository", e);
        }
    }

    @Override
    public Optional<JsonNode> getUserConfig(String userId, Optional<String> version) {
        Path userConfigPath = Paths.get(configPath, userId + "_config.json");

        try {
            // If a specific version is requested
            if (version.isPresent()) {
                String requestedVersion = version.get();
                ObjectId versionCommitId = git.getRepository().resolve(requestedVersion);
                if (versionCommitId == null) {
                    return Optional.empty();  // Version not found
                }

                Iterable<RevCommit> commits = git.log().add(versionCommitId).call();
                RevCommit commit = commits.iterator().next();

                if (commit != null) {
                    RevTree tree = commit.getTree();

                    try (TreeWalk treeWalk = new TreeWalk(git.getRepository())) {
                        treeWalk.addTree(tree);
                        treeWalk.setRecursive(true);
                        treeWalk.setFilter(PathFilter.create(userConfigPath.toString()));

                        if (!treeWalk.next()) {
                            return Optional.empty();  // File not found at that version
                        }

                        ObjectLoader loader = git.getRepository().open(treeWalk.getObjectId(0));
                        String content = new String(loader.getBytes());
                        JsonNode config = objectMapper.readTree(content);
                        return Optional.of(config);
                    }
                }
            }

            // If no version is specified, read the latest config from the file system
            if (Files.exists(userConfigPath)) {
                String content = Files.readString(userConfigPath);
                JsonNode config = objectMapper.readTree(content);
                return Optional.of(config);
            }
        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not retrieve user config", e);
        }

        return Optional.empty();
    }

    @Override
    public JsonNode saveUserConfig(String userId, JsonNode config) {
        Path userConfigPath = Paths.get(configPath, userId + "_config.json");

        try {
            Files.createDirectories(userConfigPath.getParent());
            Files.writeString(userConfigPath, config.toString());

            // Commit changes to Git
            git.add().addFilepattern(userId + "_config.json").call();
            String version = config.get("version").asText(); // Read version from config
            git.commit().setMessage("Save user config version: " + version).call();

            return config;
        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not save user config", e);
        }
    }
}
