package io.graphoenix.graphql.builder.config;

import com.typesafe.config.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperties;

@ConfigProperties(prefix = "builder")
public class GraphQLBuilderConfig {

    @Optional
    private Boolean build = true;

    @Optional
    private Boolean englishPlural = false;

    public Boolean getBuild() {
        return build;
    }

    public void setBuild(Boolean build) {
        this.build = build;
    }

    public Boolean getEnglishPlural() {
        return englishPlural;
    }

    public void setEnglishPlural(Boolean englishPlural) {
        this.englishPlural = englishPlural;
    }
}
