package io.graphoenix.gradle;

import io.graphoenix.gradle.task.GenerateGraphQLSourceTask;
import io.graphoenix.spi.config.JavaGeneratorConfig;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class GraphoenixPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getExtensions().create("javaCodegen", JavaGeneratorConfig.class);
        project.getTasks().create("generateGraphQLSource", GenerateGraphQLSourceTask.class);
    }
}
