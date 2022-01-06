package io.graphoenix.core.config;

import com.typesafe.config.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperties;

@ConfigProperties(prefix = "graphql")
public class GraphQLConfig {

    @Optional
    private String graphQL;

    @Optional
    private String graphQLFileName;

    @Optional
    private String graphQLPath;

    @Optional
    private Boolean build = true;

    @Optional
    private Boolean englishPlural = false;

    public String getGraphQL() {
        return graphQL;
    }

    public void setGraphQL(String graphQL) {
        this.graphQL = graphQL;
    }

    public String getGraphQLFileName() {
        return graphQLFileName;
    }

    public void setGraphQLFileName(String graphQLFileName) {
        this.graphQLFileName = graphQLFileName;
    }

    public String getGraphQLPath() {
        return graphQLPath;
    }

    public void setGraphQLPath(String graphQLPath) {
        this.graphQLPath = graphQLPath;
    }

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
