package io.graphoenix.gradle;

import io.graphoenix.gradle.config.CodegenConfiguration;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaLibraryPlugin;

public class GraphoenixPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(JavaLibraryPlugin.class);
        CodegenConfiguration codegenConfiguration = project.getExtensions()
                .create("graphQLCodegen", CodegenConfiguration.class);

        project.task("codegen")
                .doLast(task -> System.out.println("Hello Gradle!"));



    }
}
