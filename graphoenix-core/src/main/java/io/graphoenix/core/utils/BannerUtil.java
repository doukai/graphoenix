package io.graphoenix.core.utils;

import com.jcabi.manifests.Manifests;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public enum BannerUtil {
    BANNER_UTIL;

    public static final String BANNER_FILE_NAME = "banner.txt";
    public static final String VERSION_KEY = "Graphoenix-Version";
    public static final String DATABASE_KEY = "Graphoenix-Database";
    public static final String PROTOCOL_KEY = "Graphoenix-Protocol";

    public Optional<String> getBanner() {
        try {
            if (this.getClass().getClassLoader().getResource(BANNER_FILE_NAME) != null) {
                String banner = Files.readString(Path.of(Objects.requireNonNull(this.getClass().getClassLoader().getResource(BANNER_FILE_NAME)).toURI()), StandardCharsets.US_ASCII);
                if (banner != null) {
                    if (Manifests.exists(VERSION_KEY)) {
                        banner = banner.replace("${" + VERSION_KEY + "}", Manifests.read(VERSION_KEY));
                    }
                    if (Manifests.exists(DATABASE_KEY)) {
                        banner = banner.replace("${" + DATABASE_KEY + "}", Manifests.read(DATABASE_KEY));
                    }
                    if (Manifests.exists(PROTOCOL_KEY)) {
                        banner = banner.replace("${" + PROTOCOL_KEY + "}", Manifests.read(PROTOCOL_KEY));
                    }
                    return Optional.of(banner);
                }
            }
        } catch (IOException | URISyntaxException ignored) {
        }
        return Optional.empty();
    }
}
