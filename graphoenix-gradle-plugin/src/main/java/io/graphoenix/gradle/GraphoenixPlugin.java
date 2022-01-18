package io.graphoenix.gradle;

import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.gradle.task.GenerateGraphQLSourceTask;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class GraphoenixPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getExtensions().create(GraphQLConfig.class.getAnnotation(ConfigProperties.class).prefix(), GraphQLConfig.class);
        project.getTasks().create("generateGraphQLSource", GenerateGraphQLSourceTask.class);
    }
}
