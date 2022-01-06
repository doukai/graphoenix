package io.graphoenix.gradle;

import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.gradle.task.GenerateGraphQLSourceTask;
import io.graphoenix.java.generator.config.JavaGeneratorConfig;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class GraphoenixPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getExtensions().create(GraphQLConfig.class.getAnnotation(ConfigProperties.class).prefix(), GraphQLConfig.class);
        project.getExtensions().create(JavaGeneratorConfig.class.getAnnotation(ConfigProperties.class).prefix(), JavaGeneratorConfig.class);
        project.getTasks().create("generateGraphQLSource", GenerateGraphQLSourceTask.class);
    }
}
