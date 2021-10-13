package io.graphoenix.graphql.builder.config;

public class BuilderConfiguration {

    boolean englishPlural = false;

    public boolean isEnglishPlural() {
        return englishPlural;
    }

    public BuilderConfiguration setEnglishPlural(boolean englishPlural) {
        this.englishPlural = englishPlural;
        return this;
    }
}
