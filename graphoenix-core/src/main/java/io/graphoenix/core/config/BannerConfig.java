package io.graphoenix.core.config;

import com.typesafe.config.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperties;

@ConfigProperties(prefix = "banner")
public class BannerConfig {

    @Optional
    private String text = "GraPhoenix";

    @Optional
    private String[] lines = {"Database : ${Graphoenix-Database}", "Protocol : ${Graphoenix-Protocol}", "Powered by Graphoenix ${Graphoenix-Version}"};

    @Optional
    private String font = "Soft";

    @Optional
    private String[] args = {"34"};

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String[] getLines() {
        return lines;
    }

    public void setLines(String[] lines) {
        this.lines = lines;
    }

    public String getFont() {
        return font;
    }

    public void setFont(String font) {
        this.font = font;
    }

    public String[] getArgs() {
        return args;
    }

    public void setArgs(String[] args) {
        this.args = args;
    }
}