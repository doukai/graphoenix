package io.graphoenix.gradle;

import io.graphoenix.gradle.task.GenerateGraphQLSourceTask;
import io.graphoenix.spi.config.JavaGeneratorConfig;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class GraphoenixPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {

        ConfigProperties configProperties = JavaGeneratorConfig.class.getAnnotation(ConfigProperties.class);
        project.getExtensions().create(configProperties.prefix(), JavaGeneratorConfig.class);
        project.getTasks().create("generateGraphQLSource", GenerateGraphQLSourceTask.class);
    }
}
