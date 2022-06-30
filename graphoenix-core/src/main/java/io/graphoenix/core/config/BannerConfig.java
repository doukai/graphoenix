package io.graphoenix.core.config;

import com.typesafe.config.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperties;

@ConfigProperties(prefix = "banner")
public class BannerConfig {

    @Optional
    private String text = "GraPhoenix";

    @Optional
    private String font = "Soft";

    @Optional
    private String[] args = {"34"};

    public String getText() {
        return text;
    }

    public BannerConfig setText(String text) {
        this.text = text;
        return this;
    }

    public String getFont() {
        return font;
    }

    public BannerConfig setFont(String font) {
        this.font = font;
        return this;
    }

    public String[] getArgs() {
        return args;
    }

    public BannerConfig setArgs(String[] args) {
        this.args = args;
        return this;
    }
}