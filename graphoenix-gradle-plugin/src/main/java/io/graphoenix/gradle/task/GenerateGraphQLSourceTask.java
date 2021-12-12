package io.graphoenix.gradle.task;

import io.graphoenix.graphql.builder.schema.DaggerDocumentBuilderFactory;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.java.generator.builder.DaggerJavaFileBuilderFactory;
import io.graphoenix.java.generator.builder.JavaFileBuilder;
import io.graphoenix.spi.config.JavaGeneratorConfig;
import org.gradle.api.DefaultTask;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

import java.io.*;
import java.nio.file.Path;

public class GenerateGraphQLSourceTask extends DefaultTask {

    @TaskAction
    public void generateGraphQLSource() {
        JavaGeneratorConfig javaGeneratorConfig = getProject().getExtensions().findByType(JavaGeneratorConfig.class);
        assert javaGeneratorConfig != null;
        JavaFileBuilder javaFileBuilder = DaggerJavaFileBuilderFactory.create().get();
        DocumentBuilder documentBuilder = DaggerDocumentBuilderFactory.create().get();
        SourceSet sourceSet = getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);

        try {
            if (javaGeneratorConfig.getGraphQL() != null) {
                documentBuilder.registerGraphQL(javaGeneratorConfig.getGraphQL());
            } else if (javaGeneratorConfig.getGraphQLFileName() != null) {
                File graphQLFile = sourceSet.getResources().getFiles().stream()
                        .filter(file -> file.getName().equals(javaGeneratorConfig.getGraphQLFileName()))
                        .findFirst()
                        .orElseThrow();
                documentBuilder.registerFile(graphQLFile);
            } else if (javaGeneratorConfig.getGraphQLPath() != null) {
                Path graphQLPath = sourceSet.getResources().getSrcDirs().stream()
                        .findFirst()
                        .map(file -> Path.of(file.getPath() + File.pathSeparator + javaGeneratorConfig.getGraphQLPath()))
                        .orElseThrow();
                documentBuilder.registerPath(graphQLPath);
            }
            documentBuilder.buildManager();
            sourceSet.getJava().getSrcDirs().stream()
                    .findFirst()
                    .ifPresent(javaFileBuilder::writeToPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
