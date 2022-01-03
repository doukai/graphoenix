package io.graphoenix.gradle.task;

import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.java.generator.builder.JavaFileBuilder;
import io.graphoenix.java.generator.config.JavaGeneratorConfig;
import org.gradle.api.tasks.SourceSet;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class GraphQLSourceGenerator {

    private final DocumentBuilder documentBuilder;

    private final JavaFileBuilder javaFileBuilder;

    private final JavaGeneratorConfig javaGeneratorConfig;

    @Inject
    public GraphQLSourceGenerator(DocumentBuilder documentBuilder, JavaFileBuilder javaFileBuilder, JavaGeneratorConfig javaGeneratorConfig) {
        this.documentBuilder = documentBuilder;
        this.javaFileBuilder = javaFileBuilder;
        this.javaGeneratorConfig = javaGeneratorConfig;
    }

    public void generate(SourceSet sourceSet) {

//        try {
//            if (javaGeneratorConfig.getGraphQL() != null) {
//                documentBuilder.registerGraphQL(javaGeneratorConfig.getGraphQL());
//            } else if (javaGeneratorConfig.getGraphQLFileName() != null) {
//                File graphQLFile = sourceSet.getResources().getFiles().stream()
//                        .filter(file -> file.getName().equals(javaGeneratorConfig.getGraphQLFileName()))
//                        .findFirst()
//                        .orElseThrow();
//                documentBuilder.registerFile(graphQLFile);
//            } else if (javaGeneratorConfig.getGraphQLPath() != null) {
//                Path graphQLPath = sourceSet.getResources().getSrcDirs().stream()
//                        .findFirst()
//                        .map(file -> Path.of(file.getPath() + File.pathSeparator + javaGeneratorConfig.getGraphQLPath()))
//                        .orElseThrow();
//                documentBuilder.registerPath(graphQLPath);
//            }
//            documentBuilder.buildManager();
//            sourceSet.getJava().getSrcDirs().stream()
//                    .findFirst()
//                    .ifPresent(javaFileBuilder::writeToPath);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

}
