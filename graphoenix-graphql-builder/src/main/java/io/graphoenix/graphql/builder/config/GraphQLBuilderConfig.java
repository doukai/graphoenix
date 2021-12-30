package io.graphoenix.graphql.builder.config;

import org.eclipse.microprofile.config.inject.ConfigProperties;

@ConfigProperties(prefix = "builder")
public class GraphQLBuilderConfig {

    private Boolean englishPlural = false;

    public GraphQLBuilderConfig(Boolean englishPlural) {
        this.englishPlural = englishPlural;
    }

    public Boolean getEnglishPlural() {
        return englishPlural;
    }

    public void setEnglishPlural(Boolean englishPlural) {
        this.englishPlural = englishPlural;
    }
}
