package io.graphoenix.gradle;

import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.gradle.task.GenerateGraphQLSourceTask;
import io.graphoenix.gradle.task.GenerateIntrospectionSQLTask;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class GraphoenixPlugin implements Plugin<Project> {
    private static final String GROUP_NAME = "graphoenix";

    @Override
    public void apply(Project project) {
        project.getExtensions().create(GraphQLConfig.class.getAnnotation(ConfigProperties.class).prefix(), GraphQLConfig.class);
        project.getTasks().create("generateGraphQLSource", GenerateGraphQLSourceTask.class).setGroup(GROUP_NAME);
        project.getTasks().create("generateIntrospectionSQL", GenerateIntrospectionSQLTask.class).setGroup(GROUP_NAME);
    }
}
