package io.graphoenix.spi.config;


public class GraphQLBuilderConfig {

    boolean englishPlural = false;

    public boolean isEnglishPlural() {
        return englishPlural;
    }

    public GraphQLBuilderConfig setEnglishPlural(boolean englishPlural) {
        this.englishPlural = englishPlural;
        return this;
    }
}
